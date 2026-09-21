# buildSrc

Gradle build logic. Not a product library and not on the WAR classpath.

## Does

- Pack `.cseapp` and `.csetheme` archives.
- Resolve the Cargo deploy target and the git branch used in the WAR name.

## Does not

- Run inside Tomcat or talk to Mongo.
- Decide product behavior. Module boundaries are the `cse-*`, `app-*`, and `theme-*` projects.
