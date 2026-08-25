package com.jacafi.tech.features.client.create;

import com.jacafi.tech.features.client.domain.ClientRepository;
import com.jacafi.tech.features.client.domain.PersonType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateClientUseCaseTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private CreateClientUseCase useCase;

    @Test
    void createsAClientWithANormalizedTaxIdentifier() {
        var command = new CreateClientCommand(
                PersonType.INDIVIDUAL,
                "529.982.247-25",
                "Maria",
                null,
                "maria@example.com",
                "11999999999");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var client = useCase.execute(command);

        assertThat(client.getParty().getTaxIdentifier().getValue()).isEqualTo("52998224725");
        assertThat(client.isActive()).isTrue();
        verify(repository).save(client);
    }

    @Test
    void rejectsAnExistingTaxIdentifier() {
        var command = new CreateClientCommand(
                PersonType.INDIVIDUAL,
                "52998224725",
                "Maria",
                null,
                "maria@example.com",
                "11999999999");
        when(repository.existsByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
                PersonType.INDIVIDUAL,
                "52998224725"))
                .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ClientAlreadyExistsException.class);
    }
}
