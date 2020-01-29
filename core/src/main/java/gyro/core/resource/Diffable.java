/*
 * Copyright 2019, Perfect Sense, Inc.
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import gyro.core.GyroInputStream;
import gyro.core.GyroUI;
import gyro.core.diff.Change;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.FileScope;
import gyro.core.validation.ValidationError;

public abstract class Diffable {

    boolean external;
    Diffable parent;
    String name;
    DiffableScope scope;
    Change change;
    Set<String> configuredFields;
    final List<Modification<? extends Diffable>> modifications = new ArrayList<>();

    public abstract String primaryKey();

    public Diffable parent() {
        return parent;
    }

    public Resource parentResource() {
        for (Diffable d = parent(); d != null; d = d.parent()) {
            if (d instanceof Resource) {
                return (Resource) d;
            }
        }

        return null;
    }

    public <T extends Resource> Stream<T> findByClass(Class<T> resourceClass) {
        return scope.getRootScope().findResourcesByClass(resourceClass);
    }

    public <T extends Resource> T findById(Class<T> resourceClass, Object id) {
        return scope.getRootScope().findResourceById(resourceClass, id);
    }

    public GyroInputStream openInput(String file) {
        FileScope fileScope = scope.getFileScope();
        Path parent = Paths.get(fileScope.getFile()).getParent();

        return fileScope.getRootScope().openInput(parent != null
            ? parent.resolve(file).toString()
            : file);
    }

    protected <T extends Diffable> T newSubresource(Class<T> diffableClass) {
        return DiffableType.getInstance(diffableClass).newInternal(new DiffableScope(scope, null), null);
    }

    protected void requires(String fieldName) {
        if (!scope.isEmpty()) {
            DiffableType.getInstance(this).getField(fieldName).setValue(this, scope.get(fieldName));
        }
    }

    public List<ValidationError> validate(Set<String> configuredFields) {
        return validate();
    }

    public boolean writePlan(GyroUI ui, Change change) {
        return false;
    }

    public boolean writeExecution(GyroUI ui, Change change) {
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(parent(), name, primaryKey());
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Diffable otherDiffable = (Diffable) other;

        if (external) {
            DiffableField idField = DiffableType.getInstance(getClass()).getIdField();
            return Objects.equals(idField.getValue(this), idField.getValue(otherDiffable));

        } else {
            return Objects.equals(parent(), otherDiffable.parent())
                && Objects.equals(name, otherDiffable.name)
                && Objects.equals(primaryKey(), otherDiffable.primaryKey());
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        DiffableType<Diffable> type = DiffableType.getInstance(this);
        String typeName = type.getName();

        if (typeName != null) {
            builder.append(typeName);
            builder.append(' ');

            if (external) {
                builder.append("id=");
                builder.append(type.getIdField().getValue(this));

            } else {
                builder.append(name);
            }

        } else {
            builder.append(name);
            builder.append(' ');
            builder.append(primaryKey());
        }

        return builder.toString();
    }

    /**
     * @deprecated Use {@link #validate(Set)} instead.
     */
    @Deprecated
    public List<ValidationError> validate() {
        return null;
    }
}
