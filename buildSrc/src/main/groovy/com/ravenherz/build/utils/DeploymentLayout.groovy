package com.ravenherz.build.utils

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

/**
 * Reads {@code .cse-deployment.json}.
 * Merge order: shared, then instancesOverride[instance], then scalar defaults on the
 * application, then applicationsOverride[application][instance].
 */
class DeploymentLayout {

    private final Object root

    DeploymentLayout(File file) {
        if (file == null || !file.isFile()) {
            throw new IllegalStateException('Missing .cse-deployment.json at the repository root')
        }
        this.root = new JsonSlurper().parse(file)
    }

    Map resolve(String instance, String application) {
        if (instance == null || instance.trim().isEmpty()) {
            throw new IllegalStateException('Deployment instance is empty')
        }
        def instances = root.instancesOverride
        if (!(instances instanceof Map) || !instances.containsKey(instance)) {
            def known = instances instanceof Map ? instances.keySet() : []
            throw new IllegalStateException("Unknown instance '${instance}'. Known: ${known}")
        }
        Map merged = new LinkedHashMap()
        overlay(merged, asMap(root.shared))
        overlay(merged, asMap(instances[instance]))
        def apps = root.applicationsOverride
        if (application != null && apps instanceof Map && apps[application] instanceof Map) {
            Map appDefaults = new LinkedHashMap()
            Map appInstance = new LinkedHashMap()
            (apps[application] as Map).each { key, value ->
                if (value instanceof Map) {
                    if (key.toString() == instance) {
                        overlay(appInstance, asMap(value))
                    }
                } else if (value != null) {
                    appDefaults[key.toString()] = value
                }
            }
            overlay(merged, appDefaults)
            overlay(merged, appInstance)
        }
        return merged
    }

    Map serverConfig(String instance) {
        Map opti = resolve(instance, 'cse-optideployer')
        Map cse = resolve(instance, 'cse')
        require(opti, ['protocol', 'hostname', 'port', 'username', 'password', 'context'], "cse-optideployer/${instance}")
        require(cse, ['context', 'warPath'], "cse/${instance}")
        if (opti.secret == null || opti.secret.toString().trim().isEmpty()) {
            throw new IllegalStateException(".cse-deployment.json is missing secret for cse-optideployer/${instance}. Add it on that application.")
        }
        String origin = origin(opti.protocol.toString(), opti.hostname.toString(), portOf(opti))
        String prefix = webPath(opti.context as String)
        String upload = origin + (prefix == '/' ? '/upload-and-deploy/' : prefix + '/upload-and-deploy/')
        Map config = new LinkedHashMap()
        config.secret = opti.secret.toString()
        config.uploadUrl = upload
        config.warPath = cse.warPath.toString()
        config.context = webPath(cse.context as String)
        config.managerUrl = origin + '/manager/text'
        config.username = opti.username.toString()
        config.password = opti.password.toString()
        return config
    }

    String serverJson(String instance) {
        return new JsonBuilder(serverConfig(instance)).toPrettyString()
    }

    static int portOf(Map resolved) {
        if (resolved.port == null || resolved.port.toString().trim().isEmpty()) {
            throw new IllegalStateException('.cse-deployment.json is missing port')
        }
        return Integer.parseInt(resolved.port.toString())
    }

    static String cargoContext(Object context) {
        String path = webPath(context == null ? null : context.toString())
        return path == '/' ? 'ROOT' : path.substring(1)
    }

    static String webPath(String context) {
        if (context == null) {
            return '/'
        }
        String value = context.trim()
        if (value.isEmpty() || value.equalsIgnoreCase('ROOT') || value == '/') {
            return '/'
        }
        if (!value.startsWith('/')) {
            value = '/' + value
        }
        if (value.length() > 1 && value.endsWith('/')) {
            value = value.substring(0, value.length() - 1)
        }
        return value
    }

    static String origin(String protocol, String host, int port) {
        boolean omit = ('https' == protocol && port == 443) || ('http' == protocol && port == 80)
        return omit ? protocol + '://' + host : protocol + '://' + host + ':' + port
    }

    private static void require(Map map, List keys, String who) {
        for (String key : keys) {
            if (map[key] == null || map[key].toString().trim().isEmpty()) {
                throw new IllegalStateException(".cse-deployment.json resolved ${who} is missing ${key}. Add it on that application or instance.")
            }
        }
    }

