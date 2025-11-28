package peerlinkfilesharingsystem.Service.UserService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import peerlinkfilesharingsystem.Model.UserPrinciple;
import peerlinkfilesharingsystem.Model.Users;
import peerlinkfilesharingsystem.Repo.UserRepo;


@Service
public class UserPrincipleService implements UserDetailsService {
    private final UserRepo userRepo;
    public UserPrincipleService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {


        Users user = userRepo.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new UserPrinciple(user);
    }
}
