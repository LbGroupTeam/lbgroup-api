package br.lbgroup.nescharge.sandbox;

import br.lbgroup.nescharge.payment.isolated.StripePayment;
import br.lbgroup.nescharge.test.TestUtils;
import br.lbgroup.commons.user.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Disabled
class StripePaymentSandboxTest {

    @Autowired
    StripePayment stripePayment;

    @Test
    void createLinkForPayment() {
        User user = TestUtils.createSampleUser();
        double amount = 0.5;
        String link = stripePayment.createLinkForPayment(user, amount);

        System.out.println(link);

        assertNotNull(link);
    }
}