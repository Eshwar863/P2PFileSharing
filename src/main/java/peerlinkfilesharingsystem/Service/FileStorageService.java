package peerlinkfilesharingsystem.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import peerlinkfilesharingsystem.Exception.UnauthorizedFileAccessException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.storage.path:./uploads}")
    private String baseUploadDirectory;


    public String createUserDirectory(String userId) {
        String userDirPath = baseUploadDirectory + "/user_" + userId;
        File userDir = new File(userDirPath);

        if (!userDir.exists()) {
            boolean created = userDir.mkdirs();
            if (created) {
                log.info("Created new user directory: {}", userDirPath);
            } else {
                log.error("Failed to create user directory: {}", userDirPath);
                throw new RuntimeException("Could not create user directory");
            }
        }

        return userDirPath;
    }

    public String getUserFilePath(String userId, String transferId) {
        String userDir = createUserDirectory(userId);
        return userDir + "/" + transferId + ".gz";
    }

    public boolean fileExists(String userId, String transferId) {
        String filePath = getUserFilePath(userId, transferId);
        return new File(filePath).exists();
    }


    public void validateUserAccess(String requestingUserId, String filePath) {
        try {
            Path normalizedPath = Paths.get(filePath).normalize().toAbsolutePath();
            Path expectedUserDir = Paths.get(baseUploadDirectory, "user_" + requestingUserId)
                    .normalize().toAbsolutePath();

            if (!normalizedPath.startsWith(expectedUserDir)) {
                log.warn("SECURITY VIOLATION: User {} tried accessing {}", requestingUserId, filePath);
                throw new UnauthorizedFileAccessException("Access denied: You cannot access this file");
            }

            log.debug("Access granted: User {} accessing {}", requestingUserId, filePath);
        } catch (Exception e) {
            log.error("Access validation failed for user {}: {}", requestingUserId, e.getMessage());
            throw new UnauthorizedFileAccessException("Access validation failed: " + e.getMessage());
        }
    }


    public void validateTransferOwnership(String userId, String transferId, String storedPath) {
        if (!storedPath.contains("user_" + userId)) {
            log.warn("OWNERSHIP VIOLATION: User {} tried accessing transfer {} owned by another user",
                    userId, transferId);
            throw new UnauthorizedFileAccessException("Access denied: This transfer does not belong to you");
        }
    }


    public boolean deleteFile(String userId, String transferId) {
        String filePath = getUserFilePath(userId, transferId);
        validateUserAccess(userId, filePath);

        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            log.info("File deletion for user {}, transfer {}: {}", userId, transferId, deleted);
            return deleted;
        }
        return false;
    }


    public String getUserDirectoryPath(String userId) {
        return baseUploadDirectory + "/user_" + userId;
    }

    public String getBaseUploadDirectory() {
        return baseUploadDirectory;
    }
}
