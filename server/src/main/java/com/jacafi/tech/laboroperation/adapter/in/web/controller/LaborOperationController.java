package com.jacafi.tech.laboroperation.adapter.in.web.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacafi.tech.laboroperation.adapter.in.web.api.LaborOperationApi;
import com.jacafi.tech.laboroperation.adapter.in.web.dto.CreateLaborOperationRequest;
import com.jacafi.tech.laboroperation.adapter.in.web.dto.LaborOperationResponse;
import com.jacafi.tech.laboroperation.adapter.in.web.dto.UpdateLaborOperationRequest;
import com.jacafi.tech.laboroperation.application.service.DeactivateLaborOperationService;
import com.jacafi.tech.laboroperation.application.service.FindLaborOperationService;
import com.jacafi.tech.laboroperation.application.service.ListLaborOperationsService;
import com.jacafi.tech.laboroperation.application.service.RegisterLaborOperationService;
import com.jacafi.tech.laboroperation.application.service.UpdateLaborOperationService;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.adapter.in.web.SortableFields;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.application.SortCriterion;

@RestController
@RequestMapping("/api/v1/labor-operations")
public class LaborOperationController implements LaborOperationApi {
    private static final SortableFields SORTABLE = SortableFields.of("id", "name", "basePrice", "createdAt");

    private final RegisterLaborOperationService register;
    private final FindLaborOperationService find;
    private final ListLaborOperationsService list;
    private final UpdateLaborOperationService update;
    private final DeactivateLaborOperationService deactivate;

    public LaborOperationController(
            RegisterLaborOperationService register,
            FindLaborOperationService find,
            ListLaborOperationsService list,
            UpdateLaborOperationService update,
            DeactivateLaborOperationService deactivate) {
        this.register = register;
        this.find = find;
        this.list = list;
        this.update = update;
        this.deactivate = deactivate;
    }

    @Override
    @PostMapping
    public ResponseEntity<LaborOperationResponse> create(@Valid @RequestBody CreateLaborOperationRequest request) {
        var operation = register.register(request.name(), request.description(), request.basePrice());
        return ResponseEntity.created(URI.create("/api/v1/labor-operations/" + operation.id()))
                .body(LaborOperationResponse.from(operation));
    }

    @Override
    @GetMapping("/{id}")
    public LaborOperationResponse findById(@PathVariable UUID id) {
        return LaborOperationResponse.from(find.findById(id));
    }

    @Override
    @GetMapping
    public PageResult<LaborOperationResponse> list(PageParameters paging) {
        return list.list(pageQuery(paging)).map(LaborOperationResponse::from);
    }

    @Override
    @PutMapping("/{id}")
    public LaborOperationResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateLaborOperationRequest request) {
        return LaborOperationResponse.from(
                update.update(id, request.name(), request.description(), request.basePrice()));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deactivate.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    private static PageQuery pageQuery(PageParameters paging) {
        PageQuery query = paging.toQuery(SORTABLE);
        if (paging.sort() != null && !paging.sort().isEmpty()) {
            return query;
        }
        return new PageQuery(
                query.page(), query.size(), List.of(SortCriterion.ascending("name"), SortCriterion.ascending("id")));
    }
}
