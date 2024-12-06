package br.lbgroup.commons.security;

import br.lbgroup.commons.security.model.LoginRequest;
import br.lbgroup.commons.user.User;
import br.lbgroup.commons.user.UserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final static String ERROR_MESSAGE = "Invalid username or password";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthenticationService(UserService userService) {
        this.userService = userService;
    }

    public void authenticate(LoginRequest loginRequest) throws AuthenticationException {
        if (!isUsernameValid(loginRequest.username())) {
            throw new BadCredentialsException(ERROR_MESSAGE);
        }

        User user = userService.getUserByCpf(loginRequest.username());
        if (user == null) {
            throw new BadCredentialsException(ERROR_MESSAGE);
        }

        String passwordWithSalt = loginRequest.password() + user.passwordSalt();

        if (!passwordEncoder.matches(passwordWithSalt, user.passwordHash())) {
            throw new BadCredentialsException(ERROR_MESSAGE);
        }
    }

    private boolean isUsernameValid(String username) {
        return username != null && username.matches("\\d{11}");
    }
}
