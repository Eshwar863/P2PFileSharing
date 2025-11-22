package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Model.IntelligentModelParametersEntity;

import java.util.List;
import java.util.Optional;

public interface IntelligentModelParametersRepo extends JpaRepository<IntelligentModelParametersEntity,Long> {

    Optional<IntelligentModelParametersEntity> findByFileTypeAndNetworkCondition(String fileType, String networkCondition);

    @Query("SELECT COUNT(ft) FROM FileTransferEntity ft WHERE ft.success = true")
    Long countSuccessfulTransfers();

    @Query("SELECT ft FROM FileTransferEntity ft WHERE ft.success = true ORDER BY ft.createdAt DESC")
    List<FileTransferEntity> findAllSuccessfulTransfers();

    @Query("SELECT ft FROM FileTransferEntity ft WHERE ft.fileType = ?1 AND ft.success = true")
    List<FileTransferEntity> findSuccessfulByFileType(String fileType);

    @Query("SELECT ft FROM FileTransferEntity ft WHERE ft.networkSpeedMbps < ?1 AND ft.success = true")
    List<FileTransferEntity> findByNetworkSpeed(Double speed);

    // Average transfer duration
    @Query("SELECT AVG(ft.transferDurationSeconds) FROM FileTransferEntity ft WHERE ft.success = true")
    Double getAverageTransferDuration();

    // Average compression ratio
    @Query("SELECT AVG((1.0 - CAST(ft.bytesTransferred AS double) / ft.fileSize) * 100) " +
            "FROM FileTransferEntity ft WHERE ft.success = true")
    Double getAverageCompressionRatio();}
