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
    private Long fileSize;
    private Long bytesTransferred;
    private Long bytesCompressed;
    @Column(columnDefinition = "VARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;
    private String resumeToken;
    private Integer uploadAttempts;
    private LocalDateTime lastAttemptAt;
    private String failureReason;
    private Integer compressionLevel;
    private Integer chunkSize;
    private Integer noOfChunksUploaded;
    private Integer TotalChunks;
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


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        uploadAttempts = 1;
        uploadStatus = UploadStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

