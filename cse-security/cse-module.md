# cse-security

Accounts, roles, and the checks that use them.

## Does

- Account and role documents and their store interfaces.
- Spring Security entry points, CSRF, capabilities, and the editor surface gate.

## Does not

- Register the HTTP security filter chain. That wiring stays in the WAR.
- Own pages, files, themes, or apps.
- Open Mongo. `cse-db-mongo` implements the stores.
