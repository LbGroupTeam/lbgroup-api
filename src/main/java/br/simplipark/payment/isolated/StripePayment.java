package br.simplipark.payment.isolated;

import br.simplipark.payment.PaymentGateway;
import br.simplipark.payment.model.CompletedPayment;
import br.simplipark.payment.model.PaymentOutcome;
import br.simplipark.user.User;
import br.simplipark.util.Cryptographer;
import br.simplipark.util.Util;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.WebhookEndpointCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class StripePayment implements PaymentGateway {
    private final String webhookSecret;
    private final String lbCoinsPurchaseLink;

    private final boolean disableSignatureCheck;

    private final Map<String, Consumer<CompletedPayment>> callbacks = new HashMap<>();

    public StripePayment(@Value("${stripe.api_key}") String apiKey, @Value("${stripe.webhook_secret}") String webhookSecret, @Value("${stripe.lb_coins_purchase_link}") String lbCoinsPurchaseLink, @Value("${stripe.disable_signature_check:false}") boolean disableSignatureCheck) {
        Stripe.apiKey = apiKey;

        this.webhookSecret = webhookSecret;
        this.lbCoinsPurchaseLink = lbCoinsPurchaseLink;
        this.disableSignatureCheck = disableSignatureCheck;
    }

    @Override
    public String createLinkForPayment(User user, double amount) {
        try {
            var userId = String.valueOf(user.id());
            var url = createStripeSession(userId, amount).getUrl();

            log.info("Payment link created for user [{}]: {}", userId, url);
            return url;
        } catch (StripeException e) {
            throw new RuntimeException("Error creating payment link for user " + user.id(), e);
        }
    }

    @Override
    public void onPaymentOutcome(User user, Consumer<CompletedPayment> outcomeHandler) {
        callbacks.put(String.valueOf(user.id()), outcomeHandler);
        log.info("Callback registered for user [{}]", user.id());
    }

    @Override
    public String createLinkForLBCoinsPurchase(User user) {
        try {
            return lbCoinsPurchaseLink + "?client_reference_id=" + Cryptographer.encryptUrlSafe(String.valueOf(user.id()));
        } catch (Exception e) {
            throw new RuntimeException("Could not create link for LB Coins purchase for User: " + user, e);
        }
    }

    @PostMapping(value = "/stripe-webhook")
    public void receiveMessagePost(HttpEntity<String> request) {
        log.debug("Received request at Stripe webhook: {}", request.getBody());

        String signatureHeader = extractSignatureHeader(request);
        Event event = parseEventFromRequest(request.getBody(), signatureHeader);

        log.info("Received event: {}", event);

        var stripeEventDeserializer = event.getDataObjectDeserializer();
        if (stripeEventDeserializer.getObject().isEmpty()) {
            log.warn("No object found in the Stripe event");
            return;
        }

        var stripeObject = stripeEventDeserializer.getObject().get();

        if (!isRegistredCheckoutEvent(event)) {
            log.warn("Received unregistered event: {}", event.getType());
            return;
        }

        var session = (Session) stripeObject;
        handleCheckoutEvent(session, parseUserId(session));
    }

    private static String extractSignatureHeader(HttpEntity<String> request) {
        List<String> values = request.getHeaders().get("Stripe-Signature");
        if (values == null) {
            throw new RuntimeException("No Stripe-Signature header found in request");
        }

        return values.getFirst();
    }

    private Event parseEventFromRequest(String payload, String signatureHeader) {
        try {
            if (disableSignatureCheck) {
                return Webhook.constructEvent(payload, signatureHeader, webhookSecret, 0);
            }

            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Stripe event", e);
        }
    }

    private void handleCheckoutEvent(Session session, String userId) {
        var outcome = retrievePaymentOutcome(session);
        double amountPurchased = session.getAmountTotal() / 100.0;

        log.debug("Calling Stripe callback for user [{}]", userId);

        var callback = callbacks.remove(userId);
        if (callback == null) {
            log.warn("No callback found for user [{}]", userId);

            return;
        }

        Util.wrapRunnableWithTryCatch(() -> callback.accept(new CompletedPayment(amountPurchased, outcome))).run();

        log.debug("Callback called for user [{}]", userId);

        log.info("Payment outcome for user [{}]: {}", userId, outcome);
    }

    private PaymentOutcome retrievePaymentOutcome(Session session) {
        return switch (session.getStatus()) {
            case "complete" -> PaymentOutcome.ACCEPTED;
            case "open", "expired" -> PaymentOutcome.REJECTED;
            default -> throw new RuntimeException("Unknown payment status: " + session.getStatus());
        };
    }

    private static Session createStripeSession(String userId, double amount) throws StripeException {
        PriceCreateParams priceParams = PriceCreateParams.builder()
                .setCurrency("brl")
                .setUnitAmount(getAmountInCents(amount))
                .setProductData(
                        PriceCreateParams.ProductData.builder().setName("Recarga de carro elétrico").build()
                )
                .build();

        Price price = Price.create(priceParams);

        SessionCreateParams sessionParams = SessionCreateParams.builder()
                .setClientReferenceId(userId)
                .setSuccessUrl("https://fleet-magnetic-chigger.ngrok-free.app/payment-info.html")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(price.getId())
                                .setQuantity(1L)
                                .build()
                )
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .build();

        return Session.create(sessionParams);
    }

    private static Long getAmountInCents(double amount) {
        return (long) (amount * 100);
    }

    private boolean isRegistredCheckoutEvent(Event event) {
        for (WebhookEndpointCreateParams.EnabledEvent enabledEvent : getCheckoutEventsToRegister()) {
            if (event.getType().equals(enabledEvent.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static List<WebhookEndpointCreateParams.EnabledEvent> getCheckoutEventsToRegister() {
        return List.of(
                WebhookEndpointCreateParams.EnabledEvent.CHECKOUT__SESSION__COMPLETED,
                WebhookEndpointCreateParams.EnabledEvent.CHECKOUT__SESSION__EXPIRED
        );
    }

    private static String parseUserId(Session session) {
        if (Util.isNotInt(session.getClientReferenceId())) {
            try {
                return Cryptographer.decrypt(session.getClientReferenceId());
            } catch (Exception e) {
                throw new RuntimeException("Error decrypting user ID", e);
            }
        }

        return session.getClientReferenceId();
    }
}
