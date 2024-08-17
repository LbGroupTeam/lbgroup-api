package br.simplipark.payment;

import br.simplipark.payment.currency.LBCoinsConverter;
import br.simplipark.payment.exceptions.UnsuficientBalanceException;
import br.simplipark.payment.model.Payment;
import br.simplipark.payment.model.PaymentOutcome;
import br.simplipark.payment.model.PaymentStatus;
import br.simplipark.user.User;
import br.simplipark.user.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentService {
    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final UserService userService;

    public PaymentService(PaymentGateway paymentGateway, UserService userService, PaymentRepository paymentRepository) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.userService = userService;
    }

    public String createLinkForCreditCardPayment(User user, Consumer<PaymentOutcome> onOutcome, List<Payment> payments) {
        log.info("Creating credit card payment link for User [{}].", user.id());

        var paymentsFromDatabase = fetchPaymentsFromDatabase(payments);

        double totalCost = calculateTotalCost(payments);
        log.info("Total cost for credit card payment: [{}] for User [{}].", totalCost, user.id());

        String paymentLink = paymentGateway.createLinkForPayment(user, totalCost);
        log.info("Payment link created for User [{}]: [{}]", user.id(), paymentLink);

        Consumer<PaymentOutcome> onOutcomeWithDatabaseUpdate = (outcome) -> {
            if (outcome == PaymentOutcome.ACCEPTED) {
                log.info("Payment accepted for User [{}]. Setting all payments as complete.", user.id());
                setAllPaymentsAsComplete(paymentsFromDatabase);
            } else {
                log.warn("Payment not accepted for User [{}]. Outcome: [{}]", user.id(), outcome);
            }

            onOutcome.accept(outcome);
        };

        paymentGateway.onPaymentOutcome(user, onOutcomeWithDatabaseUpdate);
        log.debug("Registered payment outcome listener for User [{}].", user.id());

        return paymentLink;
    }

    public void handleLBCoinsPayment(User user, List<Payment> payments) throws UnsuficientBalanceException {
        log.info("Handling LB Coins payment for User [{}].", user.id());

        var paymentsFromDatabase = fetchPaymentsFromDatabase(payments);

        double totalCost = LBCoinsConverter.convertBRLToLBCoins(calculateTotalCost(paymentsFromDatabase));
        log.info("Total cost in LB Coins: [{}] for User [{}].", totalCost, user.id());

        var currentBalance = userService.getLbCoinsBalance(user);
        log.debug("Current LB Coins balance for User [{}]: [{}]", user.id(), currentBalance);

        if (currentBalance < totalCost) {
            log.error("Insufficient LB Coins balance for User [{}]. Balance: [{}], Cost: [{}]", user.id(), currentBalance, totalCost);
            throw new UnsuficientBalanceException("Saldo insuficiente. Saldo de LB Coins: " + currentBalance + ". Custo da recarga: " + totalCost + " LB Coins.");
        }

        setAllPaymentsAsComplete(paymentsFromDatabase);
        log.info("All payments set as complete for User [{}].", user.id());

        userService.setLbCoinsBalance(user, currentBalance - totalCost);
        log.info("Updated LB Coins balance for User [{}] after payment.", user.id());
    }

    public String createLinkForLBCoinsPurchase(User user, double amount, Consumer<PaymentOutcome> onOutcome) {
        log.info("Creating link for LB Coins purchase for User [{}]. Amount: [{}]", user.id(), amount);

        double amountInBRL = LBCoinsConverter.convertLBCoinsToBRL(amount);
        log.info("Converted amount to BRL: [{}] for User [{}].", amountInBRL, user.id());

        String paymentLink = paymentGateway.createLinkForPayment(user, amountInBRL);
        log.info("Payment link created for LB Coins purchase for User [{}]: [{}]", user.id(), paymentLink);

        paymentGateway.onPaymentOutcome(user, (outcome) -> {
            if (outcome == PaymentOutcome.ACCEPTED) {
                log.info("Payment accepted for LB Coins purchase for User [{}]. Updating balance.", user.id());
                userService.setLbCoinsBalance(user, userService.getLbCoinsBalance(user) + amount);
            } else {
                log.warn("Payment not accepted for LB Coins purchase for User [{}]. Outcome: [{}]", user.id(), outcome);
            }

            onOutcome.accept(outcome);
        });

        return paymentLink;
    }

    public boolean hasPendingPayment(User user) {
        log.info("Checking for pending payments for User [{}].", user.id());
        boolean hasPending = !getPendingPayments(user).isEmpty();
        log.debug("Pending payments check result for User [{}]: [{}]", user.id(), hasPending);
        return hasPending;
    }

    public List<Payment> getPendingPayments(User user) {
        log.info("Fetching pending payments for User [{}].", user.id());
        return paymentRepository.findByUserIdAndStatus(user.id(), PaymentStatus.PENDING);
    }

    public void addPayment(Payment payment) {
        log.info("Adding new payment for User [{}]. Amount: [{}], Reason: [{}]", payment.getUserId(), payment.getAmount(), payment.getReason());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);
        log.info("Payment added successfully for User [{}].", payment.getUserId());
    }

    public static double calculateTotalCost(List<Payment> payments) {
        return payments.stream().mapToDouble(Payment::getAmount).sum();
    }

    private List<Payment> fetchPaymentsFromDatabase(List<Payment> payments) {
        log.debug("Fetching payments from database with IDs: [{}]", payments.stream().map(Payment::getId).toList());
        var fetchedPayments = paymentRepository.findAllById(payments.stream().map(Payment::getId).toList());
        log.debug("Fetched payments from database: {}", fetchedPayments);

        return fetchedPayments;
    }

    private void setAllPaymentsAsComplete(List<Payment> payments) {
        log.info("Setting all payments as complete.");
        payments.forEach(payment -> payment.setStatus(PaymentStatus.COMPLETED));
        paymentRepository.saveAll(payments);
        log.info("All payments have been set to completed.");
    }
}