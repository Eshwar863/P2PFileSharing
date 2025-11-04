package peerlinkfilesharingsystem.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
    private Double networkSpeedMbps;
    private Integer latencyMs;
    private Double packetLossPercentage;
    private Integer transferDurationSeconds;
    private Long bytesTransferred;
    private Boolean success;
    private Integer retryCount;
    private String deviceType;
    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
    private String storagePath;
    @Column(nullable = false)
    private String clientIp;

}
