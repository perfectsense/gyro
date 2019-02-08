package beam.aws.cloudfront;

import beam.core.diff.Diffable;

import java.util.List;

public class CloudFrontGeoRestriction extends Diffable {

    private String type;
    private List<String> restrictions;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<String> restrictions) {
        this.restrictions = restrictions;
    }

    @Override
    public String primaryKey() {
        return "geo-restriction";
    }

    @Override
    public String toDisplayString() {
        return "geo restriction";
    }
}
