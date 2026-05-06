package com.docvecrag.backend.controller;

import com.docvecrag.backend.dto.ChatRequest;
import com.docvecrag.backend.dto.ChatResponse;
import com.docvecrag.backend.service.ChatService;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import com.docvecrag.backend.service.session.SessionRuntimeConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final String SESSION_HEADER = "X-Client-Session-Id";

    private final ChatService chatService;
    private final SessionRuntimeConfigService sessionRuntimeConfigService;

    public ChatController(ChatService chatService,
            SessionRuntimeConfigService sessionRuntimeConfigService) {
        this.chatService = chatService;
        this.sessionRuntimeConfigService = sessionRuntimeConfigService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionId) {
        String normalizedKbName = sessionRuntimeConfigService.normalizeKbName(request.getKbName());
        request.setKbName(normalizedKbName);

        SessionRuntimeConfigContextHolder.setCurrent(
                sessionRuntimeConfigService.resolveEffective(sessionId, normalizedKbName));
        try {
            return chatService.chat(request);
        } finally {
            SessionRuntimeConfigContextHolder.clear();
        }
    }
}
