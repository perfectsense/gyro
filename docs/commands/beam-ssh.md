# SSH

The `beam ssh` command is used to login or execute a command on an instance.

## Usage

Usage: `beam [-debug] ssh [options] <env>`

* `<env>` is the name of the environment yaml file.

* `-g, -gateway` - Use the nearest gateway, if available, as a jump host.

* `-e <command>, -execute <command>` - Command to execute on instance(s).

* `-i <instanceId>, -instance-id <instanceId>` - Instance ID(s) to login to or execute command on.
            
* `-l <layer>, -layer <layer>` - Filter instances by layer(s). Can be a comma delimited list of layers. List should not contain spaces, for example: `-l frontend,master`.

* `-s <subnet>, -subnet <subnet>` - Filter instances by subnet type.

* `-k, -keyfile` - The path to the private key file to use when executing commands against the remote server.

* `-u, -user` - The name of the user to use when executing commands against the remote server.

* `-r, -refresh` - Reload instance data from providers.
            
## Details

The `beam ssh` command can be used to quickly login to an instance without knowning the exact hostname of the instance. If more than one instance is available with a given filter (i.e. `-l <layer>`) then a list similar to the one provided by `beam list` will be presented allowing an instance to be picked.

If multiple instances are available when using the `-e` option the command will be executed on all instances serially. If any instance returns a non-zero exit code execution will stop.

The `beam ssh` command is a wrapper around the `ssh` command so any settings in `~/.ssh/config` should work as expected.