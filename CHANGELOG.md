## 0.12.5

ISSUES FIXED:

* Wait for instance profile to be created before creating a new launch configuration
* beam credentials output a log message that broke scripts that parsed its output
* beam up ensures layers are sized according to the current production settings
* beam up -l works in more cases now by including gateways
* beam iam services zone information to fix an issue with recent version of fog
* Deployment file checksums are compared to prevent redeploying a poorly named war file
* Service discovery logs less when it can't find the private dns name (falls back to search mode)
* Fixed bug where key path cannot be relative for provision command
* Fixed bug that causes `--debug` to recursively log to Beam Enterprise

NEW FEATURES:

* Schedules now have the ability to set desired capacity
* AWS NAT gateways are now supported

```
subnets:
  - types: [public, gateway]
    natGateway: true
    natGatewayIp: 54.81.115.143
```

* beam up should be much faster for AWS due to asynchronous/parallel API calls for most lookups
* Support for NewRelic deployment markers

**prod.yml**:

```
- name: frontend
  deployment:
      type: newrelic
      buildNumber: 4
      jenkinsBucket: ops-test-ops
      jenkinsBuildPath: jobs/master
      jenkinsWarfile: ops-test-1.0-SNAPSHOT.war
```

**$HOME/.beam/config.yml**

```
projects:
  - name: ops-test
    newrelic:
        applicationId: {applicationId}
        apiKey: {apiKey}
```


## 0.12.1

ISSUES FIXED:

  * Prevent Elastic IP association failing due to a race condition
- Send complete `beam provision` output to Beam Enterprise audit log
- Only return instances from active region when using Beam Enterprise
- Ignore VPC peering routes (prevents Beam from wanting to delete them)
- Create instances without an instance profile if one is not configured
- Remove launch configuration when using `reset` during a deployment

NEW FEATURES:

- Allow version of Chef to be configured in provisioner settings
- Service discovery persists primary tag state
- Add support for Azure
- Terminate non-ELB instances during a deployment reset
- Record Beam version in Beam Enterprise audit logs
- Set autoscale desired size to match current live autoscale group size
- Faster `beam up` when using Beam Enterprise credentials
- Extended credentials (24 hrs instead of 1 hr) when using `beam credentials <env> --extended`

## 0.10.0

ISSUES FIXED:

  * beam/up: Refresh credentials before running subcommands.
  * beam/credentials: Create `~/.aws/credentials` file if it doesn't exist.

NEW FEATURES:

  * beam/provision,
    beam/primary,
    beam/up,
    beam/down,
    beam/mark: Added real-time streaming auditing to Beam Enterprise.
  * beam/list,
    beam/ssh: Improved performance when using Beam Enterprise.

## 0.9.6

NEW FEATURES:

  * beam/network: Added option to allow security groups to exclude themselves.


## 0.9.5

NEW FEATURES:

  * beam/iam: Added iam server to simulate AWS instance profile APIs.

## 0.9.4

ISSUES FIXED:

  * beam/discovery: Fixed issue that prevented automatic primary for "master" from being selected.

## 0.9.3

ISSUES FIXED:

  * beam/openstack: Fixed issue that prevented multiple layer placements.

## 0.9.2

ISSUES FIXED:

  * beam/discovery: Fixed memory leak in service discovery replication.

## 0.9.1

ISSUES FIXED:

  * beam/up: Fixed issue that caused security rules to be added and deleted when there should be no changes.

## 0.9.0

NEW FEATURES:

  * Service Discovery now works across multiple regions.

IMPROVEMENTS:

  * beam/discovery: Replication is now batched for improved performance.
  * beam/discovery: `/v2/getMonitor` discovery api provides data for monitoring systems.
  * beam/discovery: beam.project-<project>.environment-<env> bucket/container dependency has been removed. Service discovery instances now share primary state data with each other and store state locally.
  * beam/deployment: <project>-<env>-<serial>-<hash> bucket/container is no longer created for deployed war files.
  * beam/aws: Private hosted zones are now supported using the `privateHostnames` attribute.
  * beam/ssh: Added experimental tmux support (`--tmux` option to `beam ssh`)that launches tmux and connects to all instances by `beam ssh`.
  
