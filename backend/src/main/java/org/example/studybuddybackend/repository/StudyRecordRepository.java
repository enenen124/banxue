package org.example.studybuddybackend.repository;

import org.example.studybuddybackend.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {
    List<StudyRecord> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(r.minutes), 0) FROM StudyRecord r WHERE r.userId = :userId")
    Integer sumMinutesByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    List<StudyRecord> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
