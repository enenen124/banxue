package org.example.studybuddybackend.repository;

import org.example.studybuddybackend.entity.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {
    List<StudyRoom> findByStatusOrderByCreatedAtDesc(String status);
    List<StudyRoom> findAllByOrderByCreatedAtDesc();
}
