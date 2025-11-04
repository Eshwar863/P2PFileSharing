package peerlinkfilesharingsystem.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadRequest {
    private MultipartFile file;
    private String clientId;
    private Double networkSpeedMbps;
    private String DeviceType;
    private Integer latencyMs;
    private String deviceType;
}