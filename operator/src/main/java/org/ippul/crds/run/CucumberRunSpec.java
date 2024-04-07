package org.ippul.crds.run;

import java.util.List;
import java.util.Map;

public class CucumberRunSpec {

    private String cronExpression;
    
    private String mavenSettings;

    private String cucumberInstanceNameRef;
    
    private List<String> stepJars;

    private List<String> glues;

    private List<String> filterTags;

    private List<SecretRef> secrets;
    
    private List<ConfigMapRef> configMaps;
    
    private Map<String, String> featrues;

    public String getCronExpression() {
        return cronExpression;
    }
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    public String getMavenSettings() {
        return mavenSettings;
    }
    public void setMavenSettings(String mavenSettings) {
        this.mavenSettings = mavenSettings;
    }
    public List<String> getStepJars() {
        return stepJars;
    }
    public void setStepJars(List<String> stepJars) {
        this.stepJars = stepJars;
    }
    public List<SecretRef> getSecrets() {
        return secrets;
    }
    public void setSecrets(List<SecretRef> secrets) {
        this.secrets = secrets;
    }
    public List<ConfigMapRef> getConfigMaps() {
        return configMaps;
    }
    public void setConfigMaps(List<ConfigMapRef> configMaps) {
        this.configMaps = configMaps;
    }
    public Map<String, String> getFeatrues() {
        return featrues;
    }
    public void setFeatrues(Map<String, String> featrues) {
        this.featrues = featrues;
    }
    public String getCucumberInstanceNameRef() {
        return cucumberInstanceNameRef;
    }
    public void setCucumberInstanceNameRef(String cucumberInstanceNameRef) {
        this.cucumberInstanceNameRef = cucumberInstanceNameRef;
    }
    public List<String> getGlues() {
        return glues;
    }
    public void setGlues(List<String> glues) {
        this.glues = glues;
    }
    public List<String> getFilterTags() {
        return filterTags;
    }
    public void setFilterTags(List<String> filterTags) {
        this.filterTags = filterTags;
    }
    @Override
    public String toString() {
        return "CucumberScenarioSpec [cronExpression=" + cronExpression + ", mavenSettings=" + mavenSettings
                + ", cucumberInstanceNameRef=" + cucumberInstanceNameRef + ", stepJars=" + stepJars + ", glues=" + glues
                + ", filterTags=" + filterTags + ", secrets=" + secrets + ", configMaps="
                + configMaps + ", featrues=" + featrues + "]";
    }
    
}
