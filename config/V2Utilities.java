package antonio.mexico.service.v2ray;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

public class V2Utilities {

    public static void CopyFiles(InputStream src, File dst) throws IOException {
        try (OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = src.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    public static String getUserAssetsPath(Context context) {
        File extDir = context.getExternalFilesDir("assets");
        if (extDir == null) {
            return "";
        }
        if (!extDir.exists()) {
            return context.getDir("assets", 0).getAbsolutePath();
        } else {
            return extDir.getAbsolutePath();
        }
    }

    public static void copyAssets(final Context context) {
        String extFolder = getUserAssetsPath(context);
        try {
            String geo = "geosite.dat,geoip.dat";
            for (String assets_obj : context.getAssets().list("")) {
                if (geo.contains(assets_obj)) {
                    CopyFiles(context.getAssets().open(assets_obj), new File(extFolder, assets_obj));
                }
            }
        } catch (Exception e) {
            Log.e("V2Utilities", "copyAssets failed=>", e);
        }
    }


    public static String convertIntToTwoDigit(int value) {
        if (value < 10) return "0" + value;
        else return value + "";
    }

    public static String parseTraffic(final double bytes, final boolean inBits, final boolean isMomentary) {
        double value = inBits ? bytes * 8 : bytes;
        if (value < V2Configs.KILO_BYTE) {
            return String.format(Locale.getDefault(), "%.1f " + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value);
        } else if (value < V2Configs.MEGA_BYTE) {
            return String.format(Locale.getDefault(), "%.1f K" + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value / V2Configs.KILO_BYTE);
        } else if (value < V2Configs.GIGA_BYTE) {
            return String.format(Locale.getDefault(), "%.1f M" + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value / V2Configs.MEGA_BYTE);
        } else {
            return String.format(Locale.getDefault(), "%.2f G" + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value / V2Configs.GIGA_BYTE);
        }
    }

    public static V2Config parseV2rayJsonFile(final String remark, String config, final ArrayList<String> blockedApplication) {
        final V2Config v2Config = new V2Config();
        v2Config.REMARK = remark;
        v2Config.BLOCKED_APPS = blockedApplication;
        v2Config.APPLICATION_ICON = V2Configs.APPLICATION_ICON;
        v2Config.APPLICATION_NAME = V2Configs.APPLICATION_NAME;
        try {
            JSONObject config_json = new JSONObject(config);
            try {
                JSONArray inbounds = config_json.getJSONArray("inbounds");
                for (int i = 0; i < inbounds.length(); i++) {
                    try {
                        if (inbounds.getJSONObject(i).getString("protocol").equals("socks")) {
                            v2Config.LOCAL_SOCKS5_PORT = inbounds.getJSONObject(i).getInt("port");
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                    try {
                        if (inbounds.getJSONObject(i).getString("protocol").equals("http")) {
                            v2Config.LOCAL_HTTP_PORT = inbounds.getJSONObject(i).getInt("port");
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
            } catch (Exception e) {
                Log.w(V2Core.class.getSimpleName(), "startCore warn => can`t find inbound port of socks5 or http.");
                return null;
            }
            try {
                v2Config.CONNECTED_V2RAY_SERVER_ADDRESS = config_json.getJSONArray("outbounds")
                        .getJSONObject(0).getJSONObject("settings")
                        .getJSONArray("vnext").getJSONObject(0)
                        .getString("address");
                v2Config.CONNECTED_V2RAY_SERVER_PORT = config_json.getJSONArray("outbounds")
                        .getJSONObject(0).getJSONObject("settings")
                        .getJSONArray("vnext").getJSONObject(0)
                        .getString("port");
            } catch (Exception e) {
                v2Config.CONNECTED_V2RAY_SERVER_ADDRESS = config_json.getJSONArray("outbounds")
                        .getJSONObject(0).getJSONObject("settings")
                        .getJSONArray("servers").getJSONObject(0)
                        .getString("address");
                v2Config.CONNECTED_V2RAY_SERVER_PORT = config_json.getJSONArray("outbounds")
                        .getJSONObject(0).getJSONObject("settings")
                        .getJSONArray("servers").getJSONObject(0)
                        .getString("port");
            }
            try {
                if (config_json.has("policy")) {
                    config_json.remove("policy");
                }
                if (config_json.has("stats")) {
                    config_json.remove("stats");
                }
            } catch (Exception ignore_error) {
                //ignore
            }
            if (V2Configs.ENABLE_TRAFFIC_AND_SPEED_STATICS) {
                try {
                    JSONObject policy = new JSONObject();
                    JSONObject levels = new JSONObject();
                    levels.put("8", new JSONObject()
                            .put("connIdle", 300)
                            .put("downlinkOnly", 1)
                            .put("handshake", 4)
                            .put("uplinkOnly", 1));
                    JSONObject system = new JSONObject()
                            .put("statsOutboundUplink", true)
                            .put("statsOutboundDownlink", true);
                    policy.put("levels", levels);
                    policy.put("system", system);
                    config_json.put("policy", policy);
                    config_json.put("stats", new JSONObject());
                    config = config_json.toString();
                    v2Config.ENABLE_TRAFFIC_STATICS = true;
                } catch (Exception e) {
                    //ignore
                }
            }
        } catch (Exception e) {
            Log.e(V2Utilities.class.getName(), "parseV2rayJsonFile failed => ", e);
            //ignore
            return null;
        }
        v2Config.V2RAY_FULL_JSON_CONFIG = config;
        return v2Config;
    }


}
