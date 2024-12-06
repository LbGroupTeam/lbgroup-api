package br.lbgroup.nescharge.user;

import br.lbgroup.commons.user.User;
import br.lbgroup.commons.user.UserService;
import br.lbgroup.commons.user.isolated.IsolatedUser;
import br.lbgroup.commons.user.isolated.IsolatedUserRepository;
import br.lbgroup.commons.util.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
        IsolatedUser user1 = createSampleIsolatedUser(1, "12345678909", 10.0f);
        isolatedUserRepository.save(user1);

        IsolatedUser user2 = createSampleIsolatedUser(2, "98765432100", 20.0f);
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

    private static IsolatedUser createSampleIsolatedUser(int idUsu, String number, float lbCoinsUsu) {
        IsolatedUser user = new IsolatedUser();
        user.setIdUsu(idUsu);
        user.setCpfUsu(Util.parseCpf(number));
        user.setLbCoinsUsu(lbCoinsUsu);
        user.setComplementoUsu("Teste");
        user.setNumCasaUsu(0);
        user.setNascUsu(LocalDate.now());
        user.setSenhaUsu("");
        user.setSalt("");
        return user;
    }
}
