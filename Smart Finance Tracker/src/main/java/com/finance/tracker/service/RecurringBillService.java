package com.finance.tracker.service;

import com.finance.tracker.dto.request.RecurringBillRequest;
import com.finance.tracker.dto.response.RecurringBillResponse;
import com.finance.tracker.entity.*;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.repository.*;
import com.finance.tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringBillService {

    private final RecurringBillRepository billRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public RecurringBillResponse create(RecurringBillRequest request) {
        User user = securityUtils.getCurrentUser();
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        RecurringBill bill = RecurringBill.builder()
                .user(user)
                .account(account)
                .billName(request.getBillName())
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .nextDueDate(request.getNextDueDate())
                .isActive(request.getIsActive())
                .build();

        return toResponse(billRepository.save(bill));
    }

    public List<RecurringBillResponse> getAll() {
        return billRepository.findByUserId(securityUtils.getCurrentUserId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<RecurringBillResponse> getActive() {
        return billRepository.findByUserIdAndIsActive(securityUtils.getCurrentUserId(), true)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RecurringBillResponse update(Long id, RecurringBillRequest request) {
        RecurringBill bill = findOwned(id);
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), securityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        bill.setAccount(account);
        bill.setBillName(request.getBillName());
        bill.setAmount(request.getAmount());
        bill.setFrequency(request.getFrequency());
        bill.setNextDueDate(request.getNextDueDate());
        bill.setIsActive(request.getIsActive());
        return toResponse(billRepository.save(bill));
    }

    public void delete(Long id) {
        billRepository.delete(findOwned(id));
    }

    // ============================================================
    // SCHEDULER: Runs every day at 8 AM - sends bill reminders
    // ============================================================
    @Scheduled(cron = "0 0 8 * * *")
    public void sendBillReminders() {
        LocalDate today = LocalDate.now();
        LocalDate inThreeDays = today.plusDays(3);

        List<RecurringBill> dueBills = billRepository.findByIsActiveTrueAndNextDueDateLessThanEqual(inThreeDays);

        for (RecurringBill bill : dueBills) {
            long daysUntil = ChronoUnit.DAYS.between(today, bill.getNextDueDate());
            String msg;
            if (daysUntil < 0) {
                msg = "OVERDUE: " + bill.getBillName() + " was due " + Math.abs(daysUntil) + " day(s) ago. Amount: ₹" + bill.getAmount();
            } else if (daysUntil == 0) {
                msg = "DUE TODAY: " + bill.getBillName() + " is due today. Amount: ₹" + bill.getAmount();
            } else {
                msg = "UPCOMING: " + bill.getBillName() + " is due in " + daysUntil + " day(s). Amount: ₹" + bill.getAmount();
            }
            notificationService.createBillNotification(bill.getUser(), msg);
            log.info("Bill reminder sent for user {} - {}", bill.getUser().getId(), bill.getBillName());
        }
    }

    // Advance next due date after acknowledgement
    public RecurringBillResponse advanceDueDate(Long id) {
        RecurringBill bill = findOwned(id);
        LocalDate next = bill.getNextDueDate();
        switch (bill.getFrequency()) {
            case DAILY -> next = next.plusDays(1);
            case WEEKLY -> next = next.plusWeeks(1);
            case MONTHLY -> next = next.plusMonths(1);
            case YEARLY -> next = next.plusYears(1);
        }
        bill.setNextDueDate(next);
        return toResponse(billRepository.save(bill));
    }

    private RecurringBill findOwned(Long id) {
        return billRepository.findByIdAndUserId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
    }

    private RecurringBillResponse toResponse(RecurringBill b) {
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), b.getNextDueDate());
        String urgency;
        if (daysUntil < 0)      urgency = "OVERDUE";
        else if (daysUntil == 0) urgency = "DUE_TODAY";
        else if (daysUntil <= 3) urgency = "DUE_SOON";
        else                     urgency = "UPCOMING";

        return RecurringBillResponse.builder()
                .id(b.getId())
                .accountId(b.getAccount().getId())
                .accountName(b.getAccount().getAccountName())
                .billName(b.getBillName())
                .amount(b.getAmount())
                .frequency(b.getFrequency())
                .nextDueDate(b.getNextDueDate())
                .isActive(b.getIsActive())
                .daysUntilDue(daysUntil)
                .urgency(urgency)
                .build();
    }
}
