package peerlinkfilesharingsystem.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import peerlinkfilesharingsystem.Enums.UploadStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_transfer_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileTransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @Column(unique = true, nullable = false)
    private String transferId;

    private String fileName;
    private String fileType;
    private Long fileSize;              // Total file size
    private Long bytesTransferred;      // Bytes uploaded so far
    private Long bytesCompressed;       // Bytes after compression

    @Column(columnDefinition = "VARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;  // PENDING, IN_PROGRESS, COMPLETED, FAILED, PAUSED

    private String resumeToken;
    private Integer uploadAttempts;
    private LocalDateTime lastAttemptAt;
    private String failureReason;

    private Integer compressionLevel;
    private Integer chunkSize;
    private Integer noOfChunksUploaded;

    private Double networkSpeedMbps;
    private Integer latencyMs;
    private String deviceType;
    private String clientIp;
    private String userId;

    private Integer transferDurationSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    private String storagePath;
    private Boolean success;

    // Retry Configuration
    private Integer maxRetries;         // Max retry attempts allowed
    private Boolean allowResume;        // Can this upload be resumed?

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        uploadAttempts = 1;
        maxRetries = 3;
        allowResume = true;
        uploadStatus = UploadStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

