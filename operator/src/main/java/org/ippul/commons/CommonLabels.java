package org.ippul.commons;

import java.util.Map;

import io.fabric8.kubernetes.client.CustomResource;

public class CommonLabels {
    
    public static final String APP_LABEL = "app.kubernetes.io/name";
    
    public static final String MANAGED_BY_KEY = "app.kubernetes.io/managed-by";
    
    public static final String MANAGED_BY_VALUE = "cucumber-operator-controller";

    public static final String MANAGED_BY_SELECTOR = MANAGED_BY_KEY + "=" + MANAGED_BY_VALUE;


    public static Map<String, String> getLabels(CustomResource resource) {
        return Map.of(
                APP_LABEL, resource.getMetadata().getName(),
                MANAGED_BY_KEY, MANAGED_BY_VALUE
        );
    }

}
