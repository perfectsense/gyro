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

import com.google.common.collect.ImmutableList;
import gyro.core.resource.Resource;
import gyro.lang.filter.Filter;
import gyro.lang.filter.FilterVisitor;

public class FoundFilter extends Filter {

    private final List<Resource> resources;

    public FoundFilter(List<Resource> resources) {
        this.resources = ImmutableList.copyOf(resources);
    }

    public List<Resource> getResources() {
        return resources;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C, R> R accept(FilterVisitor<C, R> visitor, C context) {
        return (R) resources;
    }

}
