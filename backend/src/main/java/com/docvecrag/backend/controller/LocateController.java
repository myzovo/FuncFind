package com.docvecrag.backend.controller;

import com.docvecrag.backend.dto.LocateReportRequest;
import com.docvecrag.backend.service.locate.LocateEventLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/locate")
public class LocateController {

    private static final String SESSION_HEADER = "X-Client-Session-Id";

    private final LocateEventLogService locateEventLogService;

    public LocateController(LocateEventLogService locateEventLogService) {
        this.locateEventLogService = locateEventLogService;
    }

    @PostMapping
    public Map<String, String> report(@Valid @RequestBody LocateReportRequest request,
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            request.setSessionId(sessionId.trim());
        }
        locateEventLogService.append(request);
        return Map.of("status", "logged");
    }
}
