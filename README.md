# java operator sdk example: cucumber-operator
---

- [java operator sdk example: cucumber-operator](#java-operator-sdk-example-cucumber-operator)
  - [1. Build](#1-build)
  - [2. Run the operator](#2-run-the-operator)
  - [3. Testing the operator](#3-testing-the-operator)
    - [3.1 Prepare the the namespace](#31-prepare-the-the-namespace)
    - [3.2 Deploy the test application on Openshift](#32-deploy-the-test-application-on-openshift)
    - [3.3 Create a cucumber-instance](#33-create-a-cucumber-instance)
      - [3.3.1 Create a cucumber instance](#331-create-a-cucumber-instance)
    - [3.4 Create a cucumber-feature](#34-create-a-cucumber-feature)
      - [3.4.1 Run a cucumber test\[success scenario\]](#341-run-a-cucumber-testsuccess-scenario)
      - [3.4.2 Run a cucumber test\[fail scenario\]](#342-run-a-cucumber-testfail-scenario)
    - [3.5 Observe cucumber test results](#35-observe-cucumber-test-results)
    - [3.6 Cleanup](#36-cleanup)

---

## 1. Build
```bash
oc login --token=<OC_TOKEN> --server=<OC_API_URL>
oc new-project cucumber-operator
mvn clean install
oc get is
```
As a result of the build:
- Two new 'BuildConfig' are created and the :
```
NAME         TYPE     FROM      LATEST
nginx-s2i    Source   Binary    1
runner-s2i   Source   Binary    1
```

- Two Build are triggered:
```
NAME           TYPE     FROM     STATUS        STARTED      DURATION
runner-s2i-9   Source   Binary   Complete      2 days ago   32s
nginx-s2i-8    Source   Binary   Complete      2 days ago   19s
```

- Two new 'ImageStream' are created:
```
NAME     IMAGE REPOSITORY                                                                    TAGS       UPDATED
nginx    default-route-openshift-image-registry.apps-crc.testing/cucumber-operator/nginx     1.0.0      2 days ago
runner   default-route-openshift-image-registry.apps-crc.testing/cucumber-operator/runner    1.0.0      2 days ago
```

---

## 2. Run the operator
```bash
cd operator
mvn quarkus:dev
```
---

## 3. Testing the operator
### 3.1 Prepare the the namespace
The operator relay on maven to download all the artifacts containing the cucumber steps to run and all the needed dependendencies. To run the test in this example we will use Nexus
```
oc apply -f examples/cr/nexus-operator.yml
oc apply -f examples/cr/nexus-instance.yml
```

### 3.2 Deploy the test application on Openshift
```
NEXUS_POD_NAME=$(oc get pods -o custom-columns=POD:.metadata.name --no-headers | grep nexusrepo)
NEXUS_PASSWORD=$(oc exec $NEXUS_POD_NAME -- cat /nexus-data/admin.password)
NEXUS_ROUTE_NAME=$(oc get routes -o custom-columns=HOST:.metadata.name --no-headers | grep nexusrepo)
NEXUS_ROUTE_HOST=$(oc get route example-nexusrepo-sonatype-nexus-service --output jsonpath={.spec.host})
echo "pod name: $NEXUS_POD_NAME"
echo "nexus password: $NEXUS_PASSWORD"
echo "nexus route name: $NEXUS_ROUTE_NAME"
echo "nexus rout host: $NEXUS_ROUTE_HOST"
sed \
-e"s/NEXUS_PASSWORD/$NEXUS_PASSWORD/g" \
-e"s/NEXUS_ROUTE_HOST/$NEXUS_ROUTE_HOST/g" \
-e"s|LOCAL_MAVEN_REPO|$HOME|g" \
examples/application-parent/settings-template.xml > examples/application-parent/settings.xml
mvn clean install -f examples/application-parent/pom.xml -s examples/application-parent/settings.xml -Dquarkus.kubernetes.deploy=true
mvn clean deploy -f examples/application-parent/pom.xml -s examples/application-parent/settings.xml -DaltDeploymentRepository=nexus::http://$NEXUS_ROUTE_HOST/repository/maven-releases/
```

### 3.3 Create a cucumber-instance
Create a resource of type 'CucumberInstance' to prepare the namespace for the cucumber test run, when the resource is created the operator will create:
- A PersistentVolumeClaim with name '<resource-name>-pvc' that will be used to temporaly store all the jar needed for the test execution and all the test results
```yml
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: cucumber-instance-pvc
  namespace: cucumber-operator
  labels:
    app: cucumber-nginx
    app.kubernetes.io/managed-by: cucumber-operator-controller
    app.kubernetes.io/name: cucumber-instance
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 1Gi
  volumeName: pvc-0e4bd38a-1278-4333-a155-48529e27cebd
  storageClassName: crc-csi-hostpath-provisioner
  volumeMode: Filesystem
status:
  phase: Bound
  accessModes:
    - ReadWriteMany
  capacity:
    storage: 30Gi
```

- A Deployment with name '<resource-name>-nginx' that will gives access to the cucumber test results
```yml
kind: Deployment
apiVersion: apps/v1
metadata:
  name: cucumber-instance-nginx
  namespace: cucumber-operator
  ownerReferences:
    - apiVersion: ippul.org/v1alpha1
      kind: CucumberInstance
      name: cucumber-instance
      uid: 30d2394b-ae61-4a9e-94a6-7f5208e4bfe2
  labels:
    app: cucumber-nginx
    app.kubernetes.io/managed-by: cucumber-operator-controller
    app.kubernetes.io/name: cucumber-instance
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cucumber-nginx
  template:
    metadata:
      creationTimestamp: null
      labels:
        app: cucumber-nginx
        app.kubernetes.io/managed-by: cucumber-operator-controller
        app.kubernetes.io/name: cucumber-instance
    spec:
      volumes:
        - name: cucumber-instance-pvc
          persistentVolumeClaim:
            claimName: cucumber-instance-pvc
      containers:
        - name: cucumber-nginx
          image: 'image-registry.openshift-image-registry.svc:5000/cucumber-operator/nginx:1.0.0'
          ports:
            - containerPort: 8080
              protocol: TCP
          resources: {}
          volumeMounts:
            - name: cucumber-instance-pvc
              mountPath: /data/
          terminationMessagePath: /dev/termination-log
          terminationMessagePolicy: File
          imagePullPolicy: Always
      restartPolicy: Always
      terminationGracePeriodSeconds: 30
      dnsPolicy: ClusterFirst
      securityContext: {}
      schedulerName: default-scheduler
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 25%
      maxSurge: 25%
  revisionHistoryLimit: 10
  progressDeadlineSeconds: 600
```
- A Service with name '<resource-name>-svc-nginx' that map the port 8080 of the nginx server
```yml
kind: Service
apiVersion: v1
metadata:
  name: cucumber-instance-svc-nginx
  namespace: cucumber-operator
  labels:
    app: cucumber-nginx
    app.kubernetes.io/managed-by: cucumber-operator-controller
    app.kubernetes.io/name: cucumber-instance
spec:
  clusterIP: 10.217.5.17
  externalTrafficPolicy: Cluster
  ipFamilies:
    - IPv4
  ports:
    - name: http
      protocol: TCP
      port: 8080
      targetPort: 8080
      nodePort: 30741
  internalTrafficPolicy: Cluster
  clusterIPs:
    - 10.217.5.17
  type: NodePort
  ipFamilyPolicy: SingleStack
  sessionAffinity: None
  selector:
    app: cucumber-nginx
```
- A Route with name '<resource-name>-route-nginx' that will make the nginx server accesible from outside the cluster
```yml
kind: Route
apiVersion: route.openshift.io/v1
metadata:
  name: cucumber-instance-route-nginx
  namespace: cucumber-operator
  labels:
    app: cucumber-nginx
    app.kubernetes.io/managed-by: cucumber-operator-controller
    app.kubernetes.io/name: cucumber-instance
spec:
  host: cucumber-instance-route-nginx-cucumber-operator.apps-crc.testing
  to:
    kind: Service
    name: cucumber-instance-svc-nginx
    weight: 100
  port:
    targetPort: http
  wildcardPolicy: None
```
---
#### 3.3.1 Create a cucumber instance
```
oc apply -f examples/cr/cucumber-instance.yml
```
---
### 3.4 Create a cucumber-feature

To Run a test create a resource of type 'CucumberRun', when the resource is created the operator will create:
- A ConfigMap with name '<resource-name>-feature-files' that contains all the feature files defined in the custom resource
```yml
kind: ConfigMap
apiVersion: v1
metadata:
  name: cucumber-run-success-feature-files
  namespace: cucumber-operator
  labels:
    app.kubernetes.io/managed-by: cucumber-operator-controller
    app.kubernetes.io/name: cucumber-run-success
data:
  test-with-claudio.feature: |
    Feature: Test the application with Claudio user
      As a user I want to test application-services

      Scenario: Test endpoint with user Claudio
        Given a user named 'Claudio'
        When the service '/greetings' is invoked
        Then the response is 'Hello Claudio'
  test-with-davide.feature: |
    Feature: Test the application with Davide user
      As a user I want to test application-services

      Scenario: Test endpoint with user Davide
        Given a user named 'Davide'
        When the service '/greetings' is invoked
        Then the response is 'Hello Davide'
```
- A ConfigMap with name '<resource-name>-run' that contains all the configuration to run the cucumber tests defined in the custom resource
```yml
kind: ConfigMap
apiVersion: v1
metadata:
  name: cucumber-run-fails-run
  namespace: cucumber-operator
  labels:
    app.kubernetes.io/managed-by: cucumber-operator-controller
    app.kubernetes.io/name: cucumber-run-fails
data:
  run.properties: |
    #Generated configuration file for cucumber-run-fails
    #Sun Apr 07 11:47:01 BST 2024
    cucumber.feature.steps.definition.0=org.ippul\:application-integration-tests\:1.0.0
    cucumber.glue.0=org.ippul
```
- A ConfigMap with name '<resource-name>-settings' that contains the maven settings file defined in the custome resource that will be used to retrieve the jar and the dependencies to execute the cucumebr tests
```yml
kind: ConfigMap
apiVersion: v1
metadata:
  name: cucumber-run-fails-settings
  namespace: cucumber-operator
  labels:
    app.kubernetes.io/managed-by: cucumber-operator-controller
    app.kubernetes.io/name: cucumber-run-fails
data:
  settings.xml: |
    <?xml version="1.0" encoding="UTF-8" standalone="no" ?>
    <settings>
      <mirrors>
        <mirror>
          <id>nexus</id>
          <mirrorOf>*</mirrorOf>
          <url>http://example-nexusrepo-sonatype-nexus-service-cucumber-operator.apps-crc.testing/repository/maven-public</url>
        </mirror>
      </mirrors>
      <profiles>
        <profile>
          <id>nexus</id>
          <repositories>
            <repository>
              <id>central</id>
              <url>http://central</url>
              <releases>
                <enabled>true</enabled>
              </releases>
              <snapshots>
                <enabled>true</enabled>
              </snapshots>
            </repository>
          </repositories>
          <pluginRepositories>
            <pluginRepository>
              <id>central</id>
              <url>http://central</url>
              <releases>
                <enabled>true</enabled>
              </releases>
              <snapshots>
                <enabled>true</enabled>
              </snapshots>
            </pluginRepository>
          </pluginRepositories>
        </profile>
      </profiles>
      <activeProfiles>
        <activeProfile>nexus</activeProfile>
      </activeProfiles>
    </settings>
```
- A Job with name '<resource-name>-job' that execute the cucumebr tests
```yaml
kind: Job
apiVersion: batch/v1
metadata:
  name: cucumber-run-success-job
  namespace: cucumber-operator
  labels:
    app.kubernetes.io/managed-by: cucumber-operator-controller
    app.kubernetes.io/name: cucumber-run-success
spec:
  parallelism: 1
  completions: 1
  backoffLimit: 6
  selector:
    matchLabels:
      batch.kubernetes.io/controller-uid: f407a459-12c7-42c1-8d0c-d1b1bc0a248a
  template:
    metadata:
      creationTimestamp: null
      labels:
        batch.kubernetes.io/controller-uid: f407a459-12c7-42c1-8d0c-d1b1bc0a248a
        batch.kubernetes.io/job-name: cucumber-run-success-job
        controller-uid: f407a459-12c7-42c1-8d0c-d1b1bc0a248a
        job-name: cucumber-run-success-job
    spec:
      volumes:
        - name: cucumber-run-success-features
          projected:
            sources:
              - configMap:
                  name: cucumber-run-success-feature-files
                  items:
                    - key: test-with-claudio.feature
                      path: test-with-claudio.feature
              - configMap:
                  name: cucumber-run-success-feature-files
                  items:
                    - key: test-with-davide.feature
                      path: test-with-davide.feature
            defaultMode: 420
        - name: cucumber-run-success-run
          projected:
            sources:
              - configMap:
                  name: cucumber-run-success-run
                  items:
                    - key: run.properties
                      path: cucumber-run-success.properties
              - configMap:
                  name: cucumber-run-success-settings
                  items:
                    - key: settings.xml
                      path: settings.xml
            defaultMode: 420
        - name: cucumber-run-success-pvc
          persistentVolumeClaim:
            claimName: cucumber-instance-pvc
      containers:
        - name: cucumber-run-success-test
          image: 'image-registry.openshift-image-registry.svc:5000/cucumber-operator/runner:1.0.0'
          env:
            - name: RUN_ID
              value: cucumber-run-success
          resources:
            limits:
              cpu: 250m
              memory: 1000Mi
            requests:
              cpu: 100m
              memory: 250Mi
          volumeMounts:
            - name: cucumber-run-success-features
              mountPath: /data/features/test-with-claudio.feature
              subPath: test-with-claudio.feature
            - name: cucumber-run-success-features
              mountPath: /data/features/test-with-davide.feature
              subPath: test-with-davide.feature
            - name: cucumber-run-success-run
              mountPath: /data/conf/
            - name: cucumber-run-success-pvc
              mountPath: /data/
          terminationMessagePath: /dev/termination-log
          terminationMessagePolicy: File
          imagePullPolicy: Always
      restartPolicy: Never
      terminationGracePeriodSeconds: 30
      dnsPolicy: ClusterFirst
      securityContext: {}
      schedulerName: default-scheduler
  completionMode: NonIndexed
  suspend: false
```
---

#### 3.4.1 Run a cucumber test[success scenario]
```
oc apply -f examples/cr/cucumber-feature-success.yml
```
---

#### 3.4.2 Run a cucumber test[fail scenario]
```
oc apply -f examples/cr/cucumber-feature-fails.yml 
```
---

### 3.5 Observe cucumber test results
Execute ```oc get route | grep nginx``` and open the url from a browser the opened page contains a list of folders named with the dates where at least a test had been run

<img src="/images/0-cucumber-results.png" width="100%" />

Click on one of the folder and a page containg the list of all test run in that day, the name of the test run is the '<time-of-the-run>'-resource-name

<img src="/images/1-cucumber-results.png" width="100%" />

Click on one of the test execution and output of the html plugin page will be opened 

<img src="/images/2-cucumber-results.png" width="100%" />

---

### 3.6 Cleanup
```bash
oc delete CucumberRun/cucumber-run-success
oc delete all,cm,secrets,svc,routes -l app.kubernetes.io/name=cucumber-run-success
oc delete CucumberRun/cucumber-run-fails
oc delete all,cm,secrets,svc,routes -l app.kubernetes.io/name=cucumber-run-fails
oc delete CucumberInstance/cucumber-instance
oc delete all,cm,secrets,svc,routes,pvc,pv -l app.kubernetes.io/name=cucumber-instance
```
