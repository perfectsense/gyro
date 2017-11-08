package beam.handlers;

import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceState {
    public static final String INSTANCE_STATE_FILE = "/etc/beam/beam-discovery.state";

    public static final String PRIMARY_STATE_FILE = "/etc/beam/beam-discovery.primary.state";

    private static final Map<String, ServiceInstance> INSTANCES = new ConcurrentHashMap<>();

    private static final Map<String, ServiceInstance.Mark> INSTANCE_MARKS = new ConcurrentHashMap<>();

    private static final Map<String, PrimaryData> SERVICE_PRIMARIES = new ConcurrentHashMap<>();

    private static final Map<String, Long> REPLICATION_STATUS = new ConcurrentHashMap<>();

    private static final Map<String, Long> SERVICE_MONITOR = new ConcurrentHashMap<>();

    private static final Map<String, Long> HOST_MONITOR = new ConcurrentHashMap<>();

    private static final Map<String, Long> REPLICATION_MONITOR = new ConcurrentHashMap<>();

    private static final Map<String, Long> REPLICATION_MASK = new ConcurrentHashMap<>();

    public static Map<String, ServiceInstance> getInstances() {
        return INSTANCES;
    }

    public static Map<String, ServiceInstance.Mark> getInstanceMarks() {
        return INSTANCE_MARKS;
    }

    public static Map<String, PrimaryData> getServicePrimaries() {
        return SERVICE_PRIMARIES;
    }

    public static List<ServiceInstance> getServiceInstances() {
        return new ArrayList(INSTANCES.values());
    }

    public static ServiceInstance getServiceInstance(String instanceId) {
        return INSTANCES.get(instanceId);
    }

    public static ServiceInstance.Mark getMark(String key) {
        return INSTANCE_MARKS.get(key);
    }

    public static PrimaryData getPrimaryData(String key) {
        return SERVICE_PRIMARIES.get(key);
    }

    public synchronized static void removeInstance(String instanceId) {
        INSTANCES.remove(instanceId);
    }

    public synchronized static void removeInstanceMark(String instanceId) {
        INSTANCE_MARKS.remove(instanceId);
    }

    public synchronized static void putAllInstances(Map<String, ServiceInstance> instances) {
        INSTANCES.putAll(instances);
    }

    public synchronized static void putAllMarks(Map<String, ServiceInstance.Mark> marksMap) {
        INSTANCE_MARKS.putAll(marksMap);
    }

    public synchronized static void putAllPrimaries(Map<String, PrimaryData> primariesMap) {
        SERVICE_PRIMARIES.putAll(primariesMap);
    }

    public synchronized static void setServiceInstance(String instanceId, ServiceInstance instance) {
        INSTANCES.put(instanceId, instance);
    }

    public synchronized static void markInstance(String instanceId, ServiceInstance.Mark mark) {
        INSTANCE_MARKS.put(instanceId, mark);
    }

    public synchronized static void setPrimaryData(String key, PrimaryData data) {
        SERVICE_PRIMARIES.put(key, data);
    }

    public synchronized static void writeInstances(Yaml yaml, FileWriter writer) throws Exception {
        writer.write(yaml.dump(INSTANCES));
    }

    public synchronized static void writePrimaries(Yaml yaml, FileWriter writer) throws Exception {
        writer.write(yaml.dump(SERVICE_PRIMARIES));
    }

    public static long getReplicationStatus(String senderIp) {
        return REPLICATION_STATUS.get(senderIp);
    }

    public synchronized static void setReplicationStatus(String senderIp, Long timeStamp) {
        REPLICATION_STATUS.put(senderIp, timeStamp);
    }

    public static Map<String, Long> getReplicationMonitor() {
        return REPLICATION_MONITOR;
    }

    public static Map<String, Long> getServiceMonitor() {
        updateServiceMonitor();
        return SERVICE_MONITOR;
    }

    public static Map<String, Long> getHostMonitor() {
        updateHostMonitor();
        return HOST_MONITOR;
    }

    public static boolean getReplicationMask(String senderIp) {
        if (REPLICATION_MASK.get(senderIp) == null) {
            REPLICATION_MASK.put(senderIp, 0l);
        }

        return REPLICATION_MASK.get(senderIp) != 0l;
    }

    public synchronized static void addPropagation(String senderIp) {
        long count = REPLICATION_MASK.get(senderIp);
        REPLICATION_MASK.put(senderIp, count + 1);
    }

    public synchronized static void removePropagation(String senderIp) {
        long count = REPLICATION_MASK.get(senderIp);
        REPLICATION_MASK.put(senderIp, count - 1);
    }

    public static long getPropagationCounter(String senderIp) {
        return REPLICATION_MASK.get(senderIp);
    }

    public synchronized static void incrementReplication(String privateIp) {
        if (!REPLICATION_MONITOR.containsKey(privateIp)) {
            REPLICATION_MONITOR.put(privateIp, 0l);
        }

        REPLICATION_MONITOR.put(privateIp, REPLICATION_MONITOR.get(privateIp) + 1);
    }

    public synchronized static void updateServiceMonitor() {
        SERVICE_MONITOR.clear();

        for (String id : ServiceState.getInstances().keySet()) {
            ServiceInstance instance = ServiceState.getServiceInstance(id);

            for (ServiceInstance.Service service : instance.getServiceInfo()) {
                String name = service.getName();

                if (!SERVICE_MONITOR.containsKey(name)) {
                    SERVICE_MONITOR.put(name, 0l);
                }

                if (instance.isOk() && service.isOk()) {
                    SERVICE_MONITOR.put(name, SERVICE_MONITOR.get(name) + 1);

                }
            }
        }
    }

    public synchronized static void updateHostMonitor() {
        HOST_MONITOR.clear();
        HOST_MONITOR.put("host", 0l);

        for (String id : ServiceState.getInstances().keySet()) {
            ServiceInstance instance = ServiceState.getServiceInstance(id);

            if (instance.isOk() && !instance.getServiceInfo().isEmpty()) {
                HOST_MONITOR.put("host", HOST_MONITOR.get("host") + 1);
            }
        }
    }

    public static Map<String, Object> getMonitor() {
        Map<String, Object> monitor = new ConcurrentHashMap<>();
        monitor.put("SERVICE", getServiceMonitor());
        monitor.put("HOST", getHostMonitor());
        monitor.put("REPLICATION", getReplicationMonitor());
        monitor.put("PRIMARY", getServicePrimaries());

        return monitor;
    }

    public static Set<String> getPinnedServicesForInstance(String id) {
        Set<String> pinnedServices = new HashSet<String>();
        Pattern serviceParsePattern = Pattern.compile("service-(?<service>.*?)\\.primary-");
        Matcher serviceParseMatcher = null;
        for (String key : SERVICE_PRIMARIES.keySet()) {
            PrimaryData currentPrimaryData = ServiceState.getPrimaryData(key);
            if (currentPrimaryData.getInstanceId().equals(id)) {
                serviceParseMatcher = serviceParsePattern.matcher(key);
                if (serviceParseMatcher.find() && !pinnedServices.contains(serviceParseMatcher.group("service"))) {
                    pinnedServices.add(serviceParseMatcher.group("service"));
                }
            }
        }
        return pinnedServices;
    }

    public static class PrimaryData {
        private String instanceId;
        private long timeStamp;

        public PrimaryData(String instanceId, long timeStamp) {
            this.instanceId = instanceId;
            this.timeStamp = timeStamp;
        }

        public PrimaryData() {
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }
    }
}

