package br.lbgroup.crm.model;

import br.lbgroup.commons.user.isolated.IsolatedUser;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Entity
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private IsolatedUser user;

    @OneToMany
    private List<ContractEvent> events;

    private String description;

    private String company;

    private String product;

    private double value;

    private LocalDate startDate;
    private LocalDate stopDate;
}