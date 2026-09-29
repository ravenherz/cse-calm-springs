package com.ravenherz.optideployer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class OptiDeployerConfiguration {

    static final Path DEFAULT_CONFIG = Path.of("/var/cse/optideploy.json");

    @Bean
    public OptiConfig optiConfig() throws IOException {
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("optideploy.json is missing at " + path);
        }
        try (InputStream in = Files.newInputStream(path)) {
            return OptiConfig.read(in);
        }
    }

    static Path configPath() {
        String env = System.getenv("CSE_OPTI_CONFIG");
        if (env != null && !env.isBlank()) {
            return Path.of(env);
        }
        return DEFAULT_CONFIG;
    }

    @Bean
    public ManagerClient managerClient(OptiConfig config) {
        return new TomcatManager(config);
    }

    @Bean
    public UploadService uploadService(OptiConfig config, ManagerClient managerClient) {
        return new UploadService(config, managerClient);
    }
}
