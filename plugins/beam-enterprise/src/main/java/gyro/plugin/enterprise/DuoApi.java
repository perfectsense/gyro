package gyro.plugin.enterprise;

import gyro.core.BeamCore;
import com.psddev.dari.util.ObjectUtils;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DuoApi {

    private String sessionId;
    private boolean success = false;
    private EnterpriseApi api;
    private String duoPush;

    public DuoApi(EnterpriseApi api) {
        this.api = api;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public EnterpriseApi getApi() {
        return api;
    }

    public String getDuoPush() {
        if (duoPush == null) {
            return "none";
        }

        return duoPush;
    }

    public void setDuoPush(String duoPush) {
        this.duoPush = duoPush;
    }

    public void handleNeeds2FA(Map<String, Object> authMap) {
        List<Map<String, Object>> devices = null;

        if (authMap.get("devices") != null) {
            devices = (List<Map<String, Object>>) authMap.get("devices");
        } else {
            BeamCore.ui().write("\nNo two-factor devices configured.\n");
            System.exit(1);
        }

        if ("auto".equals(getDuoPush())) {
            authAuto();
            return;
        } else if ("yubikey".equals(getDuoPush())) {
            for (Map device : devices) {
                String name = ObjectUtils.to(String.class, device.get("name"));
                if (name.startsWith("Yubikey")) {
                    displayTokenDevice(device);
                    return;
                }
            }
        }

        BeamCore.ui().write("\nAccess to Beam Enterprise requires two-factor authentication.\n");

        if (devices.size() == 1) {
            displayDevice(devices.get(0));
        } else {
            int i = 1;
            BeamCore.ui().write("\nSelect which device you would like to use for your second factor:\n\n");
            for (Map device : devices) {
                if (device.get("displayName") != null) {
                    BeamCore.ui().write(" " + i + ". " + device.get("displayName") + "\n");
                    i++;
                } else if (device.get("name") != null) {
                    String name = ObjectUtils.to(String.class, device.get("name"));
                    if (name.startsWith("Yubikey")) {
                        name = name.replace("[", " - ").replace("]", "");
                        BeamCore.ui().write(" " + i + ". " + name + "\n");
                        i++;
                    }
                }
            }

            Integer choice = null;
            do {
                choice = ObjectUtils.to(Integer.class, BeamCore.ui().readText("\nSelect device: "));
            } while (choice == null);

            Map device = devices.get(choice - 1);
            displayDevice(device);
        }
    }

    public void handleNeedsTwoFactorEnrollment(Map<String, Object> authMap) {
        String enrollUrl = ObjectUtils.to(String.class, authMap.get("enrollUrl"));

        BeamCore.ui().write("\nAccess to Beam Enterprise requires two-factor authentication.\n\n");
        BeamCore.ui().write("Your account is not currently two-factor enabled. ");
        BeamCore.ui().write("Please enroll at the following link:\n\n");

        BeamCore.ui().write("%s\n", enrollUrl);
        System.exit(0);
    }

    private void displayDevice(Map<String, Object> device) {
        String type = ObjectUtils.to(String.class, device.get("type"));

        if ("token".equals(type)) {
            displayTokenDevice(device);
        } else {
            displayPhoneDevice(device);
        }
    }

    private void displayTokenDevice(Map<String, Object> device) {
        String name = ObjectUtils.to(String.class, device.get("name"));

        if (name.startsWith("Yubikey")) {
            String passcode = BeamCore.ui().readPassword("\nTap your Yubikey now...\n");

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
            BeamCore.ui().write("\nEnter a passcode or select one of the following options: \n\n");
        } else if (capabilities.size() == 1) {
            auth(deviceId, capabilities.get(0));
        } else {
            BeamCore.ui().write("\nSelect one of the following options:\n\n");
        }

        int i = 1;
        List<String> availableCapabilities = new ArrayList<>();
        for (String factor : capabilities) {
            switch (factor) {
                case "push" :
                    BeamCore.ui().write(" " + i++ + ". " + "Duo Push to " + displayName + "\n");
                    availableCapabilities.add(factor);
                    break;

                case "sms" :
                    BeamCore.ui().write(" " + i++ + ". " + "SMS passcodes to " + displayName + "\n");
                    availableCapabilities.add(factor);
                    break;

                default :
                    BeamCore.ui().write(" " + i++ + ". " + "Phone call to " + displayName + "\n");
                    availableCapabilities.add(factor);
                    break;
            }
        }

        if (!ObjectUtils.isBlank(nextCode)) {
            BeamCore.ui().write("\nThe next SMS passcode to use starts with: " + nextCode + "\n");
        }

        Integer choice = null;
        do {
            choice = ObjectUtils.to(Integer.class, BeamCore.ui().readText(canUsePasscode
                ? "\nEnter passcode or select option: "
                : "\nSelect option: "));
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
            Map<String, Object> authMap = getApi().callUnauthenticated(
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
            ioe.printStackTrace();
        }
    }

    private void authAuto() {
        try {
            Map<String, Object> authMap = getApi().callUnauthenticated(
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
            setSuccess(false);
        }
    }

    private void auth(String device, String factor) {
        try {
            Map<String, Object> authMap = getApi().callUnauthenticated(
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
            setSuccess(false);
        }
    }
}
