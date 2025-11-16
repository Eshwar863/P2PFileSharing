package peerlinkfilesharingsystem.Model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String otp;

    @OneToOne(mappedBy = "otp")
    private Users users;
    private String sentToEmail;
    private LocalDateTime expiryTime;
    private String status;
    private LocalDateTime createdAt;
}
