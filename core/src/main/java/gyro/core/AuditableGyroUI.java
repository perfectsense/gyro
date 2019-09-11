package gyro.core;

import java.util.List;

public abstract class AuditableGyroUI implements GyroUI {
    private List<GyroAuditor> auditors;

    public List<GyroAuditor> getAuditors() {
        return auditors;
    }

    public void setAuditors(List<GyroAuditor> auditors) {
        this.auditors = auditors;
    }

    public void addAuditor(GyroAuditor auditor) {
        auditors.add(auditor);
    }

    protected void writeToStdout(String message) {
        System.out.print(message);

        sendAudit(message);
    }

    protected void sendAudit(String message) {
        try {
            for (GyroAuditor auditor : auditors) {
                auditor.append(message);
            }
        } catch (Exception e) {

        }
    }
}
