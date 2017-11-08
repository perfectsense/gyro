# Setup

### Requirements

Beam requires Java 1.7 or greater. To compile Beam, Maven 3 or greater is required.

##### Compiling Beam

```
$ git clone git@github.com:perfectsense/beam.git beam
$ cd beam
$ mvn clean package
```

After compiling the beam binary will be at `dist/beam`. Either copy this binary into your PATH or, if you plan to recompile a lot, add the `dist` directory to your PATH.

##### Installing Java Cryptography Extension (JCE) Unlimited Strength

The `beam cookbook` command generates and encrypts passwords for use by Chef. The encryption requires higher strength security than is provided by default in Java.

[Download](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html) and install the Java JCE policy files. The policy file is contained in the `local_policy.jar` and `US_export_policy.jar` files. These must be located and replaced using the downloaded policy files.

On a Mac you can use `mdfind` to quickly locate the policy files:

```
jcollins@vortac:~$ mdfind policy.jar | grep jdk1.7
/Library/Java/JavaVirtualMachines/jdk1.7.0_06.jdk/Contents/Home/jre/lib/security/local_policy.jar
/Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home/jre/lib/security/local_policy.jar
```

##### AWS Credentials

Beam uses the AWS credentials file at `$HOME/.aws/credentials` to authenticate with the AWS webservice API. This is the same crendentials file used by the AWS command-line client.

Beam expects to find a profile that matches the project name found in the project.yml.

For example, if the project is named `sandbox` then you should have the following in `$HOME/.aws/credentials`:

```
[sandbox]
aws_secret_access_key=lsadkfjlaskjdflasjkdflaksjdflakjsdfljasd
aws_access_key_id=LSKJFLSKJDFJLSKDJFLJ
```
