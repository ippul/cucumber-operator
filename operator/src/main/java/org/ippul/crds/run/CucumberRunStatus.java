package org.ippul.crds.run;

public class CucumberRunStatus {

    private String message;

    public CucumberRunStatus() {
    }

    public CucumberRunStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
