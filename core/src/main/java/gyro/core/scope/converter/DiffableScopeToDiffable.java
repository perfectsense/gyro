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
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableType;
import gyro.core.scope.DiffableScope;

public class DiffableScopeToDiffable implements ConversionFunction<DiffableScope, Diffable> {

    @Override
    public Diffable convert(Converter converter, Type returnType, DiffableScope scope) {
        @SuppressWarnings("unchecked")
        Diffable diffable = DiffableType.getInstance((Class<Diffable>) returnType).newInternal(scope, null);

        scope.process(diffable);
        return diffable;
    }

}
