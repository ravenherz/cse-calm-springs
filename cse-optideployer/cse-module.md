# cse-optideployer

Standalone WAR that receives a split upload and asks Tomcat to deploy the assembled file. Not a CSE library.

## Does

- Accept `POST /upload-and-deploy/` in eight parts, write the WAR to the path in the server config, and call Tomcat manager with a local `file:` URL.
- Check the `X-Cse-Deploy-Secret` header before the body is read, and refuse a request that is not HTTPS.
- Read that config from `CSE_OPTI_CONFIG`, or from `/var/cse/optideploy.json` when the variable is unset.

## Does not

- Depend on any `cse-*` or `app-*` project.
- Store the deployment file, the upload secret, or the manager password inside the WAR.
- Deploy itself. Cargo does that. `:cse:optiDeploy` uploads the engine WAR.
- Run as part of `build`.
