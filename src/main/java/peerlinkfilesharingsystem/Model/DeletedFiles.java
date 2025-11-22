package peerlinkfilesharingsystem.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class DeletedFiles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String FileName;
    @Column(nullable = false)
    private String FileType;
    @Column(nullable = false)
    private String FilePath;
    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private String transferId;
    @Column(nullable = false)
    private LocalDateTime deletedAt;


}
