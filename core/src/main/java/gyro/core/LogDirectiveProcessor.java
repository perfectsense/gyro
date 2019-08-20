package gyro.core;

import gyro.util.Bug;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Type("log")
public class LogDirectiveProcessor extends PrintDirectiveProcessor {

    @Override
    protected void print(String content) {
        try {
            Path home = GyroCore.getHomeDirectory();
            Files.createDirectories(home);

            Path log = home.resolve("debug.log");
            try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(log, StandardCharsets.UTF_8))) {
                printWriter.write(content);
                printWriter.write("\n");

            }
        }catch (IOException error) {
            throw new Bug(error);
        }
    }

}
