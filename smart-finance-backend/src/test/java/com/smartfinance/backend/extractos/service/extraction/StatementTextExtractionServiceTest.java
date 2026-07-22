package com.smartfinance.backend.extractos.service.extraction;

import com.smartfinance.backend.extractos.exception.EmptyStatementTextException;
import com.smartfinance.backend.extractos.exception.UnsupportedStatementFileException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementTextExtractionServiceTest {

    @Mock
    private StatementTextExtractor csvExtractor;

    @Mock
    private StatementTextExtractor pdfExtractor;

    private StatementTextExtractionService service;

    @BeforeEach
    void setUp() {
        service = new StatementTextExtractionService(List.of(pdfExtractor, csvExtractor));
    }

    @Test
    void extractTextShouldDispatchToTheExtractorThatSupportsTheFilename() {
        byte[] content = "contenido".getBytes();
        when(pdfExtractor.supports("extracto.csv")).thenReturn(false);
        when(csvExtractor.supports("extracto.csv")).thenReturn(true);
        when(csvExtractor.extract(content, null)).thenReturn("fecha,monto\n2026-06-01,100");

        String result = service.extractText("extracto.csv", content, null);

        Assertions.assertEquals("fecha,monto\n2026-06-01,100", result);
    }

    @Test
    void extractTextShouldThrowUnsupportedStatementFileExceptionWhenNoExtractorSupportsTheFilename() {
        when(pdfExtractor.supports("extracto.docx")).thenReturn(false);
        when(csvExtractor.supports("extracto.docx")).thenReturn(false);

        Assertions.assertThrows(
                UnsupportedStatementFileException.class,
                () -> service.extractText("extracto.docx", new byte[0], null)
        );
    }

    @Test
    void extractTextShouldThrowEmptyStatementTextExceptionWhenExtractedTextIsBlank() {
        when(pdfExtractor.supports("extracto.pdf")).thenReturn(true);
        when(pdfExtractor.extract(new byte[0], null)).thenReturn("   ");

        Assertions.assertThrows(
                EmptyStatementTextException.class,
                () -> service.extractText("extracto.pdf", new byte[0], null)
        );
    }
}
