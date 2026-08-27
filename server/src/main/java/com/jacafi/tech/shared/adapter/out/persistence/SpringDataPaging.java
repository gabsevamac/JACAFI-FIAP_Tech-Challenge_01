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

/**
 * Translates between the application's paging vocabulary and Spring Data's.
 *
 * <p>The whole reason {@code PageQuery} and {@code PageResult} exist is that this translation
 * happens in exactly one place, in infrastructure. Everything above it can be tested, and
 * implemented, without Spring Data on the classpath.
 */
public final class SpringDataPaging {

    private SpringDataPaging() {}

    /**
     * @param propertyNames maps API field names onto persistence property names, for the cases
     *                      where they differ. A name absent from the map passes through unchanged.
     */
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

    /**
     * Reads {@code size} back from the request rather than from the returned page.
     *
     * <p>Spring Data reports the requested size on a full page and, on the last one, still reports
     * the requested size — but relying on that is relying on an implementation detail. The size the
     * client asked for is the size the client should see echoed.
     */
    public static <S, T> PageResult<T> toPageResult(Page<S> page, PageQuery query, Function<S, T> mapper) {
        return PageResult.of(
                page.getContent().stream().map(mapper).toList(), query.page(), query.size(), page.getTotalElements());
    }
}
