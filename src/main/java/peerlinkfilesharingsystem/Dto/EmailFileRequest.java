package peerlinkfilesharingsystem.Dto;


import lombok.Data;

@Data
public class EmailFileRequest {

    String email;
    String shareToken;
}
