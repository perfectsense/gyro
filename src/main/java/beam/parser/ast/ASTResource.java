package beam.parser.ast;

public class ASTResource extends Node {

    private String provider;
    private String resource;

    public ASTResource(String provider, String resource) {
        this.provider = provider;
        this.resource = resource;
    }

    public String getProvider() {
        return provider;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return "ASTResource{" +
                "provider='" + provider + '\'' +
                ", resource='" + resource + '\'' +
                '}';
    }

}

