# cse-transfer

Site snapshot export and import (`.csesite`).

## Does

- Write and read collection JSON, including resources and chunk ids.
- Rebuild resource-group links on import.

## Does not

- Serve the Site data screen. That controller stays in the WAR.
- Run the live site from the snapshot. Import writes through the DAO port.
