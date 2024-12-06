package br.lbgroup.commons.user;

import br.lbgroup.commons.user.isolated.IsolatedUser;
import br.lbgroup.commons.user.isolated.IsolatedUserRepository;
import br.lbgroup.commons.util.Util;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final IsolatedUserRepository isolatedUserRepository;

    public UserService(IsolatedUserRepository isolatedUserRepository) {
        this.isolatedUserRepository = isolatedUserRepository;
    }

    public User getUserById(long id) {
        IsolatedUser isolatedUser = isolatedUserRepository.findById(id).orElseThrow();
        return isolatedUser.toUser();
    }

    public User getUserByCpf(String cpf) {
        long parsedCpf = Util.parseCpf(cpf);
        IsolatedUser isolatedUser = isolatedUserRepository.findByCpfUsu(parsedCpf);
        if (isolatedUser == null) {
            return null;
        }

        return isolatedUser.toUser();
    }

    public String getUserName(User user) {
        IsolatedUser isolatedUser = isolatedUserRepository.findById(user.id()).orElseThrow();
        return isolatedUser.getNomeUsu();
    }

    public void setLbCoinsBalance(User user, double amount) {
        IsolatedUser isolatedUser = isolatedUserRepository.findById(user.id()).orElseThrow();
        isolatedUser.setLbCoinsUsu((float) amount);

        isolatedUserRepository.save(isolatedUser);
    }

    public double getLbCoinsBalance(User user) {
        IsolatedUser isolatedUser = isolatedUserRepository.findById(user.id()).orElseThrow();
        return isolatedUser.getLbCoinsUsu();
    }

    public static Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return Optional.of((User) principal);
        }

        return Optional.empty();
    }
}
