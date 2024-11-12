package br.simplipark.evcs;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.evcs.model.ChargeEvent;
import br.simplipark.evcs.model.Charger;
import br.simplipark.payment.PaymentService;
import br.simplipark.payment.isolated.PricingRecordRepository;
import br.simplipark.payment.model.Payment;
import br.simplipark.payment.model.PaymentReason;
import br.simplipark.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class UserChargerService {
    private final ChargerService chargerService;
    private final PaymentService paymentService;
    private final ChargingDataRelationsService chargingDataRelationsService;
    private final PricingRecordRepository pricingRecordRepository;

    private final Map<User, Long> userCurrentChargingDataIds = new HashMap<>();

    private final List<Consumer<ChargeEvent>> listeners = new ArrayList<>();

    public UserChargerService(ChargerService chargerService, PaymentService paymentService, ChargingDataRelationsService chargingDataRelationsService, PricingRecordRepository pricingRecordRepository) {
        this.chargerService = chargerService;
        this.paymentService = paymentService;
        this.chargingDataRelationsService = chargingDataRelationsService;
        this.pricingRecordRepository = pricingRecordRepository;
    }

    public boolean startCharging(User user, Charger charger, Runnable onStopChargingAutomatically) {
        log.info("User [{}] is attempting to start charging on Charger [{}].", user.id(), charger.name());

        if (userCurrentChargingDataIds.get(user) != null) {
            throw new IllegalStateException("User " + user.id() + " is already charging. Cannot start a new session.");
        }

        var chargingData = startCharging(user, charger);
        if (chargingData == null) return false;

        registerAutoStopCallback(user, charger, onStopChargingAutomatically);

        log.debug("Notifying listeners about charge start for User [{}].", user.id());

        listeners.forEach(listener -> listener.accept(ChargeEvent.startedCharge(user, charger, chargingData)));

        return true;
    }

    public void stopCharging(User user) {
        log.info("User [{}] is attempting to stop charging.", user.id());

        var chargingDataId = userCurrentChargingDataIds.get(user);
        if (chargingDataId == null) {
            throw new IllegalStateException("User " + user.id() + " is not charging. Cannot stop the session.");
        }

        var chargerId = chargingDataRelationsService.findChargerIdByChargingDataId(chargingDataId);
        var charger = chargerService.findChargerById(chargerId);

        var chargingDataWithUpdatedValues = chargerService.stopCharging(charger);

        log.info("Charging session stopped for User [{}] on Charger [{}].", user.id(), charger.name());
        handleChargeStopped(user, charger, chargingDataWithUpdatedValues);
    }

    public void addListener(Consumer<ChargeEvent> listener) {
        listeners.add(listener);
    }

    private ChargingData startCharging(User user, Charger charger) {
        var chargingData = chargerService.startCharging(charger);
        if (chargingData == null) {
            log.error("Failed to start charging for User [{}] on Charger [{}].", user.id(), charger.name());
            return null;
        }

        chargingDataRelationsService.createRelationBetweenUserAndChargingData(user, chargingData);

        log.info("Charging session started for User [{}] on Charger [{}]. ChargingData ID: [{}]", user.id(), charger.name(), chargingData.getId());

        userCurrentChargingDataIds.put(user, chargingData.getId());
        return chargingData;
    }

    private void registerAutoStopCallback(User user, Charger charger, Runnable onStopChargingAutomatically) {
        chargerService.onStopChargingAutomatically(charger, chargingDataWithNewValues -> {
            log.info("Automatic stop detected for Charger [{}], for User [{}].", charger.name(), user.id());
            handleChargeStopped(user, charger, chargingDataWithNewValues);

            log.info("Running onStopChargingAutomatically callback for User [{}].", user.id());
            onStopChargingAutomatically.run();
        });
    }

    private void handleChargeStopped(User user, Charger charger, ChargingData chargingDataWithUpdatedMeasurements) {
        log.debug("Handling charge stop for User [{}] with ChargingData ID [{}].", user.id(), chargingDataWithUpdatedMeasurements.getId());

        var chargingDataId = userCurrentChargingDataIds.remove(user);
        if (chargingDataId == null) {
            throw new IllegalStateException("Charging data ID not found for User: " + user.id());
        }

        var chargingData = chargingDataRelationsService.updateChargingDataWithNewMeasurements(chargingDataId, chargingDataWithUpdatedMeasurements);

        log.debug("Charging data updated for User [{}]. Removed from active sessions.", user.id());

        var amountDue = addDuePayment(user, charger.owner(), chargingData).getAmount();

        log.debug("Notifying listeners about charge stop for User [{}].", user.id());

        listeners.forEach(listener -> listener.accept(ChargeEvent.stoppedCharge(user, charger, chargingData, amountDue)));
    }

    private Payment addDuePayment(User user, String chargerOwner, ChargingData chargingData) {
        log.debug("Adding due payment for User [{}] with ChargingData ID [{}].", user.id(), chargingData.getId());

        double cost = calculateChargingCost(chargingData, chargerOwner, user);

        Payment payment = new Payment(user.id(), cost, PaymentReason.EV_CHARGE, String.valueOf(chargingData.getId()));
        payment = paymentService.addPayment(payment);

        log.debug("Payment added for User [{}] with ChargingData ID [{}].", user.id(), chargingData.getId());

        return payment;
    }

    private double calculateChargingCost(ChargingData chargingData, String chargerOwner, User user) {
        log.debug("Calculating charging cost for User [{}] with ChargingData ID [{}].", user.id(), chargingData.getId());

        var pricingRecord = pricingRecordRepository.findByTypeClientPricesAndOwnerPrices(user.type(), chargerOwner);
        var costPerKwh = pricingRecord.getMultiplicatorPrices();

        log.debug("Pricing record [{}] found for User [{}] with ChargingData ID [{}].", pricingRecord, user.id(), chargingData.getId());

        double cost = costPerKwh * chargingData.getEnergyDeliveredInKWh();

        log.debug("Calculated cost [{}] for User [{}] with ChargingData ID [{}].", cost, user.id(), chargingData.getId());

        return cost;
    }
}
