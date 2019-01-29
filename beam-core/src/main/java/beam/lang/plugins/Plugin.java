package beam.lang.plugins;

import beam.lang.ast.Scope;

public abstract class Plugin {

    private String artifact;
    private Scope scope;

    public abstract String name();

    public void init() {

    }

    public void classLoaded(Class<?> klass) {

    }

    public final void artifact(String artifact) {
        this.artifact = artifact;
    }

    public final String artifact() {
        return artifact;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

}
