package peerlinkfilesharingsystem.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;



@Entity
@Table(name = "transfer_metrics", indexes = {
        @Index(name = "idx_transfer_id", columnList = "transferId"),
        @Index(name = "idx_chunk_number", columnList = "chunkNumber")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String transferId;  // links to FileTransferEntity.transferId

    @Column(nullable = false)
    private Integer chunkNumber;  // the chunk index in the transfer

    @CreationTimestamp
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Integer originalChunkSize;  // size before compression, in bytes

    @Column(nullable = false)
    private Integer compressedChunkSize; // size after compression, in bytes

    private Double instantaneousSpeedMbps;  // optional, can be null

    private Integer retryAttempt;

    private Boolean success;

    private String errorReason;
}
