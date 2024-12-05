package br.lbgroup.nescharge.evcs.model;

public record Address(String rua, String bairro, String cidade, String cep) {
    @Override
    public String toString() {
        return rua + ", " + bairro + ", " + cidade + " - " + cep.substring(0, 5) + "-" + cep.substring(5);
    }
}