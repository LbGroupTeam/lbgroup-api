package br.lbgroup.commons.util.files;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FilesController {

    private final PdfReportGenerator pdfReportGenerator;

    public FilesController(PdfReportGenerator pdfReportGenerator) {
        this.pdfReportGenerator = pdfReportGenerator;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<ByteArrayResource> fetchPdf(@PathVariable String id) {
        var pdfFile = pdfReportGenerator.getPdfById(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfFile.length)
                .body(new ByteArrayResource(pdfFile));
    }
}