package peerlinkfilesharingsystem.Service.UserService;

import org.springframework.http.ResponseEntity;
import peerlinkfilesharingsystem.Dto.LoginDTO;
import peerlinkfilesharingsystem.Model.Users;

public interface UserService {
    ResponseEntity<?> register(Users users);

    String verify(LoginDTO loginDTO);
}
