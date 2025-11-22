package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import peerlinkfilesharingsystem.Model.DeletedFiles;

public interface DeletedFilesRepo extends JpaRepository<DeletedFiles,Long> {
}
