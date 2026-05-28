package ru.copperside.sbprouter.management.routingconfig.domain;

public record FieldBinding(String name, String parent, String key, String path) {

    public boolean hasParentKey() {
        return parent != null && !parent.isBlank() && key != null && !key.isBlank();
    }

    public boolean hasPath() {
        return path != null && !path.isBlank();
    }

    /** Exactly one of (parent+key) or path must be present, and name must be set. */
    public boolean isValid() {
        if (name == null || name.isBlank()) {
            return false;
        }
        return hasParentKey() ^ hasPath();
    }
}
