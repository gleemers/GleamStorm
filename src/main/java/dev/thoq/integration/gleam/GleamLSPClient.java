/*
 * Copyright (c) GleamStorm 2025.
 *
 *  This file is a part of the GleamStorm IDE, an IDE for
 *  the Gleam programming language.
 *
 * GleamStorm GitHub: https://github.com/gleemers/gleamstorm.git
 *
 * GleamStorm does NOT come with a warranty.
 *
 * GleamStorm is licensed under the MIT license.
 * Whilst contributing, modifying, or distributing, make sure
 * you agree to the MIT license.
 * If you did not receive a copy of the MIT license,
 * you may obtain one here:
 * MIT License: https://opensource.org/license/mit
 */

package dev.thoq.integration.gleam;

import dev.thoq.integration.lsp.IDiagnosticsListener;
import dev.thoq.integration.lsp.RDiagnostic;
import dev.thoq.log.Logger;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("SameParameterValue")
public class GleamLSPClient implements dev.thoq.integration.lsp.ILSPClient {
    private final java.util.concurrent.ConcurrentHashMap<Integer, java.util.function.Consumer<String>> hoverCallbacks = new java.util.concurrent.ConcurrentHashMap<>();

    private Process lspProcess;
    private BufferedWriter writer;
    private InputStream inputStream;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private int requestId = 0;
    private final boolean debugMode = false;
    private Thread responseHandler;
    private String currentDocUri = null;
    private int currentDocVersion = 0;
    private String workspaceRoot = null;
    private IDiagnosticsListener diagnosticsListener;

    public boolean connect() {
        return connectWithFallback();
    }

    private boolean connectWithFallback() {
        if(debugMode) {
            Logger.info("Attempting to connect to Gleam LSP...");
        }

        if(tryGleamLSP()) {
            return true;
        }

        if(debugMode) {
            Logger.info("Direct gleam integration failed, trying alternative methods...");
        }

        if(tryAlternativeCommands()) {
            return true;
        }

        if(debugMode) {
            Logger.info("All LSP connection attempts failed. Running in offline mode.");
            Logger.info("To enable LSP features:");
            Logger.info("1. Install Gleam: brew install gleam (macOS) or from https://gleam.run");
            Logger.info("2. Verify with: gleam --version");
            Logger.info("3. Make sure you're in a directory with gleam.toml file");
        }

        return false;
    }

    private void startResponseHandler() {
        responseHandler = new Thread(() -> {
            try {
                while(connected.get()) {
                    String json = readLspMessage();
                    if(json == null) {
                        break;
                    }
                    if(debugMode && requestId <= 5) {
                        Logger.info("LSP Response: " + json);
                    }
                    handleIncoming(json);
                }
            } catch(IOException e) {
                if(debugMode && connected.get()) {
                    Logger.error("LSP connection lost: " + e.getMessage());
                }
                connected.set(false);
            }
        });
        responseHandler.setDaemon(true);
        responseHandler.start();
    }

    private String readLine(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while((b = is.read()) != -1) {
            if(b == '\n') {
                break;
            }
            if(b != '\r') {
                baos.write(b);
            }
        }
        if(b == -1 && baos.size() == 0) return null;
        return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String readLspMessage() throws IOException {
        if(inputStream == null) return null;
        int contentLength = -1;

        while(true) {
            String line = readLine(inputStream);
            if(line == null) return null;
            if(line.isEmpty()) break;
            String lower = line.toLowerCase();
            if(lower.startsWith("content-length:")) {
                String num = line.substring(line.indexOf(':') + 1).trim();
                try { contentLength = Integer.parseInt(num); } catch (NumberFormatException ignored) {}
            }
        }
        if(contentLength < 0) return null;
        byte[] buf = inputStream.readNBytes(contentLength);
        return new String(buf, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void handleIncoming(String json) {
        if(json == null) return;
        if(json.contains("textDocument/publishDiagnostics")) {
            String uri = extractString(json, "uri");
            java.util.List<RDiagnostic> list = extractDiagnostics(json);
            if(diagnosticsListener != null && uri != null) {
                try {
                    diagnosticsListener.onDiagnostics(uri, list);
                } catch(Exception ignored) {}
            }
            return;
        }
        int id = extractId(json);
        if(id != -1) {
            java.util.function.Consumer<String> cb = hoverCallbacks.remove(id);
            if(cb != null) {
                String hoverText = parseHoverText(json);
                try { cb.accept(hoverText); } catch (Exception ignored) {}
            }
        }
    }

    private String extractString(String json, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"");
        java.util.regex.Matcher m = p.matcher(json);
        if(m.find()) return m.group(1);
        return null;
    }

    private int extractId(String json) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher m = p.matcher(json);
            if(m.find()) return Integer.parseInt(m.group(1));
        } catch(Exception ignored) {}
        return -1;
    }

