# cse-scripting-js

JavaScript scripts stored as documents, and the records of their runs.

## Does

- Own `ScriptEntity` and `ScriptService`. They extend `cse-dal-core`.
- Own `ScriptRunEntity` and `ScriptRunService`. A run points at a script by `EntityId` and keeps the script id as a label.
- Check a script id, folder path, and source length before save.
- Build the folder tree the editor shows.
- Run a script with `JsRuntime`. The host object is `cse`. `cse.deleteOrphans()` is the bound call; the WAR supplies what it deletes.

## Does not

- Open Mongo. `cse-db-mongo` implements the stores.
- Decide which documents are orphans, send HTTP, or render HTML.
- Import page, file, account, theme, or app types.
