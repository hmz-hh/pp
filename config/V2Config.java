package antonio.mexico.service.v2ray;


public class V2Configs {

    public static final long BYTE = 1;
    public static final long KILO_BYTE = BYTE * 1024;
    public static final long MEGA_BYTE = KILO_BYTE * 1024;
    public static final long GIGA_BYTE = MEGA_BYTE * 1024;
    public static V2RAY_CONNECTION_MODES V2RAY_CONNECTION_MODE = V2RAY_CONNECTION_MODES.VPN_TUN;
    public static String APPLICATION_NAME = null;
    public static int APPLICATION_ICON = 0;
    public static V2Config V2RAY_CONFIG = null;
    public static V2RAY_STATES V2RAY_STATE = V2RAY_STATES.V2RAY_DISCONNECTED;
    public static boolean ENABLE_TRAFFIC_AND_SPEED_STATICS = true;

    public enum V2RAY_SERVICE_COMMANDS {
        START_SERVICE,
        STOP_SERVICE,
        MEASURE_DELAY
    }

    public enum V2RAY_STATES {
        V2RAY_CONNECTED,
        V2RAY_DISCONNECTED,
        V2RAY_CONNECTING
    }

    public enum V2RAY_CONNECTION_MODES {
        VPN_TUN,
        PROXY_ONLY
    }


}
