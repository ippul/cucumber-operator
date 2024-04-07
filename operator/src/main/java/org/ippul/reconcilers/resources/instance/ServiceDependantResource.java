package org.ippul.reconcilers.resources.instance;

import java.util.Map;
import java.util.Set;

import org.ippul.CucumberInstance;
import org.ippul.commons.CommonLabels;
import org.ippul.reconcilers.resources.instance.discriminators.ServiceDiscriminator;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

@KubernetesDependent(labelSelector = CommonLabels.MANAGED_BY_SELECTOR, resourceDiscriminator = ServiceDiscriminator.class)
public class ServiceDependantResource extends CRUDKubernetesDependentResource<Service, CucumberInstance> implements SecondaryToPrimaryMapper<Service> {

    public ServiceDependantResource() {
        super(Service.class);
    }
    
    @Override
    protected Service desired(CucumberInstance resource, Context<CucumberInstance> context) {
        return new ServiceBuilder()
            .withNewMetadata()
                .withName(resource.getMetadata().getName() + "-svc-nginx")
                .withLabels(CommonLabels.getLabels(resource))
                .addToLabels("app", "cucumber-nginx")
            .endMetadata()
            .withNewSpec()
                .withSelector(Map.of("app", "cucumber-nginx"))
                .withPorts(new ServicePortBuilder().withPort(8080).withName("http").build())
                .withType("NodePort")
            .endSpec()
            .build();
    }

    @Override
    public Result<Service> match(Service actual, CucumberInstance primary, Context<CucumberInstance> context) {
        return Result.nonComputed(actual.getMetadata().getName().equals(primary.getMetadata().getName() + "-svc-nginx"));
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(Service resource) {
        String name = resource.getMetadata().getName();
        return Set.of(new ResourceID(name, resource.getMetadata().getNamespace()));
    }

}
