package br.lbgroup.crm;

import  br.lbgroup.commons.user.User;
import br.lbgroup.crm.model.Contract;
import br.lbgroup.crm.model.ContractRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContractService {
    private final ContractRepository contractRepository;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public List<Contract> fetchContractsByUser(User user) {
        return contractRepository.findAllByUser_IdUsu(user.id());
    }

    public Contract fetchContractById(long id) {
        return contractRepository.findById(id).orElseThrow();
    }
}
