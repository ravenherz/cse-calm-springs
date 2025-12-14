package com.ravenherz.rhzwe.util.pluggable;

import com.ravenherz.rhzwe.util.pluggable.extensions.SocialNetworkDefinition;

import java.util.HashMap;

public interface SocialNetworkDefinitionLoader {
    HashMap <String, SocialNetworkDefinition> getAllDefinitions ();
}
