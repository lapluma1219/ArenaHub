package com.arenahub.repository;

import com.arenahub.entity.MatchRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {
    List<MatchRecord> findByWinnerIdOrLoserIdOrderByFinishedAtDesc(Long winnerId, Long loserId);
}
