package peerlinkfilesharingsystem.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
    public class FileUploadResponse {
        private long fileId;
        private String transferId;
        private String fileName;
        private Long fileSizeBytes;
        private Long compressedSizeBytes;
        private String compressionRatioPercent;
        private Integer appliedCompressionLevel;
        private Integer appliedChunkSize;
        private Integer totalChunks;
        private Integer uploadedChunks;
        private String uploadStatus;
        private Boolean success;
        private String message;
        private String resumeToken;
        private LocalDateTime uploadedAt;

}