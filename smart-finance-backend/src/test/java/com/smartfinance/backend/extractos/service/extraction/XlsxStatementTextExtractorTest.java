package com.smartfinance.backend.extractos.service.extraction;

import com.smartfinance.backend.extractos.exception.StatementExtractionException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class XlsxStatementTextExtractorTest {

    private final XlsxStatementTextExtractor extractor = new XlsxStatementTextExtractor();

    @Test
    void supportsShouldReturnTrueForXlsxExtension() {
        Assertions.assertTrue(extractor.supports("extracto.xlsx"));
        Assertions.assertTrue(extractor.supports("EXTRACTO.XLSX"));
    }

    @Test
    void supportsShouldReturnFalseForOtherExtensions() {
        Assertions.assertFalse(extractor.supports("extracto.csv"));
        Assertions.assertFalse(extractor.supports(null));
    }

    @Test
    void extractShouldReturnTabSeparatedTextForEachRow() throws IOException {
        byte[] workbookBytes = buildWorkbook();

        String result = extractor.extract(workbookBytes, null);

        Assertions.assertTrue(result.contains("Fecha\tDescripcion\tMonto"));
        Assertions.assertTrue(result.contains("2026-06-01\tCompra supermercado\t100"));
    }

    @Test
    void extractShouldThrowStatementExtractionExceptionForCorruptedContent() {
        byte[] invalidContent = "not a real xlsx file".getBytes();

        Assertions.assertThrows(StatementExtractionException.class, () -> extractor.extract(invalidContent, null));
    }

    private static byte[] buildWorkbook() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Movimientos");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Fecha");
            header.createCell(1).setCellValue("Descripcion");
            header.createCell(2).setCellValue("Monto");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026-06-01");
            row.createCell(1).setCellValue("Compra supermercado");
            row.createCell(2).setCellValue(100);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
