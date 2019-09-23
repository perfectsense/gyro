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

import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class ComparisonFilter extends Filter {

    private final String operator;
    private final String key;
    private final Node value;

    public static final String EQUALS_OPERATOR = "=";
    public static final String NOT_EQUALS_OPERATOR = "!=";

    public ComparisonFilter(GyroParser.ComparisonFilterContext context) {
        this.operator = context.relOp().getText();
        this.key = context.IDENTIFIER().getText();
        this.value = Node.create(context.value());
    }

    public String getOperator() {
        return operator;
    }

    public String getKey() {
        return key;
    }

    public Node getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(FilterVisitor<C, R> visitor, C context) {
        return visitor.visitComparison(this, context);
    }

}
