package gyro.core.plugin;

import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.collect.ImmutableList;
import gyro.core.resource.DirectiveProcessor;
import gyro.core.resource.RootScope;
import org.eclipse.aether.resolution.DependencyResult;

class Plugin {

    private static final PluginClassLoader PLUGIN_CLASS_LOADER = new PluginClassLoader();

    private final List<Class<? extends Provider>> providerClasses;
    private final List<Class<? extends DirectiveProcessor>> directiveProcessorClasses;
    private final List<Class<?>> otherClasses;

    @SuppressWarnings("unchecked")
    public Plugin(DependencyResult result) throws Exception {
        Thread.currentThread().setContextClassLoader(PLUGIN_CLASS_LOADER);
        PLUGIN_CLASS_LOADER.add(result);

        ImmutableList.Builder<Class<? extends Provider>> providerClasses = ImmutableList.builder();
        ImmutableList.Builder<Class<? extends DirectiveProcessor>> directiveProcessorClasses = ImmutableList.builder();
        ImmutableList.Builder<Class<?>> otherClasses = ImmutableList.builder();

        try (JarFile jar = new JarFile(result.getRoot().getArtifact().getFile())) {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();

                if (!name.endsWith(".class")) {
                    continue;
                }

                name = name.substring(0, name.length() - 6);
                name = name.replace('/', '.');
                Class<?> c = Class.forName(name, false, PLUGIN_CLASS_LOADER);

                if (Provider.class.isAssignableFrom(c)) {
                    providerClasses.add((Class<? extends Provider>) c);

                } else if (DirectiveProcessor.class.isAssignableFrom(c)) {
                    directiveProcessorClasses.add((Class<? extends DirectiveProcessor>) c);

                } else {
                    int modifiers = c.getModifiers();

                    if (!Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)) {
                        otherClasses.add(c);
                    }
                }
            }
        }

        this.providerClasses = providerClasses.build();
        this.directiveProcessorClasses = directiveProcessorClasses.build();
        this.otherClasses = otherClasses.build();
    }

    public void execute(RootScope rootScope) throws Exception {
        Thread.currentThread().setContextClassLoader(PLUGIN_CLASS_LOADER);

        for (Class<? extends Provider> c : providerClasses) {
            Provider provider = c.newInstance();

            provider.setScope(rootScope);
            otherClasses.forEach(provider::classLoaded);
            provider.init();
        }

        if (!directiveProcessorClasses.isEmpty()) {
            Map<String, DirectiveProcessor> directiveProcessors = rootScope.getDirectiveProcessors();

            for (Class<? extends DirectiveProcessor> c : directiveProcessorClasses) {
                DirectiveProcessor processor = c.newInstance();

                directiveProcessors.put(processor.getName(), processor);
            }
        }
    }

}
