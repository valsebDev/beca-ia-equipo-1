package com.ids.ms_users.controller;

import com.ids.ms_users.dto.UpdateProfileRequest;
import com.ids.ms_users.dto.UpdateUserStatusRequest;
import com.ids.ms_users.dto.UserResponse;
import com.ids.ms_users.service.TokenService;
import com.ids.ms_users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    public UserController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userIdentifier = tokenService.extractUserIdentifier(authHeader);
        return ResponseEntity.ok(userService.getProfile(userIdentifier));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        String userIdentifier = tokenService.extractUserIdentifier(authHeader);
        return ResponseEntity.ok(userService.updateProfile(userIdentifier, request));
    }

    // HU-USR-05: El servicio valida el rol antes de cambiar el estado.
    @PatchMapping("/{userIdentifier}/status")
    public ResponseEntity<UserResponse> updateUserStatusByAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String userIdentifier,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        List<String> roles = tokenService.extractRoles(authHeader);
        return ResponseEntity.ok(userService.updateStatusByAdmin(userIdentifier, request, roles));
    }
}