    private static Map asMap(Object value) {
        if (value == null) {
            return new LinkedHashMap()
        }
        if (!(value instanceof Map)) {
            throw new IllegalStateException('Expected an object in .cse-deployment.json')
        }
        return (Map) value
    }

    private static void overlay(Map into, Map over) {
        over.each { key, value ->
            if (value != null) {
                into[key.toString()] = value
            }
        }
    }

    static void selfCheck() {
        File file = File.createTempFile('cse-deployment', '.json')
        file.deleteOnExit()
        file.text = '''
        {
          "shared": {
            "hostname": "shared.example",
            "containerId": "tomcat10x",
            "port": 443,
            "protocol": "https",
            "username": "mgr",
            "password": "pw",
            "context": "cse"
          },
          "instancesOverride": {
            "production": { "hostname": "nl3.example" },
            "nl": { "hostname": "nl.example" },
            "stage": { "hostname": "stage.example" },
            "dev": { "hostname": "dev.example" }
          },
          "applicationsOverride": {
            "cse-optideployer": {
              "secret": "top-secret",
              "production": { "context": "cse-optideployer" },
              "nl": { "context": "cse-optideployer" },
              "stage": { "context": "cse-optideployer", "secret": "stage-secret" },
              "dev": { "context": "cse-optideployer" }
            },
            "cse": {
              "warPath": "/var/cse/app.war",
              "production": { "context": "ROOT" },
              "nl": { "context": "ROOT" },
              "stage": { "context": "rhz-we", "warPath": "/var/cse/stage.war" },
              "dev": { "context": "rhz-we" }
            }
          }
        }
        '''
        DeploymentLayout layout = new DeploymentLayout(file)
        Map prodOpti = layout.resolve('production', 'cse-optideployer')
        assert prodOpti.hostname == 'nl3.example'
        assert prodOpti.context == 'cse-optideployer'
        assert prodOpti.protocol == 'https'
        assert portOf(prodOpti) == 443
        assert prodOpti.containerId == 'tomcat10x'
        assert prodOpti.username == 'mgr'
        assert prodOpti.password == 'pw'
        assert prodOpti.secret == 'top-secret'
        assert prodOpti.context != 'cse'
        assert cargoContext(prodOpti.context) == 'cse-optideployer'

        Map stageOpti = layout.resolve('stage', 'cse-optideployer')
        assert stageOpti.secret == 'stage-secret'
        assert stageOpti.hostname == 'stage.example'

        Map prodCse = layout.resolve('production', 'cse')
        assert prodCse.context == 'ROOT'
        assert prodCse.warPath == '/var/cse/app.war'
        assert cargoContext(prodCse.context) == 'ROOT'

        Map stageCse = layout.resolve('stage', 'cse')
        assert stageCse.context == 'rhz-we'
        assert stageCse.hostname == 'stage.example'
        assert webPath(stageCse.context.toString()) == '/rhz-we'
        assert stageCse.warPath == '/var/cse/stage.war'

        Map devCse = layout.resolve('dev', 'cse')
        assert devCse.hostname == 'dev.example'
        assert devCse.context == 'rhz-we'
        assert devCse.warPath == '/var/cse/app.war'

        Map nlCse = layout.resolve('nl', 'cse')
        assert nlCse.hostname == 'nl.example'
        assert nlCse.context == 'ROOT'

        Map server = layout.serverConfig('production')
        assert server.context == '/'
        assert server.uploadUrl == 'https://nl3.example/cse-optideployer/upload-and-deploy/'
        assert server.managerUrl == 'https://nl3.example/manager/text'
        assert server.warPath == '/var/cse/app.war'
        assert server.secret == 'top-secret'

        Map stageServer = layout.serverConfig('stage')
        assert stageServer.context == '/rhz-we'
        assert stageServer.secret == 'stage-secret'
        assert stageServer.uploadUrl == 'https://stage.example/cse-optideployer/upload-and-deploy/'

        assert origin('http', '127.0.0.1', 8080) == 'http://127.0.0.1:8080'
        boolean threw = false
        try {
            layout.resolve('missing', 'cse')
        } catch (IllegalStateException ignored) {
            threw = true
        }
        assert threw
    }
}
