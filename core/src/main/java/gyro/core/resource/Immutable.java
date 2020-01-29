/*
 * Copyright 2020, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.core.resource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate the changes in the value will be ignored by the diff system once it's set.
 * Note that this takes precedence over {@link Updatable} annotation,
 * so {@link Updatable} will be ignored if they are used together.
 *
 * <p>
 * e.g.:
 * <p>
 * <pre><code>
 * class Foo extends Diffable {
 *     &#064;Immutable
 *     public String getFooValue() {
 *         ...
 *     }
 * }
 * </code></pre>
 *
 * `foo.gyro`:
 * <pre>
 * provider::foo foo-example
 *     foo-value: "foo"
 * end
 * </pre>
 *
 * Run `gyro up foo.gyro`. Supposed that the `foo-value` field were updated to 'FOO MODIFIED' by a provider,
 * so `state/foo.gyro` would look like:
 * <pre>
 * provider::foo 'foo-example'
 *     'foo-value': 'FOO MODIFIED'
 * end
 * </pre>
 *
 * The diff system doesn't see it as a change, so subsequent `gyro up foo.gyro` results in:
 * <pre>
 * ...
 * Looking for changes...
 *
 *
 * No changes.
 * </pre>
 * </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Immutable {

}
