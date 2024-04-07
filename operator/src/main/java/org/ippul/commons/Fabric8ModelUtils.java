package org.ippul.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;

public class Fabric8ModelUtils {

    private static final Logger logger = LoggerFactory.getLogger(Fabric8ModelUtils.class);

    private final OpenShiftClient openshiftClient;

    public Fabric8ModelUtils(OpenShiftClient openshiftClient) {
        this.openshiftClient = openshiftClient;
    }

    public void safeCreateResource(CustomResource<?, ?> customeResource, HasMetadata obj) {
        final HasMetadata resource = openshiftClient.resource(obj).inNamespace(customeResource.getMetadata().getNamespace()).get();
        if (resource == null) {
            logger.info("No resource with {} found...creating one", obj.getClass().getName());
            openshiftClient.resource(obj).inNamespace(customeResource.getMetadata().getNamespace()).create();
        }
    }

    public void safeDeleteResource(CustomResource<?, ?> customeResource) {
        openshiftClient.resources(Job.class).inNamespace(customeResource.getMetadata().getNamespace()).withLabels(CommonLabels.getLabels(customeResource)).delete();
        openshiftClient.resources(CronJob.class).inNamespace(customeResource.getMetadata().getNamespace()).withLabels(CommonLabels.getLabels(customeResource)).delete();
        openshiftClient.resources(ConfigMap.class).inNamespace(customeResource.getMetadata().getNamespace()).withLabels(CommonLabels.getLabels(customeResource)).delete();
        openshiftClient.resources(Secret.class).inNamespace(customeResource.getMetadata().getNamespace()).withLabels(CommonLabels.getLabels(customeResource)).delete();
        openshiftClient.resources(PersistentVolumeClaim.class).inNamespace(customeResource.getMetadata().getNamespace()).withLabels(CommonLabels.getLabels(customeResource)).delete();
        openshiftClient.resources(Service.class).inNamespace(customeResource.getMetadata().getNamespace()).withLabels(CommonLabels.getLabels(customeResource)).delete();
        openshiftClient.resources(Route.class).inNamespace(customeResource.getMetadata().getNamespace()).withLabels(CommonLabels.getLabels(customeResource)).delete();
        openshiftClient.resources(Deployment.class).inNamespace(customeResource.getMetadata().getNamespace()).withLabels(CommonLabels.getLabels(customeResource)).delete();
    }

}
