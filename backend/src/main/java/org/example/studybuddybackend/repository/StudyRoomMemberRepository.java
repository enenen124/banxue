package org.example.studybuddybackend.repository;

import org.example.studybuddybackend.entity.StudyRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRoomMemberRepository extends JpaRepository<StudyRoomMember, Long> {
    List<StudyRoomMember> findByRoomId(Long roomId);
    Optional<StudyRoomMember> findByRoomIdAndUserId(Long roomId, Long userId);
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
    long countByRoomId(Long roomId);
    long countByUserId(Long userId);
}
