package br.simplipark.payment;

import br.simplipark.payment.model.Payment;
import br.simplipark.payment.model.PaymentReason;
import br.simplipark.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserIdAndStatus(long userId, PaymentStatus status);
    List<Payment> findAllByReasonAndReasonDataIn(PaymentReason reason, List<String> reasonData);
}
