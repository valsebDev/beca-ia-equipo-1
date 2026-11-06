package com.ids.ms_tickets.security;

import com.ids.ms_tickets.model.UserRole;

/** Identidad simulada desde los encabezados X-User-Id y X-User-Role hasta la Práctica 3 (JWT). */
public record CurrentUser(Long id, UserRole role) {
}
