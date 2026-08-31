package com.jacafi.tech.servicecatalog.adapter.in.web.controller;

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

import com.jacafi.tech.servicecatalog.adapter.in.web.api.ServiceCatalogApi;
import com.jacafi.tech.servicecatalog.adapter.in.web.dto.CreateServiceCatalogItemRequest;
import com.jacafi.tech.servicecatalog.adapter.in.web.dto.ServiceCatalogItemResponse;
import com.jacafi.tech.servicecatalog.adapter.in.web.dto.UpdateServiceCatalogItemRequest;
import com.jacafi.tech.servicecatalog.application.service.DeactivateServiceCatalogItemService;
import com.jacafi.tech.servicecatalog.application.service.FindServiceCatalogItemService;
import com.jacafi.tech.servicecatalog.application.service.ListServiceCatalogItemsService;
import com.jacafi.tech.servicecatalog.application.service.RegisterServiceCatalogItemService;
import com.jacafi.tech.servicecatalog.application.service.UpdateServiceCatalogItemService;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.adapter.in.web.SortableFields;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.application.SortCriterion;

@RestController
@RequestMapping("/api/v1/service-catalog-items")
public class ServiceCatalogController implements ServiceCatalogApi {
    private static final SortableFields SORTABLE = SortableFields.of("id", "name", "basePrice", "createdAt");

    private final RegisterServiceCatalogItemService register;
    private final FindServiceCatalogItemService find;
    private final ListServiceCatalogItemsService list;
    private final UpdateServiceCatalogItemService update;
    private final DeactivateServiceCatalogItemService deactivate;

    public ServiceCatalogController(
            RegisterServiceCatalogItemService register,
            FindServiceCatalogItemService find,
            ListServiceCatalogItemsService list,
            UpdateServiceCatalogItemService update,
            DeactivateServiceCatalogItemService deactivate) {
        this.register = register;
        this.find = find;
        this.list = list;
        this.update = update;
        this.deactivate = deactivate;
    }

    @Override
    @PostMapping
    public ResponseEntity<ServiceCatalogItemResponse> create(
            @Valid @RequestBody CreateServiceCatalogItemRequest request) {
        var item = register.register(request.name(), request.description(), request.basePrice());
        return ResponseEntity.created(URI.create("/api/v1/service-catalog-items/" + item.id()))
                .body(ServiceCatalogItemResponse.from(item));
    }

    @Override
    @GetMapping("/{id}")
    public ServiceCatalogItemResponse findById(@PathVariable UUID id) {
        return ServiceCatalogItemResponse.from(find.findById(id));
    }

    @Override
    @GetMapping
    public PageResult<ServiceCatalogItemResponse> list(PageParameters paging) {
        return list.list(pageQuery(paging)).map(ServiceCatalogItemResponse::from);
    }

    @Override
    @PutMapping("/{id}")
    public ServiceCatalogItemResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateServiceCatalogItemRequest request) {
        return ServiceCatalogItemResponse.from(
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
