Introduction
------------

Gyro is a command-line tool for automating creating, updating, and maintaining cloud infrastructure. Gyro makes
infrastructure-as-code possible.

Using Gyro allows you to model your infrastructure using the Gyro language and then create, update, and
maintain that infrastructure using the ``gyro`` command-line tool.

The Gyro language is a DSL (domain specific language) for defining cloud infrastructure in a human-readable format. It
was built with readability and organizational flexbility in mind. The language provides the ability to concisely define
cloud infrastructure resources along with language constructs such a ``for`` loops, ``if`` conditionals, and
``virtual-resource`` definitions for packaging resources into reusable components.

Why Use Gyro?
+++++++++++++

Gyro is ideal for anyone looking to automate managing infrastructure in a cloud provider such as AWS, or Azure. Here
are several areas Gyro can help:

**Cloud Operations Teams**

Gyro can help teams develop processes for infrastructure changes. Traditionally cloud infrastructure changes
were accomplished using cloud vendor web interfaces which make it difficult verify and track changes. Using
Gyro's DSL to define infrastructure as code teams can manage infrastructure changes using more formal processes
of review. Infrastructure changes can be reviewed to ensure only the requested changes are being made. Changes can
be tracked using any version control sytem.

**Self Service**

Gyro can help organizations implement self service infrastructure. Traditionally cloud infrastructure creation has to
go through a central operations team. As a company grows the ability for an operations team keep pace with these
requests can be difficult or require growing the operations team. Using Gyro common infrastructure can be defined and
shared across an organization allow teams outside the operations team to build and maintain their own infrastructure.

.. raw:: pdf

    PageBreak
