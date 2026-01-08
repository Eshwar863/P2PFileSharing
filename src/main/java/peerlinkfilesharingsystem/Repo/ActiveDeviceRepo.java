package peerlinkfilesharingsystem.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import peerlinkfilesharingsystem.Model.ActiveDevice;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface ActiveDeviceRepo extends JpaRepository<ActiveDevice, Long> {



    Optional<ActiveDevice> findBySessionToken(String sessionToken);
    
    void deleteByLastSeenBefore(LocalDateTime cutoff);

    List<ActiveDevice> findByIpAddress(String clientIp);
}