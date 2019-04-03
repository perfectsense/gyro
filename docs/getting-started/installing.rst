
Installing Gyro
---------------

Official distributions of Gyro are available for macOS, and Linux operating systems. Official distributions
include a stripped-down OpenJDK 11 so it is not necessary to download and install Java.

Official distributions are:

================== =================
OS                  Archive
================== =================
**macOS**          `gyro-osx-0.14-20190401.141851-57.zip <https://artifactory.psdops.com/gyro-snapshots/gyro/gyro-osx/0.14-SNAPSHOT/gyro-osx-0.14-20190401.141851-57.zip>`_
**Linux**          `gyro-linux-0.14-20190401.141747-56.zip <https://artifactory.psdops.com/gyro-snapshots/gyro/gyro-linux/0.14-SNAPSHOT/gyro-linux-0.14-20190401.141747-56.zip>`_
================== =================

macOS and Linux
+++++++++++++++

Download the distribution and extract it into ``/usr/local/bin``. For example:

.. code:: shell

    $ unzip -d /usr/local/bin gyro-osx-0.14-20190401.141851-57.zip

        Archive:  gyro-osx-0.14-20190401.141851-57.zip
       creating: /usr/local/bin/gyro-rt/
       creating: /usr/local/bin/gyro-rt/bin/
      inflating: /usr/local/bin/gyro-rt/bin/java
      inflating: /usr/local/bin/gyro-rt/bin/jrunscript
      inflating: /usr/local/bin/gyro-rt/bin/keytool
      .
      .
      .
      inflating: /usr/local/bin/gyro-rt/release
      inflating: /usr/local/bin/gyro
    $

Test Your Installation
++++++++++++++++++++++

Check that Gyro is installed and working by creating a small test configuration and running Gyro in test mode. Create
a file named ``test.gyro`` with the following contents:

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

    aws::vpc example-vpc
        cidr-block: "10.0.0.0/16"
    end


To verify the installation run ``gyro`` in test mode. If ``y`` is given at the prompt a state file will be created
in the local directory named ``test.gyro.state``.

.. code:: shell

    $ /usr/local/bin/gyro up --test test.gyro
    Loading plugin: gyro:gyro-aws-provider:0.14-SNAPSHOT...

    Looking for changes...

    + Create vpc 10.0.0.0/16 - example-vpc

    Are you sure you want to change resources? (y/N) y

    + Creating vpc 10.0.0.0/16 - example-vpc OK
    $

.. raw:: pdf

    PageBreak