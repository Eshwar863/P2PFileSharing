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

    @Column(unique = true, nullable = false)
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
    private Integer transferDurationSeconds = Integer.MAX_VALUE;
    private Long bytesTransferred;
    private Boolean success;
    private int downloadCount = 0;
    @Column(unique = true)
    private String shareToken;
    private String deviceType;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long userId;
    private MarkFileAs markFileAs = MarkFileAs.PRIVATE;
    private String status;
    private LocalDateTime completedAt;
    private String storagePath;
    @Column(nullable = false)
    private String clientIp;

}
