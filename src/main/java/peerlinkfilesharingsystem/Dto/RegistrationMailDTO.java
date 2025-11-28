package peerlinkfilesharingsystem.Dto;


import lombok.Data;

@Data
public class RegistrationMailDTO {
    private String username;
    private String email;

    public RegistrationMailDTO(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // getters
}
