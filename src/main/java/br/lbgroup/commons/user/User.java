package br.lbgroup.commons.user;

public record User(long id, String cpf, String name, String type, String passwordHash, String passwordSalt) {
}
