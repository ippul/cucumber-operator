package org.ippul.reconcilers.resources.instance.discriminators;

import java.util.Optional;

import org.ippul.CucumberInstance;

import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public class RouteDiscriminator  implements ResourceDiscriminator<Route, CucumberInstance> {

    @Override
    public Optional<Route> distinguish(Class<Route> resource, CucumberInstance primary, Context<CucumberInstance> context) {
        InformerEventSource<Route, CucumberInstance> ies =
        (InformerEventSource<Route, CucumberInstance>) context.eventSourceRetriever().getResourceEventSourceFor(Route.class, "routes");
        return ies.get(new ResourceID(primary.getMetadata().getName() + "-route-nginx", primary.getMetadata().getNamespace()));
    }
}