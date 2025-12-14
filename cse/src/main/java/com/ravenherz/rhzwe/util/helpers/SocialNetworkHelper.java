package com.ravenherz.rhzwe.util.helpers;

import com.ravenherz.rhzwe.constants.Strings;
import com.ravenherz.rhzwe.util.pluggable.SocialNetworkDefinitionLoader;
import com.ravenherz.rhzwe.util.pluggable.extensions.SocialNetworkDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SocialHelper provides methods, which are being used to
 * manipulate social-networks related configuration dal
 */

@Lazy
@Service
@Scope(value = "singleton")
@DependsOn(value = "socialNetworkDefinitionLoader")
public class SocialNetworkHelper {

    public class SocialNetworkData {

        private String logo;
        private String url;
        private String tooltip;
        private String name;

        public SocialNetworkData(String id, SocialNetworkDefinition def) {
            this.url = String.format(def.getProtocol() + "://" + def.getAddress(), id);
            this.logo = def.getLogo();
            this.tooltip = String.format(def.getTooltipText(), id);
            this.name = def.getLoader().getPluginName();
        }

        public String getLogo() {
            return logo;
        }

        public String getUrl() {
            return url;
        }

        public String getTooltip() {
            return tooltip;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SocialNetworkData that = (SocialNetworkData) o;
            return Objects.equals(logo, that.logo) &&
                    Objects.equals(url, that.url) &&
                    Objects.equals(tooltip, that.tooltip) &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {

            return Objects.hash(logo, url, tooltip, name);
        }
    }

    private Integer cachedHash = null;
    private List<SocialNetworkData> cachedData = null;

    private SocialNetworkDefinitionLoader socialNetworkDefinitionLoader;
    private HashMap<String, SocialNetworkDefinition> definitions = new HashMap<>();

    @Lazy @Autowired
    public void setSocialNetworkDefinitionLoader(
            SocialNetworkDefinitionLoader socialNetworkDefinitionLoader) {
        this.socialNetworkDefinitionLoader = socialNetworkDefinitionLoader;
    }

    @PostConstruct
    public void init() {
        definitions = socialNetworkDefinitionLoader.getAllDefinitions();
    }

    public SocialNetworkHelper() {
    }

    public boolean isSocialNetworkDefinition(String name) {
        return definitions.containsKey(name);
    }

    public List<SocialNetworkData> makeSocialNetworkDataFromString(String rawString) {
        synchronized (SocialNetworkHelper.class) {
            ArrayList<SocialNetworkData> socialNetworkDefinitions = new ArrayList<>();
            rawString = rawString.replaceAll(":+", Strings.STR_DELIM_COLON);
            List<String> filteredStrings = Arrays.asList(rawString.split(Strings.STR_DELIM_WS))
                    .stream()
                    .filter(item -> (item.length() != 0))
                    .filter(item -> (item.split(Strings.STR_DELIM_COLON).length > 1))
                    .filter(item -> definitions.keySet()
                            .contains(item.split(Strings.STR_DELIM_COLON)[0]))
                    .filter(item -> (item.split(Strings.STR_DELIM_COLON)[0].length() > 0))
                    .collect(Collectors.toList());
            for (String source : filteredStrings
                    ) {
                String plugin = source.split(Strings.STR_DELIM_COLON)[0];
                String id = source.split(Strings.STR_DELIM_COLON)[1];

                SocialNetworkData result = new SocialNetworkData(id, definitions.get(plugin));

                if (!socialNetworkDefinitions.contains(result)) {
                    socialNetworkDefinitions.add(result);
                }
            }
            cachedData = socialNetworkDefinitions;
            cachedHash = rawString.hashCode();
            return cachedData;
        }
    }
}
