package br.simplipark.util.files;

import br.simplipark.user.reporting.HistoricalChargingData;
import br.simplipark.user.reporting.ReportData;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PdfReportGeneratorTest {

    private final PdfReportGenerator pdfReportGenerator = new PdfReportGenerator();

    @Test
    void shouldGeneratePdfReportAndGetNonNullBytesTwice() {
        MessageableFile file = pdfReportGenerator.generatePdfReport(generateSampleData());
        String firstFileId = file.id();

        assertFileIsCorrect(file);

        byte[] pdfBytes = pdfReportGenerator.getPdfById(file.id());

        assertByteArrayIsCorrect(pdfBytes);


        file = pdfReportGenerator.generatePdfReport(generateSampleData());

        assertFileIsCorrect(file);

        assertNotEquals(firstFileId, file.id());

        pdfBytes = pdfReportGenerator.getPdfById(file.id());

        assertByteArrayIsCorrect(pdfBytes);
    }

    @Test
    void shouldGenerateDefaultPdfWhenIdIsNotFound() {
        pdfReportGenerator.generatePdfReport(generateSampleData());
        pdfReportGenerator.generatePdfReport(generateSampleData());

        byte[] pdfBytes = pdfReportGenerator.getPdfById("non-existing-id");

        assertByteArrayIsCorrect(pdfBytes);
    }

    private static void assertFileIsCorrect(MessageableFile file) {
        assertNotNull(file);
        assertNotNull(file.id());
    }

    private static void assertByteArrayIsCorrect(byte[] pdfBytes) {
        assertNotNull(pdfBytes);
        assertNotEquals(0, pdfBytes.length);
    }

    private ReportData generateSampleData() {
        var chargingData = new ArrayList<HistoricalChargingData>();

        for (int i = 0; i < 55; i++) {
            double energyDeliveredInWatts = 1000 * Math.random();
            double costBrl = energyDeliveredInWatts * 1.9;

            chargingData.add(new HistoricalChargingData(1000 * energyDeliveredInWatts, LocalDateTime.now(), LocalDateTime.now().plusHours(1), costBrl, costBrl * 9.3));
        }

        return new ReportData(chargingData, "Gilson Marchini Lourenço");
    }
}