## Provisioning an Instance

The EC2 instance we created in the previous section was a base Ubuntu 14.04 Linux host. In this section we'll show how to use provisioners to do initial setup of an instance.

Beam is designed to work with pre-built machine images. Provisioners are useful for instances that cannot be easily replaced but may need to be updated.

### Adding a Provisioner

Beam currently supports two provisions: shell and knife-solo. Let's add a simple shell provisioner.

To add a shell provisioner, add the following `provisioner` section to your _development_ layer in the `dev.yml` file:

```yaml
    provisioners:
      - type: shell
        sudo: true
        inlines:
          - "apt-get update"
          - "apt-get install -y apache2"
```

The shell provisioner we added above will update the apt package manager and install the _apache2_ package. These commands will be run using sudo since the `sudo` attribute is set to _true_.

### Running a Provisioner

The provisioner command works just like `beam ssh`. To run a provision run `beam provision dev`. This will provision all instances in the _dev_ environment. If there is more than one instance and you only want to provision a single instance add the `--ask` option to have Beam prompt you for the instance to provision.

Before executing a provisioner Beam will display all instances that will be provisioned and prompt for confirmation. Any output from the provisioners will be output to the terminal:

```bash
18:10 $ beam provision -k ~/.ssh/example_project-us-east-1-sandbox.pem -u ubuntu dev
The following instances will be provisioned:

- example_project dev serial-1 development [us-east-1] i-55316a81

Are you sure you want to provision these instances in ec2 cloud (y/N) y

Provisioning '52.23.157.227' with shell (sandbox)...

Reading package lists...
Building dependency tree...
Reading state information...
The following NEW packages will be installed:
  jq
0 upgraded, 1 newly installed, 0 to remove and 6 not upgraded.
Need to get 97.8 kB of archives.
After this operation, 296 kB of additional disk space will be used.
Get:1 http://us-east-1.ec2.archive.ubuntu.com/ubuntu/ trusty/universe jq amd64 1.3-1.1ubuntu1 [97.8 kB]
Fetched 97.8 kB in 0s (5,839 kB/s)
Selecting previously unselected package jq.
(Reading database ... 51149 files and directories currently installed.)
Preparing to unpack .../jq_1.3-1.1ubuntu1_amd64.deb ...
Unpacking jq (1.3-1.1ubuntu1) ...
Processing triggers for man-db (2.6.7.1-1ubuntu1) ...
Setting up jq (1.3-1.1ubuntu1) ...
```

### Next

We've now covered the basics of the infrastructure create and management portions of Beam. 

Next we'll expore how Beam can be [used to deploy code](deployment.md).