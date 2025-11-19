package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import peerlinkfilesharingsystem.Model.FileTransferEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileTransferRepo extends JpaRepository<FileTransferEntity,UUID> {

    Optional<FileTransferEntity> findByTransferId(String transferId);

    @Query(value = "SELECT * FROM file_transfer_entity " +
            "WHERE user_id = :userId " +
            "ORDER BY file_id DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<FileTransferEntity> findLastUploads(@Param("userId") String userId,
                                             @Param("limit") int limit);

    Optional<FileTransferEntity> findByShareToken(String shareToken);
}
