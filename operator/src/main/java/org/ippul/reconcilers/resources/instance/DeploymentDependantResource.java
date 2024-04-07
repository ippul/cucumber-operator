package org.ippul.reconcilers.resources.instance;

import java.util.Set;

import org.ippul.CucumberInstance;
import org.ippul.CucumberRun;
import org.ippul.commons.CommonLabels;
import org.ippul.reconcilers.resources.instance.discriminators.DeploymentDiscriminator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

@KubernetesDependent(labelSelector = CommonLabels.MANAGED_BY_SELECTOR, resourceDiscriminator = DeploymentDiscriminator.class)
public class DeploymentDependantResource extends CRUDKubernetesDependentResource<Deployment, CucumberInstance> implements SecondaryToPrimaryMapper<Deployment> {

    public DeploymentDependantResource() {
        super(Deployment.class);
    }
    
    @Override
    protected Deployment desired(CucumberInstance resource, Context<CucumberInstance> context) {

        final PersistentVolumeClaimVolumeSource persistentVolumeClaimVolumeSource = new PersistentVolumeClaimVolumeSourceBuilder()
            .withClaimName(resource.getMetadata().getName() + "-pvc")
            .withReadOnly(false)
            .build();
        final Volume volume = new VolumeBuilder()
            .withName(resource.getMetadata().getName() + "-pvc")
            .withPersistentVolumeClaim(persistentVolumeClaimVolumeSource)
            .build();
        final VolumeMount volumeMount = new VolumeMountBuilder()
            .withName(resource.getMetadata().getName() + "-pvc")
            .withMountPath("/data/")
            .build();
        return new DeploymentBuilder()
            .withNewMetadata()
                .withName(resource.getMetadata().getName() + "-nginx")
                .withLabels(CommonLabels.getLabels(resource))
                .addToLabels("app", "cucumber-nginx")
            .endMetadata()
            .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                    .addToMatchLabels("app", "cucumber-nginx")
                .endSelector()
            .withNewTemplate()
                .withNewMetadata()
                    .withLabels(CommonLabels.getLabels(resource))
                    .addToLabels("app", "cucumber-nginx")
                .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName("cucumber-nginx")
                        .withImage("image-registry.openshift-image-registry.svc:5000/cucumber-operator/nginx:1.0.0")
                        .withImagePullPolicy("Always")
                        .addNewPort()
                            .withContainerPort(8080)
                        .endPort()
                    .withVolumeMounts(volumeMount)
                    .endContainer()
                    .withVolumes(volume)
                .endSpec()
            .endTemplate()
            .endSpec()
        .build();
    }

    @Override
    public Result<Deployment> match(Deployment actual, CucumberInstance primary, Context<CucumberInstance> context) {
        return Result.nonComputed(actual.getMetadata().getName().equals(primary.getMetadata().getName() + "-nginx"));
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(Deployment resource) {
        String name = resource.getMetadata().getName();
        return Set.of(new ResourceID(name, resource.getMetadata().getNamespace()));
    }

}
