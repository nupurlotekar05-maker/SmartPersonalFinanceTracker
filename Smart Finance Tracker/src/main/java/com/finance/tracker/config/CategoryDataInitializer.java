package com.finance.tracker.config;

import com.finance.tracker.entity.Category;
import com.finance.tracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // Run after DatabaseMigrationRunner
public class CategoryDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        long globalCount = categoryRepository.findByUserIdIsNull().size();
        boolean hasFood = categoryRepository.findByNameIgnoreCaseAndUserIdIsNull("Food").isPresent();
        
        if (globalCount < 5 || !hasFood) {
            log.info("Global categories are missing or incomplete (count={}). Initializing defaults...", globalCount);
            initializeDefaults();
        } else {
            log.info("Global categories already initialized (count={}).", globalCount);
        }
    }

    private void initializeDefaults() {
        List<Category> defaults = Arrays.asList(
            // Expenses
            createCat("Food", Category.CategoryType.EXPENSE, "Groceries, Restaurants, Snacks", "Utensils", "#ef4444"),
            createCat("Shopping", Category.CategoryType.EXPENSE, "Clothes, Electronics, Home Decor", "ShoppingBag", "#3b82f6"),
            createCat("Transport", Category.CategoryType.EXPENSE, "Fuel, Public Transport, Ride Sharing", "Car", "#8b5cf6"),
            createCat("Bills", Category.CategoryType.EXPENSE, "Electricity, Water, Internet, Phone", "FileText", "#f59e0b"),
            createCat("Sports", Category.CategoryType.EXPENSE, "Gym, Fitness, Sports equipment", "Dumbbell", "#10b981"),
            createCat("Entertainment", Category.CategoryType.EXPENSE, "Movies, Gaming, Streaming Services", "Film", "#ec4899"),
            createCat("Health", Category.CategoryType.EXPENSE, "Medicines, Hospital Bills, Checkups", "HeartPulse", "#ef4444"),
            createCat("Travel", Category.CategoryType.EXPENSE, "Flights, Hotels, Sightseeing", "Plane", "#06b6d4"),
            createCat("Education", Category.CategoryType.EXPENSE, "School, College, Courses, Books", "BookOpen", "#6366f1"),
            createCat("Others", Category.CategoryType.EXPENSE, "Miscellaneous expenses", "MoreHorizontal", "#94a3b8"),

            // Income
            createCat("Salary", Category.CategoryType.INCOME, "Monthly salary", "Wallet", "#10b981"),
            createCat("Freelance", Category.CategoryType.INCOME, "Gigs and side projects", "Briefcase", "#3b82f6"),
            createCat("Business", Category.CategoryType.INCOME, "Business profits", "TrendingUp", "#8b5cf6"),
            createCat("Investment", Category.CategoryType.INCOME, "Stocks, Dividends, Interest", "PieChart", "#f59e0b"),
            createCat("Bonus", Category.CategoryType.INCOME, "Performance bonuses", "Award", "#ec4899"),
            createCat("Gift", Category.CategoryType.INCOME, "Gifts from others", "Gift", "#f43f5e"),
            createCat("Other Income", Category.CategoryType.INCOME, "Miscellaneous income", "PlusCircle", "#94a3b8")
        );

        categoryRepository.saveAll(defaults);
        log.info("Successfully initialized {} default categories.", defaults.size());
    }

    private Category createCat(String name, Category.CategoryType type, String desc, String icon, String color) {
        return Category.builder()
                .name(name)
                .categoryType(type)
                .description(desc)
                .icon(icon)
                .color(color)
                .isDefault(true)
                .createdByAdmin(true)
                .status(Category.CategoryStatus.ACTIVE)
                .user(null) // Global category
                .build();
    }
}
