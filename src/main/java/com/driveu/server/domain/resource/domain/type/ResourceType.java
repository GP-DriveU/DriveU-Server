package com.driveu.server.domain.resource.domain.type;

public enum ResourceType {
    FILE, NOTE, LINK;

    public static ResourceType of(String name) {
        for (ResourceType resourceType : ResourceType.values()) {
            if (resourceType.name().equals(name)) {
                return resourceType;
            }
        }
        throw new IllegalArgumentException("Invalid ResourceType: " + name);
    }
}
