## Accessing Instances

You now have an EC2 instance and have opened up port 22 so you can ssh to the new instance. Beam provides an ssh wrapper to make connecting to your instances easy without needing to know the hostname or IP.

### Connecting to an Instance

When using `beam ssh` Beam will read your local configuration to get project information and use that to lookup instances using the AWS (or OpenStack) APIs.

Let's connect to our new instance:

```bash
$ beam ssh -k ~/.ssh/example_project-us-east-1-sandbox.pem -u ubuntu dev
Welcome to Ubuntu 14.04.3 LTS (GNU/Linux 3.13.0-66-generic x86_64)

ubuntu@ip-10-0-0-51:~$
```

You'll notice that the `-k` (private ssh file) option was provided with the path to the private ssh key Beam created when the infrastructure was first created. The `-u` (user) option was also provided. If these options were omitted Beam would attempt to ssh as your user.

Since `beam ssh` is just a wrapper around the `ssh` command any configuration options you set in `$HOME/.ssh/config` will be honored.

### Choosing an Instance

If Beam finds more than one instance it will provide a list to chose from. Select the instance you want to login and Beam will continue. For the example below add a second layer identical to the first to the `dev.yml` named _qa_ and `beam up dev` to add a second instance in a different layer.

```bash
12:26 $ beam ssh -k ~/.ssh/example_project-us-east-1-sandbox.pem -u ubuntu dev
+----+--------------+-----------------+--------------+--------------+--------------+----------------------------------------------------------+
| #  | Instance ID  | Environment     | Location     | Layer        | State        | Hostname                                                 |
+----+--------------+-----------------+--------------+--------------+--------------+----------------------------------------------------------+
| 1  | i-55316a81   | sandbox (dev)  | us-east-1a   | development  | running      | i-55316a81.development.layer.dev.example.internal        |
| 2  | i-09e9cfdd   | sandbox (dev)  | us-east-1a   | qa           | running      | i-09e9cfdd.qa.layer.dev.example.internal                 |
+----+--------------+-----------------+--------------+--------------+--------------+----------------------------------------------------------+

More than one instance matched your criteria, pick one to log into:
```

In this case we have two servers across two layers within the same environment so Beam asks us which instance to log into. Optionally we can provide a layer option to `beam ssh` to limit it to just instances in the layer we want. For instance we can limit to just the qa layer using `-l qa`.

```bash
$ beam ssh -k ~/.ssh/example_project-us-east-1-sandbox.pem -u ubuntu -l qa dev
Welcome to Ubuntu 14.04.3 LTS (GNU/Linux 3.13.0-66-generic x86_64)

ubuntu@ip-10-0-0-41:~$
```

Multiple layers can be provided using a comma delimited list, like `-l development,qa`.

### Executing Commands on an Instance

Beam can execute commands on instances. Use the `-e` option to provide a shell command to execute. If Beam finds more than one instance it will execute the command on all instances it finds.

Let's execute a simple command, `uptime` on all instances in the dev environment.

```bash
$ beam ssh -k ~/.ssh/example_project-us-east-1-sandbox.pem -u ubuntu -e "uptime" dev
Executing uptime on i-55316a81.development.layer.dev.example.internal (sandbox)...
 16:44:31 up 16:10,  0 users,  load average: 0.00, 0.01, 0.05
Executing uptime on i-09e9cfdd.qa.layer.dev.example.internal (sandbox)...
 16:44:33 up 19 min,  0 users,  load average: 0.00, 0.01, 0.05
```

### Copying Files to an Instance

Beam can copy local files to remote instances using the `beam copy` command. This is a wrapper around `scp`. All the same options that work for `beam ssh` work for the copy command as well.

```bash
$ echo "hello" > hello.txt
$ beam copy --src hello.txt --dest /tmp/hello.txt -k ~/.ssh/example_project-us-east-1-sandbox.pem -u ubuntu -l qa dev
Copying hello.txt to /tmp/hello.txt on i-09e9cfdd.qa.layer.dev.example.internal (sandbox)...
hello.txt                                                                                                                                   100%    6     0.0KB/s   00:00
```

### Next

We've now created, updated and connected to our infrastructure. We current have two layers, _dev_ and _qa_ running. In the next section we'll [destroy the qa layer](destroy.md).