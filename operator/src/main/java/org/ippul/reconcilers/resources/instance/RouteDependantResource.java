package org.ippul.reconcilers.resources.instance;

import java.util.Set;

import org.ippul.CucumberInstance;
import org.ippul.commons.CommonLabels;
import org.ippul.reconcilers.resources.instance.discriminators.RouteDiscriminator;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

@KubernetesDependent(labelSelector = CommonLabels.MANAGED_BY_SELECTOR, resourceDiscriminator=RouteDiscriminator.class)
public class RouteDependantResource extends CRUDKubernetesDependentResource<Route, CucumberInstance> implements SecondaryToPrimaryMapper<Route> {

    public RouteDependantResource() {
        super(Route.class);
    }
    
    @Override
    protected Route desired(CucumberInstance resource, Context<CucumberInstance> context) {
        return new RouteBuilder()
            .withNewMetadata()
                .withName(resource.getMetadata().getName() + "-route-nginx")
                .withLabels(CommonLabels.getLabels(resource))
                .addToLabels("app", "cucumber-nginx")
            .endMetadata()
            .withNewSpec()
                .withTo(new RouteTargetReferenceBuilder()
                    .withKind("Service")
                    .withName(resource.getMetadata().getName() + "-svc-nginx")
                    .withWeight(100).build()
                )
                .withNewPort()
                    .withTargetPort(new IntOrString("http"))
                .endPort()
                .withWildcardPolicy("None")
            .endSpec()
            .build();
    }

    @Override
    public Result<Route> match(Route actual, CucumberInstance primary, Context<CucumberInstance> context) {
        return Result.nonComputed(actual.getMetadata().getName().equals(primary.getMetadata().getName() + "-route-nginx"));
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(Route resource) {
        String name = resource.getMetadata().getName();
        return Set.of(new ResourceID(name, resource.getMetadata().getNamespace()));
    }

}