    private String parseHoverText(String json) {
        java.util.regex.Pattern pValue = java.util.regex.Pattern.compile("\"value\"\\s*:\\s*\"(.*?)\"", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher mValue = pValue.matcher(json);
        if(mValue.find()) {
            return unescapeJsonString(mValue.group(1));
        }
        java.util.regex.Pattern pContents = java.util.regex.Pattern.compile("\"contents\"\\s*:\\s*\"(.*?)\"", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher mContents = pContents.matcher(json);
        if(mContents.find()) {
            return unescapeJsonString(mContents.group(1));
        }
        java.util.regex.Pattern pFirstValue = java.util.regex.Pattern.compile("\"contents\"\\s*:\\s*\\[(.*?)]", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher mArr = pFirstValue.matcher(json);
        if(mArr.find()) {
            String arr = mArr.group(1);
            java.util.regex.Matcher mv2 = pValue.matcher(arr);
            if(mv2.find()) return unescapeJsonString(mv2.group(1));
            java.util.regex.Pattern pStr = java.util.regex.Pattern.compile("\"(.*?)æ\"");
            java.util.regex.Matcher mStr = pStr.matcher(arr);
            if(mStr.find()) return unescapeJsonString(mStr.group(1));
        }
        return null;
    }

    private String unescapeJsonString(String s) {
        if(s == null) return null;
        return s.replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r").replace("\\\"", "\"");
    }

    private java.util.List<RDiagnostic> extractDiagnostics(String json) {
        int dIdx = json.indexOf("\"diagnostics\"");
        if(dIdx == -1) return java.util.Collections.emptyList();
        int arrStart = json.indexOf('[', dIdx);
        if(arrStart == -1) return java.util.Collections.emptyList();
        int arrEnd = findMatching(json, arrStart, '[', ']');
        if(arrEnd == -1) return java.util.Collections.emptyList();
        String array = json.substring(arrStart + 1, arrEnd);
        java.util.List<RDiagnostic> list = new java.util.ArrayList<>();
        int idx = 0;
        while(idx < array.length()) {
            int objStart = array.indexOf('{', idx);
            if(objStart == -1) break;
            int objEnd = findMatching(array, objStart, '{', '}');
            if(objEnd == -1) break;
            String obj = array.substring(objStart, objEnd + 1);
            RDiagnostic d = parseDiagnostic(obj);
            list.add(d);
            idx = objEnd + 1;
        }
        return list;
    }

    private int findMatching(String s, int start, char open, char close) {
        int depth = 0;
        for(int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if(c == open) depth++;
            else if(c == close) {
                depth--;
                if(depth == 0) return i;
            }
        }
        return -1;
    }

    private RDiagnostic parseDiagnostic(String obj) {
        java.util.regex.Pattern pStart = java.util.regex.Pattern.compile("\"start\"\\s*:\\s*\\{[^}]*?\"line\"\\s*:\\s*(\\d+)[^}]*?\"character\"\\s*:\\s*(\\d+)", java.util.regex.Pattern.DOTALL);
        java.util.regex.Pattern pEnd = java.util.regex.Pattern.compile("\"end\"\\s*:\\s*\\{[^}]*?\"line\"\\s*:\\s*(\\d+)[^}]*?\"character\"\\s*:\\s*(\\d+)", java.util.regex.Pattern.DOTALL);
        java.util.regex.Pattern pMsg = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"(.*?)\"", java.util.regex.Pattern.DOTALL);
        java.util.regex.Pattern pSev = java.util.regex.Pattern.compile("\"severity\"\\s*:\\s*(\\d+)");
        java.util.regex.Matcher mStart = pStart.matcher(obj);
        java.util.regex.Matcher mEnd = pEnd.matcher(obj);
        java.util.regex.Matcher mMsg = pMsg.matcher(obj);
        java.util.regex.Matcher mSev = pSev.matcher(obj);
        int sl = 0, sc = 0, el = 0, ec = 0;
        String msg = null;
        Integer sev = null;
        if(mStart.find()) { sl = Integer.parseInt(mStart.group(1)); sc = Integer.parseInt(mStart.group(2)); }
        if(mEnd.find()) { el = Integer.parseInt(mEnd.group(1)); ec = Integer.parseInt(mEnd.group(2)); }
        if(mMsg.find()) { msg = mMsg.group(1); }
        if(mSev.find()) { sev = Integer.parseInt(mSev.group(1)); }
        return new RDiagnostic(sl, sc, el, ec, msg != null ? msg : "", sev);
    }

    public void setDiagnosticsListener(IDiagnosticsListener listener) {
        this.diagnosticsListener = listener;
    }

    private boolean tryGleamLSP() {
        return tryCommand(new String[]{"gleam", "lsp"}) ||
                tryCommand(new String[]{"gleam", "language-server"});
    }

    private boolean tryAlternativeCommands() {
        String os = System.getProperty("os.name").toLowerCase();

        if(os.contains("win")) {
            return tryCommand(new String[]{"cmd", "/c", "gleam", "lsp"}) ||
                    tryCommand(new String[]{"powershell", "-Command", "gleam", "lsp"}) ||
                    tryCommand(new String[]{"gleam.exe", "lsp"});
        } else {
            return tryCommand(new String[]{"sh", "-c", "gleam lsp"}) ||
                    tryCommand(new String[]{"bash", "-c", "gleam lsp"}) ||
                    tryCommand(new String[]{"/usr/local/bin/gleam", "lsp"}) ||
                    tryCommand(new String[]{"/opt/homebrew/bin/gleam", "lsp"}) ||
                    tryCommand(new String[]{System.getProperty("user.home") + "/.gleam/bin/gleam", "lsp"}) ||
                    tryCommand(new String[]{"./gleam", "lsp"});
        }
    }

    private boolean tryCommand(String[] command) {
        try {
            if(debugMode) {
                Logger.info("Trying command: " + String.join(" ", command));
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);

            String currentPath = System.getenv("PATH");
            String additionalPaths = System.getProperty("user.home") + "/.gleam/bin" +
                    File.pathSeparator + "/usr/local/bin" +
                    File.pathSeparator + "/opt/homebrew/bin" +
                    File.pathSeparator + "C:\\Program Files\\gleam\\bin";

            pb.environment().put("PATH", currentPath + File.pathSeparator + additionalPaths);

            lspProcess = pb.start();

            Thread.sleep(100);

            if(!lspProcess.isAlive()) {
                if(debugMode)
                    Logger.info("Process failed to start");

                return false;
            }

            writer = new BufferedWriter(new OutputStreamWriter(lspProcess.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));
            inputStream = lspProcess.getInputStream();

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(lspProcess.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8));
            new Thread(() -> {
                try {
                    String line;
                    while((line = errorReader.readLine()) != null) {
                        if(debugMode && !line.trim().isEmpty()) {
                            Logger.error("LSP Error: " + line);
                        }
                    }
                } catch(IOException e) {
                    // Ignore
                }
            }).start();

            connected.set(true);

            startResponseHandler();

            if(sendInitializeRequest()) {
                if(debugMode)
                    Logger.info("Successfully connected to Gleam LSP with command: " + String.join(" ", command));
                return true;
            } else {
                connected.set(false);
                return false;
            }

        } catch(Exception e) {
            if(debugMode)
                Logger.info("Command failed: " + e.getMessage());

            connected.set(false);
            if(lspProcess != null) {
                lspProcess.destroyForcibly();
                lspProcess = null;
            }
        }

        return false;
    }

