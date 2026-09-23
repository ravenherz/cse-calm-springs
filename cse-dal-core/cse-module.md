# cse-dal-core

Shared document base. Feature modules extend this. They do not share one entity jar.

## Does

- `EntityId` (24-hex string). Mongo converts it to `ObjectId` only inside `cse-db-mongo`.
- `BasicEntity`, access rules, history, `EntityAccess`, and a generic get/save/delete store.
- The creator of a document is an `EntityId`, not an account type.

## Does not

- Define pages, files, accounts, themes, or apps.
- Open a database or serve HTTP.
- Use `@Document` or `ObjectId`.
