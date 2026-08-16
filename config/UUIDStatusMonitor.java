package antonio.mexico.service.v2ray;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import antonio.mexico.service.logger.SkStatus;
import antonio.mexico.vpn.UUIDManager;

public class UUIDStatusMonitor {

    private static final String TAG = "UUIDStatusMonitor";

    // Usa tu dominio principal; si falla, el código intentará con la IP de fallback.
    private static final String API_URL_PRIMARY = "http://chile.antoniomx.shop:89/?uuid=";
    private static final String API_URL_FALLBACK = "http://159.112.132.197:89/?uuid=";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int TIMEOUT_MS = 20000;
    
   private static V2Listener v2Listener;
    

    private static boolean isChecking = false;

    // Colores/professional look (puedes cambiarlos)
    private static final String COLOR_PRIMARY = "#00E676";
    private static final String COLOR_WARNING = "#FFD54F";
    private static final String COLOR_ERROR   = "#FF5252";
    private static final String COLOR_TEXT    = "#ECEFF1";

    /**
     * Verifica UNA vez al conectar (llamar desde StartV2ray justo después de start service).
     */
    public static synchronized void start(final Context context) {
        if (isChecking) return;
        isChecking = true;

        // Espera breve para asegurarse SkStatus/servicio esté listo
        MAIN.postDelayed(() -> new Thread(() -> verifyOnce(context)).start(), 2500);
    }

    private static void verifyOnce(final Context context) {
        try {
            String uuid = UUIDManager.getOrCreateUUID(context);
            Log.d(TAG, "UUIDManager returned: " + uuid);
            if (uuid == null || uuid.isEmpty()) {
                mostrarHtml("<font color='" + COLOR_ERROR + "'><b>⚠️ UUID no encontrado</b></font>");
                isChecking = false;
                return;
            }

            // Intenta primero con dominio, si falla, intenta con fallback IP
            String[] endpoints = new String[] {
                    API_URL_PRIMARY + uuid,
                    API_URL_FALLBACK + uuid
            };

            String response = null;
            String usedEndpoint = null;
            for (String url : endpoints) {
                Log.d(TAG, "Intentando endpoint: " + url);
                response = httpGet(url, TIMEOUT_MS);
                Log.d(TAG, "Respuesta raw: " + response);
                if (response != null && !response.trim().isEmpty()) {
                    usedEndpoint = url;
                    break;
                } else {
                    Log.w(TAG, "No hubo respuesta válida desde: " + url);
                }
            }

            if (response == null || response.trim().isEmpty()) {
                mostrarHtml("<font color='" + COLOR_WARNING + "'><b>⚠️ No se pudo contactar al servidor de validación</b></font>");
                isChecking = false;
                return;
            }

            // Asegurarse que venga JSON (si no, mostrar el cuerpo para debug)
            String respTrim = response.trim();
            if (!respTrim.startsWith("{")) {
                Log.e(TAG, "Respuesta no JSON: " + respTrim);
                mostrarHtml("<font color='" + COLOR_ERROR + "'><b>❌ Respuesta inválida del servidor</b></font>");
                isChecking = false;
                return;
            }

            JSONObject json = new JSONObject(respTrim);
            int dias = json.optInt("days", -1);
            Log.d(TAG, "Parseado days=" + dias + " (endpoint usado: " + usedEndpoint + ")");

            if (dias > 0) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, dias);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String fechaExpira = sdf.format(cal.getTime());

                String color = (dias <= 3) ? COLOR_WARNING : COLOR_PRIMARY;
                String emoji = (dias <= 3) ? "⏰" : "✅";

                String html = "<font color='" + color + "'><b>" + emoji + " UUID Activo</b></font><br>"
                        + "<font color='" + COLOR_TEXT + "'>📅 Días restantes: <b>" + dias + "</b><br>"
                        + "⏳ Expira el: <b>" + fechaExpira + "</b></font>";

                mostrarHtml(html);
                Log.i(TAG, "UUID activo: " + dias + " días (expira: " + fechaExpira + ")");
            } else {
                mostrarHtml("<font color='" + COLOR_ERROR + "'><b>❌ UUID no activado o expirado — Desconectando</b></font>");
                Log.e(TAG, "UUID inválido/expirado, deteniendo V2Ray");
                MAIN.post(() -> {
                    try {
                        V2Tunnel.StopV2ray(context);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error al detener V2Ray: " + ex.getMessage(), ex);
                    }
                    
                   
                });
               
                
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en verifyOnce: " + e.getMessage(), e);
            mostrarHtml("<font color='" + COLOR_ERROR + "'><b>❌ Error verificando UUID</b></font>");
        } finally {
            isChecking = false;
        }
    }

    private static String httpGet(String urlString, int timeout) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(urlString);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "close");

            int code = conn.getResponseCode();
            Log.d(TAG, "HTTP code=" + code + " for " + urlString);
            if (code != HttpURLConnection.HTTP_OK) return null;

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();
            return sb.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "httpGet error: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void mostrarHtml(String html) {
        MAIN.post(() -> {
            try {
                SkStatus.logInfo("<font face='monospace' size='4'>" + html + "</font>");
            } catch (Exception e) {
                Log.e(TAG, "mostrarHtml error: " + e.getMessage(), e);
            }
        });
    }
}
