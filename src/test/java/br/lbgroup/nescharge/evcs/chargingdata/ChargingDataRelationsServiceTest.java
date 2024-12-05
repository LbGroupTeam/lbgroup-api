package br.lbgroup.nescharge.evcs.chargingdata;

import static org.junit.jupiter.api.Assertions.*;

import br.lbgroup.nescharge.evcs.chargingdata.relations.ChargepointChargingDataRelationRepository;
import br.lbgroup.nescharge.evcs.chargingdata.relations.UserChargingDataRelationRepository;
import br.lbgroup.nescharge.test.TestUtils;
import br.lbgroup.commons.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
@Transactional
public class ChargingDataRelationsServiceTest {

    @Autowired
    private ChargingDataRelationsService chargingDataRelationsService;

    @Autowired
    private ChargepointChargingDataRelationRepository chargepointChargingDataRelationRepository;

    @Autowired
    private UserChargingDataRelationRepository userChargingDataRelationRepository;

    private final User user = TestUtils.createSampleUser();

    @Test
    public void testFindChargingDataById() {
        ChargingData savedChargingData = chargingDataRelationsService.saveChargingData(createSampleChargingData());
        assertNotNull(savedChargingData.getId());

        ChargingData foundChargingData = chargingDataRelationsService.findChargingDataById(savedChargingData.getId());
        assertNotNull(foundChargingData);

        assertEquals(savedChargingData.getId(), foundChargingData.getId());
    }

    @Test
    public void testSaveChargingData() {
        ChargingData newChargingData = createSampleChargingData();

        ChargingData savedChargingData = chargingDataRelationsService.saveChargingData(newChargingData);
        assertNotNull(savedChargingData.getId());
        assertEquals(newChargingData.getEnergyDeliveredInWatts(), savedChargingData.getEnergyDeliveredInWatts());
    }

    @Test
    public void testUpdateChargingDataWithNewMeasurements() {
        ChargingData newChargingData = createSampleChargingData(200.0);

        ChargingData savedChargingData = chargingDataRelationsService.saveChargingData(newChargingData);
        assertNotNull(savedChargingData.getId());

        savedChargingData.setStoppedAt(LocalDateTime.now());
        savedChargingData.setEnergyDeliveredInWatts(250.0);

        ChargingData updatedChargingData = chargingDataRelationsService.updateChargingDataWithNewMeasurements(savedChargingData.getId(), savedChargingData);

        assertEquals(savedChargingData.getId(), updatedChargingData.getId());
        assertEquals(savedChargingData.getEnergyDeliveredInWatts(), updatedChargingData.getEnergyDeliveredInWatts());
    }

    @Test
    public void testCreateRelationBetweenChargerAndChargingData() {
        long chargerId = 1;

        ChargingData savedChargingData = chargingDataRelationsService.saveChargingData(createSampleChargingData());
        chargingDataRelationsService.createRelationBetweenChargerAndChargingData(chargerId, savedChargingData);

        var relations = chargepointChargingDataRelationRepository.findAll();
        assertFalse(relations.isEmpty());
        assertEquals(chargerId, relations.getFirst().getChargepointId());
        assertEquals(savedChargingData.getId(), relations.getFirst().getChargingDataId());
    }

    @Test
    public void testCreateRelationBetweenUserAndChargingData() {
        ChargingData savedChargingData = chargingDataRelationsService.saveChargingData(createSampleChargingData());
        chargingDataRelationsService.createRelationBetweenUserAndChargingData(user, savedChargingData);

        var relations = userChargingDataRelationRepository.findAll();
        assertFalse(relations.isEmpty());
        assertEquals(user.id(), relations.getFirst().getUserId());
        assertEquals(savedChargingData.getId(), relations.getFirst().getChargingDataId());
    }

    @Test
    public void testFindChargerIdByChargingDataId() {
        long chargerId = 1;

        ChargingData savedChargingData = chargingDataRelationsService.saveChargingData(createSampleChargingData());
        chargingDataRelationsService.createRelationBetweenChargerAndChargingData(chargerId, savedChargingData);

        long foundChargerId = chargingDataRelationsService.findChargerIdByChargingDataId(savedChargingData.getId());
        assertEquals(chargerId, foundChargerId);
    }

    @Test
    public void testFindChargingDataById_NotFound() {
        long nonExistentId = -1;

        Exception exception = assertThrows(RuntimeException.class, () -> chargingDataRelationsService.findChargingDataById(nonExistentId));

        String expectedMessage = "Charging data not found for ID: " + nonExistentId;
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    private static ChargingData createSampleChargingData() {
        return createSampleChargingData(100.0);
    }

    private static ChargingData createSampleChargingData(double energyDeliveredInWatts) {
        ChargingData newChargingData = new ChargingData();
        newChargingData.setStoppedAt(LocalDateTime.now());
        newChargingData.setEnergyDeliveredInWatts(energyDeliveredInWatts);
        return newChargingData;
    }
}
