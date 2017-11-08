## Changing Infrastructure

In the [previous section](getting-started-create.md) we launched a new EC2 instance using `beam up`. However, we did not define any security groups for this new instance. By default AWS will assign the `default` security group to your new instance if no other security groups are added by Beam.

In this section we'll modify the configuration to add a security group to control access to this instance and show how Beam handles changes to the configuration.

### Configuration

First we need to add a security rule to the `network.yml` file. The following `rules` list should be placed at the top level of the `network.yml` file, typically just before the `clouds` lists.

```yaml
rules:
  - name: ssh
    permissions:
      - name: ssh
        cidr: 0.0.0.0/0
        ports:
          - 22
```

This security rule will open port 22 to anyone. The name, _ssh_, will be used to reference this security rules in our layer definition as well as to name the security group in EC2.

Now that we have a security rule defined we need to assign it to our instance. We do this by adding a `securityRules` list to our environments layer definition:

```    
layers:
  - name: development
    image: ubuntu-trusty-14.04-amd64-server-20151019
    instanceType: t2.micro

	 securityRules:
      - ssh
```

### Update Infrastructure

We can apply these configuration changes by running `beam up dev` again. Don't worry, Beam will show you exactly what actions it's going take before it does them allowing you to be sure you're making the right change:

```yaml
$ beam up dev
Looking for changes...

vpc-69fd550d 10.0.0.0/16
    + Create security group example_project-ssh-us-east-1-v1
        + Create IP permission - allow all traffic from beam:sg-ssh
        + Create IP permission - allow tcp traffic at 22 from everywhere
    subnet-cf09e5b9 10.0.0.0/26 in us-east-1a
        * Update example_project dev serial-1 development layer instance [i-55316a81] (securityGroups: [sg-72208114] -> [beam  g-ssh])

Are you sure you want to create and/or update resources in ec2 cloud in account sandbox? (y/N)
```

The output is slightly different this time. The `vpc-69fd550d 10.0.0.0/16` no longer has a `+` in front of it. This is because this resource has already been created. It's being display because a resource that depends on it is being created and updated. The resource being updated, the EC2 instance, also shows what specific attribute of that instance is about to be updated. In this case, it's changing the security group from the default group to the new security rule we defined.

Resources that are created start with a `+`, in-place updates start with a `*` and deletes start with a `-`.

```yaml
Executing: + Create security group example_project-ssh-us-east-1-v1 OK
Executing: + Create IP permission - allow all traffic from sg-cd08a9ab OK
Executing: + Create IP permission - allow tcp traffic at 22 from everywhere OK
Executing: * Update example_project dev serial-1 development layer instance [i-55316a81] (securityGroups: [sg-72208114] -> [beam:sg-ssh]) OK
```

### Next

Now we have an EC2 instance and we can access it on port 22 (ssh). We've also seen how Beam can modify existing infrastructure with the safety of knowing exactly what changes will be made before they're made.

In the next section we'll [ssh to our instance using Beam](access.md).