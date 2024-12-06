package br.lbgroup.crm;

import br.lbgroup.commons.user.User;
import br.lbgroup.commons.user.UserService;
import br.lbgroup.crm.model.ContractDTO;
import br.lbgroup.crm.model.ContractEventDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContractDTO> getContracts() {
        User loggedUser = UserService.getAuthenticatedUser().orElseThrow();
        return service.fetchContractsByUser(loggedUser).stream().map(ContractDTO::from).toList();
    }

    @GetMapping("/{id}/events")
    public List<ContractEventDTO> getContractEvents(@PathVariable long id) {
        return service.fetchContractById(id).getEvents().stream().map(ContractEventDTO::from).toList();
    }
}
