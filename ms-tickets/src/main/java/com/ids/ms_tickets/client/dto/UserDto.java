package com.ids.ms_tickets.client.dto;

public record UserDto(Long id, String email, String fullName, String role, boolean active) {
}
