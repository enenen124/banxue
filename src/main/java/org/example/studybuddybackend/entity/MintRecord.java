package org.example.studybuddybackend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mint_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_achievement_mint", columnNames = {"user_id", "achievement_code"})
})
public class MintRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "achievement_code", nullable = false, length = 50)
    private String achievementCode;

    @Column(name = "token_id", length = 100)
    private String tokenId;

    @Column(name = "tx_hash", length = 100)
    private String txHash;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'minted'")
    private String status = "minted";

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @Column(name = "minted_at", updatable = false)
    private LocalDateTime mintedAt;
}
