package com.ravenherz.cse.security;

import java.util.List;

/** Installed app slugs. The WAR supplies this so security does not import app documents. */
public interface AppSlugSource {

    List<String> slugs();
}
