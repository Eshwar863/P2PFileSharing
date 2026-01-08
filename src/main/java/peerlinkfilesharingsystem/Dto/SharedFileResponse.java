package peerlinkfilesharingsystem.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SharedFileResponse {
    private Long id;
    private String userId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String shareToken;
    private String shareUrl;
    private LocalDateTime sharedAt;
    private LocalDateTime expiresAt;
    private Long shareId;
    private Integer daysUntilExpiry;
    private Boolean isExpired;
}