    public boolean isConnected() {
        return connected.get() && lspProcess != null && lspProcess.isAlive();
    }

    public boolean testConnection() {
        if(debugMode) {
            Logger.info("=== Testing Gleam LSP Connection ===");
            Logger.info("Current directory: " + System.getProperty("user.dir"));
            Logger.info("Java version: " + System.getProperty("java.version"));
            Logger.info("OS: " + System.getProperty("os.name"));
            Logger.info("PATH: " + System.getenv("PATH"));

            File gleamToml = new File("gleam.toml");

            if(gleamToml.exists())
                Logger.info("gleam.toml found: YES");
            else
                Logger.info("gleam.toml found: NO (LSP may not work properly)");

            testGleamInstallation();

            if(isConnected()) {
                Logger.info("LSP Status: CONNECTED");
                Logger.info("Process alive: " + lspProcess.isAlive());
                Logger.info("Connected flag: " + connected.get());
                return true;
            } else {
                Logger.info("LSP Status: NOT CONNECTED");
                Logger.info("Process alive: " + (lspProcess != null ? lspProcess.isAlive() : "null"));
                Logger.info("Connected flag: " + connected.get());
                return false;
            }
        }

        return isConnected();
    }

    private void testGleamInstallation() {
        try {
            ProcessBuilder pb = new ProcessBuilder("gleam", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            if(line != null)
                Logger.info("Gleam version: " + line);
            else
                Logger.info("Gleam version: Could not determine");

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if(!finished)
                Logger.info("Version check timed out");

        } catch(Exception e) {
            Logger.info("Gleam installation check failed: " + e.getMessage());
            Logger.info("Install Gleam: brew install gleam (macOS) or https://gleam.run/getting-started/installing/");
        }
    }

    private boolean sendInitializeRequest() {
        try {
            String root = (workspaceRoot != null && !workspaceRoot.isEmpty()) ? workspaceRoot : System.getProperty("user.dir");
            String rootUri = "file://" + root.replace("\\", "/");

            String initRequest = loadTemplate("init.json");
            initRequest = initRequest
                    .replace("\"id\": 1", "\"id\": " + (++requestId))
                    .replace("\"processId\": null", "\"processId\": " + ProcessHandle.current().pid())
                    .replace("\"rootUri\": null", "\"rootUri\": \"" + rootUri + "\"");

            if(debugMode)
                Logger.info("Initializing LSP with rootUri: " + rootUri);

            sendRequestDirect(initRequest);
            Thread.sleep(500);

            String initializedRequest = loadTemplate("initialized.json");
            sendNotificationDirect("initialized", initializedRequest);

            if(debugMode)
                Logger.info("Initialize request sent successfully");

            return true;
        } catch(Exception e) {
            if(debugMode)
                Logger.error("Failed to send initialize request: " + e.getMessage());

            return false;
        }
    }

    private String loadTemplate(String filename) throws IOException {
        try(InputStream is = getClass().getResourceAsStream("/lsp/gleam/" + filename)) {
            if(is == null)
                throw new IOException("Template not found: " + filename);

            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private synchronized void sendRequestDirect(String content) throws IOException {
        byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String message = "Content-Length: " + bytes.length + "\r\n\r\n" + content;

        if(debugMode && requestId <= 3)
            Logger.info("Sending LSP request: " + message);

        writer.write(message);
        writer.flush();
    }

    private synchronized void sendRequest(String content) throws IOException {
        if(!isConnected())
            throw new IOException("LSP not connected");

        sendRequestDirect(content);
    }

    private void sendNotificationDirect(String method, String params) throws IOException {
        String notification = "{\n" +
                "  \"jsonrpc\": \"2.0\",\n" +
                "  \"method\": \"" + method + "\",\n" +
                "  \"params\": " + params + "\n" +
                "}";
        sendRequestDirect(notification);
    }

    public void openDocument(String filePath, String content) {
        if(!isConnected()) {
            if(debugMode) Logger.info("Cannot open document - LSP not connected");
            return;
        }

        try {
            String uri = "file://" + filePath.replace("\\", "/");
            currentDocUri = uri;
            currentDocVersion = 1;

            String params = loadTemplate("didOpen.json");
            params = params
                    .replace("\"uri\": null", "\"uri\": \"" + uri + "\"")
                    .replace("\"version\": 1", "\"version\": " + currentDocVersion)
                    .replace("\"text\": null", "\"text\": \"" + escapeJson(content) + "\"");

            sendNotification("textDocument/didOpen", params);
        } catch(IOException e) {
            if(debugMode)
                Logger.error("Failed to open document: " + e.getMessage());

            connected.set(false);
        }
    }

    public void saveDocument(String filePath, String ignoredContent) {
        if(!isConnected()) {
            if(debugMode) Logger.info("Cannot save document - LSP not connected");
            return;
        }

        try {
            String params = loadTemplate("didSave.json");
            params = params.replace("\"uri\": null", "\"uri\": \"file://" + filePath.replace("\\", "/") + "\"");

            sendNotification("textDocument/didSave", params);
        } catch(IOException e) {
            if(debugMode)
                Logger.error("Failed to save document: " + e.getMessage());

            connected.set(false);
        }
    }

    public String formatDocument(String filePath) {
        if(!isConnected()) {
            if(debugMode) Logger.info("Cannot format document - LSP not connected");
            return null;
        }

        try {
            String formatRequest = loadTemplate("formatting.json");
            formatRequest = formatRequest
                    .replace("\"id\": 1", "\"id\": " + (++requestId))
                    .replace("\"uri\": null", "\"uri\": \"file://" + filePath.replace("\\", "/") + "\"");

            sendRequest(formatRequest);
            return null;
        } catch(IOException e) {
            if(debugMode) {
                Logger.error("Failed to format document: " + e.getMessage());
            }
            connected.set(false);
            return null;
        }
    }

    public void requestHover(String filePath, int line, int ch, java.util.function.Consumer<String> callback) {
        if(!isConnected()) {
            if(callback != null) try { callback.accept(null); } catch (Exception ignored) {}
            return;
        }

        int id = ++requestId;
        if(callback != null) hoverCallbacks.put(id, callback);

        try {
            String uri = "file://" + filePath.replace("\\", "/");
            String params = "{\n" +
                    "  \"textDocument\": { \"uri\": \"" + uri + "\" },\n" +
                    "  \"position\": { \"line\": " + line + ", \"character\": " + ch + " }\n" +
                    "}";
            String req = "{\n" +
                    "  \"jsonrpc\": \"2.0\",\n" +
                    "  \"id\": " + id + ",\n" +
                    "  \"method\": \"textDocument/hover\",\n" +
                    "  \"params\": " + params + "\n" +
                    "}";
            sendRequest(req);
        } catch(IOException e) {
            hoverCallbacks.remove(id);
            if(callback != null) try { callback.accept(null); } catch (Exception ignored) {}
        }
    }

    public boolean checkSyntax(String filePath, String content) {
        if(!isConnected()) {
            if(debugMode) Logger.info("Cannot check syntax - LSP not connected");
            return true;
        }

        try {
            String uri = "file://" + filePath.replace("\\", "/");

            if(!uri.equals(currentDocUri)) {
                currentDocUri = uri;
                currentDocVersion = 1;
            } else {
                if(currentDocVersion < Integer.MAX_VALUE - 2) {
                    currentDocVersion++;
                }
            }

            String params = loadTemplate("didChange.json");
            params = params
                    .replace("\"uri\": null", "\"uri\": \"" + uri + "\"")
                    .replace("\"version\": 1", "\"version\": " + currentDocVersion)
                    .replace("\"text\": null", "\"text\": \"" + escapeJson(content) + "\"");

            sendNotification("textDocument/didChange", params);

            return true;
        } catch(IOException e) {
            if(debugMode)
                Logger.error("Failed to check syntax: " + e.getMessage());

            connected.set(false);
            return false;
        }
    }

    private void sendNotification(String method, String params) throws IOException {
        String notification = "{\n" +
                "  \"jsonrpc\": \"2.0\",\n" +
                "  \"method\": \"" + method + "\",\n" +
                "  \"params\": " + params + "\n" +
                "}";
        sendRequest(notification);
    }

    private String escapeJson(String text) {
        if(text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    public void disconnect() {
        connected.set(false);
        if(responseHandler != null)
            responseHandler.interrupt();

        if(lspProcess != null)
            lspProcess.destroyForcibly();

        try {
            if(writer != null) writer.close();
            if(inputStream != null) inputStream.close();
        } catch(IOException e) {
            if(debugMode)
                Logger.error("Error closing LSP connections: " + e.getMessage());
        }
    }

    public void setWorkspaceRoot(String path) {
        this.workspaceRoot = path;
    }

    @Override
    public String displayName() {
        return "Gleam LSP";
    }
}
