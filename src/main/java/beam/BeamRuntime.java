package beam;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import beam.cli.AbstractCloudCommand;
import beam.config.ProjectsConfig;
import beam.utils.AuditStream;
import beam.utils.CapturingInputStream;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.ObjectUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fusesource.jansi.AnsiRenderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import beam.config.CloudConfig;
import beam.config.ConfigConstructor;
import beam.config.NetworkConfig;
import beam.config.RootConfig;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

public class BeamRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeamRuntime.class);

    private static Set<BeamAuditor> AUDITORS;
    private static List<String> AUDIT_COMMAND_ARGUMENTS;

    private static AuditStream AUDIT_STREAM;

    private static BeamRuntime CURRENT_RUNTIME;
    private static Map<String, BeamRuntime> ENVIRONMENT_RUNTIME = new ConcurrentHashMap<>();

    private final RootConfig config;
    private final String environment;
    private final String project;
    private final String account;
    private final String serial;
    private final String internalDomain;
    private final String subDomain;
    private final Set<BeamCloud> clouds;
    private BeamStorage storage;
    private boolean finished = false;

    public static synchronized void startAudit(AbstractCloudCommand command) {
        AUDIT_COMMAND_ARGUMENTS = command.getUnparsedArgument();

        final BeamRuntime currentRuntime = BeamRuntime.getCurrentRuntime();
        final PrintStream out = System.out;
        currentRuntime.startAudit();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    currentRuntime.finishAudit(false);
                } catch (Exception ex) {
                    ex.printStackTrace(out);
                }
            }
        });

        if (AUDIT_STREAM == null) {
            AUDIT_STREAM = new AuditStream(AUDITORS, System.out, command);
        }

        System.setIn(new CapturingInputStream(System.in, AUDIT_STREAM.getOutputStream()));
        //System.setErr(AUDIT_STREAM.getOutputStream());
        System.setOut(AUDIT_STREAM.getOutputStream());
    }

    public static synchronized Process startAuditSubprocess(ProcessBuilder pb) throws IOException {
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader subProcessOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        PrintWriter out = new AnsiRenderWriter(System.out, true);

        int value;
        while ((value = subProcessOutput.read()) != -1) {
            char c = (char) value;
            out.print(c);
            out.flush();
        }

        return process;
    }

    /**
     * @return Never {@code null}.
     */
    public static BeamRuntime getCurrentRuntime() {
        if (CURRENT_RUNTIME == null) {
            for (Class<? extends BeamRuntimeCreator> creatorClass : Beam.getReflections().getSubTypesOf(BeamRuntimeCreator.class)) {
                try {
                    BeamRuntimeCreator creator = creatorClass.newInstance();

                    try {
                        BeamRuntime runtime = creator.createRuntime();

                        if (runtime != null) {
                            CURRENT_RUNTIME = runtime;
                            ENVIRONMENT_RUNTIME.put(runtime.getEnvironment(), runtime);

                            return runtime;
                        }

                    } catch (Exception error) {
                        LOGGER.debug(String.format(
                                "Can't create an runtime using [%s]!",
                                creatorClass.getName()),
                                error);
                    }

                } catch (IllegalAccessException | InstantiationException error) {
                    LOGGER.warn(String.format(
                            "Can't create an instance of [%s]!",
                            creatorClass.getName()),
                            error);
                }
            }

            throw new BeamException("Can't determine the current runtime!");
        }

        return CURRENT_RUNTIME;
    }

    public static void setCurrentRuntime(BeamRuntime runtime) {
        CURRENT_RUNTIME = runtime;
    }

    public static BeamRuntime getRuntimeForEnvironment(String environment) {
        if (ENVIRONMENT_RUNTIME.get(environment) == null) {
            for (Class<? extends BeamRuntimeCreator> creatorClass : Beam.getReflections().getSubTypesOf(BeamRuntimeCreator.class)) {
                try {
                    BeamRuntimeCreator creator = creatorClass.newInstance();

                    try {
                        BeamRuntime runtime = creator.createRuntime(environment);

                        if (runtime != null) {
                            ENVIRONMENT_RUNTIME.put(runtime.getEnvironment(), runtime);

                            return runtime;
                        }

                    } catch (Exception error) {
                        LOGGER.debug(String.format(
                                        "Can't create an runtime using [%s]!",
                                        creatorClass.getName()),
                                error);
                    }

                } catch (IllegalAccessException | InstantiationException error) {
                    LOGGER.warn(String.format(
                                    "Can't create an instance of [%s]!",
                                    creatorClass.getName()),
                            error);
                }
            }

            throw new BeamException("Can't determine the current runtime!");
        }

        return ENVIRONMENT_RUNTIME.get(environment);
    }

    /**
     * @param environmentName Can't be {@code null}.
     * @return May be {@code null}.
     */
    public static BeamRuntime setCurrentRuntime(String environmentName) {
        Preconditions.checkNotNull(environmentName, "environmentName");

        File configFile = null;
        String configFilename = null;

        File appFile = null;
        if (System.getProperty("beam.app") != null) {
            appFile = new File(System.getProperty("beam.app"));
        }

        String projectAlias = null;
        if (appFile != null && appFile.exists() && !appFile.getName().equals("beam")) {
            projectAlias = appFile.getName();
        }

        if (environmentName.contains(":") || projectAlias != null) {
            File projectsConfigPath = new File(System.getProperty("user.home") +  File.separator + ".beam/projects.yml");

            if (!projectsConfigPath.exists()) {
                throw new BeamException("Unable to load projects.yml");
            }

            String projectName = null;
            if (projectAlias != null) {
                projectName = projectAlias;
            } else {
                String[] parts = environmentName.split(":");
                projectName = parts[0];
                environmentName = parts[1];
            }

            try {
                Constructor configConstructor = new ConfigConstructor(ProjectsConfig.class, projectsConfigPath);
                Yaml configYaml = new Yaml(configConstructor);

                ProjectsConfig projectsConfig = (ProjectsConfig) configYaml.load(new FileInputStream(projectsConfigPath));
                for (ProjectsConfig.ProjectConfig projectConfig : projectsConfig.getProjects()) {
                    if (projectName.equals(projectConfig.getName()) || projectName.equals(projectConfig.getAlias())) {
                        configFile = new File(projectConfig.getPath() + File.separator + environmentName + ".yml");
                        break;
                    }
                }
            } catch (FileNotFoundException ffe) {
                throw new BeamException("Environment '" + environmentName + "' not found. Check that you are in a beam directory.");
            } catch (Exception error) {
                throw new BeamException("Invalid config file!", error);
            }

            if (configFile == null) {
                throw new BeamException("Unable to find project " + projectName + " in projects.yml");
            }

        } else {
            configFilename = environmentName + ".yml";
            configFile = new File(configFilename);
        }

        LOGGER.debug("Config file at: {}", configFile);
        LOGGER.debug("Environment is: {}", environmentName);

        if (configFile != null) {
            if (configFile.exists()) {
                try {
                    Constructor configConstructor = new ConfigConstructor(RootConfig.class, configFile);
                    Yaml configYaml = new Yaml(configConstructor);
                    RootConfig config = (RootConfig) ("network.yml".equals(configFile.getName()) ?
                            configYaml.load(new ByteArrayInputStream("network: network.yml".getBytes(Charsets.UTF_8))) :
                            configYaml.load(new FileInputStream(configFile)));

                    config.setEnvironment(environmentName);

                    CURRENT_RUNTIME = new BeamRuntime(config);

                    return CURRENT_RUNTIME;
                } catch (Exception error) {
                    throw new BeamException("Invalid config file!", error);
                }

            } else {
                RootConfig config = null;
                boolean foundAlias = false;
                try {
                    File currentDirectory = new File(".");
                    File[] allFiles = currentDirectory.listFiles();
                    int i = 0;
                    while (allFiles != null && i < allFiles.length) {
                        try {
                            File file = allFiles[i];
                            if (file.isFile() && file.getName().toLowerCase().endsWith(".yml")) {
                                configFile = file;
                                Constructor configConstructor = new ConfigConstructor(RootConfig.class, configFile);
                                Yaml configYaml = new Yaml(configConstructor);
                                config = (RootConfig) configYaml.load(new FileInputStream(configFile));

                                if (environmentName.equals(config.getAlias())) {
                                    environmentName = file.getName().substring(0, file.getName().length()-4);
                                    foundAlias = true;
                                    break;
                                }
                            }
                            i++;

                        } catch (Exception e) {
                            i++;
                        }
                    }

                    if (foundAlias) {
                        config.setEnvironment(environmentName);
                        CURRENT_RUNTIME = new BeamRuntime(config);
                        return CURRENT_RUNTIME;

                    } else {
                        throw new Exception("Environment or Alias: " + environmentName);
                    }

                } catch (Exception error) {
                    throw new BeamException("Invalid config file!", error);
                }
            }
        }

        return null;
    }

    /**
     * @param environment Can't be {@code null}.
     * @param project Can't be {@code null}.
     * @param serial Can't be {@code null}.
     * @param internalDomain Can't be {@code null}.
     * @param clouds Can't be blank.
     */
    public BeamRuntime(String environment, String project, String account, String serial, String internalDomain, Set<BeamCloud> clouds) {
        Preconditions.checkNotNull(environment, "environment");
        Preconditions.checkNotNull(project, "project");
        Preconditions.checkNotNull(serial, "serial");
        Preconditions.checkNotNull(internalDomain, "internalDomain");
        Preconditions.checkNotNull(clouds, "clouds");

        this.config = null;
        this.environment = environment;
        this.project = project;
        this.account = account;
        this.serial = serial;
        this.internalDomain = internalDomain;
        this.subDomain = null;
        this.clouds = Collections.unmodifiableSet(clouds);
    }

    /**
     * @param config Can't be {@code null}.
     */
    public BeamRuntime(RootConfig config) {
        Preconditions.checkNotNull(config, "config");

        this.config = config;
        this.environment = config.getEnvironment();

        NetworkConfig networkConfig = config.getNetworkConfig();

        this.project = networkConfig.getName();
        this.account = networkConfig.getAccount();
        this.serial = networkConfig.getSerial();
        this.internalDomain = networkConfig.getInternalDomain();
        this.subDomain = networkConfig.getSubdomain();

        Set<BeamCloud> clouds = new HashSet<BeamCloud>();
        Map<String, BeamCloud> cloudsByName = new HashMap<String, BeamCloud>();

        for (Class<? extends BeamCloud> cloudClass : Beam.getReflections().getSubTypesOf(BeamCloud.class)) {
            try {
                BeamCloud cloud = cloudClass.getConstructor(BeamRuntime.class).newInstance(this);

                cloudsByName.put(cloud.getName(), cloud);

            } catch (IllegalAccessException |
                    InstantiationException |
                    InvocationTargetException |
                    NoSuchMethodException error) {

                LOGGER.warn(
                        String.format("Can't create an instance of [%s]!", cloudClass),
                        error);
            }
        }

        for (CloudConfig cloudConfig : networkConfig.getClouds()) {
            String name = cloudConfig.getCloud();
            BeamCloud cloud = cloudsByName.get(name);

            cloud.setActiveRegions(cloudConfig.getActiveRegions());

            if (cloud != null) {
                clouds.add(cloud);

            } else {
                LOGGER.warn(
                        "Can't find a subclass of [{}] associated with [{}]!",
                        BeamCloud.class.getName(),
                        name);
            }
        }

        this.clouds = Collections.unmodifiableSet(clouds);
    }

    /**
     * @return May be {@code null}.
     */
    public RootConfig getConfig() {
        return config;
    }

    /**
     * @return Never {@code null}.
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * @return Never {@code null}.
     */
    public String getProject() {
        return project;
    }


    /**
     * @return Never {@code null}.
     */
    public String getAccount() {
        if (account == null) {
            return getProject();
        }

        return account;
    }

    /**
     * @return Never {@code null}.
     */
    public String getSerial() {
        return serial;
    }

    /**
     * @return Never {@code null}.
     */
    public String getInternalDomain() {
        return internalDomain;
    }

    public String getSubDomain() {
        return subDomain;
    }

    /**
     * @return Never blank. Immutable.
     */
    public Set<BeamCloud> getClouds() {
        return clouds;
    }

    /**
     * @return Never {@code null}.
     */
    public BeamStorage getStorage() {
        if (storage == null) {
            for (Class<? extends BeamStorageCreator> creatorClass : Beam.getReflections().getSubTypesOf(BeamStorageCreator.class)) {
                try {
                    BeamStorageCreator creator = creatorClass.newInstance();

                    try {
                        BeamStorage storage = creator.createStorage(this);

                        if (storage != null) {
                            this.storage = storage;

                            return storage;
                        }

                    } catch (Exception error) {
                        LOGGER.debug(String.format(
                                "Can't create a storage using [%s]!",
                                creatorClass.getName()),
                                error);
                    }

                } catch (IllegalAccessException | InstantiationException error) {
                    LOGGER.warn(String.format(
                            "Can't create a storage of [%s]!",
                            creatorClass.getName()),
                            error);
                }
            }

            throw new BeamException("Can't find the storage!");
        }

        return storage;
    }

    public void startAudit() {
        AUDITORS = new HashSet<>();
        for (Class<? extends BeamAuditor> c : Beam.getReflections().getSubTypesOf(BeamAuditor.class)) {
            if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
                continue;
            }

            try {
                BeamAuditor auditor = c.newInstance();
                AUDITORS.add(auditor);
            } catch (Exception error) {
                System.out.println("Error auditing!");
                error.printStackTrace();
            }
        }
    }

    public void finishAudit(boolean success) {
        if (finished) {
            return;
        }

        finished = true;

        AUDIT_STREAM.finishAuditing();

        Map<String, Object> log = new ImmutableMap.Builder<String, Object>().
                put("accountName", getAccount()).
                put("projectName", getProject()).
                put("environment", getEnvironment()).
                put("serial", getSerial()).
                put("commandArguments", AUDIT_COMMAND_ARGUMENTS).
                build();

        for (BeamAuditor auditor : AUDITORS) {
            try {
                auditor.finish(log, success);
            } catch (Exception error) {
                error.printStackTrace();
            }
        }
    }

    public static Map<String, Object> callApi(String method, String endpoint, Map<String, String> headers, Map<String, String> parameters, String payload) throws Exception {
        ErrorUtils.errorIfBlank(method, "method");
        ErrorUtils.errorIfBlank(endpoint, "endpoint");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            java.lang.reflect.Method httpMethod;

            try {
                httpMethod = RequestBuilder.class.getMethod(method);
            } catch (NoSuchMethodException e) {
                throw new BeamException(e.getMessage());
            }

            RequestBuilder requestBuilder;
            try {
                requestBuilder = (RequestBuilder) httpMethod.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new BeamException(e.getMessage());
            }

            requestBuilder.setUri(endpoint);
            for (String headerKey: headers.keySet()) {
                requestBuilder.addHeader(headerKey, headers.get(headerKey));
            }

            for (String parameterKey : parameters.keySet()) {
                requestBuilder.addParameter(parameterKey, parameters.get(parameterKey));
            }

            if (!ObjectUtils.isBlank(payload)) {
                requestBuilder.setEntity(new StringEntity(payload));
            }

            HttpUriRequest request = requestBuilder.build();

            try (CloseableHttpResponse response = client.execute(request)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) ObjectUtils.fromJson(
                        EntityUtils.toString(response.getEntity(), Charsets.UTF_8));

                return responseMap;
            }
        }
    }
}
