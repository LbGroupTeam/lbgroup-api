package br.lbgroup.commons.security;

import br.lbgroup.commons.user.User;
import br.lbgroup.commons.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = findTokenFromCookies(request);

        handleJwtToken(token);

        filterChain.doFilter(request, response);
    }

    private static String findTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("authToken")) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void handleJwtToken(String token) {
        String cpf = jwtUtil.extractSubject(token);

        if (cpf != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            setAuthenticatedUser(cpf);
        }
    }

    private void setAuthenticatedUser(String cpf) {
        User user = userService.getUserByCpf(cpf);

        UsernamePasswordAuthenticationToken authToken = UsernamePasswordAuthenticationToken.authenticated(user, null, null);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
