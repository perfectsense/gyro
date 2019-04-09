
Updating Infrastructure
==================

This section will explain how Gyro handles changes to the configuration of the infrastructure.

Configuration
-----------

Use Case 1: Create a custom route table associated to your VPC which will control the routing for the subnet.

Use Case 2: Create an internet gateway attached to your VPC and ensure that your subnet's route table points to the internet gateway.

.. image:: ../images/vpc-route.png

Add the below given configs in the vpc.gyro file inorder to update an existing setup environment.

.. code::

	aws::subnet subnet-public-us-east-1a
  	    vpc-id: $(aws::vpc vpc-example | vpc-id)
  	    cidr-block: "10.0.0.0/24"
  	    availability-zone: "us-east-1a"
	end

	aws::internet-gateway ig-example
	    vpc-id: $(aws::vpc vpc-example | vpc-id)
	end

	aws::route-table route-table-example
	    vpc-id: $(aws::vpc vpc-example | vpc-id)
	    subnet-ids: $(aws::subnet subnet-public-us-east-1a | subnet-id)
	end

	aws::route route-example
	    route-table-id: $(aws::route-table route-table-example | route-table-id)
	    destination-cidr-block: "0.0.0.0/0"
	    gateway-id: $(aws::internet-gateway ig-example | internet-gateway-id)
	    cidr-block: "10.0.0.0/16"
	end

Update Infrastructure
-----------

Gyro will give a diff of variables that have changed for this file, in our case you will see the creates.

Apply the configuration changes by running gyro up again. Gyro will show you what actions are required.

.. code:: shell

	$ /usr/local/bin/gyro up --test vpc.gyro

	Loading plugin: gyro:gyro-aws-provider:0.14-SNAPSHOT...
	Looking for changes...
	
	+ Create internet gateway
	+ Create route table
	+ Create route 0.0.0.0/0
	
	Are you sure you want to change resources? (y/N) y
	
	+ Creating internet gateway OK
	+ Creating route table OK
	+ Creating route 0.0.0.0/0 through gateway test-internet-gateway-id-828368e3837140d7 OK

At this point the network environment displayed in the overview diagram is set up.


.. raw:: pdf

    PageBreak
