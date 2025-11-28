package peerlinkfilesharingsystem.Service.MailService;

import org.springframework.http.ResponseEntity;
import peerlinkfilesharingsystem.Dto.ForgotPassword;
import peerlinkfilesharingsystem.Dto.RegistrationMailDTO;
import peerlinkfilesharingsystem.Dto.ShareFileResponse;
import peerlinkfilesharingsystem.Model.Users;

public interface MailService {
    boolean sendRegistrationMail(RegistrationMailDTO registrationMailDTO);

    boolean sendLoginMail(RegistrationMailDTO registrationMailDTO);

    boolean sendLinkToMail(ShareFileResponse shareFileResponse , String email);

    Object SendOtp(String email);

    Object ValidateOtp(String otp, String username);

    ResponseEntity<?> forgotpassword(ForgotPassword forgotPassword);
}
