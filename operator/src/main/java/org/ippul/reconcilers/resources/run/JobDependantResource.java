package org.ippul.reconcilers.resources.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ippul.CucumberRun;
import org.ippul.commons.CommonLabels;
import org.ippul.crds.run.ConfigMapRef;
import org.ippul.crds.run.SecretRef;
import org.ippul.reconcilers.resources.run.discriminators.FeatureFileDiscriminator;
import org.ippul.reconcilers.resources.run.discriminators.JobFileDiscriminator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapProjection;
import io.fabric8.kubernetes.api.model.ConfigMapProjectionBuilder;
import io.fabric8.kubernetes.api.model.KeyToPath;
import io.fabric8.kubernetes.api.model.KeyToPathBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.SecretProjection;
import io.fabric8.kubernetes.api.model.SecretProjectionBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.VolumeProjection;
import io.fabric8.kubernetes.api.model.VolumeProjectionBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

@KubernetesDependent(labelSelector = CommonLabels.MANAGED_BY_SELECTOR, resourceDiscriminator = JobFileDiscriminator.class)
public class JobDependantResource extends CRUDKubernetesDependentResource<Job, CucumberRun> implements SecondaryToPrimaryMapper<Job> {

    public JobDependantResource() {
        super(Job.class);
    }
    
    @Override
    protected Job desired(CucumberRun resource, Context<CucumberRun> context) {
        final List<VolumeMount> volumeMounts = new ArrayList<>();
        final List<Volume> volumes = new ArrayList<>();
        final List<VolumeProjection> fetureFileVolumeProjections = new ArrayList<>();
        final List<VolumeProjection> confVolumeProjections = new ArrayList<>();
        // Feature files
        for(Map.Entry<String, String> entry : resource.getSpec().getFeatrues().entrySet()) {
            fetureFileVolumeProjections.add(creaVolumeProjectionConfigMap(resource.getMetadata().getName() + "-feature-files", entry.getKey(), entry.getKey()));
            volumeMounts.add(new VolumeMountBuilder()
                    .withName(resource.getMetadata().getName() + "-features")
                    .withMountPath("/data/features/" + entry.getKey())
                    .withSubPath(entry.getKey())
                .build());
        }
        volumes.add(new VolumeBuilder()
                .withName(resource.getMetadata().getName() + "-features")
                .withNewProjected()
                    .withSources(fetureFileVolumeProjections)
                .endProjected()
                .build());
        
        //
        //
        confVolumeProjections.add(creaVolumeProjectionConfigMap(resource.getMetadata().getName() + "-run", "run.properties", resource.getMetadata().getName() + ".properties"));
        confVolumeProjections.add(creaVolumeProjectionConfigMap(resource.getMetadata().getName() + "-settings", "settings.xml", "settings.xml"));
        if(resource.getSpec().getSecrets()!=null){
            for(SecretRef secretRef : resource.getSpec().getSecrets()) {
                confVolumeProjections.add(creaVolumeProjectionSecret(secretRef.getName(), secretRef.getKey(), secretRef.getPath()));
            }
        }
        if(resource.getSpec().getConfigMaps()!=null){
            for(ConfigMapRef configmapRef : resource.getSpec().getConfigMaps()) {
                confVolumeProjections.add(creaVolumeProjectionConfigMap(configmapRef.getName(), configmapRef.getKey(), configmapRef.getPath()));
            }
        }

        volumes.add(new VolumeBuilder()
                .withName(resource.getMetadata().getName() + "-run")
                .withNewProjected()
                    .withSources(confVolumeProjections)
                .endProjected()
                .build());
        volumeMounts.add(new VolumeMountBuilder()
                    .withName(resource.getMetadata().getName() + "-run")
                    .withMountPath("/data/conf/")
                .build());

        final PersistentVolumeClaimVolumeSource persistentVolumeClaimVolumeSource = new PersistentVolumeClaimVolumeSourceBuilder()
                .withClaimName(resource.getSpec().getCucumberInstanceNameRef() + "-pvc")
                .withReadOnly(false)
                .build();
        volumes.add(new VolumeBuilder()
                .withName(resource.getMetadata().getName() + "-pvc")
                .withPersistentVolumeClaim(persistentVolumeClaimVolumeSource)
                .build());
        volumeMounts.add(new VolumeMountBuilder()
                .withName(resource.getMetadata().getName() + "-pvc")
                .withMountPath("/data/")
                .build());
                
        return new JobBuilder()
          .withApiVersion("batch/v1")
          .withNewMetadata()
            .withName(resource.getMetadata().getName() + "-job")
            .withLabels(CommonLabels.getLabels(resource))
          .endMetadata()
          .withNewSpec()
            .withBackoffLimit(0)
            .withNewTemplate()
                .withNewSpec()
                    .withRestartPolicy("Never")
                    .addNewContainer()
                        .addNewEnv()
                            .withName("RUN_ID")
                            .withValue(resource.getMetadata().getName())
                        .endEnv()
                        .withName(resource.getMetadata().getName() + "-test")
                        .withImage("image-registry.openshift-image-registry.svc:5000/cucumber-operator/runner:1.0.0")
                        .withImagePullPolicy("Always")
                        .withVolumeMounts(volumeMounts)
                        .withNewResources()
                            .addToLimits(Collections.singletonMap("cpu", new Quantity("250m")))
                            .addToRequests(Collections.singletonMap("cpu", new Quantity("100m")))
                            .addToLimits(Collections.singletonMap("memory", new Quantity("1000Mi")))
                            .addToRequests(Collections.singletonMap("memory", new Quantity("250Mi")))
                        .endResources()
                    .endContainer()
                    .withVolumes(volumes)
                    .withRestartPolicy("Never")
                .endSpec()
            .endTemplate()
          .endSpec()
          .build();
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(Job resource) {
        String name = resource.getMetadata().getName();
        return Set.of(new ResourceID(name, resource.getMetadata().getNamespace()));
    }


    private VolumeProjection creaVolumeProjectionConfigMap(String configMapProjectionName, String key, String path){
        final KeyToPath keyToPath = new KeyToPathBuilder().withKey(key).withPath(path).build();
        final ConfigMapProjection configMapProjection = new ConfigMapProjectionBuilder()
        .withName(configMapProjectionName)
        .withItems(keyToPath)
        .build();
        return new VolumeProjectionBuilder()
            .withConfigMap(configMapProjection)
            .build();
    }

    private VolumeProjection creaVolumeProjectionSecret(String secretProjectionName, String key, String path){
        final KeyToPath keyToPath = new KeyToPathBuilder().withKey(key).withPath(path).build();
        final SecretProjection secretProjection = new SecretProjectionBuilder()
        .withName(secretProjectionName)
        .withItems(keyToPath)
        .build();
        return new VolumeProjectionBuilder()
            .withSecret(secretProjection)
            .build();
    }
}
