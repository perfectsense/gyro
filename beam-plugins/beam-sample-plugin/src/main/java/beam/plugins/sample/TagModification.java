package beam.plugins.sample;

import beam.aws.ec2.Ec2TaggableResource;
import beam.core.diff.ResourceName;
import beam.lang.Modification;
import beam.lang.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ResourceName("tag-modification")
public class TagModification extends Modification {

    private Map<String, String> tags;

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public List<String> modifies() {
        return Arrays.asList("beam.aws.ec2.Ec2TaggableResource");
    }

    @Override
    public void modify(Resource resource) {
        if (getTags() != null) {
            Ec2TaggableResource taggableResource = (Ec2TaggableResource) resource;
            taggableResource.getTags().putAll(getTags());
        }
    }

}
