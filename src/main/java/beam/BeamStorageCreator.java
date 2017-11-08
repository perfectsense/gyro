package beam;

public interface BeamStorageCreator {

    /**
     * @return May be {@code null}.
     */
    public BeamStorage createStorage(BeamRuntime runtime) throws Exception;
}
