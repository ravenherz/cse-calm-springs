# cse-db-postgres

Placeholder for a PostgreSQL adapter.

## Does

- Compile a marker type (`CsePostgres`) against the Postgres JDBC driver and `cse-db-api`.

## Does not

- Implement queries, migrations, or connection setup.
- Sit on the WAR classpath. The engine depends on `cse-db-mongo` only.
