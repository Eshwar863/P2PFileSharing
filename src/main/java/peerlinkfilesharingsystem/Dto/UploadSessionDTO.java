package peerlinkfilesharingsystem.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSessionDTO implements Serializable {
    private String uploadId;
    private String userId;
    private String fileName;
    private int totalChunks;
    private int uploadedChunks;
    private String status; // IN_PROGRESS, PAUSED, COMPLETED
    private long createdAt;
}
