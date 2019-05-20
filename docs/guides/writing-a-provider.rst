Writing a Provider Plugin
=========================

Provider plugins extend Gyro by making new resource types available to the Gyro Configuration Language. Each resource
implementation is responsible for refreshing, creating, updating, and deleting a resource.

Provider plugins are written in Java and distributed as JARs using the Maven repository system.

Code Organization
-----------------

While provider implementor's are free to organize code however they see fit, the following are best practices that
we've found to work well when organizing provider implementations.

Each provider should be placed in a unique Java package named for the provider. For example, the AWS provider
should be placed in the ``gyro.provider.aws`` package.

Additional packages should be used to logically separate provider services. For example, AWS CloudFront service
should be placed in the ``gyro.provider.aws.cloudfront`` package, while AWS Autoscaling service should be placed
in the ``gyro.provider.aws.autoscaling`` package.

Example:

.. code-block:: shell

    build.gradle
    src/main/java
    src/main/java/gyro/sample/compute/NetworkResource.java
    src/main/java/gyro/sample/compute/ComputeResource.java
    src/main/java/gyro/sample/compute/package-info.java
    src/main/java/gyro/sample/SampleCredentials.java
    src/main/java/gyro/sample/SampleResource.java
    src/main/java/gyro/sample/SampleResourceFinder.java
    src/main/java/gyro/sample/SampleResourceFinder.java
    src/main/java/gyro/sample/SampleProvider.java
    src/main/java/package-info.java

Credentials
-----------

Every provider must implement a subclass of the ``gyro.core.Credentials`` class. This class is responsible for
loading credential information necessary to use the cloud provider's API.

It is up to the implementor to determine the best method for storing credentials. Typically storing credentials
should follow the cloud provider's API convention. For example, the AWS provider can use environment variables or
the ``$HOME/.aws/credentials`` file to load credentials.

Implementation
--------------

Annotations
+++++++++++

The class should be annotated with the ``@ResourceType(string)`` annotation. The name provided by this annotation is
used by the Gyro language to lookup the resource implementation. For example, ``ResourceType("instance")`` in the AWS
provider will make ``aws::instance`` available to the Gyro language.

The Java package(s) that make up a provider should be annotated with one or more of the following:

**@ResourceNamespace(string)**
    This is name the primary namespace of the provider that will be exposed to the Gyro language. For example, for
    the resource ``aws::instance`` this annotation defines the ``aws`` portion of the resource name. The namespace
    should be short, all-lower case, unique, and concisely describe the provider.

    This is a ``package`` annotation intended to be used in a ``package-info.java`` file at the root of the provider
    projects main package.

**@DocNamespace(string)**
    Same as above but used for auto-generated documentation.

**@DocGroup(string)**
    As explained above each provider should be logically separated into packages based on the provider's API and
    service groupings. Each of these service group packages should define this annotation.

    This is a ``package`` annotation intended to be used in a ``package-info.java`` file.

Fields should be annotated with one of the following annotations, if applicable:

**@ResourceId**
    This annotation marks the field that is the unique identifier for the resource.

**@ResourceOutput**
    This annotation marks fields that are read-only and that will be updated after the initial creation of a resource. This
    annotation's primary purpose is for auto-generated documentation.

**@ResourceUpdatable**
    This annotation marks fields which can be updated independently using the provider's API.

Methods
+++++++

Each resource must extend the ``Resource`` class and implement the abstract methods shown below.

.. code-block:: java

    public abstract class Resource extends Diffable {
        public abstract boolean refresh();
        public abstract void create();
        public abstract void update(Resource current, Set<String> changedFieldNames);
        public abstract void delete();
    }


**refresh()**

The ``refresh()`` method is called by Gyro to refresh the state of a resource. Implementations should query the
provider API and update the current object instance with updated data.

If the object no longer exists in the cloud provider this method should return ``false``, otherwise return ``true`` to
indicate the data has been updated.

The following example implementation of ``refresh()`` updates an EBS volume in AWS.

.. code-block:: java

    @Override
    protected boolean doRefresh() {
        Ec2Client client = createClient(Ec2Client.class);
        Volume volume = getVolume(client);
        if (volume == null) {
            return false;
        }

        setAvailabilityZone(volume.availabilityZone());
        setCreateTime(Date.from(volume.createTime()));
        setEncrypted(volume.encrypted());
        setIops(volume.iops());
        setKmsKeyId(volume.kmsKeyId());
        setSize(volume.size());
        setSnapshotId(volume.snapshotId());
        setState(volume.stateAsString());
        setVolumeType(volume.volumeTypeAsString());

        DescribeVolumeAttributeResponse responseAutoEnableIo = client.describeVolumeAttribute(
            r -> r.volumeId(getVolumeId())
                .attribute(VolumeAttributeName.AUTO_ENABLE_IO)
        );

        setAutoEnableIo(responseAutoEnableIo.autoEnableIO().value());

        return true;
    }

