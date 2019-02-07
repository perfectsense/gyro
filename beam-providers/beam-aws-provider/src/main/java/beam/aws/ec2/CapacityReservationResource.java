package beam.aws.ec2;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import com.psddev.dari.util.ObjectUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CapacityReservation;
import software.amazon.awssdk.services.ec2.model.CreateCapacityReservationResponse;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * Creates a Capacity Reservation.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::capacity-reservation capacity-reservation-example
 *         availability-zone: "us-west-2a"
 *         ebs-optimized: false
 *         end-date-type: "unlimited"
 *         ephemeral-storage: false
 *         instance-count: 2
 *         instance-match-criteria: "targeted"
 *         instance-platform: "Linux/UNIX"
 *         instance-type: "t2.micro"
 *         tenancy: "default"
 *     end
 */
@ResourceName("capacity-reservation")
public class CapacityReservationResource extends Ec2TaggableResource<CapacityReservation> {

    private String capacityReservationId;
    private String availabilityZone;
    private Boolean ebsOptimized;
    private Date endDate;
    private String endDateType;
    private Boolean ephemeralStorage;
    private String instanceMatchCriteria;
    private String instancePlatform;
    private String instanceType;
    private String tenancy;
    private Integer instanceCount;
    private Integer availableInstanceCount;
    private Date createDate;

    public String getCapacityReservationId() {
        return capacityReservationId;
    }

    public void setCapacityReservationId(String capacityReservationId) {
        this.capacityReservationId = capacityReservationId;
    }

    /**
     * The Availability Zone in which to create the Capacity Reservation. (Required)
     */
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    /**
     * Indicates whether the Capacity Reservation supports EBS-optimized instances. (Required)
     */
    public Boolean getEbsOptimized() {
        return ebsOptimized;
    }

    public void setEbsOptimized(Boolean ebsOptimized) {
        this.ebsOptimized = ebsOptimized;
    }

    /**
     * The date and time at which the Capacity Reservation expires. Required if 'end-date-type' set to 'limited'.
     */
    @ResourceDiffProperty(updatable = true)
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Indicates the way in which the Capacity Reservation ends. Valid values [ 'unlimited', 'limited' ]. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public String getEndDateType() {
        return endDateType != null ? endDateType.toLowerCase() : null;
    }

    public void setEndDateType(String endDateType) {
        this.endDateType = endDateType;
    }

    /**
     * Indicates whether the Capacity Reservation supports instances with temporary, block-level storage. (Required)
     */
    public Boolean getEphemeralStorage() {
        return ephemeralStorage;
    }

    public void setEphemeralStorage(Boolean ephemeralStorage) {
        this.ephemeralStorage = ephemeralStorage;
    }

    /**
     * Indicates the type of instance launches that the Capacity Reservation accepts. Valid values [ 'open', 'targeted' ]. (Required)
     */
    public String getInstanceMatchCriteria() {
        return instanceMatchCriteria != null ? instanceMatchCriteria.toLowerCase() : null;
    }

    public void setInstanceMatchCriteria(String instanceMatchCriteria) {
        this.instanceMatchCriteria = instanceMatchCriteria;
    }

    /**
     * The type of operating system for which to reserve capacity. (Required)
     */
    public String getInstancePlatform() {
        return instancePlatform;
    }

    public void setInstancePlatform(String instancePlatform) {
        this.instancePlatform = instancePlatform;
    }

    /**
     * The instance type for which to reserve capacity. (Required)
     */
    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    /**
     * Indicates the tenancy of the Capacity Reservation. Valid values [ 'default', 'dedicated' ]. (Required)
     */
    public String getTenancy() {
        return tenancy != null ? tenancy.toLowerCase() : null;
    }

    public void setTenancy(String tenancy) {
        this.tenancy = tenancy;
    }

