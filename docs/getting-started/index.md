# Getting Started

## Installation

Beam is commandline tool that must be installed locally on your machine. Beam can be installed from binary packages or compiled from source.

### Prerequisites

Beam requires Java 1.7 or greater. If compiling Beam from source, Maven 3 or greater is required.

### Installing Binary Package

To install Beam, [download the latest version](https://github.com/perfectsense/beam/releases). For Linux and Mac OX Beam is distributed as a single executable binary. For Windows it is distributed as an executable jar file. Ensure the `beam` binary is in your `PATH`.

### Verifying Installation

On Linux and Mac OS X you can verify the installation of the `beam` command by opening a terminal and executing `beam version`. You should see output similar to that below:

```sh
$ beam version
Beam version 0.9.1-final
```

On Windows you can verify the installation of the `beam` command by opening a command prompt and executing `java -jar beam.jar version`. The output should be similar to that above.

## Credentials

For AWS, Beam uses the same credentials file as the AWS CLI tool, `$HOME/.aws/credentials`. Beam uses _named profiles_ to segregate accounts. More information on this file format  and _named profiles_ can be found in the AWS CLI [documentation](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-multiple-profiles).

By default Beam will search for a named profile based on the Beam project name. Beam can also be told to use a specific profile. For examples in the Getting Started guide the following entry should exist in `$HOME/.aws/credentials` with valid access keys for the account you want to use.

```ini
[example_project]
aws_access_key_id = AKJFKAJFAAAAAAAAAAAA
aws_secret_access_key = JLFKsfjasdfa9dasfjakljflj000000000000000
```

For Rackspace, Beam uses a yaml formatted configuration file to define _named accounts_. To add Rackspace credentials edit the file `$HOME/.beam/openstack.yml` and add the following yaml configuration:

```yaml
accounts:
  - name: example_project
    username: yourusername
    password: <password>
    apiKey: <api_key>
```

### Next

At this point the beam binary is installed and working. You should also have valid access keys defined. In the next section we'll [create a new EC2 instance](create.md) using Beam.