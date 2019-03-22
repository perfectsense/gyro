package gyro.lang.ast.scope;

import gyro.lang.ast.Node;

public class VirtualResourceScope extends Scope {

    private String name;

    public VirtualResourceScope(Scope parent, String name) {
        super(parent);

        this.name = name;
        this.putAll(parent);
    }

    public String getName() {
        return name;
    }

    @Override
    public Object find(String path, Node node) {
        return get(path);
    }
}
