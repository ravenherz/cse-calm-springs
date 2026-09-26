package com.ravenherz.cse.core.admin;

import java.util.List;

/**
 * One input on a generic editor form. {@code name} matches the owning module's store property.
 */
public final class AdminField {

    private final String name;
    private final String label;
    private final FieldType type;
    private final boolean required;
    private final List<String> options;
    private final CardPlace cardPlace;

    public AdminField(String name, String label, FieldType type, boolean required, List<String> options) {
        this(name, label, type, required, options, CardPlace.NONE);
    }

    public AdminField(String name, String label, FieldType type, boolean required, List<String> options,
            CardPlace cardPlace) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("field name is required");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("field label is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("field type is required");
        }
        if (type == FieldType.ENUM && (options == null || options.isEmpty())) {
            throw new IllegalArgumentException("enum field requires options");
        }
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
        this.options = options == null ? List.of() : List.copyOf(options);
        this.cardPlace = cardPlace == null ? CardPlace.NONE : cardPlace;
    }

    public String name() {
        return name;
    }

    public String label() {
        return label;
    }

    public FieldType type() {
        return type;
    }

    public boolean required() {
        return required;
    }

    public List<String> options() {
        return options;
    }

    public CardPlace cardPlace() {
        return cardPlace;
    }
}
