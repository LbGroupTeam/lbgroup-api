package br.simplipark.user.reporting;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.payment.PaymentService;
import br.simplipark.payment.currency.LBCoinsConverter;
import br.simplipark.payment.model.Payment;
import br.simplipark.payment.model.PaymentReason;
import br.simplipark.user.User;
import br.simplipark.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportingService {

    private final UserService userService;
    private final PaymentService paymentService;
    private final ChargingDataRelationsService chargingDataRelationsService;

    public ReportingService(UserService userService, PaymentService paymentService, ChargingDataRelationsService chargingDataRelationsService) {
        this.userService = userService;
        this.paymentService = paymentService;
        this.chargingDataRelationsService = chargingDataRelationsService;
    }

    public Optional<ReportData> generateReportForUser(User user) {
        log.info("Generating report for user with ID: {}", user.id());

        String username = userService.getUserName(user);
        log.debug("Fetched username: {}", username);

        var chargingData = findChargingDataAtCurrentMonth(user);
        if (chargingData.isEmpty()) {
            log.info("No charging data found for the current month for user ID: {}", user.id());
            return Optional.empty();
        }

        log.debug("Found {} charging records for the current month for user ID: {}", chargingData.size(), user.id());

        var chargingDataIds = chargingData.stream().mapToLong(ChargingData::getId).mapToObj(Long::toString).toList();
        log.debug("Charging data IDs for payment retrieval: {}", chargingDataIds);

        var payments = fetchPayments(chargingDataIds);

        var historicalChargingData = createHistoricalChargingData(chargingData, payments);
        log.info("Historical charging data created with {} entries", historicalChargingData.size());

        return Optional.of(new ReportData(historicalChargingData, username));
    }

    private List<ChargingData> findChargingDataAtCurrentMonth(User user) {
        var list = chargingDataRelationsService.findChargingDataByUserId(user.id());

        var filteredList = list.stream()
                .filter(c -> c.getStartedAt().getMonth().equals(LocalDate.now().getMonth()))
                .sorted(Comparator.comparing(ChargingData::getStartedAt))
                .toList();

        log.debug("Filtered {} charging records for the current month for user ID: {}", filteredList.size(), user.id());
        return filteredList;
    }

    private List<HistoricalChargingData> createHistoricalChargingData(List<ChargingData> chargingDataList, List<Payment> payments) {
        log.info("Creating historical charging data");

        var paymentMap = payments.stream()
                .collect(Collectors.toMap(Payment::getReasonData, Function.identity(), (a, b) -> {
                    log.warn("More than one payment registered for the same charging data key found: {} and {}", a, b);
                    return a;
                }));

        List<HistoricalChargingData> list = new ArrayList<>();
        for (ChargingData data : chargingDataList) {
            log.debug("Processing charging data ID: {}", data.getId());

            Payment payment = paymentMap.getOrDefault(data.getId().toString(), new Payment());
            log.debug("Matched payment amount: {} for charging data ID: {}", payment.getAmount(), data.getId());

            double lbCoinsAmount = LBCoinsConverter.convertBRLToLBCoins(payment.getAmount());
            log.debug("Converted payment amount {} to LBCoins: {}", payment.getAmount(), lbCoinsAmount);

            list.add(HistoricalChargingData.fromChargingData(data, payment.getAmount(), lbCoinsAmount));
        }

        log.info("Completed creation of historical charging data with {} entries", list.size());
        return list;
    }

    private List<Payment> fetchPayments(List<String> chargingDataIds) {
        var payments = paymentService.fetchPaymentsByReasonIdIn(PaymentReason.EV_CHARGE, chargingDataIds);
        log.debug("Fetched {} payment records associated with {} reason", payments.size(), PaymentReason.EV_CHARGE);

        if (payments.size() != chargingDataIds.size()) {
            log.warn("Mismatch between charging data and payment records: {} charging data, {} payments.", chargingDataIds.size(), payments.size());
        }

        return payments;
    }
}
