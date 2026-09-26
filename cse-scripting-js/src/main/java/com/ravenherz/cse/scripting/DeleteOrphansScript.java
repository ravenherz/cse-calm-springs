package com.ravenherz.cse.scripting;

/**
 * Shipped script. The editor stores this source; {@code cse.deleteOrphans()} removes
 * resources with no group and references that point at a missing row.
 */
public final class DeleteOrphansScript {

    public static final String ID = "delete-orphans";
    public static final String FOLDER = "maintenance";
    public static final String SOURCE = """
            cse.deleteOrphans();
            """;

    private DeleteOrphansScript() {
    }
}
