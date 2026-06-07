package org.example.studybuddybackend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "study_room")
public class StudyRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "target_minutes", nullable = false)
    private Integer targetMinutes;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @Column(name = "min_stake", nullable = false)
    private Integer minStake;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false, columnDefinition = "ENUM('进行中','已关闭') DEFAULT '进行中'")
    private String status = "进行中";

    @Column(name = "end_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime endAt;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", insertable = false, updatable = false)
    private User creator;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StudyRoomMember> members = new ArrayList<>();
}
