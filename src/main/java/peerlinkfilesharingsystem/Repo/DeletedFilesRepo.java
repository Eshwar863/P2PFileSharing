package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peerlinkfilesharingsystem.Model.DeletedFiles;

@Repository
public interface DeletedFilesRepo extends JpaRepository<DeletedFiles,Long> {
}
