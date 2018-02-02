package beam.providerHandler;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.psddev.dari.util.StringUtils;

public class GitHandler extends ProviderHandler {

    private static String LONG_GITHUB_URL = "^https://github.com/(?<short>.*)";
    private static Pattern LONG_GITHUB_URL_PAT = Pattern.compile(LONG_GITHUB_URL);

    private static String SHORT_GITHUB_URL = "(?<username>[^~/]+)/(?<repo>[^?]+)";
    private static Pattern SHORT_GITHUB_URL_PAT = Pattern.compile(SHORT_GITHUB_URL);

    private static String SSH_GIT_URL = "([^:]+):(?<short>[^.]+)\\.git";
    private static Pattern SSH_GIT_URL_PAT = Pattern.compile(SSH_GIT_URL);

    @Override
    public boolean validate(String key) {
        if (key.contains("?")) {
            key = key.split("\\?")[0];
        }

        if (key.matches(SSH_GIT_URL) ||
                key.matches(LONG_GITHUB_URL) ||
                key.matches(SHORT_GITHUB_URL)) {
            return true;
        }

        return false;
    }

    @Override
    public void handle(String key) {
        String beamPath = System.getProperty("user.home") + "/.beam";
        final String destinationPath = beamPath + "/packages";

        new File(beamPath).mkdirs();
        new File(destinationPath).mkdirs();
        String username = null;
        String packageName = null;
        String repo = null;
        String queryString = null;
        String branch = null;

        if (key.contains("?")) {
            queryString = key.split("\\?")[1];
            key = key.split("\\?")[0];
        }

        if (queryString != null) {
            branch = StringUtils.getQueryParameterValue(queryString, "branch");
        }

        Matcher longMatcher = LONG_GITHUB_URL_PAT.matcher(key);
        Matcher sshMatcher = SSH_GIT_URL_PAT.matcher(key);

        if (longMatcher.find()) {
            repo = key;
            key = longMatcher.group("short");
        } else if (sshMatcher.find()) {
            repo = key;
            key = sshMatcher.group("short");
        }

        Matcher shortMatcher = SHORT_GITHUB_URL_PAT.matcher(key);
        if (shortMatcher.find()) {
            username = shortMatcher.group("username");
            packageName = shortMatcher.group("repo");

            if (repo == null) {
                repo = String.format("https://github.com/%s/%s", username, packageName);
            }
        } else {
            repo = key;
        }

        Path packagePath = FileSystems.getDefault().getPath(destinationPath, packageName);

        List<String> arguments = new ArrayList<>();
        String workingDirectory = null;
        if (packagePath.toFile().exists()) {
            workingDirectory = packagePath.toString();

            arguments.add("git");
            arguments.add("remote");
            arguments.add("set-url");
            arguments.add("origin");
            arguments.add(repo);
            buildAndRunProcess(arguments, workingDirectory);

            arguments.clear();
            arguments.add("git");
            arguments.add("pull");
            buildAndRunProcess(arguments, workingDirectory);

            if (branch != null) {
                arguments.clear();
                arguments.add("git");
                arguments.add("checkout");
                arguments.add(branch);
                buildAndRunProcess(arguments, workingDirectory);
            }
        } else {
            arguments.add("git");
            arguments.add("clone");

            if (branch != null) {
                arguments.add("-b");
                arguments.add(branch);
            }

            arguments.add(repo);
            arguments.add(packagePath.toString());

            buildAndRunProcess(arguments, null);
        }

        LocalHandler localHandler = new LocalHandler();
        if (localHandler.validate(packagePath.toString())) {
            localHandler.handle(packagePath.toString());
        }
    }

    private int buildAndRunProcess(List<String> arguments, String workingDirectory) {
        try {
            ProcessBuilder gitProcess = new ProcessBuilder(arguments);
            if (workingDirectory != null) {
                gitProcess.directory(new File(workingDirectory));
            }
            return gitProcess.inheritIO().start().waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 1;
    }
}
