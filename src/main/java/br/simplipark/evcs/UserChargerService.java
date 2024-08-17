package br.simplipark.evcs;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.evcs.model.Charger;
import br.simplipark.payment.PaymentService;
import br.simplipark.payment.isolated.PricingRecordRepository;
import br.simplipark.payment.model.Payment;
import br.simplipark.payment.model.PaymentReason;
import br.simplipark.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserChargerService {
    private final ChargerService chargerService;
    private final PaymentService paymentService;
    private final ChargingDataRelationsService chargingDataRelationsService;
    private final PricingRecordRepository pricingRecordRepository;

    private final Map<User, Long> userCurrentChargingDataIds = new HashMap<>();

    public UserChargerService(ChargerService chargerService, PaymentService paymentService, ChargingDataRelationsService chargingDataRelationsService, PricingRecordRepository pricingRecordRepository) {
        this.chargerService = chargerService;
        this.paymentService = paymentService;
        this.chargingDataRelationsService = chargingDataRelationsService;
        this.pricingRecordRepository = pricingRecordRepository;
    }

    public boolean startCharging(User user, Charger charger, Runnable onStopChargingAutomatically) {
        log.info("User [{}] is attempting to start charging on Charger [{}].", user.id(), charger.name());

        if (userCurrentChargingDataIds.get(user) != null) {
            log.warn("User [{}] is already charging. Cannot start a new session.", user.id());
            throw new IllegalStateException("User is already charging");
        }

        var chargingData = chargerService.startCharging(charger);
        if (chargingData == null) {
            log.error("Failed to start charging for User [{}] on Charger [{}].", user.id(), charger.name());
            return false;
        }

        chargingData = chargingDataRelationsService.saveChargingData(chargingData);
        chargingDataRelationsService.createRelationBetweenUserAndChargingData(user, chargingData);

        log.info("Charging session started for User [{}] on Charger [{}]. ChargingData ID: [{}]", user.id(), charger.name(), chargingData.getId());

        userCurrentChargingDataIds.put(user, chargingData.getId());

        chargerService.onStopChargingAutomatically(charger, chargingDataWithNewValues -> {
            log.info("Automatic stop detected for Charger [{}]. Updating charging session for User [{}].", charger.name(), user.id());
            handleChargeStopped(user, charger.owner(), chargingDataWithNewValues);

            log.info("Running onStopChargingAutomatically callback for User [{}].", user.id());
            onStopChargingAutomatically.run();
        });

        return true;
    }

    public void stopCharging(User user) {
        log.info("User [{}] is attempting to stop charging.", user.id());

        var chargingDataId = userCurrentChargingDataIds.get(user);
        if (chargingDataId == null) {
            log.warn("User [{}] is not charging. Cannot stop the session.", user.id());
            throw new IllegalStateException("User is not charging");
        }

        var chargerId = chargingDataRelationsService.findChargerIdByChargingDataId(chargingDataId);
        var charger = chargerService.findChargerById(chargerId);

        var chargingDataWithUpdatedValues = chargerService.stopCharging(charger);

        log.info("Charging session stopped for User [{}] on Charger [{}]. Updating charging data.", user.id(), charger.name());
        handleChargeStopped(user, charger.owner(), chargingDataWithUpdatedValues);
    }

    private void handleChargeStopped(User user, String chargerOwner, ChargingData chargingDataWithUpdatedMeasurements) {
        log.info("Handling charge stop for User [{}] with ChargingData ID [{}].", user.id(), chargingDataWithUpdatedMeasurements.getId());

        var chargingDataId = userCurrentChargingDataIds.get(user);
        var chargingData = chargingDataRelationsService.updateChargingDataWithNewMeasurements(chargingDataId, chargingDataWithUpdatedMeasurements);

        log.info("Charging data updated for User [{}]. Removing from active sessions.", user.id());
        userCurrentChargingDataIds.remove(user);

        addDuePayment(user, chargerOwner, chargingData);
    }

    private void addDuePayment(User user, String chargerOwner, ChargingData chargingData) {
        log.info("Adding due payment for User [{}] with ChargingData ID [{}].", user.id(), chargingData.getId());

        Payment payment = new Payment();
        payment.setUserId(user.id());

        double cost = calculateChargingCost(chargingData, chargerOwner, user);

        log.info("Calculated cost [{}] for User [{}] with ChargingData ID [{}].", cost, user.id(), chargingData.getId());

        payment.setAmount(cost);
        payment.setReason(PaymentReason.EV_CHARGE);
        payment.setReasonData(String.valueOf(chargingData.getId()));

        paymentService.addPayment(payment);
        log.info("Payment added for User [{}] with ChargingData ID [{}].", user.id(), chargingData.getId());
    }

    private double calculateChargingCost(ChargingData chargingData, String chargerOwner, User user) {
        log.info("Calculating charging cost for User [{}] with ChargingData ID [{}].", user.id(), chargingData.getId());

        var pricingRecord = pricingRecordRepository.findByTypeClientPricesAndOwnerPrices(user.type(), chargerOwner);
        var costPerKwh = pricingRecord.getMultiplicatorPrices();

        double cost = costPerKwh * chargingData.getEnergyDeliveredInKWh();
        log.info("Cost calculated: [{}] for User [{}].", cost, user.id());

        return cost;
    }
}
