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

package dev.thoq.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public final class Logger {
    public enum Level {DEBUG, INFO, WARN, ERROR}

    private static final String LOG_DIR = System.getProperty("user.home") + File.separator + ".gleamstorm";
    private static final String LOG_FILE = LOG_DIR + File.separator + "gleamstorm.log";
    private static final long MAX_BYTES = 1_000_000L;
    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static BufferedWriter writer;
    private static volatile Level consoleLevel = Level.DEBUG;
    private static volatile Level fileLevel = Level.DEBUG;

    static {
        ensureWriter();
    }

    private Logger() {
    }

    public static void setConsoleLevel(Level level) {
        consoleLevel = level;
    }

    public static void setFileLevel(Level level) {
        fileLevel = level;
    }

    public static void debug(String msg) {
        log(Level.DEBUG, msg, null);
    }

    public static void info(String msg) {
        log(Level.INFO, msg, null);
    }

    public static void warn(String msg) {
        log(Level.WARN, msg, null);
    }

    public static void error(String msg) {
        log(Level.ERROR, msg, null);
    }

    public static void error(String msg, Throwable t) {
        log(Level.ERROR, msg, t);
    }

    public static void log(Level level, String msg, Throwable t) {
        String ts = TS.format(new Date());
        String thread = Thread.currentThread().getName();
        String line = String.format("%s [%s] %-5s %s", ts, thread, level, msg);

        if(level.ordinal() >= consoleLevel.ordinal()) {
            String colored = colorize(level, line);
            System.out.println(colored);
            if(t != null) t.printStackTrace(System.out);
        }

        if(level.ordinal() >= fileLevel.ordinal()) {
            LOCK.lock();
            try {
                ensureWriter();

                writer.write(line);
                writer.newLine();

                if(t != null) {
                    writer.write(String.valueOf(t));
                    writer.newLine();

                    for(StackTraceElement el : t.getStackTrace()) {
                        writer.write("    at " + el.toString());
                        writer.newLine();
                    }
                }

                writer.flush();
                rotateIfNeeded();
            } catch(IOException ignored) {
            } finally {
                LOCK.unlock();
            }
        }
    }

    private static String colorize(Level level, String s) {
        String RESET = "\u001B[0m";
        String GRAY = "\u001B[90m";
        String GREEN = "\u001B[32m";
        String YELLOW = "\u001B[33m";
        String RED = "\u001B[31m";

        return switch(level) {
            case DEBUG -> GRAY + s + RESET;
            case INFO -> GREEN + s + RESET;
            case WARN -> YELLOW + s + RESET;
            case ERROR -> RED + s + RESET;
        };
    }

    private static void ensureWriter() {
        LOCK.lock();

        try {
            if(writer == null) {
                try {
                    File dir = new File(LOG_DIR);
                    if(!dir.exists())
                        dir.mkdirs();

                    File f = new File(LOG_FILE);
                    writer = new BufferedWriter(new FileWriter(f, true));
                } catch(IOException ignored) {
                }
            }
        } finally {
            LOCK.unlock();
        }
    }

    private static void rotateIfNeeded() {
        File f = new File(LOG_FILE);

        if(f.length() > MAX_BYTES) {
            try {
                writer.close();
            } catch(Exception ignored) {
            }

            File bak = new File(LOG_FILE + ".1");

            if(bak.exists())
                bak.delete();

            f.renameTo(bak);

            writer = null;

            ensureWriter();
        }
    }
}
