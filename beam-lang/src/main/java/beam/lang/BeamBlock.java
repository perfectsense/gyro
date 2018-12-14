package beam.lang;

import beam.lang.types.BeamInlineList;
import beam.lang.types.BeamList;
import beam.lang.types.BeamScalar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeamBlock implements BeamReferable, BeamCollection, BeamContext {

    private String type;
    private List<BeamResolvable> parameters;
    private List<BeamBlock> children;

    private Map<BeamContextKey, BeamReferable> context = new HashMap<>();

    public String getType() {
        if (type == null) {
            type = "config";
        }

        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<BeamResolvable> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }

        return parameters;
    }

    public void setParameters(List<BeamResolvable> parameters) {
        this.parameters = parameters;
    }

    public List<BeamBlock> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }

        return children;
    }

    public void setChildren(List<BeamBlock> children) {
        this.children = children;
    }

    public void applyExtension(BeamInterp lang) {
    }

    // -- BeamResolvable Implementation

    protected boolean resolveParams(BeamContext context) {
        boolean success = true;
        for (BeamResolvable param : getParameters()) {
            param.resolve(context);
            success = success && param.getValue() != null;
        }

        return success;
    }

    protected boolean resolve(BeamContext parent, BeamContext root) {
        boolean progress = false;
        String id = getParameters().get(0).getValue().toString();
        BeamContextKey key = new BeamContextKey(id, getType());
        if (parent.containsKey(key)) {
            BeamBlock existingConfig = (BeamBlock) parent.get(key);

            if (existingConfig.getClass() != this.getClass()) {
                parent.add(key, this);
                progress = true;
            }
        } else {
            parent.add(key, this);
            progress = true;
        }

        return resolve(root) || progress;
    }

    @Override
    public boolean resolve(BeamContext root) {
        boolean progress = false;
        Iterator<BeamBlock> iterator = getChildren().iterator();
        while (iterator.hasNext()) {
            BeamBlock unResolvedConfig = iterator.next();
            if (unResolvedConfig.resolveParams(root)) {
                progress = unResolvedConfig.resolve(this, root) || progress;
            }
        }

        for (BeamContextKey key : keys()) {
            BeamResolvable referable = get(key);
            if (referable instanceof BeamBlock) {
                continue;
            }

            progress = referable.resolve(root) || progress;
        }

        return progress;
    }

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public Set<BeamReference> getDependencies(BeamBlock config) {
        Set<BeamReference> dependencies = new HashSet<>();
        for (BeamContextKey key : keys()) {
            BeamResolvable referable = get(key);
            dependencies.addAll(referable.getDependencies(config));
        }

        BeamList beamList = new BeamList();
        for (BeamReference reference : dependencies) {
            BeamScalar beamScalar = new BeamScalar();
            beamScalar.getElements().add(reference);
            beamList.getList().add(beamScalar);
        }

        if (!beamList.getList().isEmpty()) {
            add(new BeamContextKey("depends-on"), beamList);
        }

        return dependencies;
    }

    // -- BeamCollection Implementation

    @Override
    public BeamReferable get(String key) {
        return get(new BeamContextKey(key));
    }

    // -- BeamContext Implementation

    @Override
    public BeamReferable get(BeamContextKey key) {
        return context.get(key);
    }

    @Override
    public boolean containsKey(BeamContextKey key) {
        return context.containsKey(key);
    }

    @Override
    public void add(BeamContextKey key, BeamReferable value) {
        context.put(key, value);
    }

    @Override
    public BeamReferable remove(BeamContextKey key) {
        return context.remove(key);
    }

    @Override
    public Set<BeamContextKey> keys() {
        return new HashSet<>(context.keySet());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BeamContextKey key : keys()) {
            BeamResolvable referable = get(key);
            if (key.getType() == null && referable instanceof BeamBlock) {
                continue;
            }

            sb.append(BeamInterp.ui().dump(key.toString()));
            if (!(referable instanceof BeamBlock)) {
                sb.append(":");
            }

            if (referable instanceof BeamCollection) {
                if (referable instanceof BeamBlock) {
                    // should also add parameters
                }

                if (referable instanceof BeamInlineList) {
                    sb.append(" ");
                    sb.append(referable);
                    sb.append("\n");
                } else {
                    sb.append("\n");

                    BeamInterp.ui().indent();
                    sb.append(referable);
                    BeamInterp.ui().unindent();

                    sb.append(BeamInterp.ui().dump("end\n"));
                }
            } else {
                sb.append(" ");
                sb.append(referable);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

}
