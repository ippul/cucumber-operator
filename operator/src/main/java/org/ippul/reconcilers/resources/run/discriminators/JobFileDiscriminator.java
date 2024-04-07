package org.ippul.reconcilers.resources.run.discriminators;

import java.util.Optional;

import org.ippul.CucumberRun;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public class JobFileDiscriminator implements ResourceDiscriminator<Job, CucumberRun> {

    @Override
    public Optional<Job> distinguish(Class<Job> resource, CucumberRun primary,  Context<CucumberRun> context) {
        InformerEventSource<Job, CucumberRun> ies =
        (InformerEventSource<Job, CucumberRun>) context.eventSourceRetriever().getResourceEventSourceFor(Job.class, "cucumberjob");
        return ies.get(new ResourceID(primary.getMetadata().getName() + "-job", primary.getMetadata().getNamespace()));
    }    
}
