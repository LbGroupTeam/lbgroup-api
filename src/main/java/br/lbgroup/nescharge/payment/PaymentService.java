package br.lbgroup.nescharge.payment;

import br.lbgroup.nescharge.payment.currency.LBCoinsConverter;
import br.lbgroup.nescharge.payment.exceptions.UnsuficientBalanceException;
import br.lbgroup.nescharge.payment.model.*;
import br.lbgroup.commons.user.User;
import br.lbgroup.commons.user.UserService;
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

    public String createLinkForCreditCardPayment(User user, Consumer<CompletedPayment> onOutcome, List<Payment> payments) {
        log.info("Creating credit card payment link for User [{}].", user.id());

        var paymentsFromDatabase = fetchPaymentsFromDatabase(payments);

        double totalCost = calculateTotalCost(payments);
        log.info("Total cost for credit card payment: [{}] for User [{}].", totalCost, user.id());

        String paymentLink = paymentGateway.createLinkForPayment(user, totalCost);
        log.info("Payment link created for User [{}]: [{}]", user.id(), paymentLink);

        Consumer<CompletedPayment> onOutcomeWithDatabaseUpdate = (payment) -> {
            if (payment.outcome() != PaymentOutcome.ACCEPTED) {
                log.warn("Payment not accepted for User [{}]. Outcome: [{}]", user.id(), payment);

                onOutcome.accept(payment);

                return;
            }

            log.info("Payment accepted for User [{}]. Setting all payments as complete.", user.id());
            setAllPaymentsAsComplete(paymentsFromDatabase);

            onOutcome.accept(payment);
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

    public String createLinkForLBCoinsPurchase(User user, Consumer<CompletedPayment> onOutcome) {
        log.info("Creating link for LB Coins purchase for User [{}]", user.id());
        String paymentLink = paymentGateway.createLinkForLBCoinsPurchase(user);

        paymentGateway.onPaymentOutcome(user, (payment) -> {
            if (payment.outcome() != PaymentOutcome.ACCEPTED) {
                log.warn("Payment not accepted for LB Coins purchase for User [{}]. Outcome: [{}]", user.id(), payment);

                onOutcome.accept(payment);

                return;
            }

            double totalLbCoinsPurchased = LBCoinsConverter.convertBRLToLBCoins(payment.amountPurchased());

            log.info("Payment accepted for LB Coins purchase for User [{}]. Updating balance. ", user.id());
            log.info("Total LB Coins purchased: {}. Total BRL purchased: {}", totalLbCoinsPurchased, payment.amountPurchased());

            userService.setLbCoinsBalance(user, userService.getLbCoinsBalance(user) + totalLbCoinsPurchased);

            onOutcome.accept(payment);
        });

        return paymentLink;
    }

    public boolean hasPendingPayment(User user) {
        log.info("Checking for pending payments for User [{}].", user.id());
        boolean hasPending = !fetchPendingPayments(user).isEmpty();
        log.debug("Pending payments check result for User [{}]: [{}]", user.id(), hasPending);
        return hasPending;
    }

    public List<Payment> fetchPendingPayments(User user) {
        log.info("Fetching pending payments for User [{}].", user.id());
        return paymentRepository.findByUserIdAndStatus(user.id(), PaymentStatus.PENDING);
    }

    public Payment addPayment(Payment payment) {
        log.info("Adding new payment for User [{}]. Amount: [{}], Reason: [{}]", payment.getUserId(), payment.getAmount(), payment.getReason());

        if (payment.getAmount() < 0) {
            log.error("Invalid payment amount for User [{}]. Amount: [{}]", payment.getUserId(), payment.getAmount());
            throw new IllegalArgumentException("Invalid payment amount: " + payment.getAmount());
        }

        payment.setStatus(PaymentStatus.PENDING);

        payment = paymentRepository.save(payment);
        log.info("Payment added successfully for User [{}].", payment.getUserId());

        return payment;
    }

    public List<Payment> fetchPaymentsByReasonIdIn(PaymentReason reason, List<String> reasonIds) {
        log.debug("Fetching payments by reason ID: [{}] and reason [{}]", reasonIds, reason);

        return paymentRepository.findAllByReasonAndReasonDataIn(reason, reasonIds);
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