
package org.ippul;

import org.ippul.crds.instance.CucumberInstanceSpec;
import org.ippul.crds.instance.CucumberInstanceStatus;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("ippul.org")
public class CucumberInstance extends CustomResource<CucumberInstanceSpec, CucumberInstanceStatus> implements Namespaced {}
