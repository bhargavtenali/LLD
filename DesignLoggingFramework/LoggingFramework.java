enum LogLevel { DEBUG, INFO, WARN, ERROR }

interface Appender {
    void append(String formattedMessage);
}

class ConsoleAppender implements Appender {
    public void append(String formattedMessage) {
        System.out.println(formattedMessage);
    }
}

class FileAppender implements Appender {
    private String filePath;
    public void append(String formattedMessage) {
        // append to file
    }
}

class Logger {
    private String name;
    private LogLevel level;
    private List<Appender> appenders;

    public Logger(String name, LogLevel level, List<Appender> appenders) {
        this.name = name;
        this.level = level;
        this.appenders = appenders;
    }

    public void log(LogLevel logLevel, String message) {
        if (logLevel.ordinal() >= level.ordinal()) {
            String formatted = format(logLevel, message);
            for (Appender appender : appenders) appender.append(formatted);
        }
    }

    private String format(LogLevel level, String msg) {
        return "[" + new Date() + "][" + Thread.currentThread().getName() + "][" + level + "] " + msg;
    }
}

class LoggerFactory {
    private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();
    private static final List<Appender> defaultAppenders = List.of(new ConsoleAppender());

    public static Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, k -> new Logger(k, LogLevel.INFO, defaultAppenders));
    }
}

