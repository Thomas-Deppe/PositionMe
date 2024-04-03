package com.openpositioning.PositionMe.Utils;

import com.openpositioning.PositionMe.sensors.Wifi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public final class JsonConverter {

    public JsonConverter() {
    }

    public static JSONObject toJson(List<Wifi> wifiList) throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject fingerprint = new JSONObject();
        for (Wifi data : wifiList){
            String bssid = Long.toString(data.getBssid());
//            System.out.println(bssid);
            fingerprint.put(bssid, data.getLevel());
        }

        json.put("wf", fingerprint);

        return json;
    }
}
