package peerlinkfilesharingsystem.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

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

    @OneToOne(cascade = CascadeType.ALL)
    private FileTransferEntity fileTransferEntity;

    Map<String,String> ShareUrl;

}
