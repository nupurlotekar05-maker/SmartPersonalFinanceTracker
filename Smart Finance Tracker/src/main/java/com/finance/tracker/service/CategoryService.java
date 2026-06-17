package com.finance.tracker.service;

import com.finance.tracker.dto.request.CategoryRequest;
import com.finance.tracker.dto.response.CategoryResponse;
import com.finance.tracker.entity.Category;
import com.finance.tracker.entity.Category.CategoryType;
import com.finance.tracker.entity.Transaction.TransactionType;
import com.finance.tracker.entity.User;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;

    // Keyword → default category name mapping for auto-categorization
    private static final Map<String, String> KEYWORD_MAP = new LinkedHashMap<>() {
        {
            // EXPENSE
            put("pizza", "Food");
            put("burger", "Food");
            put("restaurant", "Food");
            put("food", "Food");
            put("swiggy", "Food");
            put("zomato", "Food");
            
            put("amazon", "Shopping");
            put("flipkart", "Shopping");
            put("myntra", "Shopping");
            put("shopping", "Shopping");
            
            put("cricket", "Sports");
            put("bat", "Sports");
            put("ball", "Sports");
            put("gym", "Sports");
            put("fitness", "Sports");
            put("sports", "Sports");
            
            put("uber", "Transport");
            put("ola", "Transport");
            put("rapido", "Transport");
            put("ride", "Transport");
            put("petrol", "Transport");
            put("fuel", "Transport");
            put("transport", "Transport");
            
            put("electricity", "Bills");
            put("water", "Bills");
            put("gas", "Bills");
            put("internet", "Bills");
            put("wifi", "Bills");
            put("bill", "Bills");
            put("rent", "Bills");
            
            put("pharmacy", "Health");
            put("medicine", "Health");
            put("medical", "Health");
            put("hospital", "Health");
            put("doctor", "Health");
            put("health", "Health");
            
            put("movie", "Entertainment");
            put("cinema", "Entertainment");
            put("netflix", "Entertainment");
            put("spotify", "Entertainment");
            put("game", "Entertainment");
            put("entertainment", "Entertainment");
            
            put("flight", "Travel");
            put("hotel", "Travel");
            put("trip", "Travel");
            put("holiday", "Travel");
            put("travel", "Travel");
            
            put("school", "Education");
            put("college", "Education");
            put("book", "Education");
            put("course", "Education");
            put("education", "Education");

            // INCOME
            put("salary", "Salary");
            put("payroll", "Salary");
            put("credited", "Salary");
            put("freelance", "Freelance");
            put("gig", "Freelance");
            put("upwork", "Freelance");
            put("business", "Business");
            put("profit", "Business");
            put("stock", "Investment");
            put("dividend", "Investment");
            put("investment", "Investment");
            put("bonus", "Bonus");
            put("gift", "Gift");
        }
    };

    public CategoryResponse create(CategoryRequest request) {
        User user = securityUtils.getCurrentUser();
        log.debug("Creating category for userId={} name={}", user.getId(), request.getName());

        Category category = Category.builder()
                .user(user)
                .name(request.getName())
                .categoryType(request.getCategoryType())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .isDefault(false)
                .status(Category.CategoryStatus.ACTIVE)
                .build();

        return toResponse(categoryRepository.save(category));
    }

    public List<CategoryResponse> getAllForCurrentUser() {
        return categoryRepository.findByUserIdOrDefault(securityUtils.getCurrentUserId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<CategoryResponse> getExpenseCategories() {
        Long userId = securityUtils.getCurrentUserId();
        List<Category> categories = categoryRepository.findByUserIdOrDefaultAndCategoryTypeIn(
                userId,
                Arrays.asList(CategoryType.EXPENSE, CategoryType.BOTH));
        log.info("Found {} expense categories for userId={}", categories.size(), userId);
        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<CategoryResponse> getIncomeCategories() {
        Long userId = securityUtils.getCurrentUserId();
        List<Category> categories = categoryRepository.findByUserIdOrDefaultAndCategoryTypeIn(
                userId, 
                Arrays.asList(CategoryType.INCOME, CategoryType.BOTH));
        log.info("Found {} income categories for userId={}", categories.size(), userId);
        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<CategoryResponse> getCategoriesByType(Category.CategoryType type) {
        Long userId = securityUtils.getCurrentUserId();
        List<CategoryType> types = new ArrayList<>();
        types.add(type);
        if (type != CategoryType.BOTH) {
            types.add(CategoryType.BOTH);
        }

        List<Category> categories = categoryRepository.findByUserIdOrDefaultAndCategoryTypeIn(userId, types);
        log.info("Found {} categories for type={} and userId={}", categories.size(), type, userId);
        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void delete(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or unauthorized"));

        if (Boolean.TRUE.equals(category.getIsDefault())) {
            throw new BadRequestException("Cannot delete default category");
        }
        categoryRepository.delete(category);
    }

    public Category autoDetectCategory(String description, Long userId, TransactionType type) {
        if (description == null || description.isBlank()) {
            return getOrCreateDefault("Others", type, userId);
        }

        String lower = description.toLowerCase();

        // Step 1: Learn from user's own past transactions
        List<Object[]> pastCategories = transactionRepository.findCategoryByKeyword(userId, lower);
        if (!pastCategories.isEmpty()) {
            String learnedCategoryName = (String) pastCategories.get(0)[0];
            Optional<Category> learned = categoryRepository.findByNameIgnoreCaseAndUserId(learnedCategoryName, userId);
            if (learned.isPresent())
                return learned.get();
        }

        // Step 2: Keyword map matching
        for (Map.Entry<String, String> entry : KEYWORD_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return getOrCreateDefault(entry.getValue(), type, userId);
            }
        }

        // Step 3: Fallback
        return getOrCreateDefault("Others", type, userId);
    }

    private Category getOrCreateDefault(String name, TransactionType type, Long userId) {
        Optional<Category> global = categoryRepository.findByNameIgnoreCaseAndUserIdIsNull(name);
        if (global.isPresent())
            return global.get();

        Optional<Category> userCat = categoryRepository.findByNameIgnoreCaseAndUserId(name, userId);
        if (userCat.isPresent())
            return userCat.get();

        // Map TransactionType to CategoryType for default creation
        CategoryType catType = (type == TransactionType.INCOME) ? CategoryType.INCOME : CategoryType.EXPENSE;

        Category newCat = Category.builder()
                .name(name)
                .categoryType(catType)
                .isDefault(true)
                .status(Category.CategoryStatus.ACTIVE)
                .build();

        return categoryRepository.save(newCat);
    }

    public CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .categoryType(c.getCategoryType())
                .isDefault(c.getIsDefault())
                .userId(c.getUser() != null ? c.getUser().getId() : null)
                .description(c.getDescription())
                .icon(c.getIcon())
                .color(c.getColor())
                .createdByAdmin(c.getCreatedByAdmin())
                .status(c.getStatus() != null ? c.getStatus().name() : "ACTIVE")
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
