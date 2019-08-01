package gyro.core.command;

import java.util.List;

public interface GyroCommandGroup {

    String getName();

    String getDescription();

    List<Class<?>> getCommands();

    Class<?> getDefaultCommand();

}
