package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import peerlinkfilesharingsystem.Model.FileTransferEntity;
import peerlinkfilesharingsystem.Model.TransferMetricsEntity;

import java.util.Optional;

public interface TransferMetricsRepo extends JpaRepository<TransferMetricsEntity,Long> {
    Optional<FileTransferEntity> findByTransferId(String transferId);
}
