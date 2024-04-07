package org.ippul.crds.instance;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

public class CucumberInstanceStatus extends ObservedGenerationAwareStatus {
    
    private String message;

    public CucumberInstanceStatus() {
    }

    public CucumberInstanceStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
