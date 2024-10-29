package br.simplipark.chatbot.e2e.integrations;

import br.simplipark.test.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@Service
public class PaymentTestDispatcher {

    private static final String PAYMENT_ENDPOINT = "/stripe-webhook";

    private static final String LB_COINS_PURCHASE_SIGNATURE = "t=1730243660,v1=73f5944004a352f916ed0e103339be24c45ea63ee1a757199d5fa151722325d1,v0=7b1e20ac937fd49ee4b98012feceb11cdf0b8aeed2b7ef658240ff8da1419868";
    private static final String CREDIT_CARD_SIGNATURE = "t=1730176979,v1=2d60713bc9589c9c56fa6e7ab2a09a019dec808b8ec2dc68167909707d43b6df,v0=35925508ee1252681684fc1091f832b05eb8721adab1ac08605525830c08775a";

    @Autowired
    private MockMvc mockMvc;

    public PaymentTestDispatcher() {
        System.setProperty("stripe.disable_signature_check", "true");
    }

    public void simulateSucessfullCreditCardPayment() throws Exception {
        performHttpRequest("credit_card_payment_complete.json", CREDIT_CARD_SIGNATURE);
    }
    
    public void simulateSucessfullLbCoinsPurchase() throws Exception {
        performHttpRequest("lb_coins_purchase_complete.json", LB_COINS_PURCHASE_SIGNATURE);
    }

    private void performHttpRequest(String fileName, String signature) throws Exception {
        String jsonBody = TestUtils.readFileFromResources("stripe/" + fileName);

        mockMvc.perform(post(PAYMENT_ENDPOINT)
                        .header("Stripe-Signature", signature)
                        .contentType("application/json")
                        .content(jsonBody))
                .andExpect(status().isOk());
    }
}
