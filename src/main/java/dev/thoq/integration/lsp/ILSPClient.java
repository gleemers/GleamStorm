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
package dev.thoq.integration.lsp;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface ILSPClient {
    boolean connect();
    void disconnect();
    boolean isConnected();
    boolean testConnection();
    void setDiagnosticsListener(IDiagnosticsListener listener);
    void setWorkspaceRoot(String path);
    void openDocument(String filePath, String content);
    void saveDocument(String filePath, String text);
    String formatDocument(String filePath);
    boolean checkSyntax(String filePath, String content);
    void requestHover(String filePath, int line, int ch, Consumer<String> callback);
    String displayName();
}
