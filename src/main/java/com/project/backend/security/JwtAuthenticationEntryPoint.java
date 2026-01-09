package com.project.backend.security;

import com.fasterxml. jackson.databind.ObjectMapper;
import com.project.backend. dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet. http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework. security.core.AuthenticationException;
import org.springframework.security. web.AuthenticationEntryPoint;
import org.springframework.stereotype. Component;

import java.io. IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType(MediaType. APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("UNAUTHORIZED")
                .message("Morate se prijaviti kako biste pristupili ovoj funkcionalnosti.")
                .authenticationRequired(true)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}