package gyro.core.resource;

import gyro.core.GyroException;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;

public class ObjectScope extends Scope {

    private final Object object;

    public ObjectScope(Scope parent, Object object) {
        super(parent);

        this.object = object;
    }

    @Override
    public Object find(Node node, String key) {
        try {
            return NodeEvaluator.getValue(node, object, key);

        } catch (GyroException error) {
            return super.find(node, key);
        }
    }

}
