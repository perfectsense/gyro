package beam.core;

import java.util.HashMap;
import java.util.Map;

public class BeamContext {

    private Map<BeamContextKey, BeamReferable> context;

    private Map<BeamContextKey, BeamReferable> nativeContext;

    public Map<BeamContextKey, BeamReferable> getContext() {
        if (context == null) {
            context = new HashMap<>();
        }

        return context;
    }

    public void setContext(Map<BeamContextKey, BeamReferable> context) {
        this.context = context;
    }

    public Map<BeamContextKey, BeamReferable> getNativeContext() {
        if (nativeContext == null) {
            nativeContext = new HashMap<>();
        }

        return nativeContext;
    }

    public void setNativeContext(Map<BeamContextKey, BeamReferable> nativeContext) {
        this.nativeContext = nativeContext;
    }

    public BeamContext scopeContext(String scope) {
        BeamContext scopedContext = new BeamContext();
        for (BeamContextKey key : getNativeContext().keySet()) {
            scopedContext.getContext().put(key.scopeContextKey(scope), getNativeContext().get(key));
        }

        return scopedContext;
    }
}