    /**
     * The number of instances for which to reserve capacity. (Required)
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(Integer instanceCount) {
        this.instanceCount = instanceCount;
    }

    public Integer getAvailableInstanceCount() {
        return availableInstanceCount;
    }

    public void setAvailableInstanceCount(Integer availableInstanceCount) {
        this.availableInstanceCount = availableInstanceCount;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Override
    protected String getId() {
        return getCapacityReservationId();
    }

    @Override
    public boolean doRefresh() {

        Ec2Client client = createClient(Ec2Client.class);

        if (ObjectUtils.isBlank(getCapacityReservationId())) {
            throw new BeamException("capacity-reservation-id is missing, unable to load capacity reservation.");
        }

        try {
            DescribeCapacityReservationsResponse response = client.describeCapacityReservations(
                r -> r.capacityReservationIds(Collections.singleton(getCapacityReservationId()))
            );

            CapacityReservation capacityReservation = response.capacityReservations().get(0);

            setAvailabilityZone(capacityReservation.availabilityZone());
            setEbsOptimized(capacityReservation.ebsOptimized());
            setEndDate(capacityReservation.endDate() != null ? Date.from(capacityReservation.endDate()) : null);
            setEndDateType(capacityReservation.endDateTypeAsString());
            setEphemeralStorage(capacityReservation.ephemeralStorage());
            setInstanceMatchCriteria(capacityReservation.instanceMatchCriteriaAsString());
            setInstancePlatform(capacityReservation.instancePlatformAsString());
            setInstanceType(capacityReservation.instanceType());
            setTenancy(capacityReservation.tenancyAsString());
            setAvailableInstanceCount(capacityReservation.availableInstanceCount());
            setCreateDate(capacityReservation.createDate() != null ? Date.from(capacityReservation.createDate()) : null);
            setInstanceCount(capacityReservation.totalInstanceCount());
        } catch (Ec2Exception ex) {
            if (ex.getLocalizedMessage().contains("does not exist")) {
                return false;
            }

            throw ex;
        }

        return true;
    }

    @Override
    public void doCreate() {
        Ec2Client client = createClient(Ec2Client.class);

        validate();

        CreateCapacityReservationResponse response = client.createCapacityReservation(
            r -> r.availabilityZone(getAvailabilityZone())
                .ebsOptimized(getEbsOptimized())
                .endDate(getEndDate() != null ? getEndDate().toInstant() : null)
                .endDateType(getEndDateType())
                .ephemeralStorage(getEphemeralStorage())
                .instanceCount(getInstanceCount())
                .instanceMatchCriteria(getInstanceMatchCriteria())
                .instancePlatform(getInstancePlatform())
                .instanceType(getInstanceType())
                .tenancy(getTenancy())
        );

        CapacityReservation capacityReservation = response.capacityReservation();

        setCapacityReservationId(capacityReservation.capacityReservationId());
        setAvailableInstanceCount(capacityReservation.availableInstanceCount());
        setCreateDate(capacityReservation.createDate() != null ? Date.from(capacityReservation.createDate()) : null);

    }

    @Override
    public void doUpdate(AwsResource config, Set<String> changedProperties) {
        Ec2Client client = createClient(Ec2Client.class);

        validate();

        client.modifyCapacityReservation(
            r -> r.capacityReservationId(getCapacityReservationId())
                .endDate(getEndDate() != null ? getEndDate().toInstant() : null)
                .endDateType(getEndDateType())
                .instanceCount(getInstanceCount())
        );
    }

    @Override
    public void delete() {
        Ec2Client client = createClient(Ec2Client.class);

        client.cancelCapacityReservation(
            r -> r.capacityReservationId(getCapacityReservationId())
        );
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("capacity reservation");

        if (!ObjectUtils.isBlank(getCapacityReservationId())) {
            sb.append(" - ").append(getCapacityReservationId());
        }

        return sb.toString();
    }

    private void validate() {
        if (getEndDateType() == null || (!getEndDateType().equals("unlimited") && !getEndDateType().equals("limited"))) {
            throw new BeamException("The value - (" + getEndDateType() + ") is invalid for parameter 'end-date-type'."
                + "Valid values [ 'unlimited', 'limited' ]");
        }

        if (getEndDateType().equals("unlimited") && !ObjectUtils.isBlank(getEndDate())) {
            throw new BeamException("The value - (" + getEndDate() + ") is invalid for parameter 'end-date' "
                + "when param 'end-date-type' is set to 'unlimited'.");
        }

        if (getEndDateType().equals("limited") && !ObjectUtils.isBlank(getEndDate())) {
            throw new BeamException("The value - (" + getEndDate() + ") is mandatory for parameter 'end-date' "
                + "when param 'end-date-type' is set to 'limited'.");
        }

        if (getInstanceMatchCriteria() == null || (!getInstanceMatchCriteria().equals("open") && !getInstanceMatchCriteria().equals("targeted"))) {
            throw new BeamException("The value - (" + getInstanceMatchCriteria() + ") is invalid for parameter 'instance-match-criteria'."
                + "Valid values [ 'open', 'targeted' ]");
        }

        if (getTenancy() == null || (!getTenancy().equals("default") && !getTenancy().equals("dedicated"))) {
            throw new BeamException("The value - (" + getTenancy() + ") is invalid for parameter 'tenancy'."
                + "Valid values [ 'default', 'dedicated' ]");
        }
    }
}
