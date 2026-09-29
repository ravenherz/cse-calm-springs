# cse-optideployer

A small Spring Boot WAR. It sits on the same Tomcat as the engine and receives the engine WAR in eight parallel HTTP streams. After the parts match, it writes one file and tells Tomcat manager to deploy that file from disk.

Cargo still deploys this WAR. The parallel path is only for the engine WAR, which is the large upload.

## Deploy this app

`ext.cseInstance` in the root `build.gradle` picks the instance. Set it to `production`, `nl`, `stage`, or `dev`, then:

```
./gradlew cargoDeployRemote
```

That builds `cse-optideployer.war` and uploads it with Cargo. It is not part of `build`.

## Deploy the engine WAR

```
./gradlew :cse:optiDeploy
```

That builds `:cse:bootWar` and posts the file to this app in eight streams. The task reads the same instance variable.

## Config

Both tasks read `.cse-deployment.yml` at the repository root. The file is gitignored.

Shared keys are the defaults. `instancesOverride` overrides hostname. `applicationsOverride` overrides context, and can override any other key for one application and instance. A plain key on an application is the default for every instance of that application.

```yaml
shared:
  hostname: example.com
  containerId: tomcat10x
  port: 443
  protocol: https
  username: manager
  password: manager-password
  context: ROOT
instancesOverride:
  production:
    hostname: example.com
  dev:
    hostname: dev.example.com
applicationsOverride:
  cse-optideployer:
    secret: "00000000-0000-0000-0000-000000000000"
    production:
      context: cse-optideployer
  cse:
    warPath: /var/cse/cse.war
    stage:
      context: rhz-we
    dev:
      context: rhz-we
```

`cse-optideployer` needs `secret`. `cse` needs `warPath`, the absolute path where the assembled WAR is written. `context` of `ROOT` deploys at `/`.

`protocol` and `port` decide the scheme. `https` and `443` produce `https://` URLs with the port omitted. The app redirects HTTP to HTTPS and refuses an upload that did not arrive on a secure request.

The WAR does not contain the upload secret or the manager password. Write the server file once:

```
./gradlew :cse-optideployer:serverConfig
```

That writes `cse-optideployer/build/optideploy.json` for the instance in `ext.cseInstance`. Copy it to `/var/cse/optideploy.json` on the server, or to the path in `CSE_OPTI_CONFIG`. Later Cargo deploys do not refresh it. `:cse:optiDeploy` still reads `.cse-deployment.yml` on the build machine.

Rotate `secret` after this change. An older value may already be in access logs.

## Upload

`POST /upload-and-deploy/` with header `X-Cse-Deploy-Secret`.

Multipart fields: `uploadId`, `partIndex` (`0`–`7`), `totalSize`, and `file`. Seven parts answer `204`. The part that completes the set answers `200` and a body that starts with `OK`. A wrong secret, or a request that is not HTTPS, is `403`. A second upload while a deploy is running is `409`.

Tomcat manager is then called as:

`GET {managerUrl}/deploy?path={context}&update=true&war=file:{warPath}`

The body must start with `OK`.
