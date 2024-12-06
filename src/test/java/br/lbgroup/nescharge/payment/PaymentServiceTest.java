package br.lbgroup.nescharge.payment;

import br.lbgroup.nescharge.payment.currency.LBCoinsConverter;
import br.lbgroup.nescharge.payment.exceptions.UnsuficientBalanceException;
import br.lbgroup.nescharge.payment.model.Payment;
import br.lbgroup.nescharge.payment.model.PaymentOutcome;
import br.lbgroup.nescharge.payment.model.PaymentStatus;
import br.lbgroup.nescharge.test.TestUtils;
import br.lbgroup.nescharge.test.mocks.MockPaymentGateway;
import br.lbgroup.nescharge.test.mocks.MockUserService;
import br.lbgroup.commons.user.User;
import br.lbgroup.commons.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
public class PaymentServiceTest {

	@Autowired
	private PaymentRepository paymentRepository;

	private final UserService userService = new MockUserService(null);

	private PaymentService paymentService;

	private User testUser;

	private final static String PAYMENT_LINK = "http://mock-payment-link.com/pay";

	private final MockPaymentGateway paymentGateway = new MockPaymentGateway(PAYMENT_LINK);

	@BeforeEach
	public void setUp() {
		paymentService = new PaymentService(paymentGateway, userService, paymentRepository);

		testUser = TestUtils.createSampleUser();
	}

	@Test
	public void testAddingPaymentThenPayingWithCreditCard() {
		Payment payment = createPayment(100.0);
        payment = paymentService.addPayment(payment);

		assertEquals(PaymentStatus.PENDING, paymentRepository.findById(payment.getId()).orElseThrow().getStatus());

		AtomicBoolean paymentOutcomeHandled = new AtomicBoolean(false);

		String link = paymentService.createLinkForCreditCardPayment(testUser, (completedPayment) -> {
			assertEquals(PaymentOutcome.ACCEPTED, completedPayment.outcome());
			paymentOutcomeHandled.set(true);
		}, List.of(payment));

		assertNotNull(link);
		assertTrue(link.contains(PAYMENT_LINK));

		paymentGateway.triggerPaymentOutcomeAccepted();

		assertTrue(paymentOutcomeHandled.get(), "Payment outcome was not handled");
		assertEquals(PaymentStatus.COMPLETED, paymentRepository.findById(payment.getId()).orElseThrow().getStatus());
	}

	@Test
	public void testAddingPaymentThenRejectingCreditCardPayment() {
		Payment payment = createPayment(100.0);
        payment = paymentService.addPayment(payment);

		assertEquals(PaymentStatus.PENDING, paymentRepository.findById(payment.getId()).orElseThrow().getStatus());

		AtomicBoolean paymentOutcomeHandled = new AtomicBoolean(false);

		String link = paymentService.createLinkForCreditCardPayment(testUser, (completedPayment) -> {
			assertEquals(PaymentOutcome.REJECTED, completedPayment.outcome());
			paymentOutcomeHandled.set(true);
		}, List.of(payment));

		assertNotNull(link);
		assertTrue(link.contains(PAYMENT_LINK));

		paymentGateway.triggerPaymentOutcomeRejected();

		assertTrue(paymentOutcomeHandled.get(), "Payment outcome was not handled");
		assertEquals(PaymentStatus.PENDING, paymentRepository.findById(payment.getId()).orElseThrow().getStatus());
	}

	@Test
	public void testAddingPaymentThenPayingWithLBCoins() throws UnsuficientBalanceException {
		Payment payment = createPayment(LBCoinsConverter.convertLBCoinsToBRL(15.0));
        payment = paymentService.addPayment(payment);

		userService.setLbCoinsBalance(testUser, 100.0);

		paymentService.handleLBCoinsPayment(testUser, List.of(payment));

		assertEquals(PaymentStatus.COMPLETED, paymentRepository.findById(payment.getId()).orElseThrow().getStatus());
		assertEquals(85.0, userService.getLbCoinsBalance(testUser)); // Balance after payment
	}

	@Test
	public void testHandleLBCoinsPaymentThrowsUnsuficientBalance() {
		Payment payment = createPayment(LBCoinsConverter.convertLBCoinsToBRL(200.0));
        payment = paymentService.addPayment(payment);

		userService.setLbCoinsBalance(testUser, 100.0);

        Payment finalPayment = payment;
        assertThrows(UnsuficientBalanceException.class, () -> paymentService.handleLBCoinsPayment(testUser, List.of(finalPayment)));
		assertEquals(PaymentStatus.PENDING, paymentRepository.findById(payment.getId()).orElseThrow().getStatus());
	}

	@Test
	public void testCreateLinkForLBCoinsPurchase() {
		AtomicBoolean paymentOutcomeHandled = new AtomicBoolean(false);

		assertEquals(0.0, userService.getLbCoinsBalance(testUser));

		String paymentLink = paymentService.createLinkForLBCoinsPurchase(testUser, (payment) -> {
			assertEquals(PaymentOutcome.ACCEPTED, payment.outcome());
			paymentOutcomeHandled.set(true);
		});

		assertNotNull(paymentLink);
		assertTrue(paymentLink.contains(PAYMENT_LINK));

		double lbCoinsAmountToAdd = 50.0;

		double amountToPurchase = LBCoinsConverter.convertLBCoinsToBRL(lbCoinsAmountToAdd);
		paymentGateway.triggerPaymentOutcome(amountToPurchase, PaymentOutcome.ACCEPTED);

		assertTrue(paymentOutcomeHandled.get(), "Payment outcome was not handled");
		assertEquals(lbCoinsAmountToAdd, userService.getLbCoinsBalance(testUser));
	}

	@Test
	public void testCreatingLinkForLBCoinsPurchaseThenRejecting() {
		AtomicBoolean paymentOutcomeHandled = new AtomicBoolean(false);

		assertEquals(0.0, userService.getLbCoinsBalance(testUser));

		String paymentLink = paymentService.createLinkForLBCoinsPurchase(testUser, (payment) -> {
			assertEquals(PaymentOutcome.REJECTED, payment.outcome());
			paymentOutcomeHandled.set(true);
		});

		assertNotNull(paymentLink);
		assertTrue(paymentLink.contains(PAYMENT_LINK));

		paymentGateway.triggerPaymentOutcomeRejected();

		assertTrue(paymentOutcomeHandled.get(), "Payment outcome was not handled");
		assertEquals(0.0, userService.getLbCoinsBalance(testUser)); // No balance change since the purchase was rejected
	}

	@Test
	public void testHasPendingPaymentAfterAddingPayment() {
		Payment payment = createPayment(10.0);
		paymentService.addPayment(payment);

		assertTrue(paymentService.hasPendingPayment(testUser));
	}

	@Test
	public void testAddZeroedPayment() {
		Payment invalidPayment = createPayment(0.0);

		assertDoesNotThrow(() -> paymentService.addPayment(invalidPayment));
	}

	@Test
	public void testAddInvalidPayment() {
		Payment invalidPayment = createPayment(-1);

		assertThrows(IllegalArgumentException.class, () -> paymentService.addPayment(invalidPayment));
	}

	private Payment createPayment(double amount) {
		Payment payment = new Payment();

		payment.setUserId(testUser.id());
		payment.setAmount(amount);

		return payment;
	}
}