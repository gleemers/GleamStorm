package dev.thoq.integration.gleam;

import dev.thoq.log.Logger;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("SameParameterValue")
public class GleamLSPClient {
    private Process lspProcess;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private int requestId = 0;
    private final boolean debugMode = true;
    private Thread responseHandler;
    private String currentDocUri = null;
    private int currentDocVersion = 0;
    private String workspaceRoot = null;

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
                String line;
                while(connected.get() && (line = reader.readLine()) != null) {
                    if(debugMode && requestId <= 5) {
                        Logger.info("LSP Response: " + line);
                    }
                }
            } catch(IOException e) {
                if(connected.get()) {
                    System.err.println("LSP connection lost: " + e.getMessage());
                    connected.set(false);
                }
            }
        });
        responseHandler.setDaemon(true);
        responseHandler.start();
    }

    private boolean tryGleamLSP() {
        return tryCommand(new String[]{"gleam", "integration"}) ||
                tryCommand(new String[]{"gleam", "language-server"});
    }

    private boolean tryAlternativeCommands() {
        String os = System.getProperty("os.name").toLowerCase();

        if(os.contains("win")) {
            return tryCommand(new String[]{"cmd", "/c", "gleam", "integration"}) ||
                    tryCommand(new String[]{"powershell", "-Command", "gleam", "integration"}) ||
                    tryCommand(new String[]{"gleam.exe", "integration"});
        } else {
            return tryCommand(new String[]{"sh", "-c", "gleam integration"}) ||
                    tryCommand(new String[]{"bash", "-c", "gleam integration"}) ||
                    tryCommand(new String[]{"/usr/local/bin/gleam", "integration"}) ||
                    tryCommand(new String[]{"/opt/homebrew/bin/gleam", "integration"}) ||
                    tryCommand(new String[]{System.getProperty("user.home") + "/.gleam/bin/gleam", "integration"}) ||
                    tryCommand(new String[]{"./gleam", "integration"});
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
                if(debugMode) {
                    Logger.info("Process failed to start");
                }
                return false;
            }

            writer = new BufferedWriter(new OutputStreamWriter(lspProcess.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(lspProcess.getInputStream()));

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(lspProcess.getErrorStream()));
            new Thread(() -> {
                try {
                    String line;
                    while((line = errorReader.readLine()) != null) {
                        if(debugMode && !line.trim().isEmpty()) {
                            System.err.println("LSP Error: " + line);
                        }
                    }
                } catch(IOException e) {
                    // Ignore
                }
            }).start();

            connected.set(true);

            startResponseHandler();

            if(sendInitializeRequest()) {
                if(debugMode) {
                    Logger.info("Successfully connected to Gleam LSP with command: " + String.join(" ", command));
                }
                return true;
            } else {
                connected.set(false);
                return false;
            }

        } catch(Exception e) {
            if(debugMode) {
                Logger.info("Command failed: " + e.getMessage());
            }
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
            if(gleamToml.exists()) {
                Logger.info("gleam.toml found: YES");
            } else {
                Logger.info("gleam.toml found: NO (LSP may not work properly)");
            }

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

            if(line != null) {
                Logger.info("Gleam version: " + line);
            } else {
                Logger.info("Gleam version: Could not determine");
            }

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if(!finished) {
                Logger.info("Version check timed out");
            }
        } catch(Exception e) {
            Logger.info("Gleam installation check failed: " + e.getMessage());
            Logger.info("Install Gleam: brew install gleam (macOS) or https://gleam.run/getting-started/installing/");
        }
    }

    private boolean sendInitializeRequest() {
        try {
            String root = (workspaceRoot != null && !workspaceRoot.isEmpty()) ? workspaceRoot : System.getProperty("user.dir");
            String rootUri = "file://" + root.replace("\\", "/");
            String initRequest = "{\n" +
                    "  \"jsonrpc\": \"2.0\",\n" +
                    "  \"id\": " + (++requestId) + ",\n" +
                    "  \"method\": \"initialize\",\n" +
                    "  \"params\": {\n" +
                    "    \"processId\": " + ProcessHandle.current().pid() + ",\n" +
                    "    \"rootUri\": \"" + rootUri + "\",\n" +
                    "    \"capabilities\": {\n" +
                    "      \"textDocument\": {\n" +
                    "        \"synchronization\": {\n" +
                    "          \"dynamicRegistration\": true,\n" +
                    "          \"willSave\": true,\n" +
                    "          \"didSave\": true\n" +
                    "        },\n" +
                    "        \"completion\": {\n" +
                    "          \"dynamicRegistration\": true\n" +
                    "        },\n" +
                    "        \"hover\": {\n" +
                    "          \"dynamicRegistration\": true\n" +
                    "        },\n" +
                    "        \"formatting\": {\n" +
                    "          \"dynamicRegistration\": true\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            if(debugMode) {
                Logger.info("Initializing LSP with rootUri: " + rootUri);
            }

            sendRequestDirect(initRequest);

            Thread.sleep(500);

            sendNotificationDirect("initialized", "{}");

            if(debugMode) {
                Logger.info("Initialize request sent successfully");
            }

            return true;
        } catch(Exception e) {
            if(debugMode) {
                System.err.println("Failed to send initialize request: " + e.getMessage());
            }
            return false;
        }
    }

    private synchronized void sendRequestDirect(String content) throws IOException {
        String message = "Content-Length: " + content.getBytes().length + "\r\n\r\n" + content;
        if(debugMode && requestId <= 3) {
            Logger.info("Sending LSP request: " + message);
        }
        writer.write(message);
        writer.flush();
    }

    private synchronized void sendRequest(String content) throws IOException {
        if(!isConnected()) {
            throw new IOException("LSP not connected");
        }
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

    private void sendNotification(String method, String params) throws IOException {
        String notification = "{\n" +
                "  \"jsonrpc\": \"2.0\",\n" +
                "  \"method\": \"" + method + "\",\n" +
                "  \"params\": " + params + "\n" +
                "}";
        sendRequest(notification);
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
            String params = "{\n" +
                    "  \"textDocument\": {\n" +
                    "    \"uri\": \"" + uri + "\",\n" +
                    "    \"languageId\": \"gleam\",\n" +
                    "    \"version\": " + currentDocVersion + ",\n" +
                    "    \"text\": \"" + escapeJson(content) + "\"\n" +
                    "  }\n" +
                    "}";
            sendNotification("textDocument/didOpen", params);
        } catch(IOException e) {
            if(debugMode) {
                System.err.println("Failed to open document: " + e.getMessage());
            }
            connected.set(false);
        }
    }

    public void saveDocument(String filePath, String ignoredContent) {
        if(!isConnected()) {
            if(debugMode) Logger.info("Cannot save document - LSP not connected");
            return;
        }

        try {
            String params = "{\n" +
                    "  \"textDocument\": {\n" +
                    "    \"uri\": \"file://" + filePath.replace("\\", "/") + "\"\n" +
                    "  }\n" +
                    "}";
            sendNotification("textDocument/didSave", params);
        } catch(IOException e) {
            if(debugMode) {
                System.err.println("Failed to save document: " + e.getMessage());
            }
            connected.set(false);
        }
    }

    public String formatDocument(String filePath) {
        if(!isConnected()) {
            if(debugMode) Logger.info("Cannot format document - LSP not connected");
            return null;
        }

        try {
            String formatRequest = "{\n" +
                    "  \"jsonrpc\": \"2.0\",\n" +
                    "  \"id\": " + (++requestId) + ",\n" +
                    "  \"method\": \"textDocument/formatting\",\n" +
                    "  \"params\": {\n" +
                    "    \"textDocument\": {\n" +
                    "      \"uri\": \"file://" + filePath.replace("\\", "/") + "\"\n" +
                    "    },\n" +
                    "    \"options\": {\n" +
                    "      \"tabSize\": 2,\n" +
                    "      \"insertSpaces\": true\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
            sendRequest(formatRequest);
            return null;
        } catch(IOException e) {
            if(debugMode) {
                System.err.println("Failed to format document: " + e.getMessage());
            }
            connected.set(false);
            return null;
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

            String params = "{\n" +
                    "  \"textDocument\": {\n" +
                    "    \"uri\": \"" + uri + "\",\n" +
                    "    \"version\": " + currentDocVersion + "\n" +
                    "  },\n" +
                    "  \"contentChanges\": [\n" +
                    "    {\n" +
                    "      \"text\": \"" + escapeJson(content) + "\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            sendNotification("textDocument/didChange", params);
            return true;
        } catch(IOException e) {
            if(debugMode) {
                System.err.println("Failed to check syntax: " + e.getMessage());
            }
            connected.set(false);
            return false;
        }
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
        if(responseHandler != null) {
            responseHandler.interrupt();
        }
        if(lspProcess != null) {
            lspProcess.destroyForcibly();
        }
        try {
            if(writer != null) writer.close();
            if(reader != null) reader.close();
        } catch(IOException e) {
            if(debugMode) {
                System.err.println("Error closing LSP connections: " + e.getMessage());
            }
        }
    }

    public void setWorkspaceRoot(String path) {
        this.workspaceRoot = path;
    }
}