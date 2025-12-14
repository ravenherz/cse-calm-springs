package com.ravenherz.build.utils

class GitHelper {
    public static def String getGitBranch() {
        var commandResult = 'git rev-parse --abbrev-ref HEAD'.execute()
        commandResult.waitFor()
        String gitBranch = commandResult.text.trim()
        gitBranch = gitBranch.isEmpty() ? "cicd_temp" : gitBranch
        return gitBranch
    }
}
