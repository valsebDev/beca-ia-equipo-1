package com.ids.ms_users.controller;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.dto.request.CreateUserRequest;
import com.ids.ms_users.dto.request.UpdateProfileRequest;
import com.ids.ms_users.dto.request.UpdateUserStatusRequest;
import com.ids.ms_users.dto.response.UserResponse;
import com.ids.ms_users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(currentUserId(jwt));
    }

    @PutMapping("/me")
    public UserResponse updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUserId(jwt), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> listUsers(@RequestParam(required = false) Role role) {
        return userService.listUsersByAdmin(role);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createByAdmin(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserByIdByAdmin(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateStatusByAdmin(id, request);
    }

    private static Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
