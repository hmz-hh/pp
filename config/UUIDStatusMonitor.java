package antonio.mexico.service.v2ray;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import antonio.mexico.service.logger.SkStatus;
// حيدنا import ديال UUIDManager و JSON و حوايج الأنترنيت حيت مبقيناش محتاجينهم

public class UUIDStatusMonitor {

    private static final String TAG = "UUIDStatusMonitor";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    
    private static V2Listener v2Listener;
    
    private static boolean isChecking = false;

    // Colores/professional look
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
            // ✅ الـ UUID الثابت اللي طلبتي
            String uuid = "9cd7c8d7-0625-333c-afcc-e0fdd1924c4b";
            
            // ✅ عدد الأيام الثابت (2 أيام)
            int dias = 2;

            Log.d(TAG, "Usando UUID estático: " + uuid + " con " + dias + " días.");

            if (dias > 0) {
                // حساب تاريخ الانتهاء بناءً على عدد الأيام (2)
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, dias);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String fechaExpira = sdf.format(cal.getTime());

                // تحديد اللون والرمز (بما أن الأيام 2، غيكون اللون أصفر ⏰)
                String color = (dias <= 3) ? COLOR_WARNING : COLOR_PRIMARY;
                String emoji = (dias <= 3) ? "⏰" : "✅";

                String html = "<font color='" + color + "'><b>" + emoji + " UUID Activo</b></font><br>"
                        + "<font color='" + COLOR_TEXT + "'>📅 Días restantes: <b>" + dias + "</b><br>"
                        + "⏳ Expira el: <b>" + fechaExpira + "</b></font>";

                mostrarHtml(html);
                Log.i(TAG, "UUID activo: " + dias + " días (expira: " + fechaExpira + ")");
            } else {
                // هاد الجزء ماغاديش يتنفذ حيت درنا dias = 2، ولكن خليتو باش يبقى الكود متكامل
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
