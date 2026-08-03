package com.example.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ApiClient {
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
    }

    public Session login(String username, String password) throws Exception {
        JSONObject request = new JSONObject();
        request.put("username", username);
        request.put("password", password);
        JSONObject response = json("POST", "/api/auth/login", null, request);
        JSONObject user = response.getJSONObject("user");
        List<String> roles = strings(user.optJSONArray("roles"));
        return new Session(
                response.getString("accessToken"),
                user.optLong("id"),
                user.optString("username"),
                user.optString("name"),
                roles
        );
    }

    public void logout(String token) throws Exception {
        json("POST", "/api/auth/logout", token, new JSONObject());
    }

    public Permit getTodayPermit(String token) throws Exception {
        JSONObject value = json("GET", "/api/work-permits/today", token, null);
        return value.length() == 0 ? null : Permit.from(value);
    }

    public TbmBriefing getTodayTbm(String token, String language) throws Exception {
        JSONObject value = json("GET", "/api/worker/tbm/today?language=" + language, token, null);
        return value.length() == 0 ? null : TbmBriefing.from(value);
    }

    public void confirmTbm(String token, long permitId) throws Exception {
        JSONObject request = new JSONObject();
        request.put("permitId", permitId);
        json("POST", "/api/worker/tbm/confirm", token, request);
    }

    public long uploadImage(String token, byte[] image, String fileType, String fileName) throws Exception {
        String boundary = "----SafetyApp" + UUID.randomUUID();
        HttpURLConnection connection = connection("POST", "/api/files", token);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setDoOutput(true);
        try (OutputStream output = connection.getOutputStream()) {
            writePart(output, boundary, "fileType", fileType);
            output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.write("Content-Type: image/jpeg\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(image);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }
        return responseObject(connection).getLong("id");
    }

    public PpeCheck submitPersonalCheck(
            String token,
            Long permitId,
            long fileId,
            boolean safetyShoes,
            boolean workwear
    ) throws Exception {
        JSONObject request = new JSONObject();
        if (permitId != null) request.put("permitId", permitId);
        request.put("fileId", fileId);
        request.put("safetyShoesConfirmed", safetyShoes);
        request.put("workwearConfirmed", workwear);
        return PpeCheck.from(json("POST", "/api/worker/personal-checks", token, request));
    }

    public PpeCheck getTodayPersonalCheck(String token) throws Exception {
        JSONObject value = json("GET", "/api/worker/personal-checks/today", token, null);
        return value.length() == 0 ? null : PpeCheck.from(value);
    }

    public String createSafetyEvent(String token, String eventType, long fileId, String description) throws Exception {
        JSONObject request = new JSONObject();
        request.put("eventType", eventType);
        request.put("fileId", fileId);
        request.put("description", description);
        return json("POST", "/api/safety-events", token, request).optString("reportNo", "");
    }

    public List<SafetyReport> getMyReports(String token) throws Exception {
        String body = request("GET", "/api/safety-events/my", token, null);
        JSONArray array = new JSONArray(body);
        List<SafetyReport> reports = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            reports.add(SafetyReport.from(array.getJSONObject(index)));
        }
        return reports;
    }

    private JSONObject json(String method, String path, String token, JSONObject body) throws Exception {
        String value = request(method, path, token, body);
        return value == null || value.isBlank() ? new JSONObject() : new JSONObject(value);
    }

    private String request(String method, String path, String token, JSONObject body) throws Exception {
        HttpURLConnection connection = connection(method, path, token);
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        return responseText(connection);
    }

    private HttpURLConnection connection(String method, String path, String token) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        connection.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isBlank()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        return connection;
    }

    private JSONObject responseObject(HttpURLConnection connection) throws Exception {
        String body = responseText(connection);
        return body.isBlank() ? new JSONObject() : new JSONObject(body);
    }

    private String responseText(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String body = stream == null ? "" : read(stream);
        connection.disconnect();
        if (status < 200 || status >= 300) {
            String message = "요청을 처리하지 못했습니다. (" + status + ")";
            try {
                JSONObject error = new JSONObject(body);
                message = error.optString("message", error.optString("detail", message));
            } catch (Exception ignored) {
                if (!body.isBlank()) message = body;
            }
            throw new ApiException(status, message);
        }
        return body;
    }

    private String read(InputStream stream) throws IOException {
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void writePart(OutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> strings(JSONArray array) {
        if (array == null) return Collections.emptyList();
        List<String> values = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) values.add(array.optString(index));
        return values;
    }

    private static String value(JSONObject object, String snake, String camel) {
        if (object.has(snake) && !object.isNull(snake)) return object.optString(snake);
        return object.optString(camel);
    }

    public static class ApiException extends Exception {
        public final int status;
        public ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    public static class Session {
        public final String token;
        public final long userId;
        public final String username;
        public final String name;
        public final List<String> roles;

        public Session(String token, long userId, String username, String name, List<String> roles) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.name = name;
            this.roles = roles;
        }

        public boolean isWorker() { return roles.contains("WORKER"); }
    }

    public static class Permit {
        public final long id;
        public final String permitNo;
        public final String workTitle;
        public final String workType;
        public final String siteName;
        public final String blockCode;
        public final String status;
        public final String startTime;
        public final String endTime;
        public final String conditions;
        public final boolean highRisk;

        public Permit(long id, String permitNo, String workTitle, String workType, String siteName,
                       String blockCode, String status, String startTime, String endTime,
                       String conditions, boolean highRisk) {
            this.id = id;
            this.permitNo = permitNo;
            this.workTitle = workTitle;
            this.workType = workType;
            this.siteName = siteName;
            this.blockCode = blockCode;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.conditions = conditions;
            this.highRisk = highRisk;
        }

        static Permit from(JSONObject value) {
            return new Permit(
                    value.optLong("id"), value(value, "permit_no", "permitNo"),
                    value(value, "work_title", "workTitle"), value(value, "work_type", "workType"),
                    value(value, "site_name", "siteName"), value(value, "block_code", "blockCode"),
                    value.optString("status"), value(value, "start_time", "startTime"),
                    value(value, "end_time", "endTime"),
                    value(value, "recommended_conditions", "recommendedConditions"),
                    value.optBoolean("is_high_risk", value.optBoolean("highRisk"))
            );
        }
    }

    public static class TbmBriefing {
        public final long permitId;
        public final String title;
        public final String content;
        public final boolean confirmed;

        public TbmBriefing(long permitId, String title, String content, boolean confirmed) {
            this.permitId = permitId;
            this.title = title;
            this.content = content;
            this.confirmed = confirmed;
        }

        static TbmBriefing from(JSONObject value) {
            return new TbmBriefing(value.optLong("permitId"), value.optString("title", "오늘의 TBM 안전 안내"),
                    value.optString("content"), value.optBoolean("confirmed"));
        }
    }

    public static class PpeCheck {
        public final String status;
        public final String message;

        public PpeCheck(String status, String message) {
            this.status = status;
            this.message = message;
        }

        static PpeCheck from(JSONObject value) {
            return new PpeCheck(value.optString("status"), value.optString("message"));
        }
    }

    public static class SafetyReport {
        public final String reportNo;
        public final String title;
        public final String description;
        public final String status;
        public final String eventTime;

        public SafetyReport(String reportNo, String title, String description, String status, String eventTime) {
            this.reportNo = reportNo;
            this.title = title;
            this.description = description;
            this.status = status;
            this.eventTime = eventTime;
        }

        static SafetyReport from(JSONObject value) {
            return new SafetyReport(value.optString("reportNo"), value.optString("title"),
                    value.optString("description"), value.optString("status"), value.optString("eventTime"));
        }
    }
}
