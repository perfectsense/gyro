## Deploying Code

Beam provides a blue-green deployment workflow for launching new code builds. Deployment information is provided as part of Beam configuration. Any time the deployment information changes Beam will go through the deployment workflow.

In order for Beam to enter the code deploy workflow it requires that a layer be autoscaled and have a load balancer assigned to it. A future release of Beam will bring blue-green deployment to all layers.

### Deployment Configuration

Deployment information consists of the location of builds to be deployed. When Beam launches a new instance in the deployed layer it will provide the deployment information in userdata. It's up to the instance to take that information and deploy the code. For example, download a war file and deploy it to the local servlet container.

Here is an example of the deployment information. Add this to the _development_ layer in the _dev_ environment:

```yaml
    deployment:
      buildNumber: 1
      jenkinsBucket: beam-demo
      jenkinsBuildPath: jobs/master
      jenkinsWarfile: beam-demo.tar.gz
```

It's the responsibility of your development pipeline to put the deployment artifact in an S3 bucket (or Cloudfiles in Rackspace). 

### Add a Load Balancer

To enable blue-green deployments first add a load balancer to the layer we want to deploy code. Load balancers are defined in `network.yml` and referenced in the layers autoscale placement.

Add the following load balancer to `network.yml` inside the region configuration:

```yaml
        loadBalancers:
          - name: dev
            subnetType: public
            listeners:
              - protocol: http
                sourcePort: 80
                destPort: 80

            healthCheck:
              protocol: http
              port: 80
              path: /

            securityRules:
              - http
```

Add the following security rule to `network.yml`. This opens port 80 on the load balancer to everyone.

```yaml
  - name: http
    permissions:
      - name: http
        cidr: 0.0.0.0/0
        ports:
          - 80
```

### Trigger a Deployment

With the configuration about the step to trigger a deployment is `beam up dev`. Beam will detect the change and trigger the deployment workflow.

```bash
13:51 $ beam up dev
Looking for changes...

vpc-69fd550d 10.0.0.0/16
    subnet-cf09e5b9 10.0.0.0/26 in us-east-1a
        - Delete example_project dev serial-1 development layer instance [i-05696dd1]
    + Create launch configuration example_project development dev v1 ami-b8ea9bd2 jobs/master 2 373c78ca46ce9cce125afcc377ed76c7
    - Delete launch configuration example_project development dev v1 ami-b8ea9bd2 jobs/master 1 f8a999c698097147919dab7827a2e693
    + Create auto scaling group example_project development dev v1 ami-b8ea9bd2 jobs/master 2 373c78ca46ce9cce125afcc377ed76c7
    - Delete auto scaling group example_project development dev v1 ami-b8ea9bd2 jobs/master 1 f8a999c698097147919dab7827a2e693

* Verify deployment for layer(s): [us-east-1 development]
matching [image: beam example-project, buildNumber: 2, jenkinsBucket: beam-demo, jenkinsBuildPath: jobs/master, jenkinsWarFile: beam-demo.tar.gz]

Are you sure you want to create and/or update resources in ec2 cloud in account sandbox? (y/N)
```

At this point Beam will create a new launch configuration and a new autoscale group with the new deployment information.

### Verification New Instances

The firt step in a deploy is verification. At this step Beam will launch temporary load balancer and the new autoscale group. All new instances will initially be placed in the verification load balancer once they're healthy.

After all instances are healthy Beam will stop and prompt for next steps. This provides a chance to verify the build is correct.

```bash
Executing: + Create launch configuration example_project development dev v1 ami-b8ea9bd2 jobs/master 2 373c78ca46ce9cce125afcc377ed76c7 OK
Executing: + Create auto scaling group example_project development dev v1 ami-b8ea9bd2 jobs/master 2 373c78ca46ce9cce125afcc377ed76c7 OK
Executing: * Verify deployment for layer(s): [us-east-1 development]
matching [image: beam example-project, buildNumber: 2, jenkinsBucket: beam-demo, jenkinsBuildPath: jobs/master, jenkinsWarFile: beam-demo.tar.gz]

Verification: Creating verification load balancer: us-east-1 example-project-dev-1-v
Creating instance: example-project-dev-1-v

Verification: Waiting for 1 instances (0 current) to be running in example_project development dev v1 ami-b8ea9bd2 jobs/master 2 373c78ca46ce9cce125afcc377ed76c7
Verification: Waiting for 1 instances to be in service in example_project development dev v1 ami-b8ea9bd2 jobs/master 2 373c78ca46ce9cce125afcc377ed76c7
Verification: Waiting for load balancer instances to be InService...
Verification: Waiting for load balancer instances to be InService...

us-east-1 example-project-dev-1 load balancer has the following groups in it:
  -> example_project development dev v1 ami-b8ea9bd2 jobs/master 1 f8a999c698097147919dab7827a2e693

us-east-1 example-project-dev-1-v load balancer has the following groups in it:
  -> example_project development dev v1 ami-b8ea9bd2 jobs/master 2 373c78ca46ce9cce125afcc377ed76c7


Verification load balancers are:
  -> us-east-1 example-project-dev-1-v: example-project-dev-1-v-1354430060.us-east-1.elb.amazonaws.com

push)   Push the new verification instances into the production load balancer and deregister old instances.
reset)  Delete verification instances and load balancer, then exit Beam.
quit)   Stop the verification process for now and exit Beam.

?)
```

