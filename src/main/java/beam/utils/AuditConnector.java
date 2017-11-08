package beam.utils;

import beam.BeamAuditor;
import beam.BeamRuntime;
import beam.cli.AbstractCloudCommand;
import beam.cli.VersionCommand;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuditConnector extends Thread {

    private Set<BeamAuditor> auditors;
    private OutputStream output;
    private InputStream input;
    private boolean keepReading;
    private Pattern pattern = Pattern.compile("begin--d3cc1f98-122c-4151-8f96-1f32e43f0ed9(?<input>.*)end--d3cc1f98-122c-4151-8f96-1f32e43f0ed9", Pattern.MULTILINE | Pattern.UNIX_LINES | Pattern.DOTALL);
    private AbstractCloudCommand command;
    private boolean auditorStarted;

    public AuditConnector(Set<BeamAuditor> auditors, InputStream input, OutputStream output, AbstractCloudCommand command) {
        this.auditors = auditors;
        this.input = input;
        this.output = output;
        this.keepReading = true;
        this.command = command;
    }

    public boolean isKeepReading() {
        return keepReading;
    }

    public void setKeepReading(boolean keepReading) {
        this.keepReading = keepReading;
    }

    public boolean isAuditorStarted() {
        return auditorStarted;
    }

    public void setAuditorStarted(boolean auditorStarted) {
        this.auditorStarted = auditorStarted;
    }

    private Map<String, Object> getStartingLog() {
        BeamRuntime runtime = BeamRuntime.getCurrentRuntime();
        InputStream stream = VersionCommand.class.getResourceAsStream("/build.properties");
        Properties properties = new Properties();
        String version = "";

        try {
            properties.load(stream);
            version = properties.get("version").toString();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Map<String, Object> log = new ImmutableMap.Builder<String, Object>().
                put("accountName", runtime.getAccount()).
                put("projectName", runtime.getProject()).
                put("environment", runtime.getEnvironment()).
                put("serial", runtime.getSerial()).
                put("commandArguments", command.getUnparsedArgument()).
                put("version", version).
                build();

        return log;
    }

    public void run () {
        Thread.currentThread().setName("Audit Connector");

        while (isKeepReading()) {
            if (command.isEverConfirmed() && !isAuditorStarted()) {
                if (!ObjectUtils.isBlank(auditors)) {
                    Map<String, Object> log = getStartingLog();
                    for (BeamAuditor auditor : auditors) {
                        try {
                            auditor.start(log);
                        } catch (Exception error) {
                            error.printStackTrace();
                        }
                    }
                }

                setAuditorStarted(true);
            }

            readStream();

            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {
                break;
            }
        }

        readStream();

        try {
            output.close();
        } catch (IOException error) {

        }
    }

    private void readStream() {
        int bytesRead = 0;
        byte[] bytes = new byte[4096];

        try {
            while ((bytesRead = input.read(bytes)) != -1) {
                String raw = new String(bytes, 0, bytesRead);
                String withInput = raw;
                String withoutInput = raw;

                Matcher matcher = pattern.matcher(raw);
                if (matcher.find()) {
                    String input = matcher.group("input");
                    withInput = matcher.replaceAll(input);
                    withoutInput = matcher.replaceAll("");
                }

                output.write(withoutInput.getBytes());
                output.flush();

                if (!ObjectUtils.isBlank(auditors)) {
                    for (BeamAuditor auditor : auditors) {
                        auditor.append(withInput);
                    }
                }
            }
        } catch (Exception error) {

        }
    }
}
