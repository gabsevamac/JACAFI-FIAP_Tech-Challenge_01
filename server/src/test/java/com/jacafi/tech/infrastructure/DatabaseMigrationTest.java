package com.jacafi.tech.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///jacafi",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "spring.datasource.username=jacafi",
            "spring.datasource.password=jacafi"
        })
// Sem o perfil de teste, jwt.secret fica sem valor e o contexto nao sobe: application.yaml
// resolve ${JWT_SECRET} na criacao do JwtService.
@ActiveProfiles("test")
class DatabaseMigrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void appliesTheInitialSchemaAndCustomerIndexes() {
        var tables = jdbcClient.sql("""
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                        """).query(String.class).list();

        var indexes = jdbcClient.sql("""
                        SELECT indexname
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                        """).query(String.class).list();

        assertThat(tables)
                .contains("flyway_schema_history", "customers", "services", "service_orders", "service_orders_service");
        assertThat(indexes).contains("uk_customers_tax_id");
        // Party foi colapsado dentro de Customer: a tabela nao existe mais.
        assertThat(tables).doesNotContain("parties");
    }

    @Test
    @DisplayName("nenhuma coluna de tempo do schema fica sem fuso")
    void everyTimestampColumnCarriesItsTimeZone() {
        // Vale para o schema inteiro, e nao para as tabelas que hoje se sabe estarem certas. Uma
        // fatia nova que criar `created_at TIMESTAMP` sem o WITH TIME ZONE e barrada aqui, sem
        // depender de alguem lembrar da regra durante a revisao.
        //
        // O tipo sem fuso guarda hora de parede sem procedencia: quem escreveu decidiu o fuso, a
        // coluna nao registrou qual foi, e nao ha como descobrir depois. Como o desafio pede tempo
        // medio de execucao dos servicos — subtracao entre timestamps — o erro nao se manifesta
        // como falha, e sim como um numero plausivel no relatorio.
        var semFuso = jdbcClient.sql("""
                        SELECT table_name || '.' || column_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND data_type = 'timestamp without time zone'
                          -- Tabela do proprio Flyway, criada por ele e fora do nosso controle.
                          AND table_name <> 'flyway_schema_history'
                        """).query(String.class).list();

        assertThat(semFuso)
                .as("colunas de tempo sem fuso: use TIMESTAMP WITH TIME ZONE e mapeie com Instant")
                .isEmpty();
    }
}
