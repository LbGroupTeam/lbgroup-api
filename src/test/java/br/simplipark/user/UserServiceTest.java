package br.simplipark.user;

import br.simplipark.user.isolated.IsolatedUser;
import br.simplipark.user.isolated.IsolatedUserRepository;
import br.simplipark.util.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Ensures that the database is rolled back after each test
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private IsolatedUserRepository isolatedUserRepository;

    @BeforeEach
    public void setUp() {
        IsolatedUser user1 = new IsolatedUser();
        user1.setIdUsu(1);
        user1.setCpfUsu(Util.parseCpfToLong("12345678909"));
        user1.setLbCoinsUsu(10.0f);
        isolatedUserRepository.save(user1);

        IsolatedUser user2 = new IsolatedUser();
        user2.setIdUsu(2);
        user2.setCpfUsu(Util.parseCpfToLong("98765432100"));
        user2.setLbCoinsUsu(20.0f);
        isolatedUserRepository.save(user2);
    }

    @Test
    public void testGetUserById() {
        User user = userService.getUserById(1L);
        assertNotNull(user);
        assertEquals(1L, user.id());
        assertEquals("12345678909", user.cpf());
    }

    @Test
    public void testGetUserByCpf() {
        User user = userService.getUserByCpf("12345678909");
        assertNotNull(user);
        assertEquals(1L, user.id());

        User nonExistentUser = userService.getUserByCpf("00000000000");
        assertNull(nonExistentUser);
    }

    @Test
    public void testSetLbCoinsBalance() {
        userService.setLbCoinsBalance(userService.getUserById(1L), 50.0);
        assertEquals(50.0, userService.getLbCoinsBalance(userService.getUserById(1L)));
    }

    @Test
    public void testGetLbCoinsBalance() {
        double balance = userService.getLbCoinsBalance(userService.getUserById(1L));
        assertEquals(10.0, balance);
    }
}