ISSUES FIXED:

  * beam/gateway/elasticip: Fixed race condition that would cause a `beam up` to fail when attempting
    to associate an elastic ip to a gateway.
  * beam/up: Fixed issue that would cause private DNS to fail to update on initial instance creation.
  * beam/up: Fixed issue that caused host entries to be deleted when an elastic ip was used on a non-gateway instance.

## 0.8.4

NEW FEATURES:

  * Ability to create region specific bucket under regions config.

  ```
    regions:
      - name: us-west-2
        cidr: 10.0.0.0/16
        buckets:
          - name: foo-bar-us-west-2
  ```

  * Ability to start/stop bucket replication.

  ```
    buckets:
      - name: foo-bar
        replicateTo: foo-bar-us-west-2
  ```

IMPROVEMENTS:

  * beam/console: Simplifed `beam console` if inside the beam directory.
  * beam/console: Allow user to login to console without a beam environment: `beam console` <account> <project>
  * beam/credentials: Simplify to `beam credentials` if inside the beam directory.
  * beam/credentials: Allow user to get credentials without a beam environment: `beam credentials` <account> <project>
  * beam/credentials: Added a --refresh option to refresh account profile for aws CLI usage.
  * beam/aws/gateway: Added ability to specify an allocated (but unassociated) elastic ip to gateway instances.

ISSUES FIXED:

  * beam/primary: Revised bucket access that follows the bucket’s location constraint.
  * beam/server: Fixed memory leak in service discovery caused by logging to stdout.
  * beam: Fixed issue that prevented using beam commands (ssh,list,mark,etc) on a cloud host.
  * beam/aws: Catches and ignores "already exists" errors on global resources.

## 0.8.3

NEW FEATURES:

  * Ability to add custom ciphers to AWS ELBs:

  ```
    - protocol: https
      sourcePort: 443
      destPort: 80
      sslCertificateName: certificate-name
      cipher:
        serverOrderPreference: false
        sslProtocols:
          - Protocol-SSLv2
          - Protocol-SSLv3
          - Protocol-TLSv1.1
        
        sslCiphers:
          - ECDHE-ECDSA-AES128-GCM-SHA256
          - AES128-SHA
  ```

IMPROVEMENTS:
  
  * beam/aws: Show instances that are in the stopping state for list based commands (list, ssh, etc).
  * beam/up [aws]: Allow security group to security group rules to define specific ports to allow.
  * beam/up [aws]: Tag S3 buckets with project information. 

ISSUES FIXED:

  * beam/service: Cache gateway lookups to improve response time of service discovery requests. 
  * beam/down: Make beam down respect environments. Prevents stopping gateways when beaming down dev.

## 0.8.2

