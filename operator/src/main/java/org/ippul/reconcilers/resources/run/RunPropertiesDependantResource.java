package org.ippul.reconcilers.resources.run;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.ippul.CucumberRun;
import org.ippul.commons.CommonLabels;
import org.ippul.reconcilers.resources.run.discriminators.RunPropertiesDiscriminator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

@KubernetesDependent(labelSelector = CommonLabels.MANAGED_BY_SELECTOR, resourceDiscriminator = RunPropertiesDiscriminator.class)
public class RunPropertiesDependantResource extends CRUDKubernetesDependentResource<ConfigMap, CucumberRun> implements SecondaryToPrimaryMapper<ConfigMap> {

    public RunPropertiesDependantResource() {
        super(ConfigMap.class);
    }
    
    @Override
    protected ConfigMap desired(CucumberRun resource, Context<CucumberRun> context) {
        return new ConfigMapBuilder()
                .withNewMetadata()
                    .withName(resource.getMetadata().getName() + "-run")
                    .withLabels(CommonLabels.getLabels(resource))
                .endMetadata()
                .addToData("run.properties", generateRunProperties(resource))
                .build();
    }

    @Override
    public Result<ConfigMap> match(ConfigMap actual, CucumberRun primary, Context<CucumberRun> context) {
        return Result.nonComputed(actual.getMetadata().getName().equals(primary.getMetadata().getName() + "-run"));
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(ConfigMap resource) {
        String name = resource.getMetadata().getName();
        return Set.of(new ResourceID(name, resource.getMetadata().getNamespace()));
    }

    // public void useEventSourceWithName(String name) {
    //     super.useEventSourceWithName("run-properties");
    // }

    private String generateRunProperties(CucumberRun resource) {
        final AtomicInteger counter = new AtomicInteger();
        final Properties properties = new Properties();
        for(String curGav : resource.getSpec().getStepJars()) {
            properties.setProperty("cucumber.feature.steps.definition." + counter.getAndIncrement(), curGav);
        }
        if(resource.getSpec().getGlues() != null){
            counter.set(0);
            for(String curGlue : resource.getSpec().getGlues()){
                properties.setProperty("cucumber.glue." + counter.getAndIncrement(), curGlue);
            }
        }
        if(resource.getSpec().getFilterTags() != null){
            counter.set(0);
            for(String curFilterTag : resource.getSpec().getFilterTags()){
                properties.setProperty("cucumber.filter.tag." + counter.getAndIncrement(), curFilterTag);
            }
        }
        final StringWriter writer = new StringWriter();
        try {
            properties.store(writer, "Generated configuration file for " + resource.getMetadata().getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return writer.getBuffer().toString();
    }
}
