## AWS Cloud Configuration

Region is a container for a list of zone configurations.

The `cidr` key defines the network to use for a region.

The `name` key defines the name of the region. For the `ec2` provider the region names must match the corresponding region names from AWS.

By convention region CIDRs should be defined as follows:

Region         | CIDR
-------------- | -------------
us-east-1      | `10.0.0.0/16`
us-west-1      | `10.1.0.0/16`
us-west-2      | `10.2.0.0/16`
eu-west-1      | `10.3.0.0/16`
ap-southeast-1 | `10.4.0.0/16`
ap-southeast-2 | `10.5.0.0/16`
ap-northeast-1 | `10.6.0.0/16`
sa-east-1      | `10.7.0.0./16`

The CIDR range 10.254.0.0/16 is reserved project VPN client addresses.

**Zone Configuration**

Each zone defines a list of named subnets. Each subnet defines a cidr and whether the subnet is public or private.

Subnets can have multiple names and the names can be the same as other subnets. This allows grouping similar subnets in different zones. For example you can name subnets without routable IP address `private` and then later in the host configuration referring to the `private` subnet will refer to all subnets without routable IPs.

The subnet names (aka `types` attribute) will be used in the environment configuration to place instances. 

By default subnets are private only. Outbound access to the internet is provided by the gateway. The gateway instance is a special instance that should at a minimum provide [nat](http://docs.aws.amazon.com/AmazonVPC/latest/UserGuide/VPC_NAT_Instance.html) services.

To make a subnet public set the key `publicAccessible` to `true`.

Example zone configuration:

```
- name: us-east-1a
  subnets:
    - types: [public, public-east, gateway]
      cidr: 10.0.0.0/26
      publicAccessible: true
      gateway:
        image: ami-b424f0dc
        ipAddress: 10.0.0.10
        instanceType: t2.medium

        provisioners:
          - type: knife-solo
            cookbookPath: ../chef
            recipe: univision::gateway

        securityRules:
          - production
          - operations

    - types: [private, private-east, private-us-east-1a]
      cidr: 10.0.0.128/25

    - types: [development, qa]
      cidr: 10.0.0.64/26
      publicAccessible: true
```

By convention zone CIDRs should using following base (each zone gets a full Class C):

Zone           | CIDR
-------------- | -------------
us-east-1a     | `10.0.0.0`
us-east-1b     | `10.0.1.0`
us-east-1c     | `10.0.2.0`
us-east-1d     | `10.0.3.0`
us-east-1e     | `10.0.4.0`
us-west-1a     | `10.1.1.0`
us-west-1b     | `10.1.2.0`
us-west-1c     | `10.1.3.0`

**Environments**

Environments in beam are named for the file the environment is defined it. The file `prod.yml` would define the `prod` environment.

Environment configuration consists of a reference to the network configuration and a list of layers.
