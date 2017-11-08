package beam;

public interface BeamRuntimeCreator {

    /**
     * @return May be {@code null}.
     */
    public BeamRuntime createRuntime() throws Exception;

    public BeamRuntime createRuntime(String env) throws Exception;

}
