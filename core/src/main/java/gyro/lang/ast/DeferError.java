package gyro.lang.ast;

import gyro.core.GyroException;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.FileNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeferError extends Error {

    private final Node node;

    public static void evaluate(Scope scope, List<Node> body) throws Exception {
        int bodySize = body.size();

        while (true) {
            List<DeferError> errors = new ArrayList<>();
            List<Node> deferred = new ArrayList<>();

            for (Node node : body) {
                try {
                    node.evaluate(scope);

                } catch (DeferError error) {
                    errors.add(error);
                    deferred.add(node);
                }
            }

            if (deferred.isEmpty()) {
                break;

            } else if (bodySize == deferred.size()) {
                StringBuilder sb = new StringBuilder();
                for (DeferError error : errors) {
                    sb.append(error.getMessage());
                }

                throw new GyroException(sb.toString());

            } else {
                body = deferred;
                bodySize = body.size();
            }
        }
    }

    public static void evaluate(Map<Node, Scope> map) throws Exception {
        int bodySize = map.size();

        while (true) {
            List<DeferError> errors = new ArrayList<>();
            Map<Node, Scope> deferred = new HashMap<>();

            for (Map.Entry<Node, Scope> entry : map.entrySet()) {
                try {
                    entry.getKey().evaluate(entry.getValue());
                    RootScope rootScope = entry.getValue().getRootScope();
                    Scope scope = entry.getValue();
                    scope.values()
                        .stream()
                        .filter(Resource.class::isInstance)
                        .map(Resource.class::cast)
                        .forEach(r -> rootScope.getResources().put(String.format("%s::%s", r.resourceType(), r.resourceIdentifier()), r));

                } catch (DeferError error) {
                    errors.add(error);
                    deferred.put(entry.getKey(), entry.getValue());
                }
            }

            if (deferred.isEmpty()) {
                break;

            } else if (bodySize == deferred.size()) {
                StringBuilder sb = new StringBuilder();
                for (DeferError error : errors) {
                    sb.append(error.getMessage());
                }

                throw new GyroException(sb.toString());

            } else {
                map = deferred;
                bodySize = map.size();
            }
        }
    }

    public DeferError(Node node) {
        this.node = node;
    }

    @Override
    public String getMessage() {
        return node.deferFailure();
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
