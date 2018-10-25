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
brightspot::frontend web 
    brightspot_build_location: s3://mybucket/builds
    brightspot_build_number: 495
end
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

The Beam Configuration Language has two types of variables: constants and resources. Constant variables that can contain 
strings, numbers, booleans, lists, and maps. Resources are cloud resources defined by providers plugins.

**Constant variable**

```
PROJECT: web
```

**Implicit variables defined by resources**

The following example sets the `frontend` resource variable.

```
aws::elb frontend
    name: frontend
end
```

Resources can be tagged by adding additional names after the resource name:

```
aws::elb frontend external
    name: frontend
end
```

**Referencing a constant**

Constant variables are referenced using `$(<name>)` syntax.

```
aws::elb frontend {
    name: $(PROJECT) frontend
}
```

**Referencing a resource**

Resources are referenced using `@(<type> <name>)` syntax or optionally an attribute of the resource
can be queried using `@(<type> <name> | <attribute>)`. The `@()` syntax always returns a single resource or
throws an error if the referenced resource is not found.

```
aws::instance webserver {
    subnet-id: @(aws::vpc vpc | vpc-id)
}
```

Tagged resources can be referenced using `#(<type> <name>)` syntax or optionally attributes of the tagged
resources can be queried using `#(<type> <name> | <attribute>)`. The `#()` syntax always returns a list. If no
resources are found an empty list is returned.

```
aws::subnet us-east-1a public 
    cidr: 10.0.0.0/24
end

aws::subnet us-east-1b public 
    cidr: 10.0.1.0/24
end

aws::elb frontend {
    subnet-ids: #(aws::subnet public | subnet-id)
}
```

## Module

A module is single file that defines one or more cloud resources. Files that define modules should with `.bc`.

## Package

A package is a group of modules. Packages can be included directly in a Beam project or they can
be defined outside the project and referenced in using the `package` keyword.

## Scoping 

Resources can be imported and referenced from modules or packages using the `import` keyword. By default resources
imported from a module are namespaced using the name of the module.

Example:

**project.bc**

```
import vpc

aws::instance webserver
    subnet-id: @(aws::vpc vpc.vpc | vpc-id)
end
```

The namespace a module is imported as can be changed using the `as` keyword:

```
import vpc as network

aws::instance webserver
    subnet-id: @(aws::vpc network.vpc | vpc-id)
end
```

## Comments

A comment starts with double forward slashes (`//`) and ends at the end of the physical line.

# Resources

Resources are the primary type in the Beam Configuration Language. Resources define cloud infrastructure
components and their attributes.

Resources always follow the format `<provider>::<resource> <newline> <resource_block> <end>`.

An example resource:

``` 
aws::instance webserver
    image:         backend hvm [1]
    instance_type: t2.medium
    
    tags: 
        * Name: "MySQL Database Backend"
end
```

## Statement: package

The `package` keyword is used to import a package. A package is a group of modules.

```
package 
    github: perfectsense/beam-package
    version: 1.0.1
end
```

Arguments are the same as a provider.

## Statement: provider

The `provider` keyword is used to import a provider. 

``` 
provider
    # Direct Jar import
    url: https://s3.amazonaws.com/mybucket/path/to/package.jar
    
    # Git import with support for Git and Github syntax
    git:    https://github.com/perfectsense/beam-provider-aws.git
    github: perfectsense/beam-provider-aws
    tag: release/1.0
    
    # Local filesystem import
    path: /path/to/beam-provider-aws
    
    # Maven import
    maven: com.psdops:beam:beam-provider-aws:1.0
end
```

The local filesystem and git based imports assume the provider is in source form.
