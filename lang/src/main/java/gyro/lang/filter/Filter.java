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

package gyro.lang.filter;

import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.tree.ParseTree;

public abstract class Filter {

    public static Filter create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();

        if (cc.equals(GyroParser.AndFilterContext.class)) {
            return new AndFilter((GyroParser.AndFilterContext) context);

        } else if (cc.equals(GyroParser.OrFilterContext.class)) {
            return new OrFilter((GyroParser.OrFilterContext) context);

        } else if (cc.equals(GyroParser.ComparisonFilterContext.class)) {
            return new ComparisonFilter((GyroParser.ComparisonFilterContext) context);
        }

        return null;
    }

    public abstract <C, R> R accept(FilterVisitor<C, R> visitor, C context);

}
