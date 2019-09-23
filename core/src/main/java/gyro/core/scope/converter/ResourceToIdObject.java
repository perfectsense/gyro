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

package gyro.core.scope.converter;

import java.lang.reflect.Type;

import com.psddev.dari.util.ConversionFunction;
import com.psddev.dari.util.Converter;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.util.Bug;

public class ResourceToIdObject implements ConversionFunction<Resource, Object> {

    @Override
    public Object convert(Converter converter, Type returnType, Resource resource) {
        DiffableType<Resource> type = DiffableType.getInstance(resource);
        DiffableField idField = type.getIdField();

        if (idField == null) {
            throw new Bug(String.format(
                "@|bold %s|@ type doesn't have an ID field!",
                type.getName()));
        }

        return converter.convert(
            returnType,
            DiffableType.getInstance(resource.getClass())
                .getIdField()
                .getValue(resource));
    }

}
