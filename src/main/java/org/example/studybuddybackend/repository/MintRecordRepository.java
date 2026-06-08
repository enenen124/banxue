package org.example.studybuddybackend.repository;

import org.example.studybuddybackend.entity.MintRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MintRecordRepository extends JpaRepository<MintRecord, Long> {
    List<MintRecord> findByUserIdOrderByMintedAtDesc(Long userId);
    Optional<MintRecord> findByUserIdAndAchievementCode(Long userId, String achievementCode);
    boolean existsByUserIdAndAchievementCode(Long userId, String achievementCode);
}
