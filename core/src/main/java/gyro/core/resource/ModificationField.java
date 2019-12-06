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

import gyro.util.Bug;

public class ModificationField extends DiffableField {

    private DiffableField originalDiffableField;

    public ModificationField(DiffableField originalDiffableField) {
        super(originalDiffableField);

        this.originalDiffableField = originalDiffableField;
    }

    @Override
    public Object getValue(Diffable diffable) {
        for (Modification<? extends Diffable> modification : DiffableInternals.getModifications(diffable)) {
            DiffableType<Modification<? extends Diffable>> modificationType = DiffableType.getInstance(modification);

            for (DiffableField field : modificationType.getFields()) {
                if (originalDiffableField == field) {
                    return super.getValue(modification);
                }
            }
        }

        DiffableType<Diffable> type = DiffableType.getInstance(diffable);
        String name = DiffableInternals.getName(diffable);

        throw new Bug(String.format(
            "Unable to match modification field '%s' with modification instance on resource %s %s",
            getName(),
            type,
            name));
    }

    @Override
    public void setValue(Diffable diffable, Object value) {
        for (Modification<? extends Diffable> modification : DiffableInternals.getModifications(diffable)) {
            DiffableType<Modification<? extends Diffable>> modificationType = DiffableType.getInstance(modification);

            for (DiffableField field : modificationType.getFields()) {
                if (originalDiffableField == field) {
                    super.setValue(modification, value);
                    return;
                }
            }
        }

        DiffableType<Diffable> type = DiffableType.getInstance(diffable);
        String name = DiffableInternals.getName(diffable);

        throw new Bug(String.format(
            "Unable to match modification field '%s' with modification instance on resource %s %s",
            getName(),
            type,
            name));
    }

}
