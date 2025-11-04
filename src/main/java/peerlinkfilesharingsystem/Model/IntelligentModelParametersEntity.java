package peerlinkfilesharingsystem.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Intelligent_model_parameters", indexes = {
        @Index(name = "idx_filetype_network", columnList = "fileType, networkCondition", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntelligentModelParametersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 50, nullable = false)
    private String fileType;
    @Column(length = 20, nullable = false)
    private String networkCondition;
    @Column(nullable = false)
    private Integer optimalCompressionLevel;
    @Column(nullable = false)
    private Integer optimalChunkSize;
    @Column(nullable = false)
    private Double successRate;
    @Column(nullable = false)
    private Integer sampleCount;
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime lastUpdated;

}
