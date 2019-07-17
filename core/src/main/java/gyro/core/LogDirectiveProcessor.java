package gyro.core;

import gyro.util.Bug;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LogDirectiveProcessor extends PrintDirectiveProcessor {

    @Override
    public String getName() {
        return "log";
    }

    @Override
    protected void print(String content) {
        Path log = GyroCore.getHomeDirectory().resolve("debug.log");
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(log, StandardCharsets.UTF_8))) {
            printWriter.write(content);
            printWriter.write("\n");

        } catch (IOException error) {
            throw new Bug(error);
        }
    }

}
