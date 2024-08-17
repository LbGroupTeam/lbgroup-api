package br.simplipark.payment.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Payment {

    @Id
    @GeneratedValue
    private long id;

    private long userId;

    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentReason reason;

    private String reasonData;
}
