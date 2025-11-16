package peerlinkfilesharingsystem.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "File_download")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class FileDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String transferId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private Integer chunkSize;
    private Double networkSpeedMbps;
    private Integer latencyMs;
    private LocalDateTime transferDurationSeconds;
    private String storagePath;
}
