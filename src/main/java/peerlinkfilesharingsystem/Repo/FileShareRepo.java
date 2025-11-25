package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import peerlinkfilesharingsystem.Model.FileShare;

public interface FileShareRepo extends JpaRepository<FileShare, Long> {
    FileShare findByShareToken(String shareToken);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FileShare f WHERE f.ShareId = :shareId")
    boolean checkShareId(@Param("shareId") Long shareId);

    @Query("SELECT f FROM FileShare f WHERE f.ShareId = :shareId")
    FileShare findByShareId(@Param("shareId") Long shareId);
}
