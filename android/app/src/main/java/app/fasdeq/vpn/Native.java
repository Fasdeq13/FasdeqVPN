package app.fasdeq.vpn;

public final class Native {
    static {
        System.loadLibrary("fasdeq_native");
    }

    private Native() {
    }

    public static native String parseLink(String conf);

    public static native String prepareImport(String input);

    public static native String fetchSubscription(String url);

    public static native String ping(String conf, String endpoint, int mode, long timeoutMs);

    public static native String innerConfig(String outbound, int socks, int http, String dns);

    public static native String pluginOpen(String path);

    public static native String pluginCall(long handle, String op, String config, int fd, long arg);

    public static native String pluginClose(long handle);
}
