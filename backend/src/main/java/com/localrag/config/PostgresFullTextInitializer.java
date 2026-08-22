package com.localrag.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.DatabaseStartupValidator;

@Configuration
public class PostgresFullTextInitializer {

    @Bean
    public CommandLineRunner initFullTextSearch(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS unaccent");
            jdbcTemplate.execute("""
                    ALTER TABLE documento_chunks 
                    ADD COLUMN IF NOT EXISTS content_tsv tsvector
                    """);
            jdbcTemplate.execute("""
                    UPDATE documento_chunks 
                    SET content_tsv = to_tsvector('spanish', unaccent(coalesce(contenido, ''))) 
                    WHERE content_tsv IS NULL
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_documento_chunks_content_tsv 
                    ON documento_chunks USING GIN(content_tsv)
                    """);
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trigger_update_content_tsv ON documento_chunks");
            jdbcTemplate.execute("""
                    CREATE OR REPLACE FUNCTION update_content_tsv() 
                    RETURNS trigger AS $$
                    BEGIN
                        NEW.content_tsv := to_tsvector('spanish', unaccent(coalesce(NEW.contenido, '')));
                        RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql
                    """);
            jdbcTemplate.execute("""
                    CREATE TRIGGER trigger_update_content_tsv 
                    BEFORE INSERT OR UPDATE ON documento_chunks 
                    FOR EACH ROW EXECUTE FUNCTION update_content_tsv()
                    """);
        };
    }
}
