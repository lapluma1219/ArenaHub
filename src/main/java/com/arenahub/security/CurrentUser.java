package com.arenahub.security;

public record CurrentUser(Long userId, String username, Long playerId) {
}
