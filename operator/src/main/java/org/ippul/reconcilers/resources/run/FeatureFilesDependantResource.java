package org.ippul.reconcilers.resources.run;

import java.util.Set;

import org.ippul.CucumberRun;
import org.ippul.commons.CommonLabels;
import org.ippul.reconcilers.resources.run.discriminators.FeatureFileDiscriminator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

@KubernetesDependent(labelSelector = CommonLabels.MANAGED_BY_SELECTOR, resourceDiscriminator = FeatureFileDiscriminator.class)
public class FeatureFilesDependantResource extends CRUDKubernetesDependentResource<ConfigMap, CucumberRun> implements SecondaryToPrimaryMapper<ConfigMap> {

    public FeatureFilesDependantResource() {
        super(ConfigMap.class);
    }
    
    @Override
    protected ConfigMap desired(CucumberRun resource, Context<CucumberRun> context) {
        return new ConfigMapBuilder()
                .withNewMetadata()
                    .withName(resource.getMetadata().getName() + "-feature-files")
                    .withLabels(CommonLabels.getLabels(resource))
                .endMetadata()
                .addToData(resource.getSpec().getFeatrues())
                .build();
    }

    @Override
    public Result<ConfigMap> match(ConfigMap actual, CucumberRun primary, Context<CucumberRun> context) {
        return Result.nonComputed(actual.getMetadata().getName().equals(primary.getMetadata().getName() + "-feature-files"));
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(ConfigMap resource) {
        String name = resource.getMetadata().getName();
        return Set.of(new ResourceID(name, resource.getMetadata().getNamespace()));
    }

}
