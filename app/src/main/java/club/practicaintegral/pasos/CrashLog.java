package club.practicaintegral.pasos;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Guarda el último fallo de la app para enseñarlo en pantalla la siguiente vez (no hay otra forma de verlo en el móvil). */
final class CrashLog {
    private CrashLog() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("fallos", Context.MODE_PRIVATE);
    }

    static void install(Context ctx) {
        final Context c = ctx.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        if (previous instanceof Handler) return;
        Thread.setDefaultUncaughtExceptionHandler(new Handler(c, previous));
    }

    static String takeLast(Context c) {
        String s = prefs(c).getString("last", null);
        if (s != null) prefs(c).edit().remove("last").apply();
        return s;
    }

    private static final class Handler implements Thread.UncaughtExceptionHandler {
        private final Context c;
        private final Thread.UncaughtExceptionHandler previous;

        Handler(Context c, Thread.UncaughtExceptionHandler previous) {
            this.c = c;
            this.previous = previous;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String text = sw.toString();
            if (text.length() > 1500) text = text.substring(0, 1500);
            prefs(c).edit().putString("last", text).commit();
            if (previous != null) previous.uncaughtException(t, e);
        }
    }
}
