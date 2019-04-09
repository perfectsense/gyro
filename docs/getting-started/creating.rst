
Creating Infrastructure
==================

Overview
-----------

The diagram below depicts the reference architecture of a virtual private network with associated resources.

.. image:: ../images/vpc-overview.png

This document will focus on the implementation of this architecture in AWS.

By the end of this guide you should have a working local Gyro environment and deployed below given resources on AWS cloud :

1. Virtual private cloud network (VPC)
2. Subnet
3. Route Table
4. Internet gateway

Configuration
-----------

The first step to creating infrastructure with gyro is to define your project credentials and global resources in the gyro config file. 

Create a file named vpc.gyro with the following configuration :

.. code::

    plugin
        artifact: 'gyro:gyro-aws-provider:0.14-SNAPSHOT'
        repositories: [
            'https://artifactory.psdops.com/public',
            'https://artifactory.psdops.com/gyro-snapshots'
        ]
    end

    aws::credentials default
        region: "us-east-1"
    end

    aws::vpc vpc-example
    	cidr-block: "10.0.0.0/16"
    end

    aws::subnet subnet-public-us-east-1a
        vpc-id: $(aws::vpc vpc-example | vpc-id)
        cidr-block: "10.0.0.0/24"
        availability-zone: "us-east-1a"
    end

VPC
**************

aws::vpc - The name of the resource which will be used by gyro to identify the VPC resource.

cidr-block - The IPv4 network range for the VPC, in CIDR notation

Subnet
**************

aws::subnet - The name of the resource which will be used by gyro to identify the subnet resource.

vpc-id : The ID of the VPC to create the subnet in, which in this case would be the vpc-example.

cidr-block : The IPv4 network range for the subnet, in CIDR notation.

availability-zone : The name of the availablity zone to create this subnet.

The above given configuration would be creating a VPC resource and a subnet associated to that network as depicted in the diagram below : 

.. image:: ../images/vpc-subnet-overview.png



Launching Infrastructure
-----------

Now that the infrastructure configuration is defined, it is ready to launch. Run gyro up in test mode. When ``y`` is given at the prompt a state file will be created in the local directory named ``vpc.gyro.state``.

You should see output similar to the following :

.. code:: shell

  $ /usr/local/bin/gyro up vpc.gyro
  Loading plugin: gyro:gyro-aws-provider:0.14-SNAPSHOT...

  Looking for changes...

  + Create vpc 10.0.0.0/16 - vpc-example
  + Create subnet 10.0.0.0/24 in us-east-1a
	
  Are you sure you want to change resources? (y/N) y
	
  + Creating vpc 10.0.0.0/16 - vpc-example OK
  + Creating subnet 10.0.0.0/24 in us-east-1a OK

.. raw:: pdf

    PageBreak
