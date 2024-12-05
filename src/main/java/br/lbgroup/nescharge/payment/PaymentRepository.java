package br.lbgroup.nescharge.payment;

import br.lbgroup.nescharge.payment.model.Payment;
import br.lbgroup.nescharge.payment.model.PaymentReason;
import br.lbgroup.nescharge.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserIdAndStatus(long userId, PaymentStatus status);
    List<Payment> findAllByReasonAndReasonDataIn(PaymentReason reason, List<String> reasonData);
}
