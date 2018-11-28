package beam.lang;

import beam.parser.antlr4.BeamParser;

import java.util.*;

public class BeamConfig implements BeamResolvable, BeamCollection {

    private Map<BeamConfigKey, BeamResolvable> context;

    private List<BeamResolvable> params;

    private List<BeamConfig> unResolvedContext;

    private String type = "config";

    private BeamParser.ExtensionContext ctx;

    private Set<BeamConfig> dependencies;

    public Map<BeamConfigKey, BeamResolvable> getContext() {
        if (context == null) {
            context = new HashMap<>();
        }

        return context;
    }

    public void setContext(Map<BeamConfigKey, BeamResolvable> context) {
        this.context = context;
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

    public List<BeamConfig> getUnResolvedContext() {
        if (unResolvedContext == null) {
            unResolvedContext = new ArrayList<>();
        }

        return unResolvedContext;
    }

    public void setUnResolvedContext(List<BeamConfig> unResolvedContext) {
        this.unResolvedContext = unResolvedContext;
    }

    public BeamParser.ExtensionContext getCtx() {
        return ctx;
    }

    public void setCtx(BeamParser.ExtensionContext ctx) {
        this.ctx = ctx;
    }

    public Set<BeamConfig> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<BeamConfig> dependencies) {
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

    protected boolean resolveParams(BeamConfig config) {
        boolean success = true;
        for (BeamResolvable param : getParams()) {
            param.resolve(config);
            success = success && param.getValue() != null;
        }

        return success;
    }

    protected boolean resolve(BeamConfig parent, BeamConfig root) {
        boolean progress = false;
        String id = getParams().get(0).getValue().toString();
        BeamConfigKey key = new BeamConfigKey(getType(), id);
        if (parent.getContext().containsKey(key)) {
            BeamConfig existingConfig = (BeamConfig) parent.getContext().get(key);

            if (existingConfig.getClass() != this.getClass()) {
                parent.getContext().put(key, this);
                progress = true;
            }
        } else {
            parent.getContext().put(key, this);
            progress = true;
        }

        return resolve(root) || progress;
    }

    @Override
    public Set<BeamConfig> getDependencies(BeamConfig config) {
        Set<BeamConfig> dependencies = new HashSet<>();
        for (BeamConfigKey key : getContext().keySet()) {
            BeamResolvable referable = getContext().get(key);
            dependencies.addAll(referable.getDependencies(config));
        }

        this.dependencies = dependencies;
        return dependencies;
    }

    @Override
    public boolean resolve(BeamConfig root) {
        boolean progress = false;
        Iterator<BeamConfig> iterator = getUnResolvedContext().iterator();
        while (iterator.hasNext()) {
            BeamConfig unResolvedConfig = iterator.next();
            if (unResolvedConfig.resolveParams(root)) {
                progress = unResolvedConfig.resolve(this, root) || progress;
            }
        }

        for (BeamConfigKey key : getContext().keySet()) {
            BeamResolvable referable = getContext().get(key);
            if (referable instanceof BeamConfig) {
                continue;
            }

            progress = referable.resolve(root) || progress;
        }

        return progress;
    }

    @Override
    public BeamResolvable get(String key) {
        return getContext().get(new BeamConfigKey(null, key));
    }

    public void applyExtension() {
        List<BeamConfig> newConfigs = new ArrayList<>();
        Iterator<BeamConfig> iterator = getUnResolvedContext().iterator();
        while (iterator.hasNext()) {
            BeamConfig config = iterator.next();
            if (BCL.getExtensions().containsKey(config.getType())) {
                Class<? extends BeamConfig> extension = BCL.getExtensions().get(config.getType());
                if (config.getClass() != extension) {
                    try {
                        BeamConfig newConfig = extension.newInstance();
                        newConfig.setCtx(config.getCtx());
                        newConfig.setType(config.getType());
                        newConfig.setContext(config.getContext());
                        newConfig.setParams(config.getParams());
                        newConfig.setUnResolvedContext(config.getUnResolvedContext());
                        newConfigs.add(newConfig);
                        newConfig.applyExtension();
                        iterator.remove();

                    } catch (InstantiationException | IllegalAccessException ie) {
                        throw new BeamLangException("Unable to instantiate " + extension.getClass().getSimpleName());
                    }
                } else {
                    config.applyExtension();
                }
            }
        }

        getUnResolvedContext().addAll(newConfigs);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BeamConfigKey key : getContext().keySet()) {
            BeamResolvable referable = getContext().get(key);
            sb.append(BCL.ui().dump(key.toString()));
            if (!(referable instanceof BeamConfig)) {
                sb.append(":");
            }

            if (referable instanceof BeamCollection) {
                sb.append("\n");

                BCL.ui().indent();
                sb.append(referable);
                BCL.ui().unindent();

                sb.append(BCL.ui().dump("end\n"));
            } else {
                sb.append(" ");
                sb.append(referable);
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
