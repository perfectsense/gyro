package beam.lang;

import beam.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeamConfig implements BeamReferable, BeamCollection, BeamContext {

    private Map<BeamContextKey, BeamReferable> context = new HashMap<>();

    private List<BeamResolvable> params;

    private List<BeamConfig> subConfigs;

    private String type = "config";

    private BeamParser.BlockBodyContext bodyContext;

    private Set<BeamReference> dependencies;

    private List<BeamContextKey> scope = new ArrayList<>();

    @Override
    public List<BeamContextKey> getScope() {
        return scope;
    }

    public List<BeamResolvable> getParams() {
        if (params == null) {
            params = new ArrayList<>();
        }

        return params;
    }

    public void setParams(List<BeamResolvable> params) {
        this.params = params;
    }

    public List<BeamConfig> getSubConfigs() {
        if (subConfigs == null) {
            subConfigs = new ArrayList<>();
        }

        return subConfigs;
    }

    public void setSubConfigs(List<BeamConfig> subConfigs) {
        this.subConfigs = subConfigs;
    }

    public BeamParser.BlockBodyContext getCtx() {
        return bodyContext;
    }

    public void setCtx(BeamParser.BlockBodyContext bodyContext) {
        this.bodyContext = bodyContext;
    }

    public Set<BeamReference> getDependencies() {
        return dependencies;
    }

    @Override
    public Set<BeamReference> getDependencies(BeamConfig config) {
        Set<BeamReference> dependencies = new HashSet<>();
        for (BeamContextKey key : listContextKeys()) {
            BeamResolvable referable = getReferable(key);
            dependencies.addAll(referable.getDependencies(config));
        }

        this.dependencies = dependencies;
        BeamList beamList = new BeamList();
        for (BeamReference reference : dependencies) {
            BeamScalar beamScalar = new BeamScalar();
            beamScalar.getElements().add(reference);
            beamList.getList().add(beamScalar);
        }

        if (!beamList.getList().isEmpty()) {
            addReferable(new BeamContextKey("depends-on"), beamList);
        }

        return dependencies;
    }


    public void setDependencies(Set<BeamReference> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public Object getValue() {
        return this;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    protected boolean resolveParams(BeamContext context) {
        boolean success = true;
        for (BeamResolvable param : getParams()) {
            param.resolve(context);
            success = success && param.getValue() != null;
        }

        return success;
    }

    protected boolean resolve(BeamContext parent, BeamContext root) {
        boolean progress = false;
        String id = getParams().get(0).getValue().toString();
        BeamContextKey key = new BeamContextKey(id, getType());
        if (parent.hasKey(key)) {
            BeamConfig existingConfig = (BeamConfig) parent.getReferable(key);

            if (existingConfig.getClass() != this.getClass()) {
                parent.addReferable(key, this);
                progress = true;
            }
        } else {
            parent.addReferable(key, this);
            progress = true;
        }

        if (getScope().isEmpty()) {
            getScope().addAll(parent.getScope());
            getScope().add(key);
        }

        return resolve(root) || progress;
    }

    @Override
    public boolean resolve(BeamContext root) {
        boolean progress = false;
        Iterator<BeamConfig> iterator = getSubConfigs().iterator();
        while (iterator.hasNext()) {
            BeamConfig unResolvedConfig = iterator.next();
            if (unResolvedConfig.resolveParams(root)) {
                progress = unResolvedConfig.resolve(this, root) || progress;
            }
        }

        for (BeamContextKey key : listContextKeys()) {
            BeamResolvable referable = getReferable(key);
            if (referable instanceof BeamConfig) {
                continue;
            }

            progress = referable.resolve(root) || progress;
        }

        return progress;
    }

    @Override
    public BeamReferable get(String key) {
        return getReferable(new BeamContextKey(key));
    }

    public void applyExtension(BeamInterp lang) {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BeamContextKey key : listContextKeys()) {
            BeamResolvable referable = getReferable(key);
            if (key.getType() == null && referable instanceof BeamConfig) {
                continue;
            }

            sb.append(BeamInterp.ui().dump(key.toString()));
            if (!(referable instanceof BeamConfig)) {
                sb.append(":");
            }

            if (referable instanceof BeamCollection) {
                if (referable instanceof BeamConfig) {
                    // should also add params
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

    @Override
    public boolean hasKey(BeamContextKey key) {
        return context.containsKey(key);
    }

    @Override
    public BeamReferable getReferable(BeamContextKey key) {
        return context.get(key);
    }

    @Override
    public void addReferable(BeamContextKey key, BeamReferable value) {
        context.put(key, value);
    }

    @Override
    public BeamReferable removeReferable(BeamContextKey key) {
        return context.remove(key);
    }

    @Override
    public List<BeamContextKey> listContextKeys() {
        return new ArrayList<>(context.keySet());
    }

    @Override
    public void importContext(BeamContext context) {
        for (BeamContextKey key : context.listContextKeys()) {
            addReferable(key, context.getReferable(key));
        }
    }
}
