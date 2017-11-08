package beam.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.psddev.dari.util.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import beam.utils.IPv4;
import com.google.common.base.Throwables;

public class RootConfig extends Config {

    private File network;
    private List<LayerConfig> layers;
    private String alias;

    private transient String environment;
    private transient NetworkConfig networkConfig;

    public File getNetwork() {
        return network;
    }

    public void setNetwork(File network) {
        this.network = network;
    }

    public List<LayerConfig> getLayers() {
        if (layers == null) {
            layers = new ArrayList<>();
        }
        return layers;
    }

    public void setLayers(List<LayerConfig> layers) {
        this.layers = layers;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public NetworkConfig getNetworkConfig() {
        if (networkConfig != null) {
            return networkConfig;

        } else {
            File networkFile = getNetwork();

            if (networkFile != null) {
                Constructor networkConfigConstructor = new ConfigConstructor(NetworkConfig.class, networkFile);
                Yaml networkConfigYaml = new Yaml(networkConfigConstructor);

                try {
                    return (NetworkConfig) networkConfigYaml.load(new FileInputStream(networkFile));

                } catch (IOException error) {
                    throw Throwables.propagate(error);
                }

            } else {
                return null;
            }
        }
    }

    public void setNetworkConfig(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

    /**
     * Returns the layer config associated with the given {@code name}.
     *
     * @param name If {@code null}, returns {@code null}.
     * @return May be {@code null}.
     */
    public LayerConfig getLayerByName(String name) {
        for (LayerConfig layer : getLayers()) {
            if (layer.getName().equals(name)) {
                return layer;
            }
        }

        return null;
    }

    public static String getLocalHostIp(String targetInterface) {
        String address = null;

        try {
            // Look for a network interface with an private IP.
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = iface.getInetAddresses();

                if (targetInterface != null) {
                    if (!iface.getName().equals(targetInterface)) {
                        continue;
                    }

                } else {
                    if (!iface.getName().startsWith("eth")) {
                        continue;
                    }
                }

                while (addresses.hasMoreElements()) {
                    InetAddress add = addresses.nextElement();

                    if (add instanceof Inet6Address) {
                        continue;
                    }

                    IPv4 ipv4 = new IPv4(add.getHostAddress(), "255.255.255.0");
                    if (ipv4.isPrivateNetwork()) {
                        address = add.getHostAddress();
                        break;
                    }
                }

                if (address != null) {
                    break;
                }
            }

        } catch (Exception error) {
        }

        return address;
    }

    private static final Executor executor = Executors.newFixedThreadPool(4);
    private static final LoadingCache<String, Boolean> gateways = CacheBuilder.newBuilder()
            .maximumSize(100)
            .refreshAfterWrite(1, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, Boolean>() {
                        public Boolean load(String host) {
                            try {
                                InetAddress beamAddress = InetAddress.getByName(host);
                                return beamAddress.isReachable(1000);
                            } catch (IOException ioe) {
                                return false;
                            }
                        }

                        @Override
                        public ListenableFuture<Boolean> reload(final String host, Boolean oldValue) throws Exception {
                            ListenableFutureTask<Boolean> task = ListenableFutureTask.create(new Callable<Boolean>() {
                                public Boolean call() {
                                    Thread.currentThread().setName("gateway-cache-loader");

                                    return load(host);
                                }
                            });
                            executor.execute(task);
                            return task;
                        }
                    });

    public static List<String> getBeamServer(String serverDNS, String targetInterface) {

        List<String> serverList = new ArrayList<>();

        try {
            String address = getLocalHostIp(targetInterface);

            if (address == null) {
                return null;
            }

            IPv4 sourceSubnet = new IPv4(address, "255.255.255.0");

            InetAddress[] candidates = null;

            if (!ObjectUtils.isBlank(serverDNS)) {
                try {
                    candidates = InetAddress.getAllByName(serverDNS);
                } catch (Exception error) {

                }
            }

            if (ObjectUtils.isBlank(candidates)) {
                // Check local class C for a server.
                IPv4 ipv4 = new IPv4(address, "255.255.255.0");

                String beamServer = ipv4.getNthIP(10);
                String nearestServer = beamServer;

                if (gateways.get(beamServer)) {
                    serverList.add(beamServer);
                }

                // Check first ten subnets in the current class B for a server.
                ipv4 = new IPv4(address, "255.255.240.0");
                int base = 0;
                for (int i = 0; i < 10; i++) {
                    beamServer = ipv4.getNthIP(10 + base);

                    if (gateways.get(beamServer) && !beamServer.equals(nearestServer)) {
                        serverList.add(beamServer);
                    }

                    base += 256;
                }

            } else {
                String nearestServer = null;

                for(InetAddress addr : candidates) {
                    // Check local class C for a server.
                    IPv4 targetSubnet = new IPv4(addr.getHostAddress(), "255.255.255.0");

                    if (sourceSubnet.getCIDR().equals(targetSubnet.getCIDR())) {
                        nearestServer = addr.getHostAddress();
                        if (gateways.get(nearestServer)) {
                            serverList.add(addr.getHostAddress());
                        }
                    }
                }

                for(InetAddress addr : candidates) {
                    if (gateways.get(addr.getHostAddress()) && !addr.getHostAddress().equals(nearestServer)) {
                        serverList.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ex) {
        }

        return serverList;
    }

}
