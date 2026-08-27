package com.jacafi.tech.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.SortDirection;

@DisplayName("the paging contract")
class PageParametersTest {

    private static final SortableFields SORTABLE = SortableFields.of("id", "name", "createdAt");

    private static PageQuery query(Integer page, Integer size, List<String> sort) {
        return new PageParameters(page, size, sort).toQuery(SORTABLE);
    }

    @Nested
    @DisplayName("bounds")
    class Bounds {

        @Test
        @DisplayName("default to page zero and size twenty when nothing is asked for")
        void appliesDefaults() {
            PageQuery query = query(null, null, null);

            assertThat(query.page()).isZero();
            assertThat(query.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("reject a size above the ceiling instead of clamping it")
        void rejectsOversizedPages() {
            // Clamping would answer 100 elements to a request for 500 and report success. A client
            // looping over pages would then skip four fifths of the data and never know: a wrong
            // answer is worse than an error, because only the error gets fixed.
            assertThatExceptionOfType(InvalidPageRequestException.class).isThrownBy(() -> query(0, 101, null));
        }

        @Test
        @DisplayName("accept exactly the ceiling")
        void acceptsTheCeiling() {
            assertThat(query(0, 100, null).size()).isEqualTo(100);
        }

        @ParameterizedTest
        @DisplayName("reject a negative page and a non-positive size")
        @ValueSource(ints = {-1, -20})
        void rejectsNegativePages(int page) {
            assertThatExceptionOfType(InvalidPageRequestException.class).isThrownBy(() -> query(page, 20, null));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void rejectsNonPositiveSizes(int size) {
            assertThatExceptionOfType(InvalidPageRequestException.class).isThrownBy(() -> query(0, size, null));
        }
    }

    @Nested
    @DisplayName("sorting")
    class Sorting {

        @Test
        @DisplayName("accept a whitelisted field and default to ascending")
        void acceptsAWhitelistedField() {
            List<com.jacafi.tech.shared.application.SortCriterion> sort =
                    query(0, 20, List.of("name")).sort();

            assertThat(sort).first().satisfies(criterion -> {
                assertThat(criterion.field()).isEqualTo("name");
                assertThat(criterion.direction()).isEqualTo(SortDirection.ASC);
            });
        }

        @Test
        @DisplayName("read the direction when given")
        void readsTheDirection() {
            assertThat(query(0, 20, List.of("name,desc")).sort().getFirst().direction())
                    .isEqualTo(SortDirection.DESC);
        }

        @Test
        @DisplayName("always end in the tie-breaker, so paging is stable")
        void appendsTheTieBreaker() {
            // Without a unique final criterion, rows sharing a value come back in whatever order
            // the database picks, and that order may differ between two queries — so a row can
            // land on page 1 and again on page 2, or on neither.
            assertThat(query(0, 20, List.of("name")).sort()).extracting("field").containsExactly("name", "id");
        }

        @Test
        @DisplayName("do not append the tie-breaker twice when it was asked for")
        void doesNotDuplicateTheTieBreaker() {
            assertThat(query(0, 20, List.of("id,desc")).sort()).hasSize(1);
        }

        @Test
        @DisplayName("sort by the tie-breaker alone when nothing was asked for")
        void defaultsToTheTieBreaker() {
            assertThat(query(0, 20, null).sort()).extracting("field").containsExactly("id");
        }

        @Test
        @DisplayName("reject a field that is not on the list")
        void rejectsAnUnknownField() {
            assertThatExceptionOfType(InvalidPageRequestException.class)
                    .isThrownBy(() -> query(0, 20, List.of("passwordHash")));
        }

        @Test
        @DisplayName("say nothing about the rejected field, nor about which fields exist")
        void doesNotDiscloseTheSchema() {
            // The whole point of the allow-list. Spring Data's PropertyReferenceException names
            // the properties that do exist, which maps the entity one guess at a time.
            assertThatExceptionOfType(InvalidPageRequestException.class)
                    .isThrownBy(() -> query(0, 20, List.of("passwordHash")))
                    .withMessageNotContaining("passwordHash")
                    .withMessageNotContaining("name")
                    .withMessageNotContaining("createdAt");
        }

        @Test
        @DisplayName("reject a direction that is neither asc nor desc")
        void rejectsAnUnknownDirection() {
            assertThatExceptionOfType(InvalidPageRequestException.class)
                    .isThrownBy(() -> query(0, 20, List.of("name,sideways")));
        }
    }
}
