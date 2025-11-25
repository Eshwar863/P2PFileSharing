package peerlinkfilesharingsystem.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String shareToken;
    private LocalDateTime shareExpiresAt;
    private Long ShareId;


}
