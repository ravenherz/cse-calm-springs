# cse-db-mongo

MongoDB adapter. The WAR depends on this module, not on `cse-db-postgres`.

## Does

- `MongoTemplate` services (`*ServiceImpl`), reference hydration, and the data provider that connects on first use.

## Does not

- Define the entity model. That is `cse-db-api`.
- Expose HTTP or choose which database the product uses at runtime beyond this adapter.
