package com.finance.tracker.service;

import com.finance.tracker.dto.request.GoalRequest;
import com.finance.tracker.dto.response.GoalResponse;
import com.finance.tracker.entity.*;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.repository.GoalRepository;
import com.finance.tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final SecurityUtils securityUtils;

    public GoalResponse create(GoalRequest request) {
        User user = securityUtils.getCurrentUser();
        Goal goal = Goal.builder()
                .user(user)
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .savedAmount(request.getSavedAmount() != null ? request.getSavedAmount() : BigDecimal.ZERO)
                .deadline(request.getDeadline())
                .build();
        return toResponse(goalRepository.save(goal));
    }

    public List<GoalResponse> getAll() {
        return goalRepository.findByUserId(securityUtils.getCurrentUserId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public GoalResponse getById(Long id) {
        return toResponse(findOwned(id));
    }

    public GoalResponse update(Long id, GoalRequest request) {
        Goal goal = findOwned(id);
        goal.setGoalName(request.getGoalName());
        goal.setTargetAmount(request.getTargetAmount());
        if (request.getSavedAmount() != null) goal.setSavedAmount(request.getSavedAmount());
        goal.setDeadline(request.getDeadline());
        return toResponse(goalRepository.save(goal));
    }

    // Add money to a goal
    public GoalResponse contribute(Long id, BigDecimal amount) {
        Goal goal = findOwned(id);
        goal.setSavedAmount(goal.getSavedAmount().add(amount));
        return toResponse(goalRepository.save(goal));
    }

    public void delete(Long id) {
        goalRepository.delete(findOwned(id));
    }

    private Goal findOwned(Long id) {
        return goalRepository.findByIdAndUserId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }

    private GoalResponse toResponse(Goal g) {
        BigDecimal remaining = g.getTargetAmount().subtract(g.getSavedAmount());
        double pct = g.getTargetAmount().compareTo(BigDecimal.ZERO) == 0 ? 0
                : g.getSavedAmount().divide(g.getTargetAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100;

        long daysLeft = g.getDeadline() != null ? ChronoUnit.DAYS.between(LocalDate.now(), g.getDeadline()) : -1;

        String status;
        if (pct >= 100) status = "ACHIEVED";
        else if (daysLeft < 0 && g.getDeadline() != null) status = "OVERDUE";
        else status = "IN_PROGRESS";

        return GoalResponse.builder()
                .id(g.getId())
                .goalName(g.getGoalName())
                .targetAmount(g.getTargetAmount())
                .savedAmount(g.getSavedAmount())
                .remainingAmount(remaining.max(BigDecimal.ZERO))
                .progressPercent(Math.min(Math.round(pct * 100.0) / 100.0, 100.0))
                .deadline(g.getDeadline())
                .daysRemaining(Math.max(daysLeft, 0))
                .status(status)
                .build();
    }
}
