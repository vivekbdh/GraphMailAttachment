import com.microsoft.graph.logger.ILogger;
import com.microsoft.graph.logger.LoggerLevel;

public class MicrosoftGraphLogger implements ILogger {

    @Override
    public void setLoggingLevel(LoggerLevel level) {
        level = LoggerLevel.DEBUG;
    }

    @Override
    public LoggerLevel getLoggingLevel() {
        return null;
    }

    @Override
    public void logDebug(String message) {
        System.out.println(message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        System.out.println("Message::"+message);
        throwable.printStackTrace();
    }
}
