package peerlinkfilesharingsystem.Service.FileShareService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import peerlinkfilesharingsystem.Config.SecurityConfig;
import peerlinkfilesharingsystem.Dto.ShareFileResponse;
import peerlinkfilesharingsystem.Enums.MarkFileAs;
import peerlinkfilesharingsystem.Model.FileShare;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Repo.FileShareRepo;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class FileShareService {

    private final FileTransferRepo fileTransferRepo;
    private final FileShareRepo fileShareRepo;
    private final SecurityConfig config;

    public FileShareService(FileTransferRepo fileTransferRepo, FileShareRepo fileShareRepo, SecurityConfig config) {
        this.fileTransferRepo = fileTransferRepo;
        this.fileShareRepo = fileShareRepo;
        this.config = config;
    }

    public ResponseEntity<?> markFileAspublic(String transferId) {

//        Optional<FileTransferEntity> fileTransferEntity = fileTransferRepo.findByTransferId(transferId);
        FileTransferEntity fileTransferEntity = fileTransferRepo.findByTransferId(transferId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File transfer not found with ID: " + transferId)
                );
        log.info("fileTransfer Entity: {}", fileTransferEntity);
        FileShare  fileShare1 = fileShareRepo.findByShareToken(fileTransferEntity.getShareToken());
        log.info("fileTransfer Entity: {}", fileShare1);
        if (fileShare1!=null && fileTransferEntity.getMarkFileAs() == MarkFileAs.PUBLIC) {
            return new ResponseEntity<>("Already Marked as Public" ,HttpStatus.OK);
        }
        FileTransferEntity file = fileTransferEntity;
        file.setMarkFileAs(MarkFileAs.PUBLIC);
        file.setShareToken(UUID.randomUUID().toString());
        fileTransferRepo.save(file);

        FileShare fileShare = new FileShare();
        fileShare.setFileName(file.getFileName());
        fileShare.setFileSize(file.getFileSize());
        fileShare.setFileType(file.getFileType());
        fileShare.setShareToken(file.getShareToken());
//        fileShare.setShareExpiresAt(LocalDateTime.now().plusSeconds(15));
        fileShare.setShareExpiresAt(LocalDateTime.now().plusDays(1));
        fileShareRepo.save(fileShare);
        return new ResponseEntity<>("File marked as PUBLIC: " + transferId, HttpStatus.OK);
    }


    public ResponseEntity<?> getShareUrl(String transferId) {

        Optional<FileTransferEntity> fileOpt = fileTransferRepo.findByTransferId(transferId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.status(404).body("File not found");
        }

        FileTransferEntity file = fileOpt.get();

        if (file.getMarkFileAs() != MarkFileAs.PUBLIC) {
            return ResponseEntity.status(403).body("File is not public");
        }

        FileShare fileShare = fileShareRepo.findByShareToken(file.getShareToken());
        if (fileShare == null) {
            return ResponseEntity.status(404).body("Share record not found");
        }

        if (fileShare.getShareExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(410).body("Share link has expired");
        }

        String shareUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/files/info/public/")
                .path(fileShare.getShareToken())
                .toUriString();

        ShareFileResponse response = new ShareFileResponse(
                fileShare.getFileName(),
                shareUrl
        );

        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> markFileAsPrivate(String transferId) throws FileNotFoundException {

//        Optional<FileTransferEntity> fileTransferEntity = Optional.ofNullable(fileTransferRepo.findByTransferId(transferId).orElseThrow(() ->
        //new FileNotFoundException("File Not Found")));
        FileTransferEntity fileTransferEntity = fileTransferRepo.findByTransferId(transferId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File transfer not found with ID: " + transferId)
                );
        FileShare  fileShare1 = fileShareRepo.findByShareToken(fileTransferEntity.getShareToken());
        if (fileTransferEntity.getMarkFileAs() == MarkFileAs.PRIVATE ) {
            return new ResponseEntity<>("Already Marked as Private" ,HttpStatus.OK);
        }

        FileTransferEntity file = fileTransferEntity;
        file.setMarkFileAs(MarkFileAs.PRIVATE);
        file.setShareToken(null);
        fileTransferRepo.save(file);
        fileShareRepo.delete(fileShare1);
        return new ResponseEntity<>("File marked as PRIVATE : " + transferId, HttpStatus.OK);
    }
}
