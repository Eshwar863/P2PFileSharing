package peerlinkfilesharingsystem.Controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import peerlinkfilesharingsystem.Dto.LoginDTO;
import peerlinkfilesharingsystem.Model.Users;
import peerlinkfilesharingsystem.Service.UserService.UserService;


@RestController
@RequestMapping("api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    private ResponseEntity<?> register(@RequestBody Users users) {
        if (users == null) {
            return new ResponseEntity<>("Users is null", HttpStatus.BAD_REQUEST);
        }
        if (users.getEmail() == null || !users.getEmail().contains("@")) {
            return new ResponseEntity<>("Email is missing or invalid",HttpStatus.BAD_REQUEST);
        }
        return userService.register(users);
    }


    @PostMapping("login")
    private String login(@RequestBody LoginDTO loginDTO) {
        if (loginDTO == null) {
            return "Invalid Login Data";
        }
        if (loginDTO.getUsername() == null || loginDTO.getPassword() == null) {
            return "UserName or Password is missing or invalid";
        }
        return userService.verify(loginDTO);
    }

}
