package org.ippul.reconcilers.resources.run.discriminators;

import java.util.Optional;

import org.ippul.CucumberRun;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public class MavenSettingDiscriminator implements ResourceDiscriminator<ConfigMap, CucumberRun> {

    @Override
    public Optional<ConfigMap> distinguish(Class<ConfigMap> resource, CucumberRun primary,  Context<CucumberRun> context) {
        InformerEventSource<ConfigMap, CucumberRun> ies =
        (InformerEventSource<ConfigMap, CucumberRun>) context.eventSourceRetriever().getResourceEventSourceFor(ConfigMap.class, "mavenSetting");
        return ies.get(new ResourceID(primary.getMetadata().getName() + "-settings", primary.getMetadata().getNamespace()));
    }   
}
