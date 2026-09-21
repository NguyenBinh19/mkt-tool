package com.example.extracttool.controller;

import com.example.extracttool.model.dto.ExtractRequestDTO;
import com.example.extracttool.model.dto.ExtractResponseDTO;
import com.example.extracttool.service.FacebookCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/facebook")
@RequiredArgsConstructor
public class FacebookCommentController {
    private final FacebookCommentService service;

    @PostMapping("/extract")
    public ResponseEntity<ExtractResponseDTO> extractComments(@Valid @RequestBody ExtractRequestDTO request) {
        ExtractResponseDTO resp = service.extractComments(request);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/browser")
    public ResponseEntity<String> openBrowserForLogin() {
        service.openBrowserForLogin();
        return ResponseEntity.ok("Browser opened. Sign in to Facebook there, complete any checkpoint, then call /api/facebook/extract.");
    }
}