NEW FEATURES:

  * Verification DNS can now be defined at the ELB level.

  ```
  loadBalancers:
    - name: web
      subnetType: public
      dns:
      routingPolicy: latency
      hostnames:
        - "prod.project.psdops.com"
      verificationHostnames:
        - "verify.prod.project.psdops.com"
        - "*.verify.prod.project.psdops.com"
  ```

  * Instance DNS can now be defined for static placements.

  ```
  placements:
    - subnetType: development
      sizePerSubnet: 1
      hostnames:
        - "dev.project.psdops.com"
        - "*.dev.project.psdops.com"
        - "jenkins.project.psdops.com"
  ```

  * Autoscale placements now support schedules in addition to policies.

  ```
    schedules:

      - name: april fools day
        startTime: "2015-04-01T00:00"
        duration: "24h"
        scaleUpPerSubnet: 4
        scaleDownPerSubnet: 2

      - name: weekly newsletter
        endTime: "2015-06-30T23:59"
        startRecurrence: "0 11 * * TUE,THU"
        endRecurrence: "0 15 * * TUE,THU"
        scaleUpPerSubnet: 4
        scaleDownPerSubnet: 2
  ```

  Results in:

  ```
  vpc-415b2d24 10.0.0.0/16
    * Update auto scaling group project frontend prod v99 ami-785d1810 jobs/project-master 695 ce30b43f2e861ac92326efde73b83ae6 (minSize: 0 -> 2, maxSize: 0 -> 16)
        + Create scheduled action (weekly newsletter start) ends on Tue Jun 30 23:59:00 EDT 2015, recurrence: [0 11 * * TUE,THU], min: 4, max: 16
        + Create scheduled action (weekly newsletter end) ends on Tue Jun 30 23:59:00 EDT 2015, recurrence: [0 15 * * TUE,THU], min: 2, max: 16
        + Create scheduled action (april fools day start) executes at Wed Apr 01 00:00:00 EDT 2015, min: 4
        + Create scheduled action (april fools day end) executes at Wed Apr 01 11:59:00 EDT 2015, min: 2, max: 16
  ```

  * EBS volumes can be defined on layers.

  ```
    - name: backend
      image: project [backend] hvm [22]
      instanceType: t2.medium
      volumes:
        - name: root
          size: 15
          volumeType: gp2
          deviceName: /dev/sda1

        - name: backup
          size: 100
          volumeType: standard
          deviceName: /dev/sdg

        - name: servers
          size: 100
          volumeType: gp2
          deviceName: /dev/sdf
  ```

IMPROVEMENTS:

  * beam/primary: Add ability to set primary tags by region on `beam primary` command.
  * beam/primary: Add -list option to just list currently set primary.
  * beam/up: Add -layer option to only bring up specific layers.
  * beam/up: Prevent `beam up` from overwriting a key pair. This can happen when launching
             a project into another account that does not already have a keypair.
  * beam/up: Add support for hostname to placement layers.
  * beam/copy: New command to copy a local file to one or more instances.
  * beam/server: Add ability to mark instance in/out of service.
  * beam/service mark: New command to mark instances out of service.

ISSUES FIXED:

  * beam/up: Metric alarm changes were not being detected for update.

## 0.8.1

IMPROVEMENTS:

  * Add support for multiple regions.
  * Add --exclude-regions <list> to all commands to exclude a region or regions.

ISSUES FIXED:

  * beam/ssh: Fix issue that caused `beam ssh` to use private IP even when a public IP was available.
  * beam/ssh,
    beam/list: Show instance state (running/stopped/etc) instead of services.
  * beam/up: Fix issue that caused `commit` to let instances go live when the intention was to bail on the deploy.
  * beam/primary: Fix issue that caused primary tags to be ignored if the beam servers were in a different environment (network vs. prod env).

## 0.8.0

IMPROVEMENTS:

  * beam/up: Shows changes and allows for confirmation before applying them.
  * beam/up: Implements stage/deployment process when autoscaled layers change.
  * beam/up: Remove resources that are removed from the configuration.
  * beam/up: Implicit "network" environment for updating network/gateway related resources.
  * beam/stage: Removed. This functionality has been folded into `beam up`.
  * beam/deploy: Removed. This functionality has been folded into `beam up`.
  * beam/provision/knife: Add "roles" and support for multiple recipes using "recipes" attributes.
  * beam/provision/knife: Add "environment" to allow overriding the Chef environment.
  * beam/discovery: Instances stick around for 24 hrs after last ping allowing DNS to continue resolving. 
  * beam/bootstrap: Removed this command, use `beam up` instead.
  * clouds/aws: Add support for Route53. (Not currently exposed in config format)
  * clouds/aws: Add elastic ip support for gateway layer.
  * clouds/aws: Add IAM roles, instance profiles and role policies.
  * clouds/aws: Add support for specifying AMI name as an alternative to ami-id.
  * clouds/aws/asg: Autoscale group now includes build information in the name.
  * clouds/aws/elb: Add "destProtocol" attribute for specifying an alternate destination protocol.
  * clouds/aws/s3: Add "buckets" attribute for specifying S3 buckets.

ISSUES FIXED:

  * beam/primary: Fix issue that caused `beam primary` to display instances from all environments.
