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

import com.google.common.collect.ImmutableList;
import gyro.parser.antlr4.GyroParser;

import java.util.List;

public abstract class AbstractCompoundFilter extends Filter {

    private final List<Filter> children;

    public AbstractCompoundFilter(GyroParser.FilterContext context) {
        ImmutableList.Builder<Filter> list = ImmutableList.builder();
        addChildren(list, create(context.getChild(0)));
        addChildren(list, create(context.getChild(2)));
        children = list.build();
    }

    public AbstractCompoundFilter(List<Filter> children) {
        this.children = ImmutableList.copyOf(children);
    }

    public List<Filter> getChildren() {
        return children;
    }

    private void addChildren(ImmutableList.Builder<Filter> list, Filter child) {
        if (getClass().isInstance(child)) {
            list.addAll(((AbstractCompoundFilter) child).getChildren());
        } else {
            list.add(child);
        }
    }
}
