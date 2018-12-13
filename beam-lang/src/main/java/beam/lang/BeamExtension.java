package beam.lang;

public class BeamExtension extends BeamConfig {

    private BeamInterp lang;

    public BeamInterp getLang() {
        return lang;
    }

    public void setLang(BeamInterp lang) {
        this.lang = lang;
    }
}
