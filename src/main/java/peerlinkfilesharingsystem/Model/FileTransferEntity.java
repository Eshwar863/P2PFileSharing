package peerlinkfilesharingsystem.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import peerlinkfilesharingsystem.Enums.MarkFileAs;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileTransferEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long fileId;

    @Column(name = "transfer_id", unique = true, nullable = false)
    private String transferId;

    private String fileName;
    private Long fileSize;
    private String fileType;
    private Integer compressionLevel;
    private Integer chunkSize;
    private Integer noOfChunksUploaded;
    private Double networkSpeedMbps;
    private Integer latencyMs;
    private Double packetLossPercentage;
    private Integer transferDurationSeconds;
    private Long bytesTransferred;
    private Boolean success;
    private int downloadCount = 0;
    @Column(unique = true)
    private String shareToken;
    private String deviceType;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private UUID userId;
    private MarkFileAs markFileAs = MarkFileAs.PRIVATE;
    private String status;
    private LocalDateTime completedAt;
    private String storagePath;
    @Column(nullable = false)
    private String clientIp;
    private Boolean deleted = false;

}
