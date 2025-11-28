package peerlinkfilesharingsystem.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShareDownloadDTO {
    private Boolean success;
    private String shareToken;
    private String fileName;
    private String fileType;
    private Long originalSizeBytes;
    private Long compressedSizeBytes;
    private String compressionRatioPercent;
    private LocalDateTime uploadedAt;
    private LocalDateTime completedAt;
}
