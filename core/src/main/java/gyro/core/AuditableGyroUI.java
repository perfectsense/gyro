package gyro.core;

import java.util.LinkedList;
import java.util.List;

public abstract class AuditableGyroUI extends GyroUI {
    private List<GyroAuditor> auditors;

    public List<GyroAuditor> getAuditors() {
        if (auditors == null) {
            auditors = new LinkedList<>();
        }
        return auditors;
    }

    public void setAuditors(List<GyroAuditor> auditors) {
        this.auditors = auditors;
    }

    public void addAuditor(GyroAuditor auditor) {
        getAuditors().add(auditor);
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
