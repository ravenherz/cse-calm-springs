package com.ravenherz.rhzwe.util.pluggable;

import java.util.Arrays;
import java.util.Objects;

public interface Validatable {

    boolean isValid();

    default boolean someIsNull (Object ... objects) {
        return Arrays.stream(objects).anyMatch(Objects::isNull);
    }
}
