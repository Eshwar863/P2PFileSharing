package peerlinkfilesharingsystem.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.InputStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkedDownloadResource {
    private InputStream inputStream;
    private String fileName;
    private Long originalSizeBytes;
    private Long compressedSizeBytes;
    private Integer chunkSize;
    private String networkCondition;
    private Boolean isCompressed;
    private String transferId;
}
