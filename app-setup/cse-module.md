# app-setup

First-boot installer UI. Packed as `setup.cseapp` and copied into the WAR.

## Does

- The setup wizard pages served at `/apps/setup/` until the site is configured.

## Does not

- Seed the database. That is `cse-install`.
- Stay available after setup. A configured site 404s the installer.
