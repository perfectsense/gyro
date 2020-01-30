CHANGELOG
=========

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