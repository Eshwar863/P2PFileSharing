package peerlinkfilesharingsystem.Model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(unique = true, nullable = false)
    private String email;
    private String role;

    HashSet<String> Sharedfiles;
}
