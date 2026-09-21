# cse-security

Authentication, CSRF, and editor capability checks.

## Does

- Spring Security entry points, CSRF cookie handling, role and capability checks, and the editor surface gate.

## Does not

- Register the HTTP security filter chain. That wiring stays in the WAR.
- Own account documents. Those are `cse-db-api`.
