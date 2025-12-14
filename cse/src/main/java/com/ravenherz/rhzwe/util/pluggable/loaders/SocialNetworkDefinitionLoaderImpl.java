package com.ravenherz.rhzwe.util.pluggable.loaders;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.io.ServletFile;
import com.ravenherz.rhzwe.util.pluggable.SocialNetworkDefinitionLoader;
import com.ravenherz.rhzwe.util.pluggable.extensions.SocialNetworkDefinition;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

@Lazy
@Component(value = "socialNetworkDefinitionLoader")
@Scope(value = "singleton")
@DependsOn(value = "settings")
public class SocialNetworkDefinitionLoaderImpl extends AbstractLoader implements
        SocialNetworkDefinitionLoader {

    private static final Logger LOGGER =  LoggerFactory.getLogger(SocialNetworkDefinitionLoaderImpl.class);

    @Override
    public HashMap<String, SocialNetworkDefinition> getAllDefinitions() {
        HashMap<String, SocialNetworkDefinition> result = new HashMap<>();
        File rootDirectory = ServletFile
                .classPathFile(Strings.PATH_SOCIAL_NETWORK_DEFINITIONS);
        if (rootDirectory.isDirectory()) {
             Arrays
                     .stream(Objects.requireNonNull(rootDirectory.listFiles()))
                     .filter(File::isFile)
                     .filter(file -> file.getName().contains(".json"))
                     .filter(File::canRead)
                     .toList()
                     .forEach(file -> {
                         try (JsonReader reader = new JsonReader(new FileReader(file))) {
                             SocialNetworkDefinition socialNetworkDefinition = new Gson()
                                     .fromJson(reader, SocialNetworkDefinition.class);
                             if (socialNetworkDefinition != null) {
                                 if (socialNetworkDefinition.isValid()) {
                                     result.put(socialNetworkDefinition.getLoader().getPluginName(),
                                             socialNetworkDefinition);
                                 }
                             } else {
                                 throw new Exception(file.getName());
                             }
                         } catch (Exception e) {
                             LOGGER.error("Cannot read SocialNetworkDefinition from '"
                                     + Strings.PATH_SOCIAL_NETWORK_DEFINITIONS + "/" + e.getMessage()
                                     + "' ", e);
                         }
                     });
            LOGGER.info("Loaded SocialNetworkDefinitions: [%s]".formatted(String.join(", ", result.keySet())));
        }

        return result;
    }
}
