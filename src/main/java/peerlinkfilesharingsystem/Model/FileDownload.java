package peerlinkfilesharingsystem.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "File_download")
@NoArgsConstructor
@AllArgsConstructor
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
    private Integer transferDurationSeconds;
    private String storagePath;
}
