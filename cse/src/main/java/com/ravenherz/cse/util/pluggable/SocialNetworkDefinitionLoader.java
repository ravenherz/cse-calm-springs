package com.ravenherz.cse.util.pluggable;

import com.ravenherz.cse.util.pluggable.extensions.SocialNetworkDefinition;

import java.util.HashMap;

public interface SocialNetworkDefinitionLoader {
    HashMap <String, SocialNetworkDefinition> getAllDefinitions ();
}
