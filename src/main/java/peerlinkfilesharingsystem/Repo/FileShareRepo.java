package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import peerlinkfilesharingsystem.Model.FileShare;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepo extends JpaRepository<FileShare, Long> {
    FileShare findByShareToken(String shareToken);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FileShare f WHERE f.ShareId = :shareId")
    boolean checkShareId(@Param("shareId") Long shareId);

    @Query("SELECT f FROM FileShare f WHERE f.ShareId = :shareId")
    FileShare findByShareId(@Param("shareId") Long shareId);


    List<FileShare> findByUserIdOrderByIdDesc(String userId);

    @Query(value = "SELECT * FROM file_share WHERE user_id = :userId ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    List<FileShare> findRecentSharedFiles(@Param("userId") String userId, @Param("limit") Integer limit);


    Optional<FileShare> findByUserIdAndFileName(String userId, String fileName);
}
