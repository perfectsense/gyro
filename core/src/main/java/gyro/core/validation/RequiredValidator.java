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

package gyro.core.validation;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableType;

import java.util.List;
import java.util.stream.Collectors;

public class RequiredValidator implements Validator<Required> {

    private String message = null;

    @Override
    public boolean isValid(Diffable diffable, Required annotation, Object fieldValue) {
        message = null;
        DiffableType<Diffable> diffableType = DiffableType.getInstance(diffable);
        DiffableField whenField = diffableType.getField(annotation.when());
        if (whenField != null) {
            Object whenFieldValue = whenField.getValue(diffable);

            if (ObjectUtils.isBlank(annotation.equals())) {
                // empty 'equals' in annotation

                if (!ObjectUtils.isBlank(whenFieldValue)) {
                    // 'when' field is set
                    // condition matched on any value of 'when' field
                    return isValidWithConstraint(diffable, annotation, diffableType, fieldValue);

                    // 'when' field is not set
                    // condition did not match
                    // invalid if field is set
                } else if (!ObjectUtils.isBlank(fieldValue)) {
                    message = String.format("Cannot be set when '%s' is not set.", annotation.when());
                    return false;
                }
            } else {
                // non empty 'equals' in annotation

                if (!ObjectUtils.isBlank(whenFieldValue) && annotation.equals().equals(whenFieldValue)) {
                    // condition matched
                    // 'when' field value matches value in 'equals'
                    return isValidWithConstraint(diffable, annotation, diffableType, fieldValue);

                    // condition did not match
                    // invalid if field is set
                } else if (!ObjectUtils.isBlank(fieldValue)) {
                    message = String.format("Cannot be set when '%s' is not set to '%s'.", annotation.when(), annotation.equals());
                    return false;
                }
            }

            return true;
        } else {
            return !ObjectUtils.isBlank(fieldValue);
        }
    }

    @Override
    public String getMessage(Required annotation) {
        return message == null ? "Required" : message;
    }

    private boolean isValidWithConstraint(Diffable diffable, Required annotation, DiffableType<Diffable> diffableType, Object fieldValue) {
        List<DiffableField> diffableFields = diffableType.getFields().stream()
            .filter(field -> {
                Required required = field.getAnnotation(Required.class);
                return required != null
                    && required.when().equals(annotation.when())
                    && required.equals().equals(annotation.equals())
                    && required.constraint() == annotation.constraint();
            }).collect(Collectors.toList());

        long count = diffableFields.stream().filter(field -> !ObjectUtils.isBlank(field.getValue(diffable))).count();

        if (annotation.constraint() == Required.RequiredConstraint.ONLY_ONE) {
            if (count > 1 && !ObjectUtils.isBlank(fieldValue)) {
                // if more than one field set among the group including the field being validated

                message = String.format("Only one of ['%s'] can be set.",
                    diffableFields.stream().map(DiffableField::getName).collect(Collectors.joining("', '")));
                return false;
            } else if (count == 0) {
                message = String.format("One of ['%s'] is required to be set when '%s'%s.",
                    diffableFields.stream().map(DiffableField::getName).collect(Collectors.joining("', '")),
                    annotation.when(),
                    ObjectUtils.isBlank(annotation.equals()) ? "" : String.format(" is set to '%s'", annotation.equals()));
                return false;
            }
        } else if (annotation.constraint() == Required.RequiredConstraint.AT_LEAST_ONE) {
            if (count == 0) {
                message = String.format("At least one of ['%s'] is required to be set when '%s'%s.",
                    diffableFields.stream().map(DiffableField::getName).collect(Collectors.joining("', '")),
                    annotation.when(),
                    ObjectUtils.isBlank(annotation.equals()) ? "" : String.format(" is set to '%s'", annotation.equals()));
                return false;
            }
        } else if (annotation.constraint() == Required.RequiredConstraint.NOT_ALLOWED) {
            if (!ObjectUtils.isBlank(fieldValue)) {
                message = String.format("Cannot be set when '%s'%s.",
                    annotation.when(),
                    ObjectUtils.isBlank(annotation.equals()) ? "" : String.format(" is set to '%s'", annotation.equals()));
                return false;
            }
        } else if (annotation.constraint() == Required.RequiredConstraint.REQUIRED) {
            if (ObjectUtils.isBlank(fieldValue)) {
                message = String.format("Required when '%s'%s.",
                    annotation.when(),
                    ObjectUtils.isBlank(annotation.equals()) ? "" : String.format(" is set to '%s'", annotation.equals()));
                return false;
            }
        }

        return true;
    }
}
