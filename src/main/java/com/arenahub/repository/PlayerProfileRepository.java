package com.arenahub.repository;

import com.arenahub.entity.PlayerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {
    Optional<PlayerProfile> findByUserId(Long userId);
}
