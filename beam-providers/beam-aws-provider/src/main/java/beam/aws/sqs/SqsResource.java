package beam.aws.sqs;

import beam.aws.AwsCredentials;
import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;
import beam.lang.Credentials;
import beam.lang.Resource;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.JsonProcessor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * Example
 * -------
 *
 * .. code-block:: beam with the standard queue
 *
 *aws::sqs sqs-example996556
 *      name : "testRedrive3345itest12345"
 *      visibility-timeout : 400
 *      message-retention-period : 864000 (in seconds)
 *      maximum-message-size : 258048 (bytes)
 *      delay-seconds : 140
 *      receive-message-wait-time-seconds : 5
 *      kms-master-key-id : 23
 *      kms-data-key-reuse-period-seconds : 200
 *      policy-doc-path: 'policy.json'
 *      dead-letter-queue-name : "testRedrive3345itest123"
 *      max-receive-count : "5"
 *      account-no : 242040583208 (needed when dead letter queue name is provided instead of the dead letter target ARN)
 * end
 *
 *.. code-block:: beam with the fifo queue
 *
 * aws::sqs sqs-example2.fifo
 *      name : "testxyz"
 *      visibility-timeout : 400
 *      message-retention-period : 864000 (in seconds)
 *      maximum-message-size : 258048 (bytes)
 *      delay-seconds : 140
 *      receive-message-wait-time-seconds : 5
 *      kms-master-key-id : 23
 *      kms-data-key-reuse-period-seconds : 200
 *      policy-doc-path: 'policy.json'
 *      dead-letter-target-arn : "arn:aws:sqs:us-east-2:242040583208:testRedrive3345.fifo"
 *      max-receive-count : "5"
 *      content-based-deduplication : 'true'
 * end
 */

@ResourceName("sqs")
public class SqsResource extends AwsResource {
    private String name;
    private String queueUrl;
    private Integer visibilityTimeout;
    private Integer messageRetentionPeriod;
    private Integer delaySeconds;
    private Integer maximumMessageSize;
    private Integer receiveMessageWaitTimeSeconds;
    private String deadLetterQueueName;
    private String deadLetterTargetArn;
    private String maxReceiveCount;
    private String contentBasedDeduplication;
    private Integer kmsMasterKeyId;
    private Integer kmsDataKeyReusePeriodSeconds;
    private transient String policyDocPath;
    private String policy;
    private String accountNo;

    /**
     * Enables setting the name of the queue. The name of a FIFO queue must end with the .fifo suffix.
     * Example for standard queue : test123
     * Example for Fifo queue : test123.fifo
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Enable setting up the attributes for the queue. See `<https://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_GetQueueAttributes.html>`_.
     * Check default values for the attributes of the queue here `<https://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_SetQueueAttributes.html>`_.
     * Valid values :
     * delay-seconds : 0-900 seconds
     * visibility-timeout : 0-12 hrs
     * message-retention-period : 1 minute - 14 days
     * maximum-message-size : 1 KiB - 256 Kib
     * receive-message-wait-time-seconds : 0-20 seconds
     */

    @ResourceDiffProperty(updatable = true)
    public Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getMessageRetentionPeriod() {
        return messageRetentionPeriod;
    }

