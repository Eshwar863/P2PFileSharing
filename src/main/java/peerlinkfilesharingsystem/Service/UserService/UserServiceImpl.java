package peerlinkfilesharingsystem.Service.UserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import peerlinkfilesharingsystem.Config.SecurityConfig;
import peerlinkfilesharingsystem.Dto.LoginDTO;
import peerlinkfilesharingsystem.Dto.RegistrationMailDTO;
import peerlinkfilesharingsystem.Enums.UserRole;
import peerlinkfilesharingsystem.Model.Users;
import peerlinkfilesharingsystem.Repo.UserRepo;
import peerlinkfilesharingsystem.Service.Jwt.JwtService;
import peerlinkfilesharingsystem.Service.MailService.MailService;

import java.util.UUID;


@Slf4j
@Service
public class UserServiceImpl implements UserService{
    private final SecurityConfig config;
    private final UserRepo userRepo;
    @Autowired
    private  MailService mailService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;

    public UserServiceImpl(UserRepo userRepo, SecurityConfig config) {
        this.userRepo = userRepo;
        this.config = config;
    }

    @Override
    public ResponseEntity<?> register(Users users) {

        Users existing = userRepo.findByEmail(users.getEmail());
        if (existing != null) {
            return new ResponseEntity<>("User already exists", HttpStatus.CONFLICT);
        }
        Users existingname = userRepo.findByUsername(users.getUsername());
        if (existingname != null) {
            return new ResponseEntity<>("User already exists", HttpStatus.CONFLICT);
        }

        if (users.getEmail().isEmpty() || users.getPassword().isEmpty() || users.getUsername().isEmpty()) {
            return new ResponseEntity<>("Please fill all the details", HttpStatus.BAD_REQUEST);
        }

        Users newUser = new Users();
        newUser.setEmail(users.getEmail());
        newUser.setUsername(users.getUsername());
        newUser.setRole(UserRole.USER);

        RegistrationMailDTO mailDTO = new RegistrationMailDTO(
                users.getUsername(),
                users.getEmail()
        );

        try {
            if (mailService.sendRegistrationMail(mailDTO)) {

                newUser.setPassword(config.bCryptPasswordEncoder().encode(users.getPassword()));
                userRepo.save(newUser);
                log.info("User saved successfully"+users);
                return new ResponseEntity<>("Registration Successful", HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error sending registration email", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Registration Failed", HttpStatus.BAD_REQUEST);
    }


    @Override
    public String verify(LoginDTO loginDTO) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginDTO.getUsername(),loginDTO.getPassword()));
        if (authentication.isAuthenticated()) {
            try {
                Users users = userRepo.findByUsername(loginDTO.getUsername());
                RegistrationMailDTO mailDTO = new RegistrationMailDTO(users.getUsername(),users.getEmail());
                mailService.sendLoginMail(mailDTO);
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return jwtService.generateToken(loginDTO.getUsername());
        }
        else
            return"Session Expired";
    }


    private Users retriveLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated())
            throw new BadCredentialsException("Bad Credentials login ");
        String username = authentication.getName();
        System.out.println("In Logged In User"+username);
        Users user = userRepo.findByUsername(username);
        if(user == null){
            throw new UsernameNotFoundException("User Not Found");
        }
        return user;
    }

}
