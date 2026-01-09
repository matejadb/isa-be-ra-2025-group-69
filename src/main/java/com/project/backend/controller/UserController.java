package com.project.backend.controller;

import com.project.backend.dto.PublicProfileResponse;
import com.project.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get public profile", description = "Returns public profile information. Accessible to all users.")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable Long id) {
        PublicProfileResponse profile = userService.getPublicProfile(id);
        return ResponseEntity.ok(profile);
    }
}