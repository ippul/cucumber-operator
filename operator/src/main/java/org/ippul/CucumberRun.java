package org.ippul;

import org.ippul.crds.run.CucumberRunSpec;
import org.ippul.crds.run.CucumberRunStatus;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("ippul.org")
public class CucumberRun extends CustomResource<CucumberRunSpec, CucumberRunStatus> implements Namespaced {}