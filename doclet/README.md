Beam Provider Reference Documentation Doclet
============================================

This doclet extracts reference documentation for provider implements from
JavaDocs.

Please follow these rules:

- Class level javadocs should have a single sentence explaining what the
  resource creates.  Include a simple example that includes all required attributes
  and possibly optional attributes if they're extremely common.  Optionally 
  include a link to AWS documention relating the resource. 

- Use the following format for examples:

```
.. code-block:: gyro

    aws::resource name
        attribute1: value1
        attribute2: value2
    end
```

- Field level javadocs should be documented on the getter for the particular
  attribute, not on the field. This allows use to read the annotations and use them
  in the documentation. For now field level documentation should be limited to a single 
  concise sentence. Do not just copy the field level documentation from the provider
  SDK. Optionally include a link to AWS documention relating the resource.

- Fields should be ordered as such: required fields first, then optional fields, finally
  read-only fields.
