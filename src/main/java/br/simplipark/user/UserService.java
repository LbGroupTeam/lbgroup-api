package br.simplipark.user;

import br.simplipark.user.isolated.IsolatedUser;
import br.simplipark.user.isolated.IsolatedUserRepository;
import br.simplipark.util.Util;
import org.springframework.stereotype.Service;

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
        long parsedCpf = Util.parseCpfToLong(cpf);
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
}
