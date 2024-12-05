package br.lbgroup.commons.util.files;

import br.lbgroup.commons.user.reporting.HistoricalChargingData;
import br.lbgroup.commons.user.reporting.ReportData;
import br.lbgroup.commons.user.reporting.SummedUpReportData;
import br.lbgroup.commons.util.FormatingUtils;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static br.lbgroup.commons.util.FormatingUtils.roundToTwoDecimals;

@Slf4j
@Service
public class PdfReportGenerator {
    private final Map<String, byte[]> pdfs = new HashMap<>();

    private static final Color GREEN_HEADER_COLOR = new DeviceRgb(146, 208, 80);

    public MessageableFile generatePdfReport(ReportData data) {
        String id = UUID.randomUUID().toString();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Document document = createDocument(byteArrayOutputStream);

        document.add(createTitle());
        document.add(createHeader(data.username()));

        document.add(createTableWithChargingData(data.historicalChargingData(), data.sumUp()));

        document.close();

        pdfs.put(id, byteArrayOutputStream.toByteArray());

        log.info("PDF with ID [{}] generated successfully, for username [{}]", id, data.username());

        return new MessageableFile(id, "historico-cargas.pdf");
    }

    public byte[] getPdfById(String id) {
        byte[] bytes = pdfs.get(id);
        if (bytes == null) {
            log.warn("PDF with ID [{}] not found. Returning default PDF. " +
                     "Ids in memory: {}", id, pdfs.keySet());

            return createDefaultPdf();
        }

        return bytes;
    }

    private static Paragraph createTitle() {
        return new Paragraph("Histórico de cargas")
                .setFont(getBoldFont())
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Paragraph createHeader(String username) {
        var text = "Data de emissão: " + FormatingUtils.formatDate(LocalDateTime.now(), "MM/yyyy") + "\n" +
                   "Cliente: " + username;

        return new Paragraph(text);
    }

    private Table createTableWithChargingData(List<HistoricalChargingData> historicalChargingData, SummedUpReportData summedUpReportData) {
        Table table = new Table(5);

        table.useAllAvailableWidth();

        table.setTextAlignment(TextAlignment.CENTER);

        table.addCell("Horário de início");
        table.addCell("Horário de parada");
        table.addCell("Total (kWh)");
        table.addCell("Custo (R$)");
        table.addCell("Custo (LB Coins)");

        table.setAutoLayout();

        for (HistoricalChargingData data : historicalChargingData) {
            table.addCell(FormatingUtils.formatDateShort(data.startedAt()));
            table.addCell(FormatingUtils.formatDateShort(data.stoppedAt()));
            table.addCell(roundToTwoDecimals(data.energyDeliveredInKWh()));
            table.addCell(roundToTwoDecimals(data.costInBrl()));
            table.addCell(roundToTwoDecimals(data.costInLbCoins()));
        }

        addSumRow(summedUpReportData, table);

        iterateThroughTable(table, cell -> {
            cell.setBorder(new SolidBorder(DeviceRgb.WHITE, 1.5F));

            if (cell.getRow() == 0 || cell.getRow() == table.getNumberOfRows() - 1) {
                cell.setFont(getBoldFont());
            }

            setCellBackgroundColor(cell, table.getNumberOfRows());
        });

        return table;
    }

    private static void addSumRow(SummedUpReportData summedUpReportData, Table table) {
        table.addCell("");
        table.addCell("Total");
        table.addCell(roundToTwoDecimals(summedUpReportData.totalEnergyDelivered()));
        table.addCell(roundToTwoDecimals(summedUpReportData.totalCostInBrl()));
        table.addCell(roundToTwoDecimals(summedUpReportData.totalCostInLbCoins()));
    }

    private static void setCellBackgroundColor(Cell cell, int numberOfRows) {
        Color color = cell.getRow() % 2 == 0 ? new DeviceRgb(217, 217, 217) : new DeviceRgb(242, 242, 242);

        if (cell.getRow() == 0) {
            color = GREEN_HEADER_COLOR;
        }

        if (cell.getRow() == numberOfRows - 1) {
            color = DeviceRgb.WHITE;
        }

        if (cell.getRow() == numberOfRows - 1 && cell.getCol() >= 1) {
            color = GREEN_HEADER_COLOR;
        }

        cell.setBackgroundColor(color);
    }

    private void iterateThroughTable(Table table, Consumer<Cell> consumer) {
        for (int row = 0; row < table.getNumberOfRows(); row++) {
            for (int column = 0; column < table.getNumberOfColumns(); column++) {
                var cell = table.getCell(row, column);
                if (cell == null) {
                    continue;
                }

                consumer.accept(cell);
            }
        }
    }

    private byte[] createDefaultPdf() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Document document = createDocument(byteArrayOutputStream);

        document.add(new Paragraph("Ocorreu um erro ao gerar o seu relatório. Por favor, tente novamente mais tarde."));

        document.close();

        return byteArrayOutputStream.toByteArray();
    }

    private static Document createDocument(ByteArrayOutputStream byteArrayOutputStream) {
        PdfWriter writer = new PdfWriter(byteArrayOutputStream);

        PdfDocument pdf = new PdfDocument(writer);
        var document = new Document(pdf);

        document.setFont(getDefaultFont());

        return document;
    }

    private static PdfFont getBoldFont() {
        try {
            return PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static PdfFont getDefaultFont() {
        try {
            return PdfFontFactory.createFont(StandardFonts.HELVETICA);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
