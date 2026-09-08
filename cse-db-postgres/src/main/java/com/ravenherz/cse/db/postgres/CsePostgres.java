package com.ravenherz.cse.db.postgres;

/**
 * PostgreSQL adapter is not implemented and is not on the WAR classpath.
 * The engine depends on {@code cse-db-mongo} only.
 */
public final class CsePostgres {

    public static final String ENGINE = "postgres";

    private CsePostgres() {
    }
}