**create()**

The ``create()`` method is called by Gyro when it determines that it should create a resource. Implementations should
create the resource and update any unique ID fields on the current object instance that will be necessary to query for
the resource ``refresh()`` method.

Gyro will call ``create()`` if the resource does not exist in state or if a non-updatable field has been modified. In
the later case Gyro will first call ``delete()``.

The following example implementation of ``create()`` creates an EBS volume in AWS:

.. code-block:: java

    @Override
    protected void create() {
        Ec2Client client = createClient(Ec2Client.class);

        CreateVolumeResponse response = client.createVolume(
            r -> r.availabilityZone(getAvailabilityZone())
                .encrypted(getEncrypted())
                .iops(getVolumeType().equals("io1") ? getIops() : null)
                .kmsKeyId(getKmsKeyId())
                .size(getSize())
                .snapshotId(getSnapshotId())
                .volumeType(getVolumeType())
        );

        setVolumeId(response.volumeId());
        setCreateTime(Date.from(response.createTime()));
        setState(response.stateAsString());
    }

**update(Resource current, Set<String> changedFieldNames)**

The ``update(..)`` method is called by Gyro when it determines that a resource attribute can be updated. This method
will only be called if the fields that changed are marked with the ``@ResourceUpdatable`` annotation. In cases where
both updatable and non-updatable fields are changed Gyro will not call this method, instead it will call ``delete()``
followed by ``create()``.

The ``changedFieldNames`` set contains the names of fields that changed. This allows implementations to minimum the
of API calls necessary to effect an update.

The following example implementation of ``update(..)`` updates an EBS volume in AWS:

.. code-block:: java

    @Override
    protected void update(AwsResource config, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);
        if (changedProperties.contains("iops") || changedProperties.contains("size") || changedProperties.contains("volume-type")) {

            client.modifyVolume(
                r -> r.volumeId(getVolumeId())
                    .iops(getVolumeType().equals("io1") ? getIops() : null)
                    .size(getSize())
                    .volumeType(getVolumeType())
            );
        }

        if (changedProperties.contains("auto-enable-io")) {
            client.modifyVolumeAttribute(
                r -> r.volumeId(getVolumeId())
                    .autoEnableIO(a -> a.value(getAutoEnableIo()))
            );
        }
    }

**delete()**

The ``delete()`` method is called by Gyro when it determines that a resource should be deleted from the provider. The
resource implementation should delete the resource from the provider.

Documentation
-------------

Documentation for providers is auto-generated using a special Java Doclet. This doclet reads specially formatted comments
on the class and method implementations for each resource.

Each resource should have a class level comment describing what the resource is followed by at least one simple example
showcasing using the resource, such as:

.. code-block:: shell

    /**
     * Creates an Instance with the specified AMI, Subnet and Security group.
     *
     * Example
     * -------
     *
     * .. code-block:: gyro
     *
     *     aws::instance instance
     *         ami-name: "amzn-ami-hvm-2018.03.0.20181129-x86_64-gp2"
     *         shutdown-behavior: "stop"
     *         instance-type: "t2.micro"
     *         key-name: "instance-static"
     *     end
     */

Each resource field getter should have a single line comment with a description of the field, possible values, side
effect of the field, and whether the field is required or optional.

.. code-block:: shell

    /**
     * The ID of an AMI that would be used to launch the instance. (Required)
     */
    public String getAmiId() {
        return amiId;
    }

Generating Documentation
++++++++++++++++++++++++

Documentation is generated using the Gyro Doclet. To generate documentation using the Doclet add the following to
the providers ``build.gradle`` file, then run ``gradle referenceDocs``:

.. code-block:: shell

    task referenceDocs(type: Javadoc) {
        title = null // Prevents -doctitle and -windowtitle from being passed to GyroDoclet
        source = sourceSets.main.allJava
        classpath = configurations.runtimeClasspath
        options.doclet = "gyro.doclet.GyroDoclet"
        options.docletpath = configurations.gyroDoclet.files.asType(List)
    }