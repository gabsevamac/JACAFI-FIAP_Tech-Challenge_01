package com.jacafi.tech.customer.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps a {@link TaxId} to the single {@code tax_id} column and back.
 *
 * <p>A sealed interface cannot be an {@code @Embeddable}, and it does not need to be: the value
 * carries its own type, so reading a row means handing the string back to {@link TaxId#of} and
 * letting it decide. There is no discriminator column to keep in step with the value.
 *
 * <p>{@code autoApply} so that every {@code TaxId} attribute is mapped without each entity having
 * to remember to say so.
 */
@Converter(autoApply = true)
public class TaxIdConverter implements AttributeConverter<TaxId, String> {

    @Override
    public String convertToDatabaseColumn(TaxId taxId) {
        return taxId == null ? null : taxId.value();
    }

    @Override
    public TaxId convertToEntityAttribute(String column) {
        return column == null ? null : TaxId.of(column);
    }
}
