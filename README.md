
# Beam

Beam is a commandline tool for automating the creation and management of the cloud resources that make up your infrastructure. Beam can manage cloud resources from Amazon Web Services and Rackspace (OpenStack) and can be extended to support other cloud providers.

### Documentation:

* [Getting Started](docs/getting-started/index.md)
* [Service Discovery](docs/service-discovery.md)
* [Configuration](docs/configuration.md)

## Commands

[beam init](docs/commands/beam-cookbook.md) - Create initial beam config.

`beam up <env>` - Launch instances in the given environment.

`beam down <env>` - Stop instances in the given environment.

[beam provision](docs/commands/beam-provision.md) - Provision an instance or layer.

[beam list](docs/commands/beam-list.md) - List hosts in this cluster.

[beam ssh](docs/commands/beam-ssh.md) - ssh to a single instance or execute a command across all instances.

`beam status <env>` - Execute beam health checks on instances.

`beam primary [-i <instance>][-service <service][-primary <tag>] <env>` - Mark an instance as a primary server for a given name. 

For example you can mark a MySQL server as the `master` server using `beam primary -i i-18c7ee48 -service mysql -primary master prod`.

## Service Discovery

`beam service start` - Start the beam service. The service provides a REST API that beam clients can use to request cluster information.

`beam service hostsfile <instance>` - Request a cluster hostsfile from a beam server instance.
