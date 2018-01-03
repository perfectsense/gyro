package beam.parser.ast;

public class ASTMethod extends Node {

    private String methodName;

    public ASTMethod(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String toString() {
        return "ASTMethod{" +
                "methodName='" + methodName + '\'' +
                '}';
    }
}
