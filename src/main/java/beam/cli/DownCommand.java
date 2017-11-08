package beam.cli;

import beam.BeamInstance;
import beam.aws.EC2Instance;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.transform.MapEntry;
import io.airlift.command.Command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beam.BeamCloud;
import beam.aws.AWSCloud;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.psddev.dari.util.ObjectUtils;

@Command(name = "down")
public class DownCommand extends AbstractInstanceCommand implements AuditableCommand {

    @Override
    protected boolean isCacheOk() {
        return false;
    }

    @Override
    protected InstanceHandler getInstanceHandler() {
        return new InstanceHandler() {

            @Override
            public void last(BeamCloud cloud, List<BeamInstance> instances) throws Exception {
                if (!(cloud instanceof AWSCloud)) {
                    out.format("Not supported for @|blue %s|@ cloud yet!", cloud.getName());
                    out.flush();
                    return;
                }

                out.format("Looking for assets in @|blue %s|@ cloud...\n", cloud.getName());
                out.flush();

                Map<Region, Set<String>> instanceIdsByRegion = new HashMap<>();
                Map<String, Region> autoScaleGroups = new HashMap<>();

                if (!instances.isEmpty()) {
                    out.println("\nInstances:");
                    for (BeamInstance i : instances) {
                        String instanceId = i.getId();
                        String name = "defaultName";
                        boolean isAutoScaleInstance = false;
                        for (Map.Entry<String, String> tag : ((EC2Instance)i).getTags().entrySet()) {
                            if (tag.getKey().equals("Name")) {
                                name = tag.getValue();
                            }

                            if (tag.getKey().equals("aws:autoscaling:groupName")) {
                                if (!autoScaleGroups.keySet().contains(tag.getValue())) {
                                    autoScaleGroups.put(tag.getValue(), RegionUtils.getRegion(i.getRegion()));
                                }
                                isAutoScaleInstance = true;
                            }
                        }

                        if (!isAutoScaleInstance && InstanceStateName.Running.toString().equals(i.getState())) {
                            Set<String> instanceIds = instanceIdsByRegion.get(RegionUtils.getRegion(i.getRegion()));
                            if (instanceIds == null) {
                                instanceIds = new HashSet<>();
                                instanceIdsByRegion.put(RegionUtils.getRegion(i.getRegion()), instanceIds);
                            }

                            instanceIds.add(instanceId);

                            out.print("-");

                            if (!ObjectUtils.isBlank(name)) {
                                out.print(' ');
                                out.print(name);
                            }

                            out.format(" [%s] %s\n", RegionUtils.getRegion(i.getRegion()), instanceId);
                        }
                    }

                    if (!autoScaleGroups.isEmpty()) {
                        out.println("\nAuto scaling groups:");
                        for (String name : autoScaleGroups.keySet()) {
                            out.format("- %s\n", name);
                        }
                    }
                }

                if (autoScaleGroups.isEmpty() && instanceIdsByRegion.isEmpty()) {
                    out.format("\nNo running auto scaling groups or instances found, beam down terminate.\n");
                    out.flush();
                    return;
                }

                out.format("\nAre you sure you want to stop all auto scaling groups and instances in @|blue %s|@ cloud (y/N) ", cloud.getName());
                out.flush();

                BufferedReader confirmReader = new BufferedReader(new InputStreamReader(System.in));

                if ("y".equalsIgnoreCase(confirmReader.readLine())) {
                    setEverConfirmed(0);

                    out.println("");
                    out.flush();

                    AWSCloud awsCloud = (AWSCloud) cloud;

                    for (String name : autoScaleGroups.keySet()) {

                        out.format("Disabling auto scaling group %s...\n", name);
                        out.flush();

                        AmazonAutoScalingClient asClient = new AmazonAutoScalingClient(awsCloud.getProvider());
                        asClient.setRegion(autoScaleGroups.get(name));

                        UpdateAutoScalingGroupRequest uasgRequest = new UpdateAutoScalingGroupRequest();

                        uasgRequest.setAutoScalingGroupName(name);
                        uasgRequest.setMaxSize(0);
                        uasgRequest.setMinSize(0);
                        asClient.updateAutoScalingGroup(uasgRequest);
                    }

                    for (Region region : instanceIdsByRegion.keySet()) {
                        Set<String> instanceIds = instanceIdsByRegion.get(region);

                        if (!instanceIds.isEmpty()) {
                            out.format("Stopping instances %s in %s...\n", instanceIds, region.getName());
                            out.flush();

                            AmazonEC2Client ec2Client = new AmazonEC2Client(awsCloud.getProvider());
                            ec2Client.setRegion(region);
                            StopInstancesRequest siRequest = new StopInstancesRequest();

                            siRequest.setInstanceIds(instanceIds);
                            ec2Client.stopInstances(siRequest);
                        }
                    }
                }
            }
        };
    }
}
