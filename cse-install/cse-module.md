# cse-install

First-run data bootstrap.

## Does

- Seed settings and URL templates, and decide what the installer may write before the site is ready.

## Does not

- Serve `/install/**`. `InstallController` stays in the WAR.
- Pack the setup app. That is `app-setup`.
