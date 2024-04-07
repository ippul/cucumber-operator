package org.ippul.reconcilers;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.*;

import java.util.concurrent.TimeUnit;
import org.ippul.CucumberInstance;
import org.ippul.commons.Fabric8ModelUtils;
import org.ippul.crds.instance.CucumberInstanceStatus;
import org.ippul.reconcilers.resources.instance.DeploymentDependantResource;
import org.ippul.reconcilers.resources.instance.PersistentVolumeClaimDependantResource;
import org.ippul.reconcilers.resources.instance.RouteDependantResource;
import org.ippul.reconcilers.resources.instance.ServiceDependantResource;
import org.ippul.reconcilers.resources.instance.discriminators.DeploymentDiscriminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.rate.RateLimited;

@RateLimited(maxReconciliations=2, within=5, unit=TimeUnit.SECONDS)
@ControllerConfiguration(
    name = "cucumber-instance-reconciler",
    maxReconciliationInterval = @MaxReconciliationInterval(
                interval = 10,
                timeUnit = TimeUnit.SECONDS),
    namespaces = Constants.WATCH_CURRENT_NAMESPACE,
    dependents = {
        @Dependent(name="pvcs", type = PersistentVolumeClaimDependantResource.class),
        @Dependent(name="deployments", type = DeploymentDependantResource.class, dependsOn = "pvcs"),
        @Dependent(name="services", type = ServiceDependantResource.class, dependsOn = "deployments"),
        @Dependent(name="routes", type = RouteDependantResource.class, dependsOn = "services")
    }
)
public class CucumberInstanceReconciler implements Reconciler<CucumberInstance>, Cleaner<CucumberInstance>, ErrorStatusHandler<CucumberInstance> {

    private static final Logger logger = LoggerFactory.getLogger(CucumberInstanceReconciler.class);
    
    private final Fabric8ModelUtils utils;

    public CucumberInstanceReconciler(KubernetesClient kubernetesClient) {
        this.utils = new Fabric8ModelUtils(kubernetesClient.adapt(OpenShiftClient.class));
    }

    @Override
    public UpdateControl<CucumberInstance> reconcile(CucumberInstance resource, Context<CucumberInstance> context) throws Exception {
        if(resource.getStatus()==null){
            logger.info("reconcile crd: {}", resource.getSpec());
        } else {
            logger.info("reconcile crd: {}, status: {}, {}", resource.getSpec(), resource.getStatus().getMessage(), resource.getStatus().getObservedGeneration());
        }
        return context.getSecondaryResource(Deployment.class, new DeploymentDiscriminator()).map(deployment -> {
            logger.info("{}", deployment.getStatus());
            CucumberInstanceStatus status = new CucumberInstanceStatus("RECONCILED");
            resource.setStatus(status);
            return UpdateControl.patchStatus(resource);
           
        })
        .orElseGet(() -> {
            logger.info("no update: {}", resource.getSpec());
            return UpdateControl.noUpdate();
        });
    }

    @Override
    public ErrorStatusUpdateControl<CucumberInstance> updateErrorStatus(CucumberInstance resource,
        Context<CucumberInstance> context,
        Exception e) {
            CucumberInstanceStatus status = new CucumberInstanceStatus();
      status.setMessage("ERROR: " + e.getMessage());
      resource.setStatus(status);
      return ErrorStatusUpdateControl.updateStatus(resource);
    }

    @Override
    public DeleteControl cleanup(CucumberInstance resource, Context<CucumberInstance> context) {
        logger.info("cleanup crd: {}", resource.getSpec());
        utils.safeDeleteResource(resource);
        return DeleteControl.defaultDelete();
    }

}
