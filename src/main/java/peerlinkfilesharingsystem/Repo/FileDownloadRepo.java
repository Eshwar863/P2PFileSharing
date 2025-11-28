package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peerlinkfilesharingsystem.Model.FileDownload;

import java.util.List;

@Repository
public interface FileDownloadRepo extends JpaRepository<FileDownload,Long> {

    FileDownload findByTransferId(String fileId);


    FileDownload findTopByTransferIdOrderByIdDesc(String transferId);

}
