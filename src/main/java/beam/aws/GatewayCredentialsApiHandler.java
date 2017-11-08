package beam.aws;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Deque;
import java.util.Map;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

import beam.BeamCloud;
import beam.BeamRuntime;

import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;

public class GatewayCredentialsApiHandler implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws IOException, JSONException {
        SocketAddress address = exchange.getConnection().getPeerAddress();

        if (address instanceof InetSocketAddress) {
            InetAddress inetAddress = ((InetSocketAddress) address).getAddress();

            if (inetAddress != null && inetAddress.getHostAddress().startsWith("10.")) {

                try {

                for (BeamCloud c : BeamRuntime.getCurrentRuntime().getClouds()) {
                    if (c instanceof AWSCloud) {
                        AWSCloud cloud = (AWSCloud) c;
                        AWSSecurityTokenServiceClient client = new AWSSecurityTokenServiceClient(cloud.getProvider());
                        AssumeRoleRequest roleRequest = new AssumeRoleRequest();
                        Deque<String> userParam = exchange.getQueryParameters().get("user");
                        JSONObject instanceIdentity = new Resty(Resty.Option.timeout(500)).json("http://169.254.169.254/latest/dynamic/instance-identity/document").object();
                        String accountId = instanceIdentity.getString("accountId");
                        String roleArn = "arn:aws:iam::" + accountId + ":role/gateway";
                        String user = ObjectUtils.firstNonNull(userParam != null ? userParam.element() : null, "guest");
                        String policy = com.amazonaws.util.IOUtils.toString(getClass().getResourceAsStream("/readonly-role.json"));

                        roleRequest.setRoleArn(roleArn);
                        roleRequest.setRoleSessionName(user);
                        roleRequest.setPolicy(policy);
                        roleRequest.setDurationSeconds(900);

                        AssumeRoleResult roleResult = client.assumeRole(roleRequest);
                        Credentials credentials = roleResult.getCredentials();

                        Map<String, String> response = new CompactMap<>();

                        response.put("accessKey", credentials.getAccessKeyId());
                        response.put("secretKey", credentials.getSecretAccessKey());
                        response.put("sessionToken", credentials.getSessionToken());
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                        exchange.getResponseSender().send(ObjectUtils.toJson(response));
                    }
                }

                } catch (Exception error) {
                    error.printStackTrace();
                }
            }
        }
    }
}
