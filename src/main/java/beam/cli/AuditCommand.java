package beam.cli;

import beam.Beam;
import beam.BeamAuditor;
import beam.BeamCloud;
import beam.BeamException;
import com.psddev.dari.util.ObjectUtils;
import io.airlift.command.Command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Command(name = "audit", description = "List audit logs.")
public class AuditCommand extends AbstractCloudCommand {

    private static final Table LIST_TABLE = new Table().
            addColumn("#", 2).
            addColumn("Time", 30).
            addColumn("Person", 15).
            addColumn("Environment", 15).
            addColumn("Serial", 10).
            addColumn("Command Arguments", 30).
            addColumn("Version", 20);

    @Override
    protected CloudHandler getCloudHandler() {
        return new CloudHandler() {

            @Override
            public void each(BeamCloud cloud) throws Exception {
                for (Class<? extends BeamAuditor> c : Beam.getReflections().getSubTypesOf(BeamAuditor.class)) {
                    if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
                        continue;
                    }

                    List<Map<String, Object>> logs = c.newInstance().list(runtime.getAccount(), runtime.getProject());

                    if (logs == null) {
                        continue;
                    }

                    if (logs.isEmpty()) {
                        out.format("@|red No audit logs available!|@");
                        return;
                    }

                    LIST_TABLE.writeHeader(out);

                    int index = 0;

                    for (Map<String, Object> log : logs) {
                        ++ index;

                        LIST_TABLE.writeRow(
                                out,
                                index,
                                ObjectUtils.to(Date.class, log.get("createTime")),
                                log.get("person"),
                                log.get("environment"),
                                log.get("serial"),
                                log.get("commandArguments"),
                                log.get("version"));
                    }

                    LIST_TABLE.writeFooter(out);

                    out.print("More details? (#/N) ");
                    out.flush();

                    BufferedReader pickReader = new BufferedReader(new InputStreamReader(System.in));
                    Integer pick = ObjectUtils.to(Integer.class, pickReader.readLine());

                    if (pick != null) {
                        if (pick > logs.size() || pick <= 0) {
                            throw new BeamException(String.format(
                                    "Must pick a number between 1 and %d!",
                                    logs.size()));
                        }

                        System.out.println("\nOUTPUT\n------\n");
                        System.out.println(logs.get(pick - 1).get("output"));
                    }
                }
            }
        };
    }
}
