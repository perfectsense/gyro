
Updating Infrastructure
==================

This section will explain how Gyro handles changes to the configuration of the infrastructure.

Gyro will output a difference between the current settings provided by the configuration files and the current state of the environment in the cloud. It will always output its proposed actions before executing them.
The user can then confirm to execute or abort.

Configuration
-----------

Use Case 1: Create a custom route table associated to your VPC which will control the network traffic rules for the subnet.

Use Case 2: Create an internet gateway attached to your VPC and ensure that your subnet's route table has an entry for the internet bound traffic to the internet gateway.

.. image:: ../images/vpc-route.png

Add the below given configs in the vpc.gyro file inorder to update an existing setup environment.

.. code::

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


Internet Gateway
**************

aws::internet-gateway - The name of the resource which will be used by gyro to identify the internet gateway.

vpc-id: This is the ID of the VPC to create an internet gateway in

Route Table
**************

aws::route-table - The name of the resource which will be used by gyro to identify the custom route table.

vpc-id: This is the ID of the VPC to create a route table for.

subnet-ids: Subnet IDs to associate with this route table, it can be a list of subnet ids. In our case there is just one subnet id.

Route
**************

aws::route - The name of the route resource. This resource will set the route for the internet-bound traffic of the subnet.

route-table-id - This is the ID of the route table to add this route to.

gateway-id - This is the ID of the internet gateway resource which is needed to add a route that directs internet-bound traffic to the internet gateway

cidr-block - This is the destination IPv4 CIDR block to scope the route to a narrower range of IP's.

This will create a custom route table with the below given entries :

================== =================
Destination             Target
================== =================
**10.0.0.0/16**        local
**0.0.0.0/0**          igw-id
================== =================

Update Infrastructure
-----------

Gyro will give a difference of variables that have changed for this file, in our case you will see the creates.

Apply the configuration changes by running gyro up again. Gyro will show you what actions are required.

.. code:: shell

	$ /usr/local/bin/gyro up vpc.gyro

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
