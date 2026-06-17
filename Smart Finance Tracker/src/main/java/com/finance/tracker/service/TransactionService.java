package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.entity.*;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.repository.*;
import com.finance.tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final SecurityUtils securityUtils;
    private final OcrService ocrService;   // FIX: OCR service injected
    private final BudgetService budgetService;

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        User user = securityUtils.getCurrentUser();
        log.info("[TX CREATE] userId={} accountId={} categoryId={} amount={} type={} date={}",
                user.getId(), request.getAccountId(), request.getCategoryId(),
                request.getAmount(), request.getType(), request.getTransactionDate());

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), user.getId())
                .orElseThrow(() -> {
                    log.error("[TX CREATE] Account not found: accountId={} userId={}", request.getAccountId(), user.getId());
                    return new ResourceNotFoundException("Account not found with id: " + request.getAccountId());
                });
        log.debug("[TX CREATE] Account resolved: id={} name={}", account.getId(), account.getAccountName());

        // Auto-categorize if categoryId not provided
        Category category;
        if (request.getCategoryId() != null) {
            Long userId = user.getId();
            category = categoryRepository.findByIdAndUserIdOrDefault(request.getCategoryId(), userId)
                    .orElseThrow(() -> {
                        log.error("[TX CREATE] Category not found or unauthorized: categoryId={} userId={}", request.getCategoryId(), userId);
                        return new ResourceNotFoundException("Category not found with id: " + request.getCategoryId());
                    });
            log.debug("[TX CREATE] Category resolved: id={} name={}", category.getId(), category.getName());
        } else {
            log.debug("[TX CREATE] No categoryId provided, auto-detecting from description='{}'", request.getDescription());
            category = categoryService.autoDetectCategory(request.getDescription(), user.getId(), request.getType());
            log.debug("[TX CREATE] Auto-detected category: id={} name={}", category.getId(), category.getName());
        }

        Transaction tx = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .paymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : Transaction.PaymentStatus.PAID)
                .source(request.getSource() != null ? request.getSource() : Transaction.TransactionSource.MANUAL)
                .notes(request.getNotes())
                .build();

        // Update account balance
        if (request.getType() == Transaction.TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(request.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(request.getAmount()));
        }

        accountRepository.save(account);
        Transaction saved = transactionRepository.save(tx);
        log.info("[TX CREATE] Saved successfully: txId={} userId={} amount={} type={}",
                saved.getId(), user.getId(), saved.getAmount(), saved.getType());

        // Check budget if it's an expense
        if (saved.getType() == Transaction.TransactionType.EXPENSE && saved.getCategory() != null) {
            budgetService.checkBudget(user, saved.getCategory(), 
                                    saved.getTransactionDate().getMonthValue(), 
                                    saved.getTransactionDate().getYear());
        }

        return toResponse(saved);
    }

    public List<TransactionResponse> getAll() {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(securityUtils.getCurrentUserId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getByDateRange(LocalDate start, LocalDate end) {
        Long userId = securityUtils.getCurrentUserId();
        return transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        userId, start, end)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TransactionResponse getById(Long id) {
        return toResponse(findOwnedTx(id));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction tx = findOwnedTx(id);
        User user = securityUtils.getCurrentUser();

        // Reverse old balance effect
        reverseBalance(tx);

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Category category;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        } else {
            category = categoryService.autoDetectCategory(request.getDescription(), user.getId(), request.getType());
        }

        tx.setAccount(account);
        tx.setCategory(category);
        tx.setAmount(request.getAmount());
        tx.setType(request.getType());
        tx.setDescription(request.getDescription());
        tx.setTransactionDate(request.getTransactionDate());
        tx.setPaymentStatus(request.getPaymentStatus());
        tx.setSource(request.getSource());
        tx.setNotes(request.getNotes());

        // Apply new balance effect
        if (request.getType() == Transaction.TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(request.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(request.getAmount()));
        }

        accountRepository.save(account);
        Transaction updated = transactionRepository.save(tx);

        // Check budget if it's an expense
        if (updated.getType() == Transaction.TransactionType.EXPENSE && updated.getCategory() != null) {
            budgetService.checkBudget(user, updated.getCategory(), 
                                    updated.getTransactionDate().getMonthValue(), 
                                    updated.getTransactionDate().getYear());
        }

        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Transaction tx = findOwnedTx(id);
        reverseBalance(tx);
        transactionRepository.delete(tx);
    }

    @Transactional
    public TransactionResponse uploadReceipt(Long id, MultipartFile file) throws IOException {
        Transaction tx = findOwnedTx(id);

        // Guard against null original filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Uploaded file must have a valid filename");
        }

        // Sanitize filename to prevent path traversal attacks
        String sanitizedFilename = Paths.get(originalFilename).getFileName().toString();
        sanitizedFilename = sanitizedFilename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        String filename = UUID.randomUUID() + "_" + sanitizedFilename;
        Path uploadPath = Paths.get("uploads/receipts");
        Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        tx.setReceiptImageUrl("uploads/receipts/" + filename);
        tx.setSource(Transaction.TransactionSource.RECEIPT);

        // ===================================================
        // FIX: Run OCR on the uploaded receipt image
        // Previously: only saved the file, NO text extraction
        // Now: extracts amount, date, merchant from receipt
        // ===================================================
        try {
            OcrService.OcrResult ocrResult = ocrService.extractFromReceipt(file);
            if (ocrResult.isSuccess()) {
                // Auto-fill amount if transaction amount is default/zero
                if (ocrResult.getAmount() != null
                        && ocrResult.getAmount().compareTo(BigDecimal.ZERO) > 0
                        && (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) == 0)) {
                    tx.setAmount(ocrResult.getAmount());
                }
                // Auto-fill description if empty and merchant was found
                if ((tx.getDescription() == null || tx.getDescription().isBlank())
                        && ocrResult.getMerchantName() != null) {
                    tx.setDescription(ocrResult.getMerchantName());
                }
                // Always append OCR notes so user can see what was read
                String existingNotes = tx.getNotes() != null ? tx.getNotes() + "\n\n" : "";
                tx.setNotes(existingNotes + ocrResult.getNotes());
            }
        } catch (Exception e) {
            // OCR failure must NOT block the receipt save — just log it
            // The file is still saved; user can manually fill fields
            tx.setNotes((tx.getNotes() != null ? tx.getNotes() + "\n" : "")
                    + "[OCR Warning] Could not extract text from image: " + e.getMessage()
                    + "\nTip: Ensure Tesseract is installed and tessdata path is correct in application.properties.");
        }

        return toResponse(transactionRepository.save(tx));
    }

    private void reverseBalance(Transaction tx) {
        Account account = tx.getAccount();
        if (tx.getType() == Transaction.TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(tx.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(tx.getAmount()));
        }
        accountRepository.save(account);
    }

    private Transaction findOwnedTx(Long id) {
        return transactionRepository.findByIdAndUserId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    public TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .accountId(t.getAccount().getId())
                .accountName(t.getAccount().getAccountName())
                .categoryId(t.getCategory() != null ? t.getCategory().getId() : null)
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : "Uncategorized")
                .categoryIcon(t.getCategory() != null ? t.getCategory().getIcon() : "MoreHorizontal")
                .categoryColor(t.getCategory() != null ? t.getCategory().getColor() : "#94a3b8")
                .amount(t.getAmount())
                .type(t.getType())
                .description(t.getDescription())
                .transactionDate(t.getTransactionDate())
                .paymentStatus(t.getPaymentStatus())
                .source(t.getSource())
                .notes(t.getNotes())
                .receiptImageUrl(t.getReceiptImageUrl())
                .createdAt(t.getCreatedAt())
                .build();
    }
}