package com.project.backend.service;

import com.project.backend.dto.PublicProfileResponse;

public interface UserService {
    PublicProfileResponse getPublicProfile(Long userId);
}