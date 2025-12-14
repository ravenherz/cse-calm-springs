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


    CargoContextResolver(Boolean isRemoteDeploy = true, Project projectLocal) {
        this.isRemoteDeploy = isRemoteDeploy
        this.whereToDeploy = "release" == 'release' ? "production" : "testing" // todo: add call for getGitBranch
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
}
