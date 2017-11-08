package beam.utils;

import beam.BeamAuditor;
import beam.cli.AbstractCloudCommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AuditStream {

    private File outputFile;
    private PrintStream outputStream;
    private InputStream inputStream;
    private AuditConnector auditThread;

    public AuditStream(Set<BeamAuditor> auditors, OutputStream output, AbstractCloudCommand command) {
        try {
            outputFile = File.createTempFile("beam.", ".out");
            outputStream = new PrintStream(new FileOutputStream(outputFile), true);
            inputStream = new FileInputStream(outputFile);

            auditThread = new AuditConnector(auditors, inputStream, output, command);
            auditThread.start();

            outputFile.deleteOnExit();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public File getOutputFile() {
        return outputFile;
    }

    public PrintStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void finishAuditing() {
        outputStream.flush();

        if (auditThread != null && auditThread.isAlive()) {
            auditThread.setKeepReading(false);
        }

        try {
            auditThread.join();
        } catch (InterruptedException ie) {

        }
    }
}
