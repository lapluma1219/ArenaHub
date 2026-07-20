package com.arenahub.dto;

import com.arenahub.entity.PlayerProfile;

public record PlayerResponse(Long id, String username, String nickname, int rating, int wins, int losses) {
    public static PlayerResponse from(PlayerProfile profile) {
        return new PlayerResponse(
                profile.getId(),
                profile.getUser().getUsername(),
                profile.getNickname(),
                profile.getRating(),
                profile.getWins(),
                profile.getLosses());
    }
}
