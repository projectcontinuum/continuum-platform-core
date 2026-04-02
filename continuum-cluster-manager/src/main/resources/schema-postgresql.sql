-- PostgreSQL-only: partial unique index for active workbenches
-- Run manually or via a migration tool (Flyway/Liquibase) on PostgreSQL
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_workbench
    ON workbench_instances (user_id, instance_name)
    WHERE status NOT IN ('DELETED', 'TERMINATING');
