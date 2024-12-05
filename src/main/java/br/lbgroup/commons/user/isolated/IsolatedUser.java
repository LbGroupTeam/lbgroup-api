package br.lbgroup.commons.user.isolated;

import br.lbgroup.commons.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "usu")
public class IsolatedUser {
    @Id
    private int idUsu;

    private long cpfUsu;
    private String nomeUsu;
    private String emailUsu;
    private String pfpUsu;
    private String pfpUsuId;
    private byte[] pfpUsuImg;
    private LocalDate nascUsu;
    private String ruaUsu;
    private int numCasaUsu;
    private String bairroUsu;
    private String cidadeUsu;
    private String estadoUsu;
    private long cepUsu;
    private long telefoneUsu;
    private String accTypeUsu;
    private String statusUsu;
    private String situacaoUsu;
    private String senhaUsu;
    private String salt;
    private String complementoUsu;
    private String recoverUsu;
    private double lbCoinsUsu;

    public User toUser() {
        return new User(idUsu, String.valueOf(cpfUsu), nomeUsu, accTypeUsu);
    }
}
