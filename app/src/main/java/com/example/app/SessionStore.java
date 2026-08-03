package com.example.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionStore {
    private static final String PREFERENCES = "worker_session";
    private static final String SESSION = "session";
    private final SharedPreferences preferences;

    public SessionStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public void save(ApiClient.Session session) {
        JSONObject value = new JSONObject();
        try {
            value.put("token", session.token);
            value.put("userId", session.userId);
            value.put("username", session.username);
            value.put("name", session.name);
            value.put("roles", new JSONArray(session.roles));
            preferences.edit().putString(SESSION, value.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public ApiClient.Session load() {
        String raw = preferences.getString(SESSION, "");
        if (raw == null || raw.isBlank()) return null;
        try {
            JSONObject value = new JSONObject(raw);
            JSONArray array = value.optJSONArray("roles");
            List<String> roles = new ArrayList<>();
            if (array != null) for (int index = 0; index < array.length(); index++) roles.add(array.optString(index));
            return new ApiClient.Session(value.getString("token"), value.optLong("userId"),
                    value.optString("username"), value.optString("name"), roles);
        } catch (Exception ignored) {
            clear();
            return null;
        }
    }

    public void clear() {
        preferences.edit().remove(SESSION).apply();
    }
}