    public void setMessageRetentionPeriod(Integer messageRetentionPeriod) {
        this.messageRetentionPeriod = messageRetentionPeriod;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getMaximumMessageSize() {
        return maximumMessageSize;
    }

    public void setMaximumMessageSize(Integer maximumMessageSize) {
        this.maximumMessageSize = maximumMessageSize;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getReceiveMessageWaitTimeSeconds() {
        return receiveMessageWaitTimeSeconds;
    }

    public void setReceiveMessageWaitTimeSeconds(Integer receiveMessageWaitTimeSeconds) {
        this.receiveMessageWaitTimeSeconds = receiveMessageWaitTimeSeconds;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    /**
     * Enables moving messages to the dead letter queue. See `<https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public String getDeadLetterTargetArn() {
        return deadLetterTargetArn;
    }

    public void setDeadLetterTargetArn(String deadLetterTargetArn) {
        this.deadLetterTargetArn = deadLetterTargetArn;
    }

    @ResourceDiffProperty(updatable = true)
    public String getMaxReceiveCount() {
        return maxReceiveCount;
    }

    public void setMaxReceiveCount(String maxReceiveCount) {
        this.maxReceiveCount = maxReceiveCount;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDeadLetterQueueName() {
        return deadLetterQueueName;
    }

    public void setDeadLetterQueueName(String deadLetterQueueName) {
        this.deadLetterQueueName = deadLetterQueueName;
    }

    /**
     * Enables content-based deduplication for FIFO Queues. See `<https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html#FIFO-queues-exactly-once-processing>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public String getContentBasedDeduplication() {
        return contentBasedDeduplication;
    }

    public void setContentBasedDeduplication(String contentBasedDeduplication) {
        this.contentBasedDeduplication = contentBasedDeduplication;
    }

    /**
     * Enables server side encryption on queues. See `<https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-server-side-encryption.html#sqs-sse-key-terms>`_.
     */
    @ResourceDiffProperty(updatable = true)
    public Integer getKmsMasterKeyId() {
        return kmsMasterKeyId;
    }

    public void setKmsMasterKeyId(Integer kmsMasterKeyId) {
        this.kmsMasterKeyId = kmsMasterKeyId;
    }

    @ResourceDiffProperty(updatable = true)
    public Integer getKmsDataKeyReusePeriodSeconds() {
        return kmsDataKeyReusePeriodSeconds;
    }

    public void setKmsDataKeyReusePeriodSeconds(Integer kmsDataKeyReusePeriodSeconds) {
        this.kmsDataKeyReusePeriodSeconds = kmsDataKeyReusePeriodSeconds;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    /**
     * Enables setting up the valid IAM policies and permissions for the queue.
     */
    @ResourceDiffProperty(updatable = true)
    public String getPolicyDocPath() {
        return policyDocPath;
    }

    public void setPolicyDocPath(String policyDocPath) {
        this.policyDocPath = policyDocPath;
        setPolicyFromPath();
    }

    /**
     * Need to change when code changes
     * This method removes all the spaces from the policy content even from the values, the better way to do it would be by using the Jackson's API
     */
    @ResourceDiffProperty(updatable = true)
    public String getPolicy() {
        return policy != null ? policy.replaceAll(System.lineSeparator(), " ").replaceAll("\t", " ").trim().replaceAll(" ", "") : policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    @Override
    public boolean refresh() {
        SqsClient client = createClient(SqsClient.class);

        ListQueuesResponse response1 = client.listQueues(r -> r.queueNamePrefix(getName()));
        if (!response1.queueUrls().isEmpty()) {
            setQueueUrl(response1.queueUrls() != null ? response1.queueUrls().get(0) : null);
        }

        if (!getQueueUrl().isEmpty()) {

            GetQueueAttributesResponse response = client.getQueueAttributes(r -> r.queueUrl(getQueueUrl())
                .attributeNames(QueueAttributeName.ALL));

            setVisibilityTimeout(Integer.valueOf(response.attributes()
                .get(QueueAttributeName.VISIBILITY_TIMEOUT)));

            setMessageRetentionPeriod(Integer.valueOf(response.attributes()
                .get(QueueAttributeName.MESSAGE_RETENTION_PERIOD)));

            setDelaySeconds(Integer.valueOf(response.attributes()
                .get(QueueAttributeName.DELAY_SECONDS)));

            setMaximumMessageSize(Integer.valueOf(response.attributes()
                .get(QueueAttributeName.MAXIMUM_MESSAGE_SIZE)));

            setReceiveMessageWaitTimeSeconds(Integer.valueOf(response.attributes()
                .get(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS)));

            if (response.attributes().get(QueueAttributeName.POLICY) != null) {
                setPolicy(response.attributes().get(QueueAttributeName.POLICY));
            }

            if (response.attributes().get(QueueAttributeName.CONTENT_BASED_DEDUPLICATION) != null) {
                setContentBasedDeduplication(response.attributes()
                    .get(QueueAttributeName.CONTENT_BASED_DEDUPLICATION));
            }

            if (response.attributes().get(QueueAttributeName.REDRIVE_POLICY) != null) {
                String policy = response.attributes().get(QueueAttributeName.REDRIVE_POLICY);
                JsonProcessor obj = new JsonProcessor();
                Object parse = obj.parse(policy);

                setDeadLetterTargetArn(((CompactMap) parse).get("deadLetterTargetArn").toString());
                setMaxReceiveCount(((CompactMap) parse).get("maxReceiveCount").toString());
            }

            if (response.attributes().get(QueueAttributeName.KMS_MASTER_KEY_ID) != null) {
                setKmsMasterKeyId(Integer.valueOf(response.attributes().get(QueueAttributeName.KMS_MASTER_KEY_ID)));

                if (response.attributes().get(QueueAttributeName.KMS_DATA_KEY_REUSE_PERIOD_SECONDS) != null) {
                    setKmsDataKeyReusePeriodSeconds(Integer.valueOf(response.attributes()
                        .get(QueueAttributeName.KMS_DATA_KEY_REUSE_PERIOD_SECONDS)));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void create() {
        SqsClient client = createClient(SqsClient.class);

        ListQueuesResponse listName = client.listQueues(r -> r.queueNamePrefix(getName()));

        if (listName.queueUrls().isEmpty()) {
            createQueue(client);
        } else {
            throw new BeamException("A queue with the name " + getName() + " already exists.");
        }
    }

    private void createQueue(SqsClient client) {
        Map<QueueAttributeName, String> attributeMap = new HashMap<>();

        if (name.substring(name.lastIndexOf(".") + 1).equals("fifo")) {
            attributeMap.put(QueueAttributeName.FIFO_QUEUE, "true");
        }

        addAttributeEntry(attributeMap, QueueAttributeName.VISIBILITY_TIMEOUT, getVisibilityTimeout());
        addAttributeEntry(attributeMap, QueueAttributeName.DELAY_SECONDS, getDelaySeconds());
        addAttributeEntry(attributeMap, QueueAttributeName.MESSAGE_RETENTION_PERIOD, getMessageRetentionPeriod());
        addAttributeEntry(attributeMap, QueueAttributeName.MAXIMUM_MESSAGE_SIZE, getMaximumMessageSize());
        addAttributeEntry(attributeMap, QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, getReceiveMessageWaitTimeSeconds());
        addAttributeEntry(attributeMap, QueueAttributeName.CONTENT_BASED_DEDUPLICATION, getContentBasedDeduplication());
        addAttributeEntry(attributeMap, QueueAttributeName.KMS_MASTER_KEY_ID, getKmsMasterKeyId());
        addAttributeEntry(attributeMap, QueueAttributeName.KMS_DATA_KEY_REUSE_PERIOD_SECONDS, getKmsDataKeyReusePeriodSeconds());

        if (getDeadLetterTargetArn() != null && getMaxReceiveCount() != null) {

            String policy = String.format("{\"maxReceiveCount\": \"%s\", \"deadLetterTargetArn\": \"%s\"}",
                getMaxReceiveCount(), getDeadLetterTargetArn());

            attributeMap.put(QueueAttributeName.REDRIVE_POLICY, policy);

        } else if (getDeadLetterQueueName() != null && getMaxReceiveCount() != null) {

            String policy = String.format("{\"maxReceiveCount\": \"%s\", \"deadLetterTargetArn\": \"%s\"}",
                getMaxReceiveCount(), createQueueArn(deadLetterQueueName));

            attributeMap.put(QueueAttributeName.REDRIVE_POLICY, policy);
        }

        if (policyDocPath != null) {
            setPolicyFromPath();
            attributeMap.put(QueueAttributeName.POLICY, getPolicy());
        }

        client.createQueue(r -> r.queueName(getName()).attributes(attributeMap));
        setQueueUrl(client.createQueue(r -> r.queueName(getName()).attributes(attributeMap)).queueUrl());
    }

    @Override
    public void update(Resource current, Set<String> changedProperties) {

        Map<QueueAttributeName, String> attributeUpdate = new HashMap<>();

        attributeUpdate.put(QueueAttributeName.VISIBILITY_TIMEOUT, getVisibilityTimeout().toString());
        attributeUpdate.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, getMessageRetentionPeriod().toString());
        attributeUpdate.put(QueueAttributeName.DELAY_SECONDS, getDelaySeconds().toString());
        attributeUpdate.put(QueueAttributeName.MAXIMUM_MESSAGE_SIZE, getMaximumMessageSize().toString());
        attributeUpdate.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, getReceiveMessageWaitTimeSeconds().toString());
        attributeUpdate.put(QueueAttributeName.KMS_MASTER_KEY_ID, getKmsMasterKeyId().toString());
        attributeUpdate.put(QueueAttributeName.KMS_DATA_KEY_REUSE_PERIOD_SECONDS, getKmsDataKeyReusePeriodSeconds().toString());
        attributeUpdate.put(QueueAttributeName.POLICY, getPolicy());
        attributeUpdate.put(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, getContentBasedDeduplication());

        SqsClient client = createClient(SqsClient.class);

        client.setQueueAttributes(r -> r.attributes(attributeUpdate).queueUrl(getQueueUrl()));
    }

    @Override
    public void delete() {
        SqsClient client = createClient(SqsClient.class);

        client.deleteQueue(r -> r.queueUrl(getQueueUrl()));

    }

    @Override
    public String toDisplayString() {
        return getName();
    }

    private void setPolicyFromPath() {
        try {
            String path = "beam-providers/beam-aws-provider/examples/sqs/" + policyDocPath;
            setPolicy(new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8));
        } catch (IOException ioex) {
            throw new BeamException(MessageFormat
                .format("Queue - {0} policy error. Unable to read policy from path [{1}]", getName(), getPolicyDocPath()));
        }
    }

    /**
     * Adding the account number in the config is a temporary fix, need to change when code changes
     */
    private String createQueueArn(String deadLetterQueueName) {
        Credentials resourceCredentials = this.getResourceCredentials();
        AwsCredentials awsCredentials = (AwsCredentials) resourceCredentials;

        return "arn:aws:sqs:" + awsCredentials.getRegion() + ":" + getAccountNo() + ":" + deadLetterQueueName;
    }

    private void addAttributeEntry(Map<QueueAttributeName, String> request, QueueAttributeName name, Object value) {
        if (value != null) {
            request.put(name, value.toString());
        }
    }
}
