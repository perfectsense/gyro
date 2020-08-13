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

package gyro.core.command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import gyro.core.FileBackend;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.LocalFileBackend;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.FileNode;
import gyro.parser.antlr4.GyroParser;
import gyro.util.Bug;
//import picocli.CommandLine.Command;

public abstract class PluginCommand implements GyroCommand, Callable<Integer> {

    /*@Command(description =
        "A space separated list of plugins specified in the format of <group>:<artifact>:<version>. "
            + "For example: gyro:gyro-aws-provider:0.1-SNAPSHOT")*/
    private List<String> plugins;

    private List<DirectiveNode> pluginNodes;

    private List<DirectiveNode> repositoryNodes;

    public List<String> getPlugins() {
        if (plugins == null) {
            plugins = Collections.emptyList();
        }

        return plugins;
    }

    public List<DirectiveNode> getPluginNodes() {
        return pluginNodes;
    }

    public List<DirectiveNode> getRepositoryNodes() {
        return repositoryNodes;
    }

    @Override
    public Integer call() throws Exception {
        execute();
        return 0;
    }

    @Override
    public final void execute() throws Exception {
        if (GyroCore.getRootDirectory() == null) {
            throw new GyroException("Can't find gyro root directory!");
        }

        for (String plugin : getPlugins()) {
            if (plugin.split(":").length != 3) {
                throw new GyroException(String.format(
                    "@|bold %s|@ isn't properly formatted!",
                    plugin));
            }
        }

        FileBackend backend = new LocalFileBackend(GyroCore.getRootDirectory());
        try (GyroInputStream input = new GyroInputStream(backend, GyroCore.INIT_FILE)) {
            List<DirectiveNode> nodes = ((FileNode) Node.parse(input, GyroCore.INIT_FILE, GyroParser::file))
                .getBody()
                .stream()
                .filter(DirectiveNode.class::isInstance)
                .map(DirectiveNode.class::cast)
                .collect(Collectors.toList());

            pluginNodes = nodes.stream()
                .filter(n -> "plugin".equals(n.getName()))
                .collect(Collectors.toList());

            repositoryNodes = nodes.stream()
                .filter(n -> "repository".equals(n.getName()))
                .collect(Collectors.toList());

        } catch (IOException error) {
            throw new Bug(error);
        }

        executeSubCommand();
    }

    protected abstract void executeSubCommand() throws Exception;

    public boolean pluginExist(String plugin) {
        NodeEvaluator evaluator = new NodeEvaluator();
        Scope scope = new Scope(null);

        for (DirectiveNode pluginNode : pluginNodes) {
            String existPlugin = (String) evaluator.visit(pluginNode.getArguments().get(0), scope);
            if (plugin.equals(existPlugin)) {
                return true;
            }
        }

        return false;
    }

    public boolean pluginNotExist(String plugin) {
        return !pluginExist(plugin);
    }

    public boolean pluginNodeExist(DirectiveNode pluginNode) {
        return getPlugins().contains(toPluginString(pluginNode));
    }

    public String toPluginString(DirectiveNode pluginNode) {
        return evaluateFirstArgument(pluginNode);
    }

    public String toRepositoryUrl(DirectiveNode repositoryNode) {
        return evaluateFirstArgument(repositoryNode);
    }

    public List<String> load() throws Exception {
        FileBackend backend = new LocalFileBackend(GyroCore.getRootDirectory());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(backend.openInput(GyroCore.INIT_FILE)))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    public void save(String s) throws Exception {
        FileBackend backend = new LocalFileBackend(GyroCore.getRootDirectory());
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(backend.openOutput(GyroCore.INIT_FILE)))) {
            writer.write(s);
        }
    }

    private String evaluateFirstArgument(DirectiveNode node) {
        NodeEvaluator evaluator = new NodeEvaluator();
        Scope scope = new Scope(null);

        return (String) evaluator.visit(node.getArguments().get(0), scope);
    }

}
