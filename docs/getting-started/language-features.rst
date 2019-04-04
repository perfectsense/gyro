Common Concepts
---------------

Gyro aims to strike a balance between being a static configuration language and a full programming
language. This section covers both the language features for defining configuration as well as the
concepts necessary to inject logic into your infrastructure configuration.

Resources
*********

A resource is the primary type in Gyro. Everything in Gyro built around defining resources. Each
resource maps to a resource in your cloud provider.

A resource is a group of key/value pairs and subresources. Resources can have one or more key/value
pairs and zero or more subresources.

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

Variables
*********

Variables in Gyro defined using the ``key: value`` syntax and can be defined directly within a file (globals) as
well as within resources and subresources. For example:

.. code::

    project: "gyro"

    aws::instance webserver
        image-id: "ami-0cd3dfa4e37921605"
        instance-type: "t2.micro"
    end

Keys must be a valid identifer, or string literal. Identifiers can be made up of letters, digits, ``_``, or ``-``. Spaces
can be included in keys by quoting the key using single quotes (``'``).

Values can by one of the following types:

Scalar Types
++++++++++++

Gyro has the following scalar types: string, numbers, and booleans.

String literals are defined as is zero or more characters enclosed within single quotes (``'my value'``).

String expressions are defined as zero or more characters enclosed within double quotes. String expressions differ from string
literals in that reference expressions will be interpolated prior to using the value (``"my value with $(key)"``).

Numbers can be integers or floats (``10``, ``10.5``, ``-10``).

Booleans are defined as ``true`` or ``false``.

Compound Types
++++++++++++++

Gyro has two compound types: maps, and lists.

Maps are zero or more comma-separated key/value pairs inside curly brackets (``{ key: 'value' }``).

Lists are zero or more comma-separated values inside square brackets (``['item1', 'item2']``).

Virtual Resources
*****************

Conditionals
************

.. raw:: pdf

    PageBreak
