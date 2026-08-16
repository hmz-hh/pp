package antonio.mexico.service.v2ray;

import android.app.Service;

public interface V2Listener {
    boolean onProtect(final int socket);
    Service getService();
    void startService();
    void stopService();
    void onConnected();
    void onError();

}
