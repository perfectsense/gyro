# Beam Service Discovery

Beam provides service discovery to instances in the cluster. This allows for easy lookup of services using DNS.

Beam itself does not provide DNS services. Instead, Beam generates a hosts file that can be served using `dnsmasq`. Each instance in a cluster runs `dnsmasq` and the Beam client. Beam will periodically fetch a new hosts file from the Beam server and `-HUP` dnsmasq to reload the file.

## Hosts File

The Beam client will place the hosts file at `/etc/beam/cluster.hosts`. The dnsmasq server can be configured to use this hosts file by adding the following to dnsmasq's configuration:

```
addn-hosts=/etc/beam/cluster.hosts
```

The hosts file contains the static name of every instance in the cluster in the format
`<instance-id>.<layer>.layer.<env>.<project>.internal`.

Exampe: `i-18c7ee48.frontend.layer.prod.psdweb.internal`

## Load Balancing

Beam service discovery provides load balancing using DNS. DNS resolution logic attempts to resolve hostnames first by proximity to a service and then by distributing entries evenly across all hosts. This means the hostname `host.mysql.service.prod.psdweb.internal` will resolve to a different IP depending on which host is resolving the it.

## Services

### Health Checks

Services are registered with Beam by providing a service check script. Each service has a directory in `/etc/beam/service.d` named after the service. For example, `/etc/beam/service.d/mysql` would define the mysql service. Inside this directory a check script named `check.sh` should exist. The check script should check the health of a service and return a non-zero exit status upon failures, otherwise it should return an exit status of zero.

### DNS Queries

Once a service is registered it can be looked up using DNS queries against the service subdomain. Using the mysql example above we could query this service like below:

```bash
$ host host.mysql.service.dev.psdweb.internal
host.mysql.service.dev.psdweb.internal has address 10.0.0.116
```

### Proximity DNS Queries

For each service a hostname is generated that will return the closest instance that provides a the service. Round-robin DNS is also provided at the zone, region and global level, if needed.

The following table describles the possible proximity DNS entries for a service:

Locality          | Hostname Format                                    | Returns
----------------- | -------------------------------------------------- | -------
Host              | `host.<service>.service.dev.psdweb.internal`       | Closest instance
Zone              | `zone.<service>.service.dev.psdweb.internal`       | All instances in host's zone
Region            | `region.<service>.service.dev.psdweb.internal`     | All instances in host's region
Global            | `global.<service>.service.dev.psdweb.internal`     | All instances of service
All               | `all.<service>.service.dev.psdweb.internal`        | All instances of service
Primary           | `<primary>.<service>.service.dev.psdweb.internal`  | Tagged primary instance for service

Only healthy instances are returned in DNS lookups. The except to this rule is the `all` proximity query. All instances for the service are returned regardless of their health for this query.

### Primary Tags

Services can also define primary tags. A primary tag is a tag that can only be assigned to a single instance at a time. A example use case for primary tags is tagging the current master MySQL instance or tagging an instance that should be responsible for backups.

Services must define the primary tags they allow. This is done by edit the `config.yml` file in the service's definition directory. The example below shows how to define two primary tags, `master` and `backup` for a service:

```
primaries: ["master", "backup"]
```

### Primary Tag DNS Queries

Once a primary tag is defined a single instance will be chosen as primary for each primary tag. This instance can be queried as follows:

```bash
$ host master.mysql.service.dev.psdweb.internal
master.mysql.service.dev.psdweb.internal has address 10.0.0.116
```

This query looks up the master instance of the mysql service.

### Chosing a Primary Instance

By default a primary instance is chosen by picking the oldest instance that provides the service. This _automatic_ mode. If the instance that was chosen is terminated or becomes unhealthy based on service checks, another host will be chosen automatically.

Using _pinned_ mode it's possible to pin a primary tag to a host so that it never moves even if the host goes unhealthy. This is useful for defining the master in a master/slave service.

To pin primary to a specific host we use the `beam primary` command:

```bash
08:03 $ beam primary --service mysql --primary master prod
Region: US-EAST-1
+------+--------------+--------------+--------------+--------------+----------------------------------------------------------+
| #    | Instance ID  | Environment  | Location     | Layer        | Hostname                                                 |
+------+--------------+--------------+--------------+--------------+----------------------------------------------------------+
| * 1  | i-bddd3f57   | prod         | us-east-1b   | backend      | i-bddd3f57.backend.layer.prod.psdweb.internal            |
|   2  | i-6ed1c685   | prod         | us-east-1b   | master       | i-6ed1c685.master.layer.prod.psdweb.internal             |
|   3  | i-2adf36d6   | prod         | us-east-1c   | backend     | i-2adf36d6.backend.layer.prod.psdweb.internal           |
+------+--------------+--------------+--------------+--------------+----------------------------------------------------------+
Instance i-6ed1c685 is the primary master for the mysql service. Change it? (#/a/N) 2
Setting instance i-6ed1c685 as the primary for service mysql...
Write to s3 Ok
Setting instance i-6ed1c685 as the primary for service mysql...
Write to s3 Ok
Changed the primary master for the mysql service from i-bddd3f57 to i-6ed1c685 in US-EAST-1 region.
```

### Marking an Instance Unavailable

Sometimes it's necessary to remove traffic from an instance to do software updates, replace an instance or stop/start it to move it to a new hypervisor. With Beam service discovery this can be done using the `beam mark` command.

Instances can have a status of `available` or `unavailable`. When an instanced is marked `unavailable` all services provided by the instance are removed from service discovery.

Running `beam mark <env>` will return a list of instances and their current status:

```bash
$ beam mark dev
+----+--------------+-----------------+-----------------+--------------+--------------+--------------+----------------------------------------------------------+
| #  | Instance ID  | Status          | Environment     | Location     | Layer        | State        | Hostname                                                 |
+----+--------------+-----------------+-----------------+--------------+--------------+--------------+----------------------------------------------------------+
| 1  | i-55316a81   | AVAILABLE       | sandbox (dev)   | us-east-1a   | development  | running      | i-55316a81.development.layer.dev.example.internal        |
| 2  | i-94466540   | AVAILABLE       | sandbox (dev)   | us-east-1a   | development  | running      | i-94466540.development.layer.dev.example.internal        |
+----+--------------+-----------------+-----------------+--------------+--------------+--------------+----------------------------------------------------------+
```

To mark an instance unavailable use `beam mark --unavailable <env>` and choose the instance:

```bash
$ beam mark dev
+----+--------------+-----------------+-----------------+--------------+--------------+--------------+----------------------------------------------------------+
| #  | Instance ID  | Status          | Environment     | Location     | Layer        | State        | Hostname                                                 |
+----+--------------+-----------------+-----------------+--------------+--------------+--------------+----------------------------------------------------------+
| 1  | i-55316a81   | AVAILABLE       | sandbox (dev)   | us-east-1a   | development  | running      | i-55316a81.development.layer.dev.example.internal        |
| 2  | i-94466540   | AVAILABLE       | sandbox (dev)   | us-east-1a   | development  | running      | i-94466540.development.layer.dev.example.internal        |
+----+--------------+-----------------+-----------------+--------------+--------------+--------------+----------------------------------------------------------+

More than one instance matched your criteria, pick one to mark into: 1
Marking instance i-55316a81 UNAVAILABLE...
Ok
```

To mark an instance available use `beam mark --available <env>`.