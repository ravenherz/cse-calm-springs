# app-login

Login panel pack. Packed as `login.cseapp` and copied into the WAR.

## Does

- Ship the login UI used by the public site.
- Show a cookie notice that blocks the page until it is accepted. The notice covers the security cookie used to check sign-in.

## Does not

- Check passwords or set cookies. Auth HTTP stays in the WAR and `cse-security`.
