package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import peerlinkfilesharingsystem.Model.Otp;
import peerlinkfilesharingsystem.Model.Users;

@Repository
public interface OtpRepo  extends JpaRepository<Otp, Long> {


    Otp findByUser(Users user);

    @Query("select b from Otp b where b.user.username =:username and b.otp=:otp")
    Otp findByOtpAndUser(String otp, String username);
}
