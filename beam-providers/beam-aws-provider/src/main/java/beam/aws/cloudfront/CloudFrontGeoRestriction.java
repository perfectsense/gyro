package beam.aws.cloudfront;

import beam.core.diff.Diffable;
import software.amazon.awssdk.services.cloudfront.model.Restrictions;

import java.util.ArrayList;
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
        if (restrictions == null) {
            restrictions = new ArrayList<>();
        }

        return restrictions;
    }

    public void setRestrictions(List<String> restrictions) {
        this.restrictions = restrictions;
    }

    public Restrictions toRestrictions() {
        return Restrictions.builder()
            .geoRestriction(r -> r.restrictionType(getType())
                .items(getRestrictions())
                .quantity(getRestrictions().size())).build();
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
