package org.ippul.reconcilers.resources.instance.discriminators;

import java.util.Optional;

import org.ippul.CucumberInstance;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public class DeploymentDiscriminator implements ResourceDiscriminator<Deployment, CucumberInstance> {

    @Override
    public Optional<Deployment> distinguish(Class<Deployment> resource, CucumberInstance primary, Context<CucumberInstance> context) {
        InformerEventSource<Deployment, CucumberInstance> ies =
        (InformerEventSource<Deployment, CucumberInstance>) context.eventSourceRetriever().getResourceEventSourceFor(Deployment.class, "deployments");
        return ies.get(new ResourceID(primary.getMetadata().getName() + "-nginx", primary.getMetadata().getNamespace()));
    }
}
