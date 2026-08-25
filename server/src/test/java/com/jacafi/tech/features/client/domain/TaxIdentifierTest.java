package com.jacafi.tech.features.client.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaxIdentifierTest {

    @Test
    void normalizesAndAcceptsValidCpf() {
        var taxIdentifier = TaxIdentifier.of(PersonType.INDIVIDUAL, "529.982.247-25");

        assertEquals("52998224725", taxIdentifier.getValue());
    }

    @Test
    void rejectsCpfWithWrongCheckDigits() {
        assertThrows(InvalidTaxIdentifierException.class,
                () -> TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224724"));
    }

    @Test
    void rejectsCpfWithRepeatedDigits() {
        assertThrows(InvalidTaxIdentifierException.class,
                () -> TaxIdentifier.of(PersonType.INDIVIDUAL, "11111111111"));
    }

    @Test
    void normalizesAndAcceptsValidNumericCnpj() {
        var taxIdentifier = TaxIdentifier.of(PersonType.LEGAL_ENTITY, "11.222.333/0001-81");

        assertEquals("11222333000181", taxIdentifier.getValue());
    }

    @Test
    void normalizesAndAcceptsOfficialAlphanumericCnpj() {
        var taxIdentifier = TaxIdentifier.of(PersonType.LEGAL_ENTITY, "00.000.000/E08G-12");

        assertEquals("00000000E08G12", taxIdentifier.getValue());
    }

    @Test
    void rejectsCnpjWithWrongCheckDigits() {
        assertThrows(InvalidTaxIdentifierException.class,
                () -> TaxIdentifier.of(PersonType.LEGAL_ENTITY, "00.000.000/E08G-13"));
    }

    @Test
    void rejectsCnpjWithRepeatedDigits() {
        assertThrows(InvalidTaxIdentifierException.class,
                () -> TaxIdentifier.of(PersonType.LEGAL_ENTITY, "00000000000000"));
    }

    @Test
    void rejectsDocumentThatDoesNotMatchPersonType() {
        assertThrows(InvalidTaxIdentifierException.class,
                () -> TaxIdentifier.of(PersonType.INDIVIDUAL, "11222333000181"));
    }
}
