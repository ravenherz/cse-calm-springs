# cse-db-api

Persistence port and CMS documents. Libraries and the WAR depend on this, not on a database driver.

## Does

- Entity types (`ItemEntity`, `PageData`, `AlbumData`, resources, accounts, roles) and DAO interfaces.
- Collection names, access constants, and capability ids.
- App-store name rules.

## Does not

- Open a database. Mongo is `cse-db-mongo`. Postgres is an unwired stub.
- Serve HTTP or render the editor.
- Still free of Mongo types: documents still use `@Document` and `ObjectId` until that split is finished.
