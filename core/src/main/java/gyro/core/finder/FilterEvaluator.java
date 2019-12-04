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

package gyro.core.finder;

import java.util.List;
import java.util.Objects;

import gyro.core.GyroException;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.lang.filter.AndFilter;
import gyro.lang.filter.ComparisonFilter;
import gyro.lang.filter.FilterVisitor;
import gyro.lang.filter.OrFilter;

public class FilterEvaluator implements FilterVisitor<FilterContext, Boolean> {

    @Override
    public Boolean visitAnd(AndFilter filter, FilterContext context) {
        return filter.getChildren().stream().allMatch(c -> visit(c, context));
    }

    @Override
    public Boolean visitComparison(ComparisonFilter filter, FilterContext context) {
        String operator = filter.getOperator();
        Object x = NodeEvaluator.getValue(null, context.getValue(), filter.getKey());
        Scope scope = context.getScope();
        Object y = scope.getRootScope().getEvaluator().visit(filter.getValue(), scope);

        switch (operator) {
            case ComparisonFilter.EQUALS_OPERATOR:
                return roughlyEquals(x, y);

            case ComparisonFilter.NOT_EQUALS_OPERATOR:
                return !roughlyEquals(x, y);

            default:
                throw new GyroException(String.format(
                    "@|bold %s|@ operator isn't supported!",
                    operator));
        }
    }

    @SuppressWarnings("unchecked")
    private boolean roughlyEquals(Object x, Object y) {
        if (Objects.equals(x, y)) {
            return true;

        } else if (x instanceof List) {
            if (!(y instanceof List)) {
                List<Object> xList = (List<Object>) x;

                if (xList.size() == 1) {
                    return roughlyEquals(xList.get(0), y);
                }
            }

        } else if (y instanceof List) {
            List<Object> yList = (List<Object>) y;

            if (yList.size() == 1) {
                return roughlyEquals(x, yList.get(0));
            }
        }

        return false;
    }

    @Override
    public Boolean visitOr(OrFilter filter, FilterContext context) {
        return filter.getChildren().stream().anyMatch(c -> visit(c, context));
    }

}
