# theme-js-client

JavaScript public shell. `packFrontend` builds `client.csetheme`; the WAR does not install it.

## Does

- Pack a JS client plus the modern skin CSS into a `.csetheme`.

## Does not

- Get copied into the WAR by `processResources`.
- Render with Thymeleaf. The Thymeleaf skins are `theme-thymeleaf-modern` and `theme-thymeleaf-2000s`.
