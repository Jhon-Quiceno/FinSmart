package com.smartfinance.backend.extractos.service.extraction;

import com.smartfinance.backend.extractos.exception.StatementExtractionException;
import com.smartfinance.backend.extractos.exception.StatementPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class PdfStatementTextExtractorTest {

    private final PdfStatementTextExtractor extractor = new PdfStatementTextExtractor();

    @Test
    void supportsShouldReturnTrueForPdfExtension() {
        Assertions.assertTrue(extractor.supports("extracto.pdf"));
        Assertions.assertTrue(extractor.supports("EXTRACTO.PDF"));
    }

    @Test
    void supportsShouldReturnFalseForOtherExtensions() {
        Assertions.assertFalse(extractor.supports("extracto.csv"));
        Assertions.assertFalse(extractor.supports(null));
    }

    @Test
    void extractShouldReturnTextFromUnprotectedPdf() throws IOException {
        byte[] pdfBytes = buildPdf("Compra supermercado 100", null);

        String result = extractor.extract(pdfBytes, null);

        Assertions.assertTrue(result.contains("Compra supermercado 100"));
    }

    @Test
    void extractShouldReturnTextFromPasswordProtectedPdfWithCorrectPassword() throws IOException {
        byte[] pdfBytes = buildPdf("Compra supermercado 100", "secreta123");

        String result = extractor.extract(pdfBytes, "secreta123");

        Assertions.assertTrue(result.contains("Compra supermercado 100"));
    }

    @Test
    void extractShouldThrowStatementPasswordExceptionWhenPasswordIsWrong() throws IOException {
        byte[] pdfBytes = buildPdf("Compra supermercado 100", "secreta123");

        Assertions.assertThrows(
                StatementPasswordException.class, () -> extractor.extract(pdfBytes, "contrasena-incorrecta")
        );
    }

    @Test
    void extractShouldThrowStatementPasswordExceptionWhenPasswordIsMissing() throws IOException {
        byte[] pdfBytes = buildPdf("Compra supermercado 100", "secreta123");

        Assertions.assertThrows(StatementPasswordException.class, () -> extractor.extract(pdfBytes, null));
    }

    @Test
    void extractShouldThrowStatementExtractionExceptionForCorruptedContent() {
        byte[] invalidContent = "not a real pdf file".getBytes();

        Assertions.assertThrows(StatementExtractionException.class, () -> extractor.extract(invalidContent, null));
    }

    private static byte[] buildPdf(String text, String password) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            if (password != null) {
                AccessPermission accessPermission = new AccessPermission();
                StandardProtectionPolicy protectionPolicy =
                        new StandardProtectionPolicy(password, password, accessPermission);
                protectionPolicy.setEncryptionKeyLength(128);
                document.protect(protectionPolicy);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
