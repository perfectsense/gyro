package beam.openstack;

public class OpenStackCredentials {

    private String username;
    private String password;
    private String apiKey;

    public OpenStackCredentials(String username, String password, String apiKey) {
        this.username = username;
        this.password = password;
        this.apiKey = apiKey;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getApiKey() {
        return apiKey;
    }
}