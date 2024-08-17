package br.simplipark.payment.isolated;

import br.simplipark.payment.PaymentGateway;
import br.simplipark.payment.model.PaymentOutcome;
import br.simplipark.user.User;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.WebhookEndpoint;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.WebhookEndpointCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class StripePayment implements PaymentGateway {
    private final Map<String, Session> userSessions = new HashMap<>();
    private final Map<String, Consumer<PaymentOutcome>> callbacks = new HashMap<>();

    public StripePayment(@Value("${stripe.api_key}") String apiKey) {
        Stripe.apiKey = apiKey;
    }

    @Override
    public String createLinkForPayment(User user, double amount) {
        try {
            var userId = String.valueOf(user.id());
            var session = createStripeSession(userId, amount);
            userSessions.put(userId, session);

            log.info("Payment link created for user [{}]: {}", userId, session.getUrl());
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Error creating payment link for user [{}]: {}", user.id(), e.getMessage());
            throw new RuntimeException("Erro ao criar link de pagamento");
        }
    }

    @Override
    public void onPaymentOutcome(User user, Consumer<PaymentOutcome> outcomeHandler) {
        callbacks.put(String.valueOf(user.id()), outcomeHandler);
        log.info("Callback registered for user [{}]", user.id());
    }

    @PostMapping(value = "/stripe-webhook")
    public void receiveMessagePost(HttpEntity<String> request) {
        Event event = parseEventFromRequest(request.getBody());

        log.info("Received event: {}", event);

        var stripeEventDeserializer = event.getDataObjectDeserializer();
        if (stripeEventDeserializer.getObject().isEmpty()) {
            log.warn("No object found in the Stripe event");
            return;
        }

        var stripeObject = stripeEventDeserializer.getObject().get();

        if (isRegistredCheckoutEvent(event)) {
            var session = (Session) stripeObject;
            handleCheckoutEvent(session.getClientReferenceId());
        }
    }

    private Event parseEventFromRequest(String payload) {
        try {
            return ApiResource.GSON.fromJson(payload, Event.class);
        } catch (Exception e) {
            log.error("Error parsing Stripe event: {}", e.getMessage());
            throw new RuntimeException("Erro ao parsear evento do Stripe");
        }
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

    private void handleCheckoutEvent(String userId) {
        if (!userSessions.containsKey(userId)) {
            log.warn("No session found for user [{}]", userId);
            return;
        }

        var sessionId = userSessions.get(userId).getId();

        try {
            var session = Session.retrieve(sessionId);

            var outcome = retrievePaymentOutcome(session);
            var callback = callbacks.get(userId);
            if (callback != null) {
                callback.accept(outcome);
                callbacks.remove(userId);
                log.info("Payment outcome for user [{}]: {}", userId, outcome);
            }
        } catch (StripeException e) {
            log.error("Error retrieving session for user [{}]: {}", userId, e.getMessage());
        }
    }

    private static Long getAmountInCents(double amount) {
        return (long) (amount * 100);
    }

    private PaymentOutcome retrievePaymentOutcome(Session session) {
        return switch (session.getStatus()) {
            case "complete" -> PaymentOutcome.ACCEPTED;
            case "open", "expired" -> PaymentOutcome.REJECTED;
            default -> {
                log.error("Unknown payment status: {}", session.getStatus());
                throw new RuntimeException("Status de pagamento desconhecido: " + session.getStatus());
            }
        };
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

    public static void main(String[] args) {
        Stripe.apiKey = "";

        WebhookEndpointCreateParams params =
                WebhookEndpointCreateParams.builder()
                        .setUrl("https://fleet-magnetic-chigger.ngrok-free.app/stripe-webhook")
                        .addAllEnabledEvent(getCheckoutEventsToRegister())
                        .build();

        try {

            WebhookEndpoint endpoint = WebhookEndpoint.create(params);
            System.out.println(endpoint);

        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

//        StripePayment stripePayment = new StripePayment("");
//
//        System.out.println(stripePayment.createLinkForPayment(new User(0, "owiemfiomew", "WOEIFM"), 0.50));
    }
}
