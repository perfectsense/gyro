
Creating Infrastructure
-----------------------

Gyro is a configuration management command line tool focused on creation and management of cloud services that are required to run an infrastructure.

By the end of this guide you should have a working local Gyro environment, and deployed these resources on AWS cloud :

1. Virtual private cloud network 
2. Subnets
3. Route Table
4. Internet gateways

Gyro uses a set of configuration gcl files to describe infrastructure, we will focus on the implementation of this architecture in AWS.
Configuration for Rackspace, Azure is similar.

Overview
-----------

.. image:: images/vpc_overview.png


Configuration
-----------

The first step to creating infrastructure with gyro is to define your project credentials and global resources in the gyro config file. The following example configurations will create a VPC on Amazon Web Services in the us-east-1 region along with a public subnet.

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
        profile: "beam-sandbox"
    end

    aws::vpc vpc-example
    cidr-block: "10.0.0.0/16"
		end

		aws::subnet subnet-public-us-east-1a
		    vpc-id: $(aws::vpc vpc-example | vpc-id)
		    cidr-block: "10.0.0.0/24"
		    availability-zone: "us-east-1a"
		end

The above configuration would be creating a VPC resource and a subnet associated to that network.

.. image:: images/vpc_subnet_overview.png


VPC
**************

A virtual private cloud is a network that defines the scope of your infrastructure, where all the resources would be launched

aws::vpc - The name of the resource which will be used by gyro to identify the VPC resource.

cidr-block - The IPv4 network range for the VPC, in CIDR notation

Subnets
**************

aws::subnet - TThe name of the resource which will be used by gyro to identify the subnet resource.

vpc-id : The ID of the VPC to create the subnet in, which in this case would be the vpc-example.

cidr-block : The IPv4 network range for the subnet, in CIDR notation.

availability-zone : The name of the availablity zone to create this subnet.

Launching Infrastructure
-----------
.. code:: shell

  $ /usr/local/bin/gyro up --test test.gyro
  Loading plugin: gyro:gyro-aws-provider:0.14-SNAPSHOT...

	Looking for changes...
	
	+ Create vpc 10.0.0.0/16 - vpc-example-demo
	+ Create subnet 10.0.0.0/24 in us-east-1a

	
	Are you sure you want to change resources? (y/N) y
	
	+ Creating vpc 10.0.0.0/16 - vpc-example-demo OK
	+ Creating subnet 10.0.0.0/24 in us-east-1a OK

.. raw:: pdf

    PageBreak