There are three options during the verification step: *push*, *reset* and *quit*.

The `reset` option deletes the new instances and temporarly load balancer and quits Beam.

The `quit` option quits Beam, leaving the system in verification state. Running `beam up <env>` again will resume where it left off.

### Push New Instances

By selecting the `push` option the new instances are placed in the live load balancer and the old instances are removed. The old instances will be placed in standby in case a roll back is necessary.

After the new instances are live Beam will prompt for next steps:

```bash
push)   Push the new verification instances into the production load balancer and deregister old instances.
reset)  Delete verification instances and load balancer, then exit Beam.
quit)   Stop the verification process for now and exit Beam.

?) push

Pushing instances for group us-east-1 example_project development dev v1 ami-b8ea9bd2 jobs/master 2 373c78ca46ce9cce125afcc377ed76c7 into production load balancer example-project-dev-1
Verification: Waiting for load balancer instances to be InService...

us-east-1 example-project-dev-1 load balancer has the following groups in it:
  -> example_project development dev v1 ami-b8ea9bd2 jobs/master 2 373c78ca46ce9cce125afcc377ed76c7

us-east-1 example-project-dev-1-v load balancer has no groups in it.


Verification load balancers are:
  -> us-east-1 example-project-dev-1-v: example-project-dev-1-v-1354430060.us-east-1.elb.amazonaws.com

revert) Revert the new verification instances into the production load balancer and deregister old instances.
commit) Commit verification instances to production load balancer.
quit)   Stop the verification process for now and exit Beam.

?)
```

There are three options available after new instances are pushed live: `revert`, `commit` and `quit`.

The `revert` option will put the old instances that are currently in standby back into the live load balancer and remove the new instances. Once this is complete it will drop you back at the `push` prompt from the previous step. At that point you can `reset` to finish the revert.

### Commit New Instances

By selecting the `commit` option Beam will make the new code deployment permanent. It will remove verification mode from the new autoscale group, delete the verification load balancer and exit Beam.

```bash
Verification load balancers are:
  -> us-east-1 example-project-dev-1-v: example-project-dev-1-v-1354430060.us-east-1.elb.amazonaws.com

revert) Revert the new verification instances into the production load balancer and deregister old instances.
commit) Commit verification instances to production load balancer.
quit)   Stop the verification process for now and exit Beam.

?) commit

Executing: + Deleting verification load balancers OK
Executing: + Committing autoscale group  OK

Skipped deletes in ec2 cloud. Run again with the --delete option to execute them.
```

At this point the old instances are still running. To clean them up run `beam up --delete <env>` and follow the prompts:

```bash
$ beam up --delete dev
Looking for changes...
Warning: You may not have enough access to bucket 'beam-demo'

vpc-69fd550d 10.0.0.0/16
    subnet-cf09e5b9 10.0.0.0/26 in us-east-1a
        - Delete example_project dev serial-1 development layer instance [i-05696dd1]
    - Delete launch configuration example_project development dev v1 ami-b8ea9bd2 jobs/master 1 f8a999c698097147919dab7827a2e693
    - Delete auto scaling group example_project development dev v1 ami-b8ea9bd2 jobs/master 1 f8a999c698097147919dab7827a2e693

Are you sure you want to delete resources from ec2 cloud in account sandbox? (y/N) y

Executing: - Delete example_project dev serial-1 development layer instance [i-05696dd1] OK
Executing: - Delete auto scaling group example_project development dev v1 ami-b8ea9bd2 jobs/master 1 f8a999c698097147919dab7827a2e693 OK
Executing: - Delete launch configuration example_project development dev v1 ami-b8ea9bd2 jobs/master 1 f8a999c698097147919dab7827a2e693 OK
```