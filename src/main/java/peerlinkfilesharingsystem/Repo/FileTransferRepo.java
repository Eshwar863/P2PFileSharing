package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import peerlinkfilesharingsystem.Model.FileTransferEntity;

import java.util.Optional;
import java.util.UUID;

public interface FileTransferRepo extends JpaRepository<FileTransferEntity,UUID> {
    Optional<FileTransferEntity> findByTransferId(String transferId);
}
