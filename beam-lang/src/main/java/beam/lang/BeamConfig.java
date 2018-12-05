package beam.lang;

import beam.parser.antlr4.BeamParser;

import java.util.*;

public class BeamConfig implements BeamResolvable, BeamCollection {

    private Map<BeamConfigKey, BeamResolvable> context;

    private List<BeamResolvable> params;

    private List<String> beamTags;

    private List<BeamConfig> unResolvedContext;

    private String type = "config";

    private BeamParser.ConfigContext ctx;

    private Set<BeamReference> dependencies;

    public List<BeamResolvable> getParams() {
        if (params == null) {
            params = new ArrayList<>();
        }

        return params;
    }

    public void setParams(List<BeamResolvable> params) {
        this.params = params;
    }

    public List<String> getBeamTags() {
        if (beamTags == null) {
            beamTags = new ArrayList<>();
        }

        return beamTags;
    }

    public void setBeamTags(List<String> beamTags) {
        this.beamTags = beamTags;
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

    public BeamParser.ConfigContext getCtx() {
        return ctx;
    }

    public void setCtx(BeamParser.ConfigContext ctx) {
        this.ctx = ctx;
    }

    public Set<BeamReference> getDependencies() {
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
    public Set<BeamReference> getDependencies(BeamConfig config) {
        Set<BeamReference> dependencies = new HashSet<>();
        for (BeamConfigKey key : getContext().keySet()) {
            BeamResolvable referable = getContext().get(key);
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
            getContext().put(new BeamConfigKey(null, "depends-on"), beamList);
        }

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

            if (referable instanceof BeamTagReference) {
                progress = referable.resolve(this) || progress;
            } else {
                progress = referable.resolve(root) || progress;
            }
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
                        newConfig.setBeamTags(config.getBeamTags());
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
            if (key.getType() == null && referable instanceof BeamConfig) {
                continue;
            }

            sb.append(BCL.ui().dump(key.toString()));
            if (!(referable instanceof BeamConfig)) {
                sb.append(":");
            }

            if (referable instanceof BeamCollection) {
                if (referable instanceof BeamConfig) {
                    // should also add params
                    BeamConfig config = (BeamConfig) referable;
                    sb.append(" ");
                    sb.append(String.join(" ", config.getBeamTags()));
                }

                if (referable instanceof BeamInlineList) {
                    sb.append(" ");
                    sb.append(referable);
                    sb.append("\n");
                } else {
                    sb.append("\n");

                    BCL.ui().indent();
                    sb.append(referable);
                    BCL.ui().unindent();

                    sb.append(BCL.ui().dump("end\n"));
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
