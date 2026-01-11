package com.project.backend.service.impl;

import com.project.backend.dto.PublicProfileResponse;
import com. project.backend.exception.ResourceNotFoundException;
import com. project.backend.model.User;
import com.project.backend.repository.UserRepository;
import com.project. backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype. Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;


    @Override
    public PublicProfileResponse getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return PublicProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profilePictureUrl(user.getProfilePictureUrl())
                .bio(null) // TODO: Add bio field to User entity later
                .createdAt(user.getCreatedAt())
                .build();
    }
}