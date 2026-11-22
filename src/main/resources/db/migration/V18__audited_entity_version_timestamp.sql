-- Optimistic lock column: BIGINT counter → TIMESTAMPTZ (maps to java.util.Date on AuditedEntity).

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'school'
          AND c.column_name = 'version'
          AND c.data_type = 'bigint'
    LOOP
        EXECUTE format('ALTER TABLE school.%I ALTER COLUMN version DROP DEFAULT', r.table_name);
        EXECUTE format(
            'ALTER TABLE school.%I ALTER COLUMN version TYPE TIMESTAMPTZ USING COALESCE(updated_at, created_at, NOW())',
            r.table_name
        );
        EXECUTE format(
            'ALTER TABLE school.%I ALTER COLUMN version SET DEFAULT NOW()',
            r.table_name
        );
    END LOOP;
END $$;