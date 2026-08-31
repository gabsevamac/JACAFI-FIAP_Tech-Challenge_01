package com.jacafi.tech.shared.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.application.SortCriterion;
import com.jacafi.tech.shared.application.SortDirection;

public final class SpringDataPaging {

    private SpringDataPaging() {}

    public static Pageable toPageable(PageQuery query, Map<String, String> propertyNames) {
        List<Sort.Order> orders = query.sort().stream()
                .map(criterion -> order(criterion, propertyNames))
                .toList();
        return PageRequest.of(query.page(), query.size(), Sort.by(orders));
    }

    public static Pageable toPageable(PageQuery query) {
        return toPageable(query, Map.of());
    }

    private static Sort.Order order(SortCriterion criterion, Map<String, String> propertyNames) {
        String property = propertyNames.getOrDefault(criterion.field(), criterion.field());
        return criterion.direction() == SortDirection.DESC ? Sort.Order.desc(property) : Sort.Order.asc(property);
    }

    public static <S, T> PageResult<T> toPageResult(Page<S> page, PageQuery query, Function<S, T> mapper) {
        return PageResult.of(
                page.getContent().stream().map(mapper).toList(), query.page(), query.size(), page.getTotalElements());
    }
}
