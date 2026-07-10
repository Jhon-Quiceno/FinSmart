package com.smartfinance.backend.ia.controller;

import com.smartfinance.backend.ia.model.dto.AiUsageResponse;
import com.smartfinance.backend.ia.model.dto.ChatMessageResponse;
import com.smartfinance.backend.ia.model.dto.ChatReplyResponse;
import com.smartfinance.backend.ia.model.dto.ChatRequest;
import com.smartfinance.backend.ia.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the current user's AI chat: {@code POST /api/ai/chat} sends a message and
 * gets a reply, {@code GET /api/ai/chat/history} lists past turns, and
 * {@code GET /api/ai/chat/usage} reports the monthly message quota usage.
 */
@RestController
@RequestMapping("/api/ai/chat")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public ResponseEntity<ChatReplyResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(aiChatService.chat(request));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<ChatMessageResponse>> getHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(aiChatService.getHistory(pageable));
    }

    @GetMapping("/usage")
    public ResponseEntity<AiUsageResponse> getUsage() {
        return ResponseEntity.ok(aiChatService.getUsage());
    }
}
