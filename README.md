<img src="etc/gyro.png" height="200"/>

[![Gitter](https://img.shields.io/gitter/room/perfectsense/gyro)](https://gitter.im/perfectsense/gyro)
[![TravisCI](https://api.travis-ci.org/perfectsense/gyro.svg?branch=master)](https://travis-ci.org/perfectsense/gyro)
[![Apache License 2.0](https://img.shields.io/github/license/perfectsense/gyro)](https://github.com/perfectsense/gyro/blob/master/LICENSE)

Gyro is command-line tool for creating, updating, and maintaining cloud infrastructure. Gyro makes
infrastructure-as-code possible.

Gyro is open source under the Apache 2.0 license.

Using Gyro allows you to describe your infrastructure using the Gyro configuration language and then
create, update, and maintain that infrastructure using the gyro command-line tool.

### Name

Why the name Gyro? It's short for Gyroscope which is an essential device that allows airplanes to
navigate in the clouds. Also, if you read "gyro" and thought of a greek sandwich, you're not the
first, definitely won't be the last, haha. That's ok though, gyro sandwiches are yummy. :)

### Background

Gyro was built by [Brightspot](https://www.brightspot.com) to automate the creation and
management of the cloud infrastructure we use to run Brightspot CMS for
our clients. We integrated several tools that are part of our DevOps lifecycle such as Chef to
install and configure software on our hosts, ssh to log into hosts, and service discovery to drain
traffic during maintenance. We use workflows to deploy our code using with the blue/green model. We
found this "one tool for your day-to-day operations activities" to be extremely valuable.  After six
years of using this tool internally, we decided to refactor the code, make it more flexible, and
open source it so others can benefit just as we have.

## Gyro Language

The Gyro language is designed specifically for defining cloud infrastructure. It was built with
readability and organizational flexibility in mind. The language provides the ability to concisely
define cloud infrastructure resources along with language constructs such a `@for` loops, `@if`
conditionals, and `@virtual` definitions for packaging resources into reusable components.

### What Makes It Different?

There are a few things that make Gyro different from similar tools. We'll try to highlight those here
but encourage you to read the [developer documentation](https://gyro.dev).

#### Gyro Configuration Language

We know, Yet Another DSL. Originally we wrote this using YAML but we wanted clean
(and limited) logic in our configuration and YAML didn't really fit the bill. We tried a few
different language based internal DSLs such as Kotlin, Groovy, and even TCL (don't hate) but the
language always bled through and didn't feel right.

We decided to design our own simplified, but powerful, language that allowed us to have
greater control over scoping rules, control structures, and runtime execution. Building a tool that
generates an internal graph of resources is extremely complex and not having complete control over
what is happening during execution makes it much more complex.

More information on the configuration syntax can be found in the [Language Guide](https://gyro.dev/guides/language/). There are also lots of working examples in each [provider](https://github.com/perfectsense/gyro-aws-provider/tree/master/examples).

<img src="etc/terminal-create.png" height="400"/>

When you run Gyro it'll tell you exactly what it's going to do.

<img src="etc/gyro-create.svg" height="400"/>

Enable verbose mode to get a more detailed view. In this example we've made a small modification
to the original configuration to add a new security group:

<img src="etc/gyro-update.svg" height="400"/>

#### Control Structures

We're aware of the debate about whether allowing logic (control structures) in a configuration is a
good thing or not. We believe it is, as long as you provide reasonable limits. With Gyro we tried to
strike a balance between no logic and too much logic (aka full programming language). To start with
we've implemented two control structures we think are most important for configuration logic, "if"
and "for".

Control structures are actually an extension of Gyro rather than baked into the language parser.

More information on control structures can be found in the [control structures](https://gyro.dev/guides/language/control-structures.html) documentation. For a real world example see our [EC2 subnet](https://github.com/perfectsense/gyro-aws-provider/blob/master/examples/ec2/subnet.gyro#L26-L37) example.

<img src="etc/terminal-logic.png" height="400"/>

#### Workflows

We think this is huge. Workflows provide the ability to define transition stages for complex cloud
infrastructure updates. Blue/green deployments are a good example of this. With Gyro you can define
a stage to create a new load balancer and new virtual machines with your updated code. After this
stage executes you can either prompt the user to continue, allowing them to validate the new
deployment, or you can automate it. Then you can define a stage to either drop those new machines
into the load balancer taking traffic or flip DNS depending on how you like to do blue/green. If at
any point things don't look right Gyro can roll back to a previous stage.

This functionality has been extremely important for us to be able to allow anyone to do deployments
and still be able to quickly roll back should anything go wrong.

More information on workflows can be found in the [workflow guide](https://gyro.dev/guides/workflows/).

<img src="etc/terminal-workflow.png" height="400"/>

#### Extensibility

We've included a number of ways you can extend Gyro with plugins.

- Add [new commands](https://gyro.dev/extending/commands)
- Add [new language functionality](https://gyro.dev/extending/directive/), aka Directives 
- Add [custom variable resolvers](https://gyro.dev/extending/resolver/)
- Add custom event hooks (undocumented) to trigger custom logic when various things happen such as a
resource is created or updated

The power of extensions allow you to integrate Gyro with your other DevOps tools and extend Gyro with
new features we haven't thought of.

To get you started we've put together a [plugin template project](https://github.com/perfectsense/gyro-sample-plugin). You can also check out the [ssh plugin](https://github.com/perfectsense/gyro-ssh-plugin).

## Getting Started

[Install](https://gyro.dev/guides/getting-started/installing.html#installing-gyro) Gyro.

See [Getting Started](https://gyro.dev/guides/getting-started/index.html) if you're new to Gyro. This is a quick tutorial that will teach you the basics of Gyro.

After the Getting Started tutorial there are plenty of examples for each provider:

- [AWS](https://github.com/perfectsense/gyro-aws-provider/tree/master/examples)
- [Azure](https://github.com/perfectsense/gyro-azure-provider/tree/master/examples)
- [Pingdom](https://github.com/perfectsense/gyro-pingdom-provider/tree/master/examples)

Join the [community](https://gyro.dev/guides/contribute/#chat) and [contribute](https://gyro.dev/guides/contribute/#contribute) to Gyro!

## Developing

Gyro is written in Java using Gradle as the build tool. 

We recommend installing [AdoptOpenJDK](https://adoptopenjdk.net/) 11 or higher if you're going to contribute to Gyro or one of its cloud
provider implementations.

The Gyro project is broken into several subprojects:

- **cli** - The Gyro CLI executable JAR. After building Gyro you'll find the executable binary in ``cli/dist/`` as well as the packaged Java runtime distribution.

- **core** - The core Gyro runtime. The bulk of Gyro lives in the package. Specifically, this package contains the diff engine, workflow implementation, virtual resource implementation, @if and @for implementation, and more.

- **lang** - The Gyro language AST.

- **parser** - The Gyro language parser. We use ANTLR4.

- **util** - Various util classes. 

### Building Gyro

Gyro uses the Gradle build tool. Once you have a JDK installed building is easy, just run `./gradlew` at the root of the Gyro project. This wrapper script will automatically download and install Gradle for you, then build Gyro.

```shell
$ ./gradlew
Downloading https://services.gradle.org/distributions/gradle-5.2.1-all.zip
..............................................................................................................................

Welcome to Gradle 5.2.1!

Here are the highlights of this release:
 - Define sets of dependencies that work together with Java Platform plugin
 - New C++ plugins with dependency management built-in
 - New C++ project types for gradle init
 - Service injection into plugins and project extensions

For more details see https://docs.gradle.org/5.2.1/release-notes.html

Starting a Gradle Daemon, 1 stopped Daemon could not be reused, use --status for details

.
.
.

BUILD SUCCESSFUL in 17s
38 actionable tasks: 28 executed, 10 from cache
$
```

## License

Gyro is open source under the [Apache License 2.0](https://github.com/perfectsense/gyro/blob/master/LICENSE).
