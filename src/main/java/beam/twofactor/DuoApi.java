package beam.twofactor;

import beam.BeamConfig;
import beam.enterprise.EnterpriseApi;
import com.psddev.dari.util.ObjectUtils;
import org.apache.http.message.BasicNameValuePair;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@TwoFactorProviderName("duo")
public class DuoApi extends TwoFactorProvider {

    @Override
    public void handleNeeds2FA(Map<String, Object> authMap) {
        List<Map<String, Object>> devices = null;

        if (authMap.get("devices") != null) {
            devices = (List<Map<String, Object>>) authMap.get("devices");
        } else {
            System.out.println("\nNo two-factor devices configured.");
            System.exit(1);
        }

        String duoPush = BeamConfig.get(String.class, "duoPush", "none");
        if ("auto".equals(duoPush)) {
            authAuto();
            return;
        } else if ("yubikey".equals(duoPush)) {
            for (Map device : devices) {
                String name = ObjectUtils.to(String.class, device.get("name"));
                if (name.startsWith("Yubikey")) {
                    displayTokenDevice(device);
                    return;
                }
            }
        }

        System.out.print("\nAccess to Beam Enterprise requires two-factor authentication.\n");

        if (devices.size() == 1) {
            displayDevice(devices.get(0));
        } else {
            int i = 1;
            System.out.println("\nSelect which device you would like to use for your second factor:\n");
            for (Map device : devices) {
                if (device.get("displayName") != null) {
                    System.out.println(" " + i + ". " + device.get("displayName"));
                    i++;
                } else if (device.get("name") != null) {
                    String name = ObjectUtils.to(String.class, device.get("name"));
                    if (name.startsWith("Yubikey")) {
                        name = name.replace("[", " - ").replace("]", "");
                        System.out.println(" " + i + ". " + name);
                        i++;
                    }
                }
            }

            Console console = System.console();

            Integer choice = null;
            do {
                System.out.print("\nSelect device: ");

                choice = ObjectUtils.to(Integer.class, console.readLine());
            } while (choice == null);

            Map device = devices.get(choice - 1);
            displayDevice(device);
        }
    }

    @Override
    public void handleNeeds2FAEnrollment(Map<String, Object> authMap) {
        String enrollUrl = ObjectUtils.to(String.class, authMap.get("enrollUrl"));

        System.out.println("\nAccess to Beam Enterprise requires two-factor authentication.\n");
        System.out.print("Your account is not currently two-factor enabled. ");
        System.out.println("Please enroll at the following link:\n");

        System.out.println(enrollUrl);
        System.exit(0);
    }

    private void displayDevice(Map<String, Object> device) {
        String type = ObjectUtils.to(String.class, device.get("type"));

        switch (type) {
            case "phone" : displayPhoneDevice(device); break;
            case "token" : displayTokenDevice(device); break;
        }
    }

    private void displayTokenDevice(Map<String, Object> device) {
        String name = ObjectUtils.to(String.class, device.get("name"));

        if (name.startsWith("Yubikey")) {
            System.out.print("\nTap your Yubikey now...\n");

            Console console = System.console();
            String passcode = new String(console.readPassword());

            authPasscode(passcode);
        }
    }

    private void displayPhoneDevice(Map<String, Object> device) {
        String displayName = ObjectUtils.to(String.class, device.get("displayName"));
        String deviceId = ObjectUtils.to(String.class, device.get("device"));
        String nextCode = ObjectUtils.to(String.class, device.get("smsNextcode"));
        List<String> capabilities = ObjectUtils.to(List.class, device.get("capabilities"));

        boolean canUsePasscode = false;
        if (capabilities.contains("mobile_otp") && capabilities.size() > 1) {
            canUsePasscode = true;
            capabilities.remove("mobile_otp");
            System.out.println("\nEnter a passcode or select one of the following options: \n");
        } else if (capabilities.size() == 1) {
            auth(deviceId, capabilities.get(0));
        } else {
            System.out.println("\nSelect one of the following options:\n");
        }

        int i = 1;
        List<String> availableCapabilities = new ArrayList<>();
        for (String factor : capabilities) {
            switch (factor) {
                case "push" :
                    System.out.println(" " + i++ + ". " + "Duo Push to " + displayName);
                    availableCapabilities.add(factor);
                    break;

                case "sms" :
                    System.out.println(" " + i++ + ". " + "SMS passcodes to " + displayName);
                    availableCapabilities.add(factor);
                    break;

                case "phone" :
                    System.out.println(" " + i++ + ". " +"Phone call to " + displayName);
                    availableCapabilities.add(factor);
                    break;
            }
        }

        if (!ObjectUtils.isBlank(nextCode)) {
            System.out.println("\nThe next SMS passcode to use starts with: " + nextCode);
        }

        Console console = System.console();

        Integer choice = null;
        do {
            if (canUsePasscode) {
                System.out.print("\nEnter passcode or select option: ");
            } else {
                System.out.print("\nSelect option: ");
            }

            choice = ObjectUtils.to(Integer.class, console.readLine());
        } while (choice == null
                || (choice < 1 || (choice > availableCapabilities.size() && choice < 99999)));

        if (choice >= 1 && choice <= availableCapabilities.size()) {
            auth(deviceId, availableCapabilities.get(choice - 1));
        } else {
            authPasscode(choice.toString());
        }
    }

    private void authPasscode(String passcode) {
        try {
            Map<String, Object> authMap = EnterpriseApi.callUnauthenticated(
                    "authenticate_2fa",
                    new BasicNameValuePair("sessionId", getSessionId()),
                    new BasicNameValuePair("factor", "passcode"),
                    new BasicNameValuePair("passcode", passcode));

            String status = ObjectUtils.to(String.class, authMap.get("status"));
            if ("ok".equals(status)) {
                setSuccess(true);
            } else {
                setSuccess(false);
            }
        } catch (IOException ioe) {

        }
    }

    private void authAuto() {
        try {
            Map<String, Object> authMap = EnterpriseApi.callUnauthenticated(
                    "authenticate_2fa",
                    new BasicNameValuePair("sessionId", getSessionId()),
                    new BasicNameValuePair("factor", "auto"));

            String status = ObjectUtils.to(String.class, authMap.get("status"));
            if ("ok".equals(status)) {
                setSuccess(true);
            } else {
                setSuccess(false);
            }
        } catch (IOException ioe) {

        }
    }

    private void auth(String device, String factor) {
        try {
            Map<String, Object> authMap = EnterpriseApi.callUnauthenticated(
                    "authenticate_2fa",
                    new BasicNameValuePair("sessionId", getSessionId()),
                    new BasicNameValuePair("factor", factor),
                    new BasicNameValuePair("device", device));

            String status = ObjectUtils.to(String.class, authMap.get("status"));
            if ("ok".equals(status)) {
                setSuccess(true);
            } else {
                setSuccess(false);
            }
        } catch (IOException ioe) {

        }
    }
}
