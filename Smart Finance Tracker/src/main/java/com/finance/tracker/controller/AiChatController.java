package com.finance.tracker.controller;

import com.finance.tracker.dto.request.AiChatRequest;
import com.finance.tracker.dto.response.AiChatResponse;
import com.finance.tracker.dto.response.ApiResponse;
import com.finance.tracker.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        log.info("Received AI Chat request: {}", request.getMessage());
        return ResponseEntity.ok(aiChatService.chat(request));
    }
}
