package com.jacafi.tech.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("the shared paging value objects")
class PagingValueObjectsTest {

    private static final List<SortCriterion> BY_ID = List.of(SortCriterion.ascending("id"));

    @Nested
    @DisplayName("PageQuery")
    class Queries {

        @ParameterizedTest
        @DisplayName("refuses a negative page")
        @ValueSource(ints = {-1, -100})
        void refusesNegativePages(int page) {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new PageQuery(page, 20, BY_ID))
                    .withMessageContaining("page");
        }

        @ParameterizedTest
        @DisplayName("refuses a non-positive size")
        @ValueSource(ints = {0, -1})
        void refusesNonPositiveSizes(int size) {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new PageQuery(0, size, BY_ID))
                    .withMessageContaining("size");
        }

        @Test
        @DisplayName("refuses an empty ordering, because unordered paging is unstable")
        void refusesAnEmptyOrdering() {

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new PageQuery(0, 20, List.of()))
                    .withMessageContaining("unstable");
        }

        @Test
        @DisplayName("refuses a null ordering")
        void refusesANullOrdering() {
            assertThatNullPointerException().isThrownBy(() -> new PageQuery(0, 20, null));
        }

        @Test
        @DisplayName("copies the ordering, so a later change to the caller's list cannot reach it")
        void defensivelyCopiesTheOrdering() {
            List<SortCriterion> mutable = new ArrayList<>(BY_ID);
            PageQuery query = new PageQuery(0, 20, mutable);

            mutable.clear();

            assertThat(query.sort()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("PageResult")
    class Results {

        @Test
        @DisplayName("derives totalPages instead of taking it, so the two cannot disagree")
        void derivesTotalPages() {
            assertThat(PageResult.of(List.of("a"), 0, 20, 41L).totalPages()).isEqualTo(3);
            assertThat(PageResult.of(List.of(), 0, 20, 0L).totalPages()).isZero();
            assertThat(PageResult.of(List.of("a"), 0, 20, 20L).totalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("refuses null content")
        void refusesNullContent() {
            assertThatNullPointerException().isThrownBy(() -> PageResult.of(null, 0, 20, 0L));
        }

        @ParameterizedTest
        @DisplayName("refuses a negative page")
        @ValueSource(ints = {-1, -5})
        void refusesNegativePages(int page) {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> PageResult.of(List.of(), page, 20, 0L));
        }

        @Test
        @DisplayName("refuses a non-positive size and a negative total")
        void refusesImpossibleMetadata() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> PageResult.of(List.of(), 0, 0, 0L));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> PageResult.of(List.of(), 0, 20, -1L));
        }

        @Test
        @DisplayName("maps the elements and keeps the paging metadata untouched")
        void mapsPreservingMetadata() {
            PageResult<String> mapped = PageResult.of(List.of(1, 2), 2, 5, 12L).map(String::valueOf);

            assertThat(mapped.content()).containsExactly("1", "2");
            assertThat(mapped.page()).isEqualTo(2);
            assertThat(mapped.size()).isEqualTo(5);
            assertThat(mapped.totalElements()).isEqualTo(12L);
            assertThat(mapped.totalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("SortCriterion")
    class Criteria {

        @ParameterizedTest
        @DisplayName("refuses a blank field")
        @ValueSource(strings = {"", "   "})
        void refusesABlankField(String field) {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new SortCriterion(field, SortDirection.ASC));
        }

        @Test
        @DisplayName("refuses a null field and a null direction")
        void refusesNulls() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new SortCriterion(null, SortDirection.ASC));
            assertThatNullPointerException().isThrownBy(() -> new SortCriterion("id", null));
        }
    }
}
