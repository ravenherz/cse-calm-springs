package com.ravenherz.build.utils

import org.gradle.api.Project

class CargoContextResolver {

    private static interface Resolver {
        def orDefault(Object defaultValue)
    }

    private final boolean remote
    private final String whereToDeploy
    private final Map resolved

    CargoContextResolver(Boolean isRemoteDeploy = true, Project projectLocal, String deployOverride) {
        this(isRemoteDeploy, projectLocal, deployOverride, 'cse-optideployer')
    }

    CargoContextResolver(Boolean isRemoteDeploy, Project projectLocal, String deployOverride, String application) {
        this.remote = isRemoteDeploy
        if (deployOverride != null && !deployOverride.trim().isEmpty()) {
            this.whereToDeploy = deployOverride.trim()
        } else if (GitHelper.gitBranch == 'main') {
            this.whereToDeploy = 'production'
        } else if (GitHelper.gitBranch == 'stage') {
            this.whereToDeploy = 'stage'
        } else {
            this.whereToDeploy = 'dev'
        }
        if (remote) {
            def file = projectLocal.rootProject.file('.cse-deployment.yml')
            this.resolved = new DeploymentLayout(file).resolve(whereToDeploy, application)
        } else {
            this.resolved = null
        }
    }

    private static class ResolveNullToDefault implements Resolver {
        private Object value

        ResolveNullToDefault(Object value) {
            this.value = value
        }

        @Override
        def orDefault(Object defaultValue) {
            return value == null ? defaultValue : value
        }
    }

    private static class FakeResolver implements Resolver {
        @Override
        def orDefault(Object defaultValue) {
            return defaultValue
        }
    }

    Resolver getByKey(String key) {
        return remote ? new ResolveNullToDefault(resolved[key]) : new FakeResolver()
    }

    String getWhereToDeploy() {
        return whereToDeploy
    }
}
