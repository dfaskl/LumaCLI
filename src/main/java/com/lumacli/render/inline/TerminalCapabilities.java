package com.lumacli.render.inline;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;

/**
 * 终端能力探测：决定 inline 渲染器的各项特性是否可启用。
 *
 * <p>探测逻辑保守——能开则开，老终端 / 非 TTY 环境优雅降级。
 */
public final class TerminalCapabilities {

    private TerminalCapabilities() {
    }

    /** 终端是否能渲染 ANSI 转义序列（颜色、光标控制、inline status 等）。 */
    public static boolean supportsAnsi(Terminal terminal) {
        if (terminal == null) {
            return false;
        }
        if (System.getenv("NO_COLOR") != null) {
            return true;
        }
        // Windows 10+ 控制台原生支持 ANSI（即使 JLine 因 JNI 加载失败报告 dumb）
        if (isWindows10OrLater()) {
            return true;
        }
        String type = terminal.getType();
        if (type != null && type.equalsIgnoreCase("dumb")) {
            return false;
        }
        String envTerm = System.getenv("TERM");
        return envTerm == null || !envTerm.equalsIgnoreCase("dumb");
    }

    /** Windows cmd.exe 不支持 DECSTBM 滚动区域，状态栏需要降级。 */
    static boolean isDumbTerminal(Terminal terminal) {
        if (terminal == null) return true;
        String type = terminal.getType();
        return type != null && type.equalsIgnoreCase("dumb");
    }

    private static boolean isWindows10OrLater() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("windows")) return false;
        try {
            String[] parts = System.getProperty("os.version", "0").split("\\.");
            return Integer.parseInt(parts[0]) >= 10;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 终端是否适合启用 inline status 状态区。
     * 同时校验终端尺寸合理（rows ≥ 5）。
     * Windows cmd.exe 等 dumb 终端不支持 DECSTBM 滚动区域，禁用状态栏。
     */
    public static boolean supportsScrollRegion(Terminal terminal) {
        if (!supportsAnsi(terminal)) {
            return false;
        }
        // cmd.exe 等 dumb 终端不支持 DECSTBM
        if (isDumbTerminal(terminal)) {
            return false;
        }
        if (Boolean.parseBoolean(System.getenv("LUMACLI_NO_STATUSBAR"))) {
            return false;
        }
        if (Boolean.parseBoolean(System.getProperty("lumacli.no.statusbar"))) {
            return false;
        }
        Size size = safeSize(terminal);
        return size.getRows() >= 5 && size.getColumns() >= 20;
    }

    /** 终端是否支持 24-bit TrueColor（用于丰富的代码高亮等）。 */
    public static boolean supportsTrueColor() {
        String colorterm = System.getenv("COLORTERM");
        return "truecolor".equalsIgnoreCase(colorterm) || "24bit".equalsIgnoreCase(colorterm);
    }

    public static Size safeSize(Terminal terminal) {
        try {
            Size s = terminal.getSize();
            if (s == null || s.getRows() <= 0 || s.getColumns() <= 0) {
                return new Size(80, 24);
            }
            return s;
        } catch (Exception e) {
            return new Size(80, 24);
        }
    }
}
