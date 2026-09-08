package com.ravenherz.cse.util.pluggable.loaders;

import com.ravenherz.cse.constants.Strings;
import com.ravenherz.cse.util.Json;
import com.ravenherz.cse.util.io.ServletFile;
import com.ravenherz.cse.util.pluggable.SocialNetworkDefinitionLoader;
import com.ravenherz.cse.util.pluggable.extensions.SocialNetworkDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;

@Lazy
@Component(value = "socialNetworkDefinitionLoader")
@Scope(value = "singleton")
@DependsOn(value = "settings")
public class SocialNetworkDefinitionLoaderImpl extends AbstractLoader implements
        SocialNetworkDefinitionLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocialNetworkDefinitionLoaderImpl.class);

    @Override
    public HashMap<String, SocialNetworkDefinition> getAllDefinitions() {
        HashMap<String, SocialNetworkDefinition> result = new HashMap<>();
        try {
            for (Resource resource : ServletFile.list(Strings.PATH_SOCIAL_NETWORK_DEFINITIONS, "*.json")) {
                String name = resource.getFilename();
                if (name == null || !resource.isReadable()) {
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    SocialNetworkDefinition socialNetworkDefinition = Json.read(in,
                            SocialNetworkDefinition.class);
                    if (socialNetworkDefinition == null) {
                        throw new Exception(name);
                    }
                    if (socialNetworkDefinition.isValid()) {
                        result.put(socialNetworkDefinition.getLoader().getPluginName(),
                                socialNetworkDefinition);
                    }
                } catch (Exception e) {
                    LOGGER.error("Cannot read SocialNetworkDefinition from '"
                            + Strings.PATH_SOCIAL_NETWORK_DEFINITIONS + "/" + e.getMessage()
                            + "' ", e);
                }
            }
            LOGGER.info("Loaded SocialNetworkDefinitions: [%s]".formatted(String.join(", ", result.keySet())));
        } catch (Exception e) {
            LOGGER.error("Cannot list SocialNetworkDefinitions", e);
        }
        return result;
    }
}
