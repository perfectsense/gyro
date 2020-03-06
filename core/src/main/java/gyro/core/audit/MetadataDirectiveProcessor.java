/*
 * Copyright 2020, Perfect Sense, Inc.
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

package gyro.core.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.Scope;
import gyro.core.workflow.DefineDirectiveProcessor;
import gyro.core.workflow.Stage;
import gyro.core.workflow.Workflow;
import gyro.lang.ast.block.BlockNode;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.value.ValueNode;

@Type("metadata")
public class MetadataDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    private static final Map<String, Map<String, Map<String, Object>>> METADATA_BY_WORKFLOW = new ConcurrentHashMap<>();

    public static Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        for (Workflow workflow : Workflow.getSuccessfullyExecutedWorkflows()) {
            String workflowKey = String.format(
                DefineDirectiveProcessor.WORKFLOW_KEY_PATTERN,
                workflow.getType(),
                workflow.getName());

            for (Stage executedStage : workflow.getExecutedStages()) {
                String stageName = executedStage.getName();
                Optional.of(METADATA_BY_WORKFLOW)
                    .map(e -> e.get(workflowKey))
                    .map(e -> e.get(stageName))
                    .ifPresent(metadata::putAll);
            }
        }

        return metadata;
    }

    @Override
    public void process(DiffableScope scope, DirectiveNode node) throws Exception {
        BlockNode block = scope.getBlock();

        if (!(block instanceof KeyBlockNode) || !((KeyBlockNode) block).getKey().equals(Workflow.STAGE_TYPE_NAME)) {
            throw new GyroException(
                String.format(
                    "The @|bold @%s|@ directive must be used inside @|bold workflow stage|@!",
                    node.getName()));
        }
        validateArguments(node, 0, 0);

        String workflowKey = DefineDirectiveProcessor.getCurrentWorkflow();
        Map<String, Map<String, Object>> stageMetadata = METADATA_BY_WORKFLOW.putIfAbsent(
            workflowKey,
            new ConcurrentHashMap<>());

        if (stageMetadata == null) {
            stageMetadata = METADATA_BY_WORKFLOW.get(workflowKey);
        }

        Map<String, Map<String, Object>> finalStageMetadata = stageMetadata;
        Optional.of(((KeyBlockNode) block).getName())
            .filter(ValueNode.class::isInstance)
            .map((ValueNode.class::cast))
            .map(ValueNode::getValue)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .ifPresent(stageName -> {
                Scope metadata = evaluateBody(scope, node);
                Map<String, Object> existingMetadata = finalStageMetadata.putIfAbsent(stageName, metadata);

                if (existingMetadata != null) {
                    existingMetadata.putAll(metadata);
                }
            });
    }
}
