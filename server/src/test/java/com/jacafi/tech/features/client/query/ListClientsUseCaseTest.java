package com.jacafi.tech.features.client.query;

import com.jacafi.tech.features.client.domain.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListClientsUseCaseTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private ListClientsUseCase useCase;

    @Test
    void listsAllClientsOrFiltersByActiveState() {
        var page = PageRequest.of(0, 20);
        when(repository.findAll(page)).thenReturn(Page.empty(page));
        when(repository.findAllByActive(true, page)).thenReturn(Page.empty(page));

        assertThat(useCase.execute(null, page)).isEmpty();
        assertThat(useCase.execute(true, page)).isEmpty();
    }
}
