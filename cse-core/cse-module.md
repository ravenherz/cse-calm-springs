# cse-core

Shared types that more than one library would otherwise copy.

## Does

- `SiteConfigured`: whether first-run setup has finished. The WAR implements it (`SiteReady`).

## Does not

- Start Spring, open Mongo, or encode media.
- Decide HTTP routes or render pages.
