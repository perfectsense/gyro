# Beam Cloud Language Reference

# Extension Points

## Event Hook Extension

An event hook extension allows extension writes to be notified at various points in the processing of
Beam configuration. The available hooks are defined below:

```java
public interface EventHookExtension {
    
    public void afterResourceCreate();  
    public void beforeResourceCreate();  
    
    public void afterResourceDelete();  
    public void beforeResourceDelete();  
    
    public void afterResourceUpdate();  
    public void beforeResourceUpdate();  
    
}
```

## Virtual Resource Extension

A virtual resource extension allows plugins to extend the Beam configuration language in much the same way
as provider extensions. The main differences is that virtual resources generate one or more cloud provider
resources as their output. State is not saved for the virtual resource, only state of the cloud provider resources
it generates are saved.

```java
public interface VirtualResource {
    
    public List<Resource> expand(Resource resource);
    
}
```

**Example:**

The example below defines a frontend layer virtual resource. This virtual resource takes the 
information given to it and generates a load balancer resource, an autoscale group resource,
and a launch configuration. This allows the frontend layer definition to contain only the 
minimal necessary configuration values.

```
brightspot::frontend web {
    brightspot_build_location: s3://mybucket/builds
    brightspot_build_number: 495
}
```

```java
@Resource("frontend")
public class FrontendLayer implements VirtualResource {
    
    public List<Resource> expand(Resource resource) {
        List<Resource> resources = new ArrayList<>();
        
        LoadBalancerResource elb = new LoadBalancerResource();
        elb.setName("frontend - build " + resource.getValue("brightspot_build_number"));
        
        LaunchConfiguration launchConfiguration = new LaunchConfiguration();
        launchConfiguration.setUserData("build: " 
            + resource.getValue("brightspot_build_location")
            + "/app-" + resource.getValue("brightspot_build_number") + ".war");
        
        AutoscaleGroupResource asgResource = new AutoscaleGroupResource();
        asgResource.setName("frontend - " + resource.getName());
        asgResource.setLaunchConfiguration(launchConfiguration);
        asgResource.setLoadBalancer(elb);
        
        resources.add(elb);
        resources.add(launchConfiguration);
        resources.add(asgResource);
        
        return resources;
    }
    
}
```

# Syntax

# Variables

Variables can be set using one of three methods: Using the `let` keyword, by giving
a resource an implicit name, or by using the `let` command in combination with defining
a resource.

**Setting variable using `let` keyword**

```
let project = "my project"
```

**Setting variable by naming a resource**

The following sets the variable `${web_elb}`.

```
aws::elb web_elb {
    name: "web elb"
}
```

**Setting variable from output of resource definition**

```
let web_elb = aws::elb {
    name: "web elb"
}
```

## Scoping 

```
let web_elb = aws::elb {
    name: "web elb"
}
```

Variables can be used from other files by using the `include` keyword with the variable 
namespace option using the `as` keyword. When this option is not present variables from included 
files cannot be referenced within the including file.

The one exception to this rule is `init.beam` files. These files are always read before any configuration 
in the same directory. Variables defined in these are global.

### Scoping Example

**init.beam**:

``` 
let project = "perfectsense"
```

**defaults.beam**:

``` 
aws::vpc project_vpc {
    name: "default vpc"
    cidr: 10.0.0.0/16
}

aws::subnet development_subnet {
    vpc: ${project_vpc}
    cidr: 10.0.0.0/24
}

aws::subnet us-east-1a {
    vpc: ${project_vpc}
    zone: us-east-1a
    cidr: 10.0.0.0/24
}

aws::subnet us-east-1b {
    vpc: ${project_vpc}
    zone: us-east-1b
    cidr: 10.0.1.0/24
}

let private_subnets = [${us-east-1b}, ${us-east-1a})]
let public_subnets = [${us-east-1b}, ${us-east-1a})]

let master_subnet = ${us-east-1a})
let development_subnet = ${us-east-1a})
```

**development/init.beam**:

``` 
let environment = "production"
```

**development/box.beam**:

```
include "../defaults.beam" as defaults

aws::instance development {
    name: "development box"
    instance_type: t2.medium
    image: ami-12312adfaf
    vpc: ${defaults.project_vpc}
    subnet: ${defaults.development_subnet}
}
```

**production/init.beam**:

``` 
let environment = "production"
```

**production/frontend.beam**:

