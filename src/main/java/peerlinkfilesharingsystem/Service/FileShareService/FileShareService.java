package peerlinkfilesharingsystem.Service.FileShareService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import peerlinkfilesharingsystem.Config.SecurityConfig;
import peerlinkfilesharingsystem.Dto.EmailFileRequest;
import peerlinkfilesharingsystem.Dto.ShareFileResponse;
import peerlinkfilesharingsystem.Dto.SharedFileResponse;
import peerlinkfilesharingsystem.Enums.MarkFileAs;
import peerlinkfilesharingsystem.Exception.UnauthorizedFileAccessException;
import peerlinkfilesharingsystem.Model.FileShare;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Model.Users;
import peerlinkfilesharingsystem.Repo.FileShareRepo;
import peerlinkfilesharingsystem.Repo.FileTransferRepo;
import peerlinkfilesharingsystem.Repo.UserRepo;
import peerlinkfilesharingsystem.Service.FileStorageService.FileStorageService;
import peerlinkfilesharingsystem.Service.MailService.MailService;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileShareService {
    @Value("${app.base-url:http://localhost:5500}")
    private String baseUrl;

    private final FileTransferRepo fileTransferRepo;
    private final FileShareRepo fileShareRepo;
    private final SecurityConfig config;
    private final FileStorageService fileStorageService;
    private final MailService mailService;
    private final UserRepo userRepo;

    public FileShareService(FileTransferRepo fileTransferRepo, FileShareRepo fileShareRepo, SecurityConfig config, FileStorageService fileStorageService, FileStorageService fileStorageService1, MailService mailService1,UserRepo userRepo) {
        this.fileTransferRepo = fileTransferRepo;
        this.fileShareRepo = fileShareRepo;
        this.config = config;
        this.fileStorageService = fileStorageService1;
        this.mailService = mailService1;
        this.userRepo = userRepo;
    }

    public ResponseEntity<?> markFileAspublic(String transferId) {

//        Optional<FileTransferEntity> fileTransferEntity = fileTransferRepo.findByTransferId(transferId);
        FileTransferEntity fileTransferEntity = fileTransferRepo.findByTransferId(transferId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File transfer not found with ID: " + transferId)
                );
        log.info("fileTransfer Entity: {}", fileTransferEntity);
        Users users = retriveLoggedInUser();

        try {
            fileStorageService.validateUserAccess(users.getId().toString(), fileTransferEntity.getStoragePath());
        } catch (UnauthorizedFileAccessException ex) {
            return new ResponseEntity<>("Invalid Access", HttpStatus.UNAUTHORIZED);
        }
//
//
//        System.out.println(fileTransferEntity.getStoragePath());
//        if (fileStorageService.validateUserAccess("13131",fileTransferEntity.getStoragePath())){
//            return new ResponseEntity<>("Invalid Access ", HttpStatus.UNAUTHORIZED);
//        }
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
        fileShare.setShareId(generateUniqueShareId());
//        fileShare.setShareExpiresAt(LocalDateTime.now().plusSeconds(15));
        fileShare.setShareExpiresAt(LocalDateTime.now().plusDays(2));
        fileShare.setUserId(users.getId().toString());
        fileShareRepo.save(fileShare);
        return new ResponseEntity<>("File marked as PUBLIC: " + transferId, HttpStatus.OK);
    }


    public ResponseEntity<?> getShareUrl(String transferId) {
        Users users = retriveLoggedInUser();
        Optional<FileTransferEntity> fileOpt = fileTransferRepo.findByTransferId(transferId);
        System.out.println(fileOpt.get());
        if (fileOpt.isEmpty()) {
            return ResponseEntity.status(404).body("File not found");
        }
        try {
            fileStorageService.validateUserAccess(users.getId().toString(), fileOpt.get().getStoragePath());
        } catch (UnauthorizedFileAccessException ex) {
            return new ResponseEntity<>("Invalid Access", HttpStatus.UNAUTHORIZED);
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
                shareUrl,
                fileShare.getShareId());
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> markFileAsPrivate(String transferId) throws FileNotFoundException {
        Users users = retriveLoggedInUser();
//        Optional<FileTransferEntity> fileTransferEntity = Optional.ofNullable(fileTransferRepo.findByTransferId(transferId).orElseThrow(() ->
        //new FileNotFoundException("File Not Found")));
        FileTransferEntity fileTransferEntity = fileTransferRepo.findByTransferId(transferId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File transfer not found with ID: " + transferId)
                );
        System.out.println(fileTransferEntity.getStoragePath());
        try {
            fileStorageService.validateUserAccess(users.getId().toString(), fileTransferEntity.getStoragePath());
        } catch (UnauthorizedFileAccessException ex) {
            return new ResponseEntity<>("Invalid Access", HttpStatus.UNAUTHORIZED);
        }
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

    public Long generateRandomId() {
        return 10000 + (long)(Math.random() * 90000);
    }

    public Long generateUniqueShareId() {
        Long randomId;

        do {
            randomId = generateRandomId();
        } while (fileShareRepo.checkShareId(randomId));

        return randomId;    }



    public ResponseEntity<?> mailShareUrl(String transferId) {
        Users users = retriveLoggedInUser();
        Optional<FileTransferEntity> fileOpt = fileTransferRepo.findByShareToken(transferId);
        System.out.println(fileOpt.get());
        if (fileOpt.isEmpty()) {
            return ResponseEntity.status(404).body("File not found");
        }
        try {
            fileStorageService.validateUserAccess(users.getId().toString(), fileOpt.get().getStoragePath());
        } catch (UnauthorizedFileAccessException ex) {
            return new ResponseEntity<>("Invalid Access", HttpStatus.UNAUTHORIZED);
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
                shareUrl,
                fileShare.getShareId());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> sendLinkToEmail(EmailFileRequest emailFileRequest ) {
        retriveLoggedInUser();
        ResponseEntity<?> shareFileResponse = mailShareUrl(emailFileRequest.getShareToken());

        if (!shareFileResponse.getStatusCode().is2xxSuccessful()) {
            return shareFileResponse; // return the error (404/401/403/410 → same)
        }

        Object body = shareFileResponse.getBody();

        if (!(body instanceof ShareFileResponse)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Invalid response format");
        }

        ShareFileResponse response = (ShareFileResponse) body;

        try {
            if (mailService.sendLinkToMail(response, emailFileRequest.getEmail())) {
                return ResponseEntity.ok("Link has been sent");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending link to email");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error sending link to email");
    }

    public ResponseEntity<?> markedStatus(String transferId) {
        Users users = retriveLoggedInUser();
        FileTransferEntity fileTransferEntity = fileTransferRepo.findByTransferId(transferId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File transfer not found with ID: " + transferId)
                );
        try {
            fileStorageService.validateUserAccess(users.getId().toString(), fileTransferEntity.getStoragePath());
        } catch (UnauthorizedFileAccessException ex) {
            return new ResponseEntity<>("Invalid Access", HttpStatus.UNAUTHORIZED);
        }
        if(fileTransferEntity.getMarkFileAs() == MarkFileAs.PRIVATE) {
            return new ResponseEntity<>(MarkFileAs.PRIVATE,HttpStatus.OK);
        }
        return new ResponseEntity<>(MarkFileAs.PUBLIC,HttpStatus.OK);
    }

    public List<SharedFileResponse> getUserSharedFiles(Integer limit) {
        Users user = retriveLoggedInUser();

        log.info("Fetching shared files for user: {}", user.getUsername());

        List<FileShare> sharedFiles;

        if (limit != null && limit > 0) {
            sharedFiles = fileShareRepo.findRecentSharedFiles(user.getId().toString(), limit);
        } else {
            sharedFiles = fileShareRepo.findByUserIdOrderByIdDesc(user.getId().toString());
        }

        log.info("Found {} shared files for user {}", sharedFiles.size(), user.getUsername());

        return sharedFiles.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    private SharedFileResponse mapToResponse(FileShare fileShare) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = fileShare.getShareExpiresAt();

        boolean isExpired = expiresAt != null && expiresAt.isBefore(now);
        Integer daysUntilExpiry = null;

        if (expiresAt != null && !isExpired) {
            daysUntilExpiry = (int) ChronoUnit.DAYS.between(now, expiresAt);
        }

        String shareUrl = baseUrl + "/#public-download/" + fileShare.getShareToken();

        return SharedFileResponse.builder()
                .id(fileShare.getId())
                .userId(fileShare.getUserId())
                .fileName(fileShare.getFileName())
                .fileType(fileShare.getFileType())
                .fileSize(fileShare.getFileSize())
                .shareToken(fileShare.getShareToken())
                .shareUrl(shareUrl)
                .sharedAt(fileShare.getShareExpiresAt() != null
                        ? fileShare.getShareExpiresAt().minusDays(7)
                        : null)  // Assuming 7-day expiry, calculate share date
                .expiresAt(fileShare.getShareExpiresAt())
                .shareId(fileShare.getShareId())
                .daysUntilExpiry(daysUntilExpiry)
                .isExpired(isExpired)
                .build();
    }


    private Users retriveLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated())
            throw new BadCredentialsException("Bad Credentials login ");
        String username = authentication.getName();
//        System.out.println(STR."In Logged In User \{username}");
        System.out.println("Logged In User "+username);
        Users user = userRepo.findByUsername(username);
        if(user == null){
            throw new UsernameNotFoundException("User Not Found");
        }
        return user;
    }
}
