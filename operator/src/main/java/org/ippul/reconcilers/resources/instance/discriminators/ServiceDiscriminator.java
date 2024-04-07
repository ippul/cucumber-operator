package org.ippul.reconcilers.resources.instance.discriminators;

import java.util.Optional;

import org.ippul.CucumberInstance;

import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public class ServiceDiscriminator implements ResourceDiscriminator<Service, CucumberInstance> {

    @Override
    public Optional<Service> distinguish(Class<Service> resource, CucumberInstance primary, Context<CucumberInstance> context) {
        InformerEventSource<Service, CucumberInstance> ies =
        (InformerEventSource<Service, CucumberInstance>) context.eventSourceRetriever().getResourceEventSourceFor(Service.class, "services");
        return ies.get(new ResourceID(primary.getMetadata().getName() + "-svc-nginx", primary.getMetadata().getNamespace()));
    }
}
