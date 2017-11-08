## Beam Configuration

Beam configuration files are YAML formatted files that describe a project setup. This includes information on private networks, subnets, security groups, load balancers and instances.

Each project has at least one network configuration file and one environment configuration file.

The network configuration describes the private network, subnets, load balancers, regions, zones and any security groups. The network configuration can be shared across several environment configuration files.

The environment configuration describes the layers, services and placement of instances for an environment.

### Network Configuration

Network configuration provides the basic information about a project. Its name, public subdomain, internal domain, environment and serial number.

The name of the YAML file becomes the name of the environment.

```
name: sandbox
account: sandbox
sandbox: false
subdomain: sandbox.psdops.com
internalDomain: sandbox.internal
serial: 3
```

The `account` attribute defines the name of the account credentials to use for this project. The provided name is used to look up credentials in the `~/.aws/credentials` file. If `account` is not provided the project `name` is used instead.

The `sandbox` attribute is a special attribute that can be used to mark a project as being sandboxed for testing. This attribute will be passed in userdata to all instances launched by Beam. This allows them to change their behavior if they are sandboxed. For instance they could disable monitoring alerts if sandbox is enabled.

The `subdomain` and `internalDomain` attributes define the DNS domains used for this project. These are for internal use only. Private hosts will only have an
internal domain. Public hosts will use `subdomain` since they will be publicly accessible.

The `serial` setting allows creation of identical environments that do not share a VPC. This allows starting up a new production environment without disrupting the current production environment. This is useful if a cluster configuration changes significantly.

### Security Rule Configuration

Security rule configuration allows opening various ports to a given IP range. Currently only ingress rules are supported.

Rules are made up of a list of permissions. Each permission defines an IP range and one or more ports to open. 

The rule names should be named for the environment and layer they belong to, `production-frontend` for the production environment's frontend, `qa` for the qa environment, etc.

The permissions should be named for the service they are opening. For example name the rule that opens port 80 should be named `http`.

Permissions can also reference other, previously defined, rules by replacing the CIDR range with the name of the rule.

```
  - name: production
    permissions:
      - name: openvpn
        cidr: 0.0.0.0/0
          - 1194

      - name: http
        cidr: 0.0.0.0/0
        ports:
          - 80
          - 443

      - name: qa
        cidr: qa
```

### Cloud Configuration

Each cloud provider has its own cloud configuration format.

Currently AWS cloud is fully supported with partial support for Rackspace Cloud (OpenStack).

[AWS Cloud](cloud-aws.md)

### Layer Configuration

A layer is a group of identical servers that are launched together. Typically there is a frontend layer for the application code and a backend layer for database servers.

A layer defines the machine image id to use, instance type, how to provision the instance, security rules and where to launch the instance.

Layers can dfined provisioners to allow for quick provisioning of an instance after it has been launched. Provisioning is currently a secondary step using the `beam provision` command. Instances are not automatically provisioned by `beam up`. Currently the only provisioner is **knife-solo**.

Security rules are a list of security rules to apply to each instance. The rules are defined in the [network configuration](configuration.md).

Placements define where to launch a layer and how many instances to launch.

For example, the following layer configuration defines a frontend server with the `sample frontend hvm [24]` image using an m3.medium instance type:

```
- name: frontend
  image: sample frontend hvm [24]
  instanceType: m3.medium
        
  placements:
    - subnetType: private
      sizePerSubnet: 2
```
