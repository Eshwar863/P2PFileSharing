package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import peerlinkfilesharingsystem.Model.FileShare;

public interface FileShareRepo extends JpaRepository<FileShare, Long> {
    FileShare findByShareToken(String shareToken);

}
