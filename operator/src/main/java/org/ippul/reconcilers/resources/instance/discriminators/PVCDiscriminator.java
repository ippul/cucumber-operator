package org.ippul.reconcilers.resources.instance.discriminators;

import java.util.Optional;

import org.ippul.CucumberInstance;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public class PVCDiscriminator implements ResourceDiscriminator<PersistentVolumeClaim, CucumberInstance> {

    @Override
    public Optional<PersistentVolumeClaim> distinguish(Class<PersistentVolumeClaim> resource, CucumberInstance primary, Context<CucumberInstance> context) {
        InformerEventSource<PersistentVolumeClaim, CucumberInstance> ies =
        (InformerEventSource<PersistentVolumeClaim, CucumberInstance>) context.eventSourceRetriever().getResourceEventSourceFor(PersistentVolumeClaim.class, "pvcs");
        return ies.get(new ResourceID(primary.getMetadata().getName() + "-pvc", primary.getMetadata().getNamespace()));
    }
}