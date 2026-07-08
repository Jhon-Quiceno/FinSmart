package com.smartfinance.backend.controller;

import com.smartfinance.backend.dto.ai.ChatMessageResponse;
import com.smartfinance.backend.dto.ai.ChatReplyResponse;
import com.smartfinance.backend.dto.ai.ChatRequest;
import com.smartfinance.backend.service.AiChatService;
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
 * gets a reply, {@code GET /api/ai/chat/history} lists past turns.
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
}
