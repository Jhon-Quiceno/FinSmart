package com.smartfinance.backend.extractos.service.dedup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DescriptionSimilarityTest {

    @Test
    void normalizeShouldLowercaseAndStripAccents() {
        Assertions.assertEquals("compra en almacen", DescriptionSimilarity.normalize("COMPRA en ALMACÉN"));
    }

    @Test
    void normalizeShouldReplaceNonAlphanumericWithSpaceAndCollapseWhitespace() {
        Assertions.assertEquals("pago netflix", DescriptionSimilarity.normalize("  PAGO -- Netflix!!  "));
    }

    @Test
    void isSimilarShouldReturnTrueWhenOneNormalizedDescriptionContainsTheOther() {
        Assertions.assertTrue(DescriptionSimilarity.isSimilar("Compra Supermercado Exito", "Supermercado Exito"));
    }

    @Test
    void isSimilarShouldReturnTrueWhenTokenSetJaccardMeetsThreshold() {
        // "pago tarjeta credito visa" vs "pago tc visa" comparten "pago" y "visa" de un total de 6 tokens -> 2/6 = 0.33... redondea justo por debajo,
        // así que se usa un caso con mayor solapamiento para asegurar el umbral 0.34.
        Assertions.assertTrue(DescriptionSimilarity.isSimilar("transferencia juan perez", "transferencia de juan perez"));
    }

    @Test
    void isSimilarShouldReturnFalseWhenDescriptionsAreUnrelated() {
        Assertions.assertFalse(DescriptionSimilarity.isSimilar("Compra Supermercado Exito", "Pago Servicio de Luz"));
    }

    @Test
    void isSimilarShouldReturnTrueWhenEitherDescriptionIsBlank() {
        Assertions.assertTrue(DescriptionSimilarity.isSimilar("", "Compra Supermercado"));
        Assertions.assertTrue(DescriptionSimilarity.isSimilar(null, null));
    }
}
