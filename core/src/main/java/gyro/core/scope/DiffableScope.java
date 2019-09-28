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

package gyro.core.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gyro.core.GyroException;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableProcessor;
import gyro.core.resource.SelfSettings;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.BlockNode;

public class DiffableScope extends Scope {

    private final BlockNode block;
    private final List<DiffableProcessor> processors;
    private final List<Node> stateNodes;

    public DiffableScope(Scope parent, BlockNode block) {
        super(parent);

        this.block = block;
        this.processors = new ArrayList<>();
        this.stateNodes = new ArrayList<>();
    }

    public DiffableScope(DiffableScope scope) {
        super(scope.getParent());

        this.block = scope.block;
        this.processors = new ArrayList<>(scope.processors);
        this.stateNodes = new ArrayList<>(scope.stateNodes);
        this.getSettingsByClass().putAll(scope.getSettingsByClass().asMap());
    }

    public BlockNode getBlock() {
        return block;
    }

    public List<Node> getStateNodes() {
        return stateNodes;
    }

    public void addProcessor(DiffableProcessor processor) {
        processors.add(processor);
    }

    public void process(Diffable diffable) {
        Set<String> configuredFields = DiffableInternals.getConfiguredFields(diffable);

        for (DiffableProcessor processor : processors) {
            try {
                Set<String> fields = processor.process(diffable);

                if (fields != null) {
                    configuredFields.addAll(fields);
                }

            } catch (Exception error) {
                throw new GyroException(
                    String.format("Can't process @|bold %s|@ using @|bold %s|@!", diffable, processor),
                    error);
            }
        }
    }

    @Override
    public Object find(Node node, String key) {
        if ("_SELF".equals(key)) {
            for (Scope s = this; s instanceof DiffableScope; s = s.getParent()) {
                SelfSettings settings = s.getSettings(SelfSettings.class);
                if (settings.getSelf() != null) {
                    return settings.getSelf();
                }
            }

            return null;
        }

        return getParent().find(node, key);
    }

}
