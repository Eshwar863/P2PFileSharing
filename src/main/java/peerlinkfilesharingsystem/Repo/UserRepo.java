package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peerlinkfilesharingsystem.Model.Users;

import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<Users, UUID> {
    Users findByEmail(String email);

    Users findByUsername(String username);
    Users findByEmailAndUsername(String email, String username);
}
