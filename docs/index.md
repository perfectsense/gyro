# Introduction

## What is Beam?

Beam is a commandline tool for automating the creation and management of the cloud resources that make up your infrastructure. Beam can manage cloud resources from Amazon Web Services and Rackspace (OpenStack) and can be extended to support other cloud providers.

Configuration files describe the cloud resources that are required to run your infrastructure. Beam has three main configuration constructs: cloud, environments and layers. 

Cloud configuration is the starting point for any Beam managed infrastructure. The cloud configuration defines basic project information, security rules, global resources and region configuration. 

Environment configuration is a way to group cloud instances based on your organizations development workflow. For instance you can create a dev, qa, staging and prod environment. Most Beam commands require an environment to be specified to ensure you are always executing those commands against a single environment.

Layer configuration defines the layers for your infrastructure within an environment. Layers define your cloud instances and where they should be launched.

Beam compares your local configuration with what is actually running in your cloud to determine what actions to take to converge your local configuration. Beam will always show you exactly what it's going to to before it does it.

Beam also integrates service discovery features allowing cloud services to register themselves and locate other services using DNS.

### Next

Follow the [getting started guide](getting-started/index.md) to see how you can use Beam to create and manage your infrastructure.