package peerlinkfilesharingsystem.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShareDownloadDTO {
    private Boolean success;
    private String ShareToken;
    private Long fileSize;
    private String fileName;
    private String fileType;

    public FileShareDownloadDTO(Boolean success, String shareToken, Long fileSize, String fileName, String fileType, HttpStatus httpStatus) {
        this.success = success;
        this.ShareToken = shareToken;
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.fileType = fileType;
    }
}
