package beam.cli;

public interface AuditableCommand {

    default public boolean shouldAudit() {
        return true;
    }

}
