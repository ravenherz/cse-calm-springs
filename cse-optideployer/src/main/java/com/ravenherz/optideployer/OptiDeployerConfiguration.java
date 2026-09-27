package com.ravenherz.optideployer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class OptiDeployerConfiguration {

    @Bean
    public OptiConfig optiConfig() throws IOException {
        ClassPathResource resource = new ClassPathResource("optideploy.json");
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "optideploy.json is not on the classpath. It is generated from .cse-deployment.json when this WAR is built.");
        }
        try (InputStream in = resource.getInputStream()) {
            return OptiConfig.read(in);
        }
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
