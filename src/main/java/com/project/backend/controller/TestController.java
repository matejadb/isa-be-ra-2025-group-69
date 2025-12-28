package com.project.backend.controller;

import com.project.backend.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public ResponseEntity<?> protectedEndpoint(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "message", "Uspešno autentifikovan!",
                "user", user.getUsername()
        ));
    }
}