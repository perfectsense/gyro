Common Concepts
---------------

Gyro aims to strike a balance between being a static configuration language and a full programming
language. This section covers both the language features for defining configuration as well as the
concepts necessary to inject logic into your infrastructure configuration.

Resources
*********

A resource is the primary type in Gyro. Everything in Gyro built around defining resources. Each
resource maps to a resource in your cloud provider.

The syntax of a resource is:

.. code::

    <RESOURCE TYPE> <RESOURCE NAME>
        <KEY>: <VALUE>

        <SUBRESOURCE>
            <KEY>: <VALUE>
        end
    end

- *RESOURCE TYPE* is the name of the resource as provided by a provider plugin (e.g. ``aws::instance``).
- *RESOURCE NAME* is a name you give this instance of the resource. This name is used by Gyro to
  track state of the resource. It's also used when referencing a resource in your own Gyro code.
- *KEY/VALUES* map the settings for a particular resource. For more information on what valid keys
  and values are see the "Key/Values" section below.
- *SUBRESOURCE* are resources tied directly to their parent resource. These resources typically
  cannot live on their own.

Putting this into practice, here is a real-word example of defining a resource:

.. code::

    aws::security-group mysql
        group-name: "database"
        vpc-id: "vpc-0042a33a8ee979101"
        description: "Allow web traffic only"

        ingress
            description: "allow inbound mysql traffic, ipv4 only"
            cidr-blocks: ["10.0.0.0/16"]
            protocol: "TCP"
            from-port: 3306
            to-port: 3306
        end
    end

This example defines a security group in AWS named "db-group" in the vpc with an id of ``vpc-0042a33a8ee979101``. Additionally it defines a single
``ingress`` subresource that opens port 3306 to TCP traffic coming from any IP in the cidr block ``10.0.0.0/16``.

Key/Values
**********

Virtual Resources
*****************

Conditionals
************

.. raw:: pdf

    PageBreak
