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
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import gyro.core.GyroUI;
import gyro.lang.ast.Node;

public class Defer extends Error {

    private final Node node;

    public Defer(Node node, String message, Defer cause) {
        super(message, cause);

        this.node = node;
    }

    public Defer(Node node, String message) {
        this(node, message, null);
    }

    public static <T> void execute(List<T> items, Consumer<T> consumer) {
        int size = items.size();

        while (true) {
            List<Defer> errors = new ArrayList<>();
            List<T> deferred = new ArrayList<>();

            for (T item : items) {
                try {
                    consumer.accept(item);

                } catch (Defer error) {
                    errors.add(error);
                    deferred.add(item);
                }
            }

            if (deferred.isEmpty()) {
                break;

            } else if (size == deferred.size()) {
                throw new ExecuteDefer(errors);

            } else {
                items = deferred;
                size = items.size();
            }
        }
    }

    public static void writeErrors(GyroUI ui, String message, Collection<? extends Defer> errors) {
        if (!errors.isEmpty()) {
            ui.write(message);

            for (Defer error : errors) {
                ui.write("\n@|red -|@ ");
                ui.indented(() -> error.write(ui));
            }
        }
    }

    public void write(GyroUI ui) {
        ui.write("@|red Error:|@ %s\n", getMessage());

        if (node != null) {
            ui.write("\nIn @|bold %s|@ %s:\n", node.getFile(), node.toLocation());
            ui.write("%s", node.toCodeSnippet());
        }

        Defer cause = getCause();

        if (cause != null) {
            ui.write("\n@|red Caused by:|@ ");
            cause.write(ui);
        }
    }

    @Override
    public synchronized Defer getCause() {
        return (Defer) super.getCause();
    }

}
