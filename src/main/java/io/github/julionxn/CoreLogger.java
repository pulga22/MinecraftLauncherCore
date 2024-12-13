package io.github.julionxn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

public class CoreLogger {

    private final Logger logger;

    public CoreLogger(LauncherData launcherData){
        logger = Logger.getLogger(launcherData.launcherName());
        logger.setUseParentHandlers(false);

        Path logFilePath = launcherData.rootPath()
                .resolve(launcherData.launcherName())
                .resolve("logs")
                .resolve(System.currentTimeMillis() + ".log");

        try {
            Files.createDirectories(logFilePath.getParent());
            FileHandler fileHandler = new FileHandler(logFilePath.toString(), true) {
                @Override
                public synchronized void publish(LogRecord record) {
                    super.publish(record);
                    flush();
                }
            };
            Formatter customFormatter = new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format(
                            "[%1$tF %1$tT] [%2$s] [%3$s] %4$s%n",
                            record.getMillis(),
                            record.getLevel().getName(),
                            record.getLoggerName(),
                            record.getMessage()
                    );
                }
            };
            fileHandler.setFormatter(customFormatter);
            logger.addHandler(fileHandler);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(customFormatter);
            logger.addHandler(consoleHandler);

        } catch (IOException e) {
            System.err.println("Failed to initialize logging: " + logFilePath);
            e.printStackTrace();
        }
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warning(String message) {
        logger.warning(message);
    }

    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public void error(String message) {
        logger.severe(message);
    }

    public void debug(String message) {
        logger.fine(message);
    }

    public void trace(String message) {
        logger.log(Level.FINER, message);
    }

}
