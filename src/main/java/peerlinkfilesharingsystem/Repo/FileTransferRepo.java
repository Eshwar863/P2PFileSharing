package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import peerlinkfilesharingsystem.Enums.UploadStatus;
import peerlinkfilesharingsystem.Model.FileTransferEntity;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileTransferRepo extends JpaRepository<FileTransferEntity,UUID> {
    Optional<FileTransferEntity> findByTransferId(String transferId);

    @Query(value = "SELECT * FROM file_transfer ORDER BY id DESC LIMIT :limit",
            nativeQuery = true)
    List<FileTransferEntity> findLastUploads(@Param("limit") int limit);

    Optional<FileTransferEntity> findByResumeToken(String resumeToken);

    List<FileTransferEntity> findByUserId(String userId);

    List<FileTransferEntity> findByUserIdAndUploadStatusIn(String userId, List<UploadStatus> statuses);
}
