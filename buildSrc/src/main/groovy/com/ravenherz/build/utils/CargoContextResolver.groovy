package com.ravenherz.build.utils

import groovy.json.JsonSlurper
import org.gradle.api.Project

class CargoContextResolver {

    private static interface Resolver {
        def orDefault(Object defaultValue)
    }

    private Boolean isRemoteDeploy
    private String whereToDeploy
    private Object cargoDataContainer


    CargoContextResolver(Boolean isRemoteDeploy = true, Project projectLocal, String deployOverride) {
        this.isRemoteDeploy = isRemoteDeploy
        this.whereToDeploy = {

        }

        if (deployOverride != null && !deployOverride.trim().isEmpty()) {
            this.whereToDeploy = deployOverride
        } else {
            if (GitHelper.gitBranch == 'main') this.whereToDeploy = "production"
            else if (GitHelper.gitBranch == 'stage') this.whereToDeploy = "staging"
            else this.whereToDeploy = "dev"
        }

        this.cargoDataContainer = new JsonSlurper().parseText(projectLocal.file('./build-info/cargo-remote.json').text)
    }

    private static class ResolveNullToDefault implements Resolver {
        private Object value;

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

    public def Resolver getByKey(String key) {
        return isRemoteDeploy ? new ResolveNullToDefault(cargoDataContainer[whereToDeploy][key]) : new FakeResolver()
    }

    public def String getWhereToDeploy() {
        return whereToDeploy
    }
}
