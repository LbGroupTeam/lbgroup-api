package br.simplipark.monitoring.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChargingDataViewController {

    private final ChargingDataViewService service;

    public ChargingDataViewController(ChargingDataViewService service) {
        this.service = service;
    }

    @GetMapping("/charging-data")
    public String getChargingDataView(Model model) {
        model.addAttribute("chargingData", service.getChargingDataView());
        return "charging-data";
    }
}
