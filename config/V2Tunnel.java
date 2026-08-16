package antonio.mexico.service.v2ray;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import antonio.mexico.service.logger.SkStatus;
import antonio.mexico.service.tunnel.TunnelManagerThread;
import antonio.mexico.vpn.ConfigLoader;
import libv2ray.Libv2ray;

public class V2Tunnel {
    private static V2Listener v2Listener;

    public V2Tunnel(Context context) {
        v2Listener = TunnelManagerThread.getV2rayServicesListener();
    }

    public static void init(final Context context, final int app_icon, final String app_name) {
        V2Utilities.copyAssets(context);
        V2Configs.APPLICATION_ICON = app_icon;
        V2Configs.APPLICATION_NAME = app_name;

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                V2Configs.V2RAY_STATE = (V2Configs.V2RAY_STATES) arg1.getExtras().getSerializable("STATE");
            }
        }, new IntentFilter("V2RAY_CONNECTION_INFO"));
    }

    public static void changeConnectionMode(final V2Configs.V2RAY_CONNECTION_MODES connection_mode) {
        if (getConnectionState() == V2Configs.V2RAY_STATES.V2RAY_DISCONNECTED) {
            V2Configs.V2RAY_CONNECTION_MODE = connection_mode;
        }
    }

    public static void StartV2ray(final Context context, final String remark, final ArrayList<String> blocked_apps) {
       

        // 1. Cargar config.json con UUID dinámico (soporta Base64 o JSON puro)
        String configString = ConfigLoader.loadConfigWithUUID(context);
        if (configString == null) {
            v2Listener.onError();
            SkStatus.logInfo("V2Ray Error: No se pudo cargar el config.json");
            return;
        }

        // 2. Parsear la configuración
        V2Config v2Config = V2Utilities.parseV2rayJsonFile(remark, configString, blocked_apps);
        if (v2Config == null) {
            v2Listener.onError();
            SkStatus.logInfo("V2Ray Error: Configuración inválida");
            return;
        }

        // Guardar la config válida
        V2Configs.V2RAY_CONFIG = v2Config;

        // 3. Preparar servicio según el modo
        Intent start_intent;
        if (V2Configs.V2RAY_CONNECTION_MODE == V2Configs.V2RAY_CONNECTION_MODES.PROXY_ONLY) {
            start_intent = new Intent(context, V2Proxy.class);
        } else if (V2Configs.V2RAY_CONNECTION_MODE == V2Configs.V2RAY_CONNECTION_MODES.VPN_TUN) {
            start_intent = new Intent(context, V2Service.class);
        } else {
            v2Listener.onError();
            SkStatus.logInfo("V2Ray Error: Modo de conexión inválido");
            return;
        }

        start_intent.putExtra("COMMAND", V2Configs.V2RAY_SERVICE_COMMANDS.START_SERVICE);
        start_intent.putExtra("V2RAY_CONFIG", V2Configs.V2RAY_CONFIG);

        // Logs
        SkStatus.logInfo(V2Tunnel.getCoreVersion());
        v2Listener.startService();
        SkStatus.logInfo("<font color='#ffffff'><strong>INICIANDO V2RAY</strong></font>");
        SkStatus.logInfo("<font color='#FFFF00'><strong>UUID V2RAY AUTETICANDO...</strong></font>");

        siNoInternet(context);

        // 4. Iniciar servicio en foreground según versión
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            context.startForegroundService(start_intent);
        } else {
            context.startService(start_intent);
        }
       UUIDStatusMonitor.start(context);
    }

    public static void StopV2ray(final Context context) {
      // UUIDStatusMonitor.stop();
        Intent stop_intent;
        if (V2Configs.V2RAY_CONNECTION_MODE == V2Configs.V2RAY_CONNECTION_MODES.PROXY_ONLY) {
            stop_intent = new Intent(context, V2Proxy.class);
        } else if (V2Configs.V2RAY_CONNECTION_MODE != V2Configs.V2RAY_CONNECTION_MODES.VPN_TUN) {
            return;
        } else {
            stop_intent = new Intent(context, V2Service.class);
        }
        stop_intent.putExtra("COMMAND", V2Configs.V2RAY_SERVICE_COMMANDS.STOP_SERVICE);
        context.startService(stop_intent);
        V2Configs.V2RAY_CONFIG = null;
    }

    public static V2Configs.V2RAY_CONNECTION_MODES getConnectionMode() {
        return V2Configs.V2RAY_CONNECTION_MODE;
    }

    public static V2Configs.V2RAY_STATES getConnectionState() {
        return V2Configs.V2RAY_STATE;
    }

    public static String getCoreVersion() {
        return Libv2ray.checkVersionX();
    }

    public static void stopAllServices(Activity activity) {
        StopV2ray(activity);
    }

    // ==========================
    // Verificar si hay internet
    // ==========================
    public static void siNoInternet(final Context context) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (verificarInternet()) {
                    Log.d("Internet", "Tienes acceso a internet");
                    SkStatus.logInfo("<font color='#ffffff'><strong>INTERNET MEXICO 2025-2026®</strong></font>");
                    v2Listener.onConnected();
                    return;
                }
                Log.d("Internet", "No tienes acceso a internet");
                SkStatus.logInfo("<font color='#ff0000'><strong>UUID NO AUTORIZADO VUELVA A CONECTAR</strong></font>");
                v2Listener.onError();
            }
        }, 2000);
    }

    public static boolean verificarInternet() {
        try {
            String command = "ping -c 1 google.com";
            return (Runtime.getRuntime().exec(command).waitFor() == 0);
        } catch (Exception e) {
            return false;
        }
    }
}
