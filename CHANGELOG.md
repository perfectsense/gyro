CHANGELOG
=========

## 1.0.0 (January 7th, 2021)

ENHANCEMENTS:

* core: Validate enum type fields. ([329](https://github.com/perfectsense/gyro/issues/329))
* core: Implement gyro exit codes. ([325](https://github.com/perfectsense/gyro/issues/325))

ISSUES FIXED:

* core: Fix error on finders if resource has missing @Id field. ([334](https://github.com/perfectsense/gyro/issues/334))

## 0.99.5 (August 25th, 2020)

ENHANCEMENTS:

* core: Convert to `Picocli` for command line argument parsing. ([308](https://github.com/perfectsense/gyro/issues/308))

PERFORMANCE:

* core: Cache plugin depenedncy. ([311](https://github.com/perfectsense/gyro/issues/311))
* core: Optimize plugin class loading. ([312](https://github.com/perfectsense/gyro/issues/312))
* core: Cache `external-query`. ([313](https://github.com/perfectsense/gyro/issues/313))
* core: Implement Beaninfo on diffable. ([314](https://github.com/perfectsense/gyro/issues/314))
* core: Add `exists(String file)` method to FileBackend. ([315](https://github.com/perfectsense/gyro/issues/315))
* core: Add `copy(String source, String destination)` method to FileBackend. ([316](https://github.com/perfectsense/gyro/issues/316))

## 0.99.4 (June 25th, 2020)

ENHANCEMENTS:

* core: Add ability to suppress version check on `gyro up`. ([294](https://github.com/perfectsense/gyro/issues/294))
* core: Implement a Diff command. ([302](https://github.com/perfectsense/gyro/issues/302))

ISSUES FIXED:

* workflow: Fix incorrect execution of workflow step. ([297](https://github.com/perfectsense/gyro/issues/297))

## 0.99.3 (May 14th, 2020)

ENHANCEMENTS:

* core: Add `@state-backend` allowing users to store and use state files remotely. ([236](https://github.com/perfectsense/gyro/issues/236))
* core: Introduces an abstract class `LockBackend`. The provider specific implementation allows locking of state files to avoid simultaneous modifications. ([269](https://github.com/perfectsense/gyro/issues/269))

ISSUES FIXED:

* core: Fix reference to non configured fields. ([263](https://github.com/perfectsense/gyro/issues/263))
* core: Maintain resource creation order based on dependencies across multiple files. ([256](https://github.com/perfectsense/gyro/issues/256))
* core: Make `Find*` methods work when used within a virtual resource. ([275](https://github.com/perfectsense/gyro/issues/275))
* workflow: Resolve references removed out of workflow scope. ([274](https://github.com/perfectsense/gyro/issues/274))
* core: Attach parent resource to a subresource on creation. ([282](https://github.com/perfectsense/gyro/issues/282))
* core: Fix validation annotations to work on Sets. ([285](https://github.com/perfectsense/gyro/issues/285))
* workflow: Fix updating non-configured fields when using `@workflow::update`. ([287](https://github.com/perfectsense/gyro/issues/287))
* core: Fix `_configured-fields` of complex types to only have user configured fields. ([288](https://github.com/perfectsense/gyro/issues/288))

## 0.99.2 (April 23rd, 2020)

ENHANCEMENTS:

* core: Add `version` command to report current version and check for newer version. ([230](https://github.com/perfectsense/gyro/issues/230))

ISSUES FIXED:

* core: Fix finder to use `@Filter` annotation for fields having different bean name. ([237](https://github.com/perfectsense/gyro/issues/237))
* core: Resource creation fails when dependencies are spread across multiple files. ([243](https://github.com/perfectsense/gyro/issues/243))
* workflow: `@Wait` directive does not properly evaluate the "n" option given. ([246](https://github.com/perfectsense/gyro/issues/246))
* core: Allow `@for` to iterate over sets. ([248](https://github.com/perfectsense/gyro/issues/248))
* core: Deleting a resource created using `@uses-credentials` fails. ([250](https://github.com/perfectsense/gyro/issues/250))
* core: Using `@external-query` under a resource block fails. ([251](https://github.com/perfectsense/gyro/issues/251))
* core: Fix `credentials` option for `@external-query`. ([252](https://github.com/perfectsense/gyro/issues/252))
* workflow: Recover from a failed workflow stage. ([254](https://github.com/perfectsense/gyro/issues/254))
* workflow: `@wait` directive not resolving variables when used inside a workflow. ([259](https://github.com/perfectsense/gyro/issues/259))

MISC:

* Add preprocessor to modify nodes before evaluation. ([241](https://github.com/perfectsense/gyro/issues/241))

## 0.99.1 (January 30th, 2020)

ENHANCEMENTS:

* core: Add `@Immutable` annotation to ignore immutable fields in the diff engine. ([227](https://github.com/perfectsense/gyro/issues/227))
* core: Add `@DependsOn` validation annotation. ([217](https://github.com/perfectsense/gyro/issues/217))
* core: Add `@CollectionMin` and `@CollectionMax` validation annotations. ([215](https://github.com/perfectsense/gyro/issues/215))
* core: Add `Diffable.requires(..)` method define field dependencies within a resource. ([193](https://github.com/perfectsense/gyro/issues/193))
* core: Add `@ConflictsWith` validation annotation. ([186](https://github.com/perfectsense/gyro/issues/186))
* lang: Add `$SELF` implicit variable to allow a resource defintion to reference itself. ([171](https://github.com/perfectsense/gyro/issues/171)) 

ISSUES FIXED:

* core: Non-configured fields shouldn't be validated. ([221](https://github.com/perfectsense/gyro/issues/221)) 
* core: Support inheritance of annotations. ([219](https://github.com/perfectsense/gyro/issues/219))
* core: Make `Diffable.primaryKey()` abstract to ensure its implemention for complex types. ([213](https://github.com/perfectsense/gyro/issues/213))
* core: Unable to update subresource fields due to `_configured-fields` information being lost. ([212](https://github.com/perfectsense/gyro/issues/212))
* workflow: Wait directive not being called when used in `@workflow::update` directive. ([206](https://github.com/perfectsense/gyro/issues/206))
* core: Copy values from state after re-evaluate to ensure output fields are available. ([203](https://github.com/perfectsense/gyro/issues/203)) 
* core: Throw an error if `@Id` is not defined on a resource and `RootScope.findResourceById(..)` is called. ([200](https://github.com/perfectsense/gyro/issues/200))
* gradle: Set Java compiler encoding to UTF-8 to solve console output issues for unicode chars. ([199](https://github.com/perfectsense/gyro/issues/199))
* core: Using `$(external-query ..)` resolver results to set a field breaks subsequent `gyro up`. ([153](https://github.com/perfectsense/gyro/issues/153))
* state: Prevent state from being erased if there is an exception while saving state. ([207](https://github.com/perfectsense/gyro/issues/207))
* core: Allow field validation to succeed when referencing an output field of another resource that hasn't yet been created. ([170](https://github.com/perfectsense/gyro/issues/170)) 
* workflow/state: Failure in a workflow would cause duplicate entires in state. ([165](https://github.com/perfectsense/gyro/issues/165))
* state: Single quotes were improperly escaped in state. ([164](https://github.com/perfectsense/gyro/issues/164))
* core: Convert enums to strings before comparing in expressions. ([191](https://github.com/perfectsense/gyro/issues/191))
* core: GyroInstance interface methods may conflict with cloud resources implementations. ([176](https://github.com/perfectsense/gyro/issues/176))
* core: Include "gyro-release" maven repo in default repositories used to search for plugins. ([172](https://github.com/perfectsense/gyro/issues/172))

MISC:

* Update checkstyle config to work with checkstyles 8.4. ([179](https://github.com/perfectsense/gyro/issues/179))
* Add codestyle configuration for IntelliJ and update code style to conform. ([187](https://github.com/perfectsense/gyro/issues/187))

## 0.99.0 (July 7th, 2019)

Initial public release of Gyro!
