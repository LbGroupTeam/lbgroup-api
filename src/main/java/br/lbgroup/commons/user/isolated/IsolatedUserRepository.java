package br.lbgroup.commons.user.isolated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IsolatedUserRepository extends JpaRepository<IsolatedUser, Long> {
    IsolatedUser findByCpfUsu(long cpf);
}
