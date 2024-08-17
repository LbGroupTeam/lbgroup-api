package br.simplipark.payment.isolated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingRecordRepository extends JpaRepository<PricingRecord, Long> {
    PricingRecord findByTypeClientPricesAndOwnerPrices(String typeClientPrices, String ownerPrices);
}
