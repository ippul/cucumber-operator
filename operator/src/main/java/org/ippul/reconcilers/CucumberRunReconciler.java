package org.ippul.reconcilers;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

import org.ippul.CucumberRun;
import org.ippul.commons.Fabric8ModelUtils;
import org.ippul.crds.run.CucumberRunStatus;
import org.ippul.reconcilers.resources.run.FeatureFilesDependantResource;
import org.ippul.reconcilers.resources.run.JobDependantResource;
import org.ippul.reconcilers.resources.run.MavenSettingsDependantResource;
import org.ippul.reconcilers.resources.run.RunPropertiesDependantResource;
import org.ippul.reconcilers.resources.run.discriminators.JobFileDiscriminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerConfiguration(
    name = "cucumber-run-reconciler", 
    namespaces = Constants.WATCH_CURRENT_NAMESPACE,
    dependents = {
        @Dependent(name = "cucumberjob", type = JobDependantResource.class, 
            dependsOn = {
                "featureFiles",
                "runProperties",
                "mavenSetting"
            }
        ),
        @Dependent(name = "featureFiles", type = FeatureFilesDependantResource.class),
        @Dependent(name = "runProperties", type = RunPropertiesDependantResource.class),
        @Dependent(name = "mavenSetting", type = MavenSettingsDependantResource.class)
    }
)
public class CucumberRunReconciler implements Reconciler<CucumberRun>, Cleaner<CucumberRun>, ErrorStatusHandler<CucumberRun> {

    private static final Logger logger = LoggerFactory.getLogger(CucumberRunReconciler.class);

    private final Fabric8ModelUtils utils;

    public CucumberRunReconciler(KubernetesClient kubernetesClient) {
        this.utils = new Fabric8ModelUtils(kubernetesClient.adapt(OpenShiftClient.class));
    }

    @Override
    public DeleteControl cleanup(CucumberRun resource, Context<CucumberRun> context) {
        logger.info("cleanup crd: {}", resource.getSpec());
        utils.safeDeleteResource(resource);
        return DeleteControl.defaultDelete();
    }

    @Override
    public UpdateControl<CucumberRun> reconcile(CucumberRun resource, Context<CucumberRun> context) throws Exception {
        logger.info("reconcile crd: {}", resource.getSpec());
        return context.getSecondaryResource(Job.class, new JobFileDiscriminator()).map(deployment -> {
            logger.info("{}", deployment.getStatus());
            resource.setStatus(new CucumberRunStatus("RECONCILED"));
            return UpdateControl.patchStatus(resource);
        })
        .orElseGet(UpdateControl::noUpdate);
    }

    @Override
    public ErrorStatusUpdateControl<CucumberRun> updateErrorStatus(CucumberRun resource,
        Context<CucumberRun> context,
        Exception e) {
            CucumberRunStatus status = new CucumberRunStatus();
      status.setMessage("ERROR: " + e.getMessage());
      resource.setStatus(status);
      return ErrorStatusUpdateControl.updateStatus(resource);
    }
}
