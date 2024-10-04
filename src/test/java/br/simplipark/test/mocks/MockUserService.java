package br.simplipark.test.mocks;

import br.simplipark.user.User;
import br.simplipark.user.UserService;
import br.simplipark.user.isolated.IsolatedUserRepository;

import java.util.HashMap;
import java.util.Map;

public class MockUserService extends UserService {
    public MockUserService(IsolatedUserRepository isolatedUserRepository) {
        super(isolatedUserRepository);
    }

    private final Map<User, Double> lbCoinsBalance = new HashMap<>();

    @Override
    public void setLbCoinsBalance(User user, double amount) {
        lbCoinsBalance.put(user, amount);
    }

    @Override
    public double getLbCoinsBalance(User user) {
        return lbCoinsBalance.getOrDefault(user, 0.0);
    }
}