```
include "../defaults.beam" as defaults

let layer = "production"

aws::elb web_elb {
    name: "web"
    
    vpc: ${defaults.project_vpc}
    subnets: ${defaults.public_subnets}
}

aws::autoscale_group web_asg {
    elb: ${web_elb}
    subnets: ${web_elb.subnets}
}
```

## Identifiers

Examples:

	foobar
	beam_is_great
	
Identifiers are consist of alphabets, decimal digits, and the underscore character, and begin with a
alphabets(including underscore). There are no restrictions on the lengths of identifiers.

# Literals

## String Literals

`"double quoted strings"` - eventually these will have variable (`$var`) interpolation

`'single quoted strings` - never interpolates

## Array Expression

`[1, 2, 3]`

`["1", "2", "3"]`

## Hash Expression

```
{
    stringKey: "value"
    literalKey: value2
    numericKey: 123
}
```

## Comments

A comment starts with a hash character (#) that is not part of a string literal, and ends at the end
of the physical line. Comments are ignored by the syntax; they are not tokens.

# Resources

Resources are the primary type in the Beam configuration language. Resources define cloud infrastructure
components and their attributes.

Resources always follow the format `<provider>::<resource> { <resource_block> }` where `resource_block` can be
either attributes to configure the resource with or nested resources.

An example resource:

``` 
aws::instance {
    image:         backend hvm [1]
    instance_type: t2.medium
    
    tags: {
        Name: "MySQL Database Backend"
    }
}
```

Resources can be nested as necessary to indicate a dependency. It is up to the resource provider to implement
nested resources where it makes sense.

An example nested resource:

```
aws::vpc {
    name: "Production VPC"
    cidr: "10.0.0.0/16"
    
    aws::subnet {
        name: "private"
        cidr: "10.0.0.0/24"
    }
}
```

In the example above the `aws::subnet` resource does not need to explicity set the `vpc_id` on the
subnet resource since it is implied based on being nested inside of the `aws::vpc` resource.

## Statement: include

The `include` method is used to include and evaluate a Beam configuration file.

Each file is scoped. There are two concepts of scoping to consider:

-  The first is when the `include` keyword is used to include another file. That file 
   may been passed some context from the parent file.
  
-  Second when using the `module` keyword a module may make available information about
   the resources it created to the parent file.

## Statement: package

The `package` method is used to define a package. A package is a group of Beam configuration files
that group functionality. Additionally packages can add custom cloud resources written in a
supported extension language (Groovy or Java).

```
package {
    name: "aws"
    version: 1.0.1
    description: "Implements basic AWS cloud resources."
}
```

#### Arguments

- **name**  - The name of the package. (Required)
- **version** - The version of the package. (Required)
- **description** - A short description of the package. (Optional)

Packages may include other packages using the `import` method. All package depdendencies are resolved at
runtime rather than compile time.

#### Package Layout

**Provider Example**

``` 
module/pom.xml
module/src/main/groovy/beam/aws/VPC.groovy
module/src/main/groovy/beam/aws/ElasticLoadBalancer.groovy
module/src/main/groovy/beam/aws/Instance.groovy
```

**Composition Example**

``` 
module/init.beam
module/vpc.beam
module/production/gateway.beam
module/production/frontend.beam
module/production/backend.beam
module/development/backend.beam
```

Commands can act on individual directories if needed. Commands like `beam ssh` would read
the file and search for instances defined in that file. If a directory is specified instead
of a file `beam ssh` would read all files in the directory. If nothing is specified `beam ssh`
would look for any files in the current directory.

Example:

`beam ssh production/frontend`

## Statement: import

The `import` method is used to import a package. Packages may be imported from the local filesystem,
a Maven repository or a URL.

``` 
import {
    package: "aws"
    
    # Direct Jar import
    url: https://s3.amazonaws.com/mybucket/path/to/package.jar
    
    # Git import with support for Git and Github syntax
    git:    https://github.com/perfectsense/beam-package.git
    github: perfectsense/beam-package
    tag: release/1.0
    
    # Local filesystem import
    path: /path/to/beam-package
    
    # Maven import
    maven: com.psdops:beam:beam-package:1.0
}
```

The local filesystem and git based imports assume the package is in source form. When using this format
for packages only Beam configuration language files and Groovy extensions are supported. Beam will not
compile Java code for inclusion in the module.

The Maven and JAR URL imports expect the package to be in `jar` format.

