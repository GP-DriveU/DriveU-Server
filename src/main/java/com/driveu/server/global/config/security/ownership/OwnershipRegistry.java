package com.driveu.server.global.config.security.ownership;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OwnershipRegistry {

    private final Map<Class<?>, OwnershipVerifier<?>> registry;

    public OwnershipRegistry(List<OwnershipVerifier<?>> verifiers) {
        this.registry = verifiers.stream()
                .collect(Collectors.toMap(OwnershipVerifier::getSupportedType, v -> v));
    }

    @SuppressWarnings("unchecked")
    public <T> OwnershipVerifier<T> getVerifier(Class<T> type) {
        OwnershipVerifier<T> verifier = (OwnershipVerifier<T>) registry.get(type);
        if (verifier == null) {
            throw new IllegalArgumentException("No OwnershipVerifier registered for type: " + type.getName());
        }
        return verifier;
    }
}