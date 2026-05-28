package ru.copperside.sbprouter.management.routingconfig.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldBinding(String name, String parent, String key, String path) {

    @JsonIgnore
    public boolean hasParentKey() {
        return parent != null && !parent.isBlank() && key != null && !key.isBlank();
    }

    @JsonIgnore
    public boolean hasPath() {
        return path != null && !path.isBlank();
    }

    /** Exactly one of (parent+key) or path must be present, and name must be set. */
    @JsonIgnore
    public boolean isValid() {
        if (name == null || name.isBlank()) {
            return false;
        }
        return hasParentKey() ^ hasPath();
    }
}
