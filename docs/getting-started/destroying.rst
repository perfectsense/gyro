
Destroying Infrastructure
==================

After creating temporary resources for testing or performing other activities, it may be necessary to destroy infrastructure.
Gyro will never destroy infrastructure without prompting.

Delete actions completely remove resources from the cloud.

In order to remove a resource from the existing infrastructure, remove the configs from the resource file.

.. code::

   aws::route route-example
       route-table-id: $(aws::route-table route-table-example | route-table-id)
       destination-cidr-block: "0.0.0.0/0"
       gateway-id: $(aws::internet-gateway ig-example | internet-gateway-id)
       cidr-block: "10.0.0.0/16"
   end


Removing the route resource will delete the internet-bound traffic route from the route table.

.. code:: shell

   $ /usr/local/bin/gyro up vpc.gyro

   Loading plugin: gyro:gyro-aws-provider:0.14-SNAPSHOT...
   Looking for changes...

   - Delete route 0.0.0.0/0 through gateway test-internet-gateway-id-8afdb2cd1cead425

   Are you sure you want to change resources? (y/N) y

   - Deleting route 0.0.0.0/0 through gateway test-internet-gateway-id-8afdb2cd1cead425 OK

Gyro confirms the deletion. Typing y will execute the delete request. All resource deletions work the same way in gyro: remove the resource section from the config file.

In order to remove the entire virtual private cloud network instead of associated resources, remove the entire VPC config section from vpc.gyro file.
Gyro will start deleting the parent resource along with the associated resources.

Example given below : remove this entire section from the vpc.gyro file :

.. code::

   aws::vpc vpc-example
       cidr-block: "10.0.0.0/16"
   end

   aws::subnet subnet-public-us-east-1a
       vpc-id: $(aws::vpc vpc-example | vpc-id)
       cidr-block: "10.0.0.0/24"
       availability-zone: "us-east-1a"
   end

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
       cidr-block: "10.0.0.0/16"
   end

The resource vpc and associated resources will get deleted after ``y`` is given at the prompt.

.. code:: shell

   $ /usr/local/bin/gyro up vpc.gyro

   Loading plugin: gyro:gyro-aws-provider:0.14-SNAPSHOT...
   Looking for changes...

   - Delete test-vpc-id-ae7c531b458e74ff 10.0.0.0/16 - vpc-example
   - Delete test-subnet-id-9167df7f6b06349d 10.0.0.0/24 in us-east-1a
   - Delete test-internet-gateway-id-8afdb2cd1cead425
   - Delete test-route-table-id-b5a7bc3483284b7d

   Are you sure you want to change resources? (y/N) y

   - Deleting test-route-table-id-b5a7bc3483284b7d OK
   - Deleting test-internet-gateway-id-8afdb2cd1cead425 OK
   - Deleting test-subnet-id-9167df7f6b06349d 10.0.0.0/24 in us-east-1a OK
   - Deleting test-vpc-id-ae7c531b458e74ff 10.0.0.0/16 - vpc-example OK

.. raw:: pdf

    PageBreak
