package peerlinkfilesharingsystem.Service.FileShareService;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import peerlinkfilesharingsystem.Dto.FileUploadResponse;
import peerlinkfilesharingsystem.Enums.MarkFileAs;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FileShareService {

    private final FileTransferRepo fileTransferRepo;

    public FileShareService(FileTransferRepo fileTransferRepo) {
        this.fileTransferRepo = fileTransferRepo;
    }

    // Generate shareable public URL
    public ResponseEntity<?> getUrl(String transferId) {

        Optional<FileTransferEntity> opt = fileTransferRepo.findByTransferId(transferId);

        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("TransferId not found");
        }

        FileTransferEntity fileTransferEntity = opt.get();

        // Mark as public if not marked
        if (fileTransferEntity.getMarkFileAs() != MarkFileAs.PUBLIC) {
            return ResponseEntity.status(500).body("File not marked as public");
        }

        // Generate share token if missing
        if (fileTransferEntity.getShareToken() == null) {
            fileTransferEntity.setShareToken(UUID.randomUUID().toString());
            fileTransferEntity.setShareExpiresAt(LocalDateTime.now().plusDays(7)); // token valid 7 days
            fileTransferEntity.setIsPublic(true);
        }

        fileTransferRepo.save(fileTransferEntity);

        // Build share URL
        String publicUrl = "http://localhost:8080/fileshare/download/" + fileTransferEntity.getShareToken();

        // Build response
        FileUploadResponse fileMeta = FileUploadResponse.builder()
                .fileId(fileTransferEntity.getFileId())
                .transferId(fileTransferEntity.getTransferId())
                .fileName(fileTransferEntity.getFileName())
                .fileSizeBytes(fileTransferEntity.getFileSize())
                .compressedSizeBytes(fileTransferEntity.getBytesTransferred())
                .compressionRatioPercent(
                        String.format("%.2f%%",
                                (1.0 - (double) fileTransferEntity.getBytesTransferred() /
                                        fileTransferEntity.getFileSize()) * 100)
                )
                .appliedCompressionLevel(fileTransferEntity.getCompressionLevel())
                .appliedChunkSize(fileTransferEntity.getChunkSize())
                .success(true)
                .message("Public share URL generated")
                .uploadedAt(fileTransferEntity.getCreatedAt())
                .build();

        Map<String, Object> output = new HashMap<>();
        output.put("file", fileMeta);
        output.put("publicUrl", publicUrl);

        return ResponseEntity.ok(output);
    }

    // Mark as public only
    public ResponseEntity<?> markFileAspublic(String transferId) {

        Optional<FileTransferEntity> opt = fileTransferRepo.findByTransferId(transferId);

        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("File not found");
        }

        FileTransferEntity file = opt.get();
        file.setMarkFileAs(MarkFileAs.PUBLIC);
        file.setIsPublic(true);
        fileTransferRepo.save(file);

        return new ResponseEntity<>("File marked as PUBLIC: " + transferId, HttpStatus.OK);
    }
}
