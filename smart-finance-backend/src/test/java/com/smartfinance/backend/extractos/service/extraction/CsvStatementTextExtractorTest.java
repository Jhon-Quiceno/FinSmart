package com.smartfinance.backend.extractos.service.extraction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class CsvStatementTextExtractorTest {

    private final CsvStatementTextExtractor extractor = new CsvStatementTextExtractor();

    @Test
    void supportsShouldReturnTrueForCsvExtension() {
        Assertions.assertTrue(extractor.supports("extracto.csv"));
        Assertions.assertTrue(extractor.supports("EXTRACTO.CSV"));
    }

    @Test
    void supportsShouldReturnFalseForOtherExtensions() {
        Assertions.assertFalse(extractor.supports("extracto.pdf"));
        Assertions.assertFalse(extractor.supports(null));
    }

    @Test
    void extractShouldDecodeContentAsUtf8() {
        String csv = "fecha,descripcion,monto\n2026-06-01,Café,100";
        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        String result = extractor.extract(content, null);

        Assertions.assertEquals(csv, result);
    }
}
