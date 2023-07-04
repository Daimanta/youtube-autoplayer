package nl.leonvanderkaap.yvplayer.management;

import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class StatusService {
    private final LinkedList<MessageLog> statusLogs = new LinkedList<>();
    public static final int MESSAGE_MAX = 10;

    public void addMessage(MessageLog log) {
        statusLogs.add(log);
        while (statusLogs.size() > MESSAGE_MAX) {
            statusLogs.removeFirst();
        }
    }

    public List<MessageLog> getStatus() {
        return statusLogs;
    }

    public void addError(String message) {
        addMessage(new MessageLog(StatusType.ERROR, message));
    }

    public void addOk(String message) {
        addMessage(new MessageLog(StatusType.OK, message));
    }

    public void addStartedDownload(String title) {
        addMessage(new MessageLog(StatusType.OK, String.format("Started downloading '%s'", title)));
    }

    public void addedToQueue(String title) {
        addMessage(new MessageLog(StatusType.OK, String.format("Added '%s' to queue", title)));
    }

}
