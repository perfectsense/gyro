package beam.lang;

import beam.parser.antlr4.BeamParser;

import java.util.*;

public class BeamConfig implements BeamReferable, BeamCollection, BeamContext {

    private Map<BeamContextKey, BeamReferable> context = new HashMap<>();

    private List<BeamResolvable> params;

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

        return resolve(root) || progress;
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

    @Override
    public boolean resolve(BeamContext root) {
        boolean progress = false;
        Iterator<BeamConfig> iterator = getUnResolvedContext().iterator();
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
                        newConfig.importContext(config);
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
        for (BeamContextKey key : listContextKeys()) {
            BeamResolvable referable = getReferable(key);
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
