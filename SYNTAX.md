# Beam Cloud Language Reference

# Extension Points

## Event Stream Plugin

An event stream plugin allows extension writers to be notified of events during Beam
processing.

Events Types:

- RESOURCE_BEFORE_CREATE
- RESOURCE_AFTER_CREATE
- RESOURCE_BEFORE_DELETE
- RESOURCE_AFTER_DELETE
- RESOURCE_BEFORE_UPDATE
- RESOURCE_AFTER_UPDATE

This mechanism is intended for extension writers that want to enforce a policy on resources. An
example use case would be requiring SSL on ELBs.

```java
public interface BeamEventStreamPlugin {
    
    // Return true to continue process, false to end processing.
    public boolean processResourceEvent(BeamResource resource, BeamEventType type);
    
}
```

## Virtual Resources

A virtual resource is similar to a regular resource provided by a provider impelementation. The difference
is that it's backed by other resources.

This mechanism is intended for extension writers to build virtual resources that are a composite of
other resources and/or have a specific workflow that requires user interaction.

An example use case is a deployment workflow that creates a new autoscale group and ELB for verification of
instances prior to making them live.

## AST Plugin

An AST plugin allows extension writers to modify the AST after it is parsed. This method would also
allow for policy enforcement as well as other more advanced use cases.

An example use case might be to ensure appropriate tags are on all resources. This could be accomplished
with the Event Stream Plugin model as well.

## Translator

A translator (written in Groovy or Java) serves as a bridge between beam configuration and resources.

This mechanism is intended for extension writers to build customized beam configurations which are not limited
by how resources are defined.

Example use cases are layers, build-in subnet routes etc.

Translator of a regular aws vpc simply passes config data over to resource:
```
aws::vpc {
    name: "AWS VPC"
    region: "us-east-1"
    cidr: "10.0.0.0/16"
}
```

```
def aws_vpc(data) {
    VpcResource vpc = new VpcResource()
    vpc.name = data.name
    vpc.region = data.region
    vpc.cidr = data.cidr
}
```

Extension writers can define a customized vpc with cidr mapping and internet gateway composition:
```
def foo_vpc(data) {
    def cidr_mappings = ["us-east-1": "10.0.0.0/16", "us-west-1": "10.1.0.0/16"]

    VpcResource vpc = new VpcResource()
    vpc.name = data.name
    vpc.region = data.region
    vpc.cidr = cidr_mappings.get(data.region)

    InternetGatewayResource igw = new InternetGatewayResource()
    vpc.internetGateway = igw
}
```

The above translator will allow the customized vpc in beam configs:

```
foo::vpc {
    name: "FOO VPC"
    region: "us-east-1"
}
```

# Syntax

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

Resources always following the format `<provider>::<resource> { <resource_block> }` where `resource_block` can be
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

# Filters

Filters are used to segregate resources in the Beam configuration language. For example, an environment filter
can segregate resources by different environments and help user to make changes only to one environment while
leaving other environments untouched.

Filters always following the format `beam::<filter> { <filter_block> }` where `filter_block` defines filter keys
and values.

An example beam filter:
```
beam::environment {
    keys: [--environment, -e]
    values: {
        prod: [frontend.beam, backend.beam]
        dev: [development.beam]
    }
}
```
The above filter segregate prod and dev environment and can facilitate environment option for `beam up` such as
`beam up -e dev`

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

**Composition Exampe**

``` 
module/init.beam
module/vpc.beam
module/production/gateway.beam
module/production/frontend.beam
module/production/backend.beam
module/development/backend.beam
```

beam ssh production/frontend

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

