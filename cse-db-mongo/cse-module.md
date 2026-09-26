# cse-db-mongo

MongoDB adapter. The WAR depends on this module, not on `cse-db-postgres`.

## Does

- Implement the feature stores with `MongoTemplate`, including `ResourceRedirectService`.
- Connect on first use (`DataProvider`), store settings documents, and convert `EntityId` to `ObjectId`.

## Does not

- Define pages, files, accounts, themes, or apps. Those documents live in the feature modules.
- Expose HTTP.
