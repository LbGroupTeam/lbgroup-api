package br.lbgroup.commons.security;

import br.lbgroup.commons.security.model.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationService authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public void login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        authenticationManager.authenticate(loginRequest);

        var cookie = jwtUtil.generateCookie(loginRequest.username());

        response.addCookie(cookie);
    }
}