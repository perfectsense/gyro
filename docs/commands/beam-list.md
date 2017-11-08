# List

The `beam list` command lists instances in a project.

## Usage

Usage: `beam list [options] <env>`

* `<env>` is the name of the environment yaml file.

* `-l <layer>, -layer <layer>` - Filter instances by layer(s). Can be a comma delimited list of layers. List should not contain spaces, for example: `-l frontend,master`.

* `-s <subnet>, -subnet <subnet>` - Filter instances by subnet type.

* `-r, -refresh` - Reload instance data from providers.
            
## Details

The `beam list` command will query each provider and display a list of instances and associated information about each instance.

Information provided includes the `instance-id`, `environment`, `location`, `layer`, `services` and `hostname`.

## Example Output

```
~/sample/beam$ beam list prod
╔══════════════╤══════════════╤══════════════╤══════════════╤══════════════════════════════╤══════════════════════════════════════════════════════════╗
║ Instance ID  │ Environment  │ Location     │ Layer        │ Services                     │ Hostname                                                 ║
╠══════════════╪══════════════╪══════════════╪══════════════╪══════════════════════════════╪══════════════════════════════════════════════════════════╣
║ i-94cb6fb8   │ prod         │ us-east-1c   │ backend      │ reader, solr, mysql          │ i-94cb6fb8.backend.layer.prod.sample.internal            ║
║ i-d33dc23d   │ prod         │ us-east-1c   │ frontend     │ brightspot                   │ i-d33dc23d.frontend.layer.prod.sample.internal           ║
║ i-8fa9baa5   │ prod         │ us-east-1a   │ gateway      │ gateway                      │ i-8fa9baa5.gateway.layer.prod.sample.internal            ║
║ i-1a140630   │ prod         │ us-east-1a   │ master       │ solr, mysql                  │ i-1a140630.master.layer.prod.sample.internal             ║
║ i-65c26649   │ prod         │ us-east-1c   │ gateway      │ gateway                      │ i-65c26649.gateway.layer.prod.sample.internal            ║
║ i-96b857bb   │ prod         │ us-east-1a   │ backend      │ reader, solr, mysql          │ i-96b857bb.backend.layer.prod.sample.internal            ║
║ i-0b8c0526   │ prod         │ us-east-1a   │ frontend     │ brightspot                   │ i-0b8c0526.frontend.layer.prod.sample.internal           ║
╚══════════════╧══════════════╧══════════════╧══════════════╧══════════════════════════════╧══════════════════════════════════════════════════════════╝
~/sample/beam$
```
