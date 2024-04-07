package org.ippul.reconcilers.resources.instance;

import java.util.Set;

import org.ippul.CucumberInstance;
import org.ippul.commons.CommonLabels;
import org.ippul.reconcilers.resources.instance.discriminators.PVCDiscriminator;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

@KubernetesDependent(labelSelector = CommonLabels.MANAGED_BY_SELECTOR, resourceDiscriminator=PVCDiscriminator.class)
public class PersistentVolumeClaimDependantResource extends CRUDKubernetesDependentResource<PersistentVolumeClaim, CucumberInstance> implements SecondaryToPrimaryMapper<PersistentVolumeClaim>{

    public PersistentVolumeClaimDependantResource() {
        super(PersistentVolumeClaim.class);
    }
    
    @Override
    protected PersistentVolumeClaim desired(CucumberInstance resource, Context<CucumberInstance> context) {
        return new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                    .withName(resource.getMetadata().getName() + "-pvc")
                    .withLabels(CommonLabels.getLabels(resource))
                    .addToLabels("app", "cucumber-nginx")
                .endMetadata()
                .withNewSpec()
                    .withStorageClassName("crc-csi-hostpath-provisioner")
                    .withAccessModes("ReadWriteMany")
                    .withNewResources()
                        .addToRequests("storage", new Quantity("1Gi"))
                    .endResources()
                .endSpec()
                .build();
    }

    @Override
    public Result<PersistentVolumeClaim> match(PersistentVolumeClaim actual, CucumberInstance primary, Context<CucumberInstance> context) {
        return Result.nonComputed(actual.getMetadata().getName().equals(primary.getMetadata().getName() + "-pvc"));
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(PersistentVolumeClaim resource) {
        String name = resource.getMetadata().getName();
        return Set.of(new ResourceID(name, resource.getMetadata().getNamespace()));
    }

}
