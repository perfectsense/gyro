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
import java.util.Iterator;

import com.psddev.dari.util.ConversionFunction;
import com.psddev.dari.util.Converter;
import gyro.core.GyroException;

public class IterableToOne implements ConversionFunction<Iterable, Object> {

    @Override
    public Object convert(Converter converter, Type returnType, Iterable iterable) {
        Iterator<?> iterator = iterable.iterator();

        if (iterator.hasNext()) {
            Object first = iterator.next();

            if (iterator.hasNext()) {
                throw new GyroException(String.format(
                    "Can't have more than 1 item in @|bold %s|@!",
                    iterable));

            } else {
                return first;
            }

        } else {
            return null;
        }
    }

}
