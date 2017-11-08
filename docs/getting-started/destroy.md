## Destroying Infrastructure

Although rare it is sometime necessary to destroy infrastructure. In particular this is useful when creating temporary layers for testing. Destroying infrastructure is based completely on removing configuration from your local configuration.

Beam will never destroy infrastructure without extra prompting to do so which we'll show below.

Let's destory the _qa_ layer we created in the last section. To destroy this layer just remove it from `dev.yml` and run `beam up dev`.

```bash
$ beam up dev
Looking for changes...

vpc-69fd550d 10.0.0.0/16
    subnet-cf09e5b9 10.0.0.0/26 in us-east-1a
        - Delete example_project dev serial-1 qa layer instance [i-09e9cfdd]

Skipped deletes in ec2 cloud. Run again with the --delete option to execute them.
```

Beam detected that we want to delete the qa layer but skipped it because we didn't explicitly ask it to delete resources. This is a safety measure to make sure we don't accidentally delete resources. Run the `beam up dev` command again but this time with `--delete`.

```bash
$ beam up --delete dev
Looking for changes...

vpc-69fd550d 10.0.0.0/16
    subnet-cf09e5b9 10.0.0.0/26 in us-east-1a
        - Delete example_project dev serial-1 qa layer instance [i-09e9cfdd]

Are you sure you want to delete resources from ec2 cloud in account sandbox? (y/N) y

Executing: - Delete example_project dev serial-1 qa layer instance [i-09e9cfdd] OK
```

This time Beam asked if we wanted to delete and after typing 'y' it will execute the delete request. We are now left with a single layer, the _dev_ layer.

All resource deletes work the same way in Beam, whether it's an instance, ELB or security group, remove it from the configuration and `beam up --delete <env>`.

### Next

With the _qa_ layer gone we'll focus on setting up the _dev_ layer. In the next section we'll [provision the dev layer](provision.md).