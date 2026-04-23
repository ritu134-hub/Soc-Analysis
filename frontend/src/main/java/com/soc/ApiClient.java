package com.soc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client wrapper for all calls to the Python FastAPI backend.
 */
public class ApiClient {

    public static String BASE_URL = "http://localhost:8000";
    private static final Gson GSON = new Gson();

    static {
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(System.getProperty("user.dir")).getParent().resolve(".env");
            if (java.nio.file.Files.exists(envPath)) {
                for (String line : java.nio.file.Files.readAllLines(envPath)) {
                    if (line.startsWith("BACKEND_PORT=")) {
                        String port = line.split("=")[1].trim();
                        BASE_URL = "http://localhost:" + port;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
    }


    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // ── Generic Helpers ───────────────────────────────────────────────────────

    public static String get(String path) throws IOException {
        Request req = new Request.Builder()
                .url(BASE_URL + path)
                .get()
                .build();
        try (Response resp = HTTP.newCall(req).execute()) {
            return resp.body() != null ? resp.body().string() : "{}";
        }
    }

    public static String post(String path, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(
                jsonBody, MediaType.parse("application/json"));
        Request req = new Request.Builder()
                .url(BASE_URL + path)
                .post(body)
                .build();
        try (Response resp = HTTP.newCall(req).execute()) {
            return resp.body() != null ? resp.body().string() : "{}";
        }
    }

    public static String delete(String path) throws IOException {
        Request req = new Request.Builder()
                .url(BASE_URL + path)
                .delete()
                .build();
        try (Response resp = HTTP.newCall(req).execute()) {
            return resp.body() != null ? resp.body().string() : "{}";
        }
    }

    // ── Log Endpoints ─────────────────────────────────────────────────────────

    /**
     * Upload a log file to the backend.
     * @return JSON string with {"status":"ok","inserted":N}
     */
    public static String uploadLogFile(File file) throws IOException {
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("text/plain"));
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request req = new Request.Builder()
                .url(BASE_URL + "/logs/upload")
                .post(body)
                .build();
        try (Response resp = HTTP.newCall(req).execute()) {
            return resp.body() != null ? resp.body().string() : "{}";
        }
    }

    public static String searchLogs(String query, String severity, int page) throws IOException {
        StringBuilder url = new StringBuilder(BASE_URL + "/logs/search?page=" + page + "&page_size=100");
        if (query != null && !query.isBlank())
            url.append("&q=").append(java.net.URLEncoder.encode(query, "UTF-8"));
        if (severity != null && !severity.equals("ALL"))
            url.append("&severity=").append(severity);

        Request req = new Request.Builder().url(url.toString()).get().build();
        try (Response resp = HTTP.newCall(req).execute()) {
            return resp.body() != null ? resp.body().string() : "{}";
        }
    }

    public static String getStats() throws IOException {
        return get("/logs/stats");
    }

    public static String clearLogs() throws IOException {
        return delete("/logs/clear");
    }

    // ── Analysis Endpoints ────────────────────────────────────────────────────

    public static String runAnalysis() throws IOException {
        return post("/analysis/run", "{}");
    }

    public static String getAlerts() throws IOException {
        return get("/analysis/alerts");
    }

    // ── Report Endpoints ──────────────────────────────────────────────────────

    public static String generateReport() throws IOException {
        return post("/reports/generate", "{}");
    }

    public static String listReports() throws IOException {
        return get("/reports/list");
    }

    public static String getReportPath(int reportId) throws IOException {
        return get("/reports/" + reportId + "/path");
    }

    // ── Live Ingestion Endpoints ──────────────────────────────────────────────

    public static String startLiveMonitor() throws IOException {
        return post("/live/start", "{}");
    }

    public static String stopLiveMonitor() throws IOException {
        return post("/live/stop", "{}");
    }

    public static String getLiveStatus() throws IOException {
        return get("/live/status");
    }

    // ── AI Endpoints ──────────────────────────────────────────────────────────

    public static String explainLog(String message) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("log_message", message);
        return post("/api/ai/explain", GSON.toJson(body));
    }

    public static String getMitigationPlan(String alertName, String description) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("alert_name", alertName);
        body.addProperty("description", description);
        return post("/api/ai/mitigate", GSON.toJson(body));
    }


    // ── Convenience ───────────────────────────────────────────────────────────

    public static JsonObject parseJson(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
