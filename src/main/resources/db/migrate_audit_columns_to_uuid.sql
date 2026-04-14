-- Migration: Convert created_by and updated_by columns from varchar to uuid
-- Run this ONCE against the database before restarting the application
-- This clears old string values (emails/"SYSTEM") and changes column type to uuid

DO $$
DECLARE
    tbl TEXT;
    tables TEXT[] := ARRAY[
        'user',
        'tenant',
        'subscription_plan',
        'shop',
        'shop_subscription',
        'shop_additional_module',
        'additional_module',
        'file_storage',
        'message_template'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables
    LOOP
        -- Check if table exists before altering
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'sales_people' AND table_name = tbl) THEN
            -- Clear old string values that can't be cast to uuid
            EXECUTE format('UPDATE sales_people.%I SET created_by = NULL, updated_by = NULL', tbl);

            -- Alter column types from varchar to uuid
            EXECUTE format('ALTER TABLE sales_people.%I ALTER COLUMN created_by TYPE uuid USING created_by::uuid', tbl);
            EXECUTE format('ALTER TABLE sales_people.%I ALTER COLUMN updated_by TYPE uuid USING updated_by::uuid', tbl);

            RAISE NOTICE 'Migrated table: sales_people.%', tbl;
        ELSE
            RAISE NOTICE 'Table not found, skipping: sales_people.%', tbl;
        END IF;
    END LOOP;
END $$;
