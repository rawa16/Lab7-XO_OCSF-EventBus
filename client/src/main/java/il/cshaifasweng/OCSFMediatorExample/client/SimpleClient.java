package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.BoardUpdate;
import il.cshaifasweng.OCSFMediatorExample.entities.GameStart;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import org.greenrobot.eventbus.EventBus;

public class SimpleClient extends AbstractClient {

    private static SimpleClient client = null;

    private SimpleClient(String host, int port) {
        super(host, port);
    }

    @Override
    protected void handleMessageFromServer(Object msg) {

        if (msg instanceof Warning) {
            EventBus.getDefault().post(new WarningEvent((Warning) msg));
        } else if (msg instanceof GameStart) {
            EventBus.getDefault().post(new GameStartEvent((GameStart) msg));
        } else if (msg instanceof BoardUpdate) {
            EventBus.getDefault().post(new BoardUpdateEvent((BoardUpdate) msg));
        } else {
            System.out.println(msg);
        }
    }

    public static SimpleClient getClient() {
        if (client == null) {
            client = new SimpleClient("192.168.47.137", 3000);
        }
        return client;
    }
}
