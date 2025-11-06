package peerlinkfilesharingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PeerLinkFileSharingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeerLinkFileSharingSystemApplication.class, args);
    }

}
