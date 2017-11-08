# Provision

The `beam provision` command provisions an instance or layer using the provisioner defined in the layer configuration.

## Usage

Usage: `beam provision [options] <env>`

* `<env>` is the name of the environment yaml file.

* `-i <instanceId>, -instance-id <instanceId>` - Instance ID(s) to provision.

* `-l <layer>, -layer <layer>` - Filter instances by layer(s). Can be a comma delimited list of layers. List should not contain spaces, for example: `-l frontend,master`.

* `-k <keyfile>, -keyfile <keyfile>` - Private key to use (i.e. ssh -i ~/.ssh/id_rsa).

* `-u <user>, -user <user>` - User to login as.

* `-r, -refresh` - Reload instance data from providers.

* `-ask` - Ask which instance to provision if more than one are available.
            
## Details

The `beam provision` command will execute all defined provisioners for each instance of a layer.

This command will always use the internal hostname and/or IP when attempting to provision an instance.

This command will set the Chef environment to `production` if the beam environment is `prod`. It will set the Chef environment to `development` if the beam enviromnet is `dev`.

Multiple layers can be provisioned by providing layers as a comma delimited list to the `-layer` option: `beam provision -l backend,master prod`

## Configuration

The `beam provision` command depends on the `provisioners` settings in layer and gateway configuration.

### Example Configuration

```
    provisioners:
      - type: knife-solo
        cookbookPath: ../chef
        recipe: sample::frontend
```

### Configuration Reference

* `type` - The type of provisioner. Currently only `knife-solo` is supported.

* `cookbookPath` - The path to the project cookbook.

* `recipe` - The recipe to use to provision. Currently only a single recipe is supported.
