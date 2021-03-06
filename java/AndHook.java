package andhook.lib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import android.os.Build;
import android.util.Log;
import android.util.Pair;

/**
 * @author rrrfff
 * @version 2.7.2
 */
@SuppressWarnings({"unused", "WeakerAccess", "JniMissingFunction"})
public final class AndHook {
    private final static String LOG_TAG = "AndHook";

    static {
        try {
            System.loadLibrary("AndHook");
        } catch (final UnsatisfiedLinkError e0) {
            try {
                // compatible with libhoudini
                System.loadLibrary("AndHookCompat");
            } catch (final UnsatisfiedLinkError e1) {
                // still failed, YunOS?
                throw new UnsatisfiedLinkError("incompatible platform, " + e0.getMessage());
            }
        }
    }

    public static void ensureNativeLibraryLoaded() {
        final AndHook dummy = new AndHook();
    }

    public static native int hook(final Method origin, final Method replace);

    public static native int hook(final Class<?> clazz, final String name,
                                  final String signature, final Method replace);

    public static native boolean recover(final int slot, final Method origin);

    public static native void hookNoBackup(final Method origin,
                                           final Method replace);

    public static native void hookNoBackup(final Class<?> clazz, final String name,
                                           final String signature, final Method replace);

    public static native boolean suspendAll();

    public static native void resumeAll();

    public static native void ensureClassInitialized(final Class<?> origin);

    public static native void enableFastDexLoad(final boolean enable);

    public static native void deoptimizeMethod(final Method target);

    @Deprecated
    public static native void replaceMethod(final Method origin,
                                            final Method replace);

    private static native void dumpClassMethods(final Class<?> clazz,
                                                final String clsname);

    public static void dumpClassMethods(final Class<?> clazz) {
        dumpClassMethods(clazz, null);
    }

    public static void dumpClassMethods(final String clsname) {
        dumpClassMethods(null, clsname);
    }

    public static void invokeVoidMethod(final int slot, final Object receiver,
                                        final Object... params) {
        if (android.os.Build.VERSION.SDK_INT <= 20) {
            Dalvik.invokeVoidMethod(slot, receiver, params);
        } else {
            invokeMethod(slot, receiver, params);
        }
    }

    public static boolean invokeBooleanMethod(final int slot,
                                              final Object receiver, final Object... params) {
        if (android.os.Build.VERSION.SDK_INT <= 20) {
            return Dalvik.invokeBooleanMethod(slot, receiver, params);
        } else {
            return (boolean) invokeMethod(slot, receiver, params);
        }
    }

    public static byte invokeByteMethod(final int slot, final Object receiver,
                                        final Object... params) {
        if (android.os.Build.VERSION.SDK_INT <= 20) {
            return Dalvik.invokeByteMethod(slot, receiver, params);
        } else {
            return (byte) invokeMethod(slot, receiver, params);
        }
    }

    public static short invokeShortMethod(final int slot,
                                          final Object receiver, final Object... params) {
        if (android.os.Build.VERSION.SDK_INT <= 20) {
            return Dalvik.invokeShortMethod(slot, receiver, params);
        } else {
            return (short) invokeMethod(slot, receiver, params);
        }
    }

    public static char invokeCharMethod(final int slot, final Object receiver,
                                        final Object... params) {
        if (android.os.Build.VERSION.SDK_INT <= 20) {
            return Dalvik.invokeCharMethod(slot, receiver, params);
        } else {
            return (char) invokeMethod(slot, receiver, params);
        }
    }

    public static int invokeIntMethod(final int slot, final Object receiver,
                                      final Object... params) {
        if (android.os.Build.VERSION.SDK_INT <= 20) {
            return Dalvik.invokeIntMethod(slot, receiver, params);
        } else {
            return (int) invokeMethod(slot, receiver, params);
        }
    }

    public static long invokeLongMethod(final int slot, final Object receiver,
                                        final Object... params) {
        if (android.os.Build.VERSION.SDK_INT <= 20) {
            return Dalvik.invokeLongMethod(slot, receiver, params);
        } else {
            return (long) invokeMethod(slot, receiver, params);
        }
    }

    public static float invokeFloatMethod(final int slot,
                                          final Object receiver, final Object... params) {
        if (android.os.Build.VERSION.SDK_INT <= 20) {
            return Dalvik.invokeFloatMethod(slot, receiver, params);
        } else {
            return (float) invokeMethod(slot, receiver, params);
        }
    }

    public static double invokeDoubleMethod(final int slot,
                                            final Object receiver, final Object... params) {
        if (android.os.Build.VERSION.SDK_INT <= 20) {
            return Dalvik.invokeDoubleMethod(slot, receiver, params);
        } else {
            return (double) invokeMethod(slot, receiver, params);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeObjectMethod(final int slot,
                                           final Object receiver, final Object... params) {
        return (T) invokeMethod(slot, receiver, params);
    }

    private static native Object invokeMethod(final int slot,
                                              final Object receiver, final Object... params);

    public static final class HookHelper {
        static final String constructorName = "<init>";
        private static final ConcurrentHashMap<Pair<String, String>, Integer> sBackups
                = new ConcurrentHashMap<>();
        private static Method getSignature = null;

        static {
            try {
                if (Build.VERSION.SDK_INT <= 25) {
                    getSignature = Constructor.class.getDeclaredMethod(
                            "getSignature", (Class<?>[]) null);
                } else {
                    final Class<?> clazz = ClassLoader.getSystemClassLoader()
                            .loadClass("libcore.reflect.Types");
                    getSignature = clazz.getDeclaredMethod("getSignature",
                            Class.class);
                }
                getSignature.setAccessible(true);
            } catch (final Exception e) {
                Log.e(AndHook.LOG_TAG, e.getMessage(), e);
            }
        }

        public static void hook(final Method origin, final Method replace) {
            final Class<?> cls0 = origin.getDeclaringClass();
            final String name0 = origin.getName();
            final Class<?> cls1 = replace.getDeclaringClass();
            final String name1 = replace.getName();
            final int slot = AndHook.hook(origin, replace);
            if (slot >= 0) {
                saveBackupSlot(slot, cls0, name0, cls1, name1, replace);
            }
        }

        public static void hook(final Class<?> clazz, final String name,
                                final String signature, final Method replace) {
            final Class<?> cls1 = replace.getDeclaringClass();
            final String name1 = replace.getName();
            final int slot = AndHook.hook(clazz, name, signature, replace);
            if (slot >= 0) {
                // @TODO Known issue: backup saving should be performed before
                // hook, e.g. ClassLoader
                saveBackupSlot(slot, clazz, name, cls1, name1, replace);
            }
        }

        private static void saveBackupSlot(final Integer slot,
                                           final Class<?> cls0, final String name0, final Class<?> cls1,
                                           final String name1, final Method md) {
            // a simple workaround for overloaded methods
            final String identifier = md.getParameterTypes().length + "";
            // ugly solution
            final Pair<String, String> origin_key = Pair.create(cls0.getName(),
                    name0 + identifier);
            final Pair<String, String> target_key = Pair.create(cls1.getName(),
                    name1 + identifier);
            if (sBackups.containsKey(origin_key)
                    || sBackups.containsKey(target_key)) {
                Log.e(AndHook.LOG_TAG, "duplicate key error!");
            }
            sBackups.put(origin_key, slot);
            sBackups.put(target_key, slot);
            if (Build.VERSION.SDK_INT <= 20
                    && !origin_key.first.equals(target_key.first)
                    && !origin_key.second.equals(target_key.second)) {
                final Pair<String, String> special_key = Pair.create(
                        target_key.first, origin_key.second);
                if (sBackups.containsKey(special_key)) {
                    Log.e(AndHook.LOG_TAG, "duplicate key error!");
                }
                sBackups.put(special_key, slot);
            }
        }

        private static int getBackupSlot(final int identifier) {
            final StackTraceElement stack = Thread.currentThread()
                    .getStackTrace()[4];
            final Integer slot = sBackups.get(Pair.create(stack.getClassName(),
                    stack.getMethodName() + identifier));
            if (slot == null) {
                Log.e(LOG_TAG, "no backup found for " + stack.getClassName()
                        + "@" + stack.getMethodName() + "@" + identifier);
                return -1;
            }
            return slot;
        }

        public static void invokeVoidOrigin(final Object receiver,
                                            final Object... params) {
            AndHook.invokeVoidMethod(getBackupSlot(params.length), receiver,
                    params);
        }

        public static boolean invokeBooleanOrigin(final Object receiver,
                                                  final Object... params) {
            return AndHook.invokeBooleanMethod(getBackupSlot(params.length),
                    receiver, params);
        }

        public static byte invokeByteOrigin(final Object receiver,
                                            final Object... params) {
            return AndHook.invokeByteMethod(getBackupSlot(params.length),
                    receiver, params);
        }

        public static short invokeShortOrigin(final Object receiver,
                                              final Object... params) {
            return AndHook.invokeShortMethod(getBackupSlot(params.length),
                    receiver, params);
        }

        public static char invokeCharOrigin(final Object receiver,
                                            final Object... params) {
            return AndHook.invokeCharMethod(getBackupSlot(params.length),
                    receiver, params);
        }

        public static int invokeIntOrigin(final Object receiver,
                                          final Object... params) {
            return AndHook.invokeIntMethod(getBackupSlot(params.length),
                    receiver, params);
        }

        public static long invokeLongOrigin(final Object receiver,
                                            final Object... params) {
            return AndHook.invokeLongMethod(getBackupSlot(params.length),
                    receiver, params);
        }

        public static float invokeFloatOrigin(final Object receiver,
                                              final Object... params) {
            return AndHook.invokeFloatMethod(getBackupSlot(params.length),
                    receiver, params);
        }

        public static double invokeDoubleOrigin(final Object receiver,
                                                final Object... params) {
            return AndHook.invokeDoubleMethod(getBackupSlot(params.length),
                    receiver, params);
        }

        @SuppressWarnings("unchecked")
        public static <T> T invokeObjectOrigin(final Object receiver,
                                               final Object... params) {
            return (T) AndHook.invokeMethod(getBackupSlot(params.length), receiver, params);
        }

        public static void setObjectField(final Object obj, final String name,
                                          final Object value) {
            final Field f = findFieldHierarchically(obj.getClass(), name);
            if (f != null) {
                try {
                    f.set(obj, value);
                } catch (final Exception e) {
                    Log.e(AndHook.LOG_TAG, "failed to set instance field "
                            + name, e);
                }
            }
        }

        public static void setStaticObjectField(final Class<?> clazz,
                                                final String name, final Object value) {
            final Field f = findFieldHierarchically(clazz, name);
            if (f != null) {
                try {
                    f.set(null, value);
                } catch (final Exception e) {
                    Log.e(AndHook.LOG_TAG, "failed to set static field " + name
                            + " for class " + clazz.getName(), e);
                }
            }
        }

        public static Class<?> findClass(final String classname) {
            return findClass(classname, AndHook.class.getClassLoader());
        }

        public static Class<?> findClass(final String classname, final ClassLoader loader) {
            try {
                return loader.loadClass(classname);
            } catch (final Exception e) {
                Log.e(AndHook.LOG_TAG, "failed to find class " + classname
                        + " on ClassLoader " + loader, e);
            }
            return null;
        }

        public static Field findFieldHierarchically(final Class<?> clazz, final String name) {
            Field f = null;
            Class<?> c = clazz;
            do {
                try {
                    f = c.getDeclaredField(name);
                } catch (final NoSuchFieldException e) {
                    c = c.getSuperclass();
                    if (c == null)
                        break;
                }
            } while (f == null);
            if (f != null) {
                f.setAccessible(true);
            } else {
                Log.e(AndHook.LOG_TAG, "failed to find field " + name
                        + " of class " + clazz.getName());
            }
            return f;
        }

        public static Method findMethodHierarchically(final Class<?> clazz,
                                                      final String name,
                                                      final Class<?>... parameterTypes) {
            Method m = null;
            Class<?> c = clazz;
            do {
                try {
                    m = c.getDeclaredMethod(name, parameterTypes);
                } catch (final NoSuchMethodException e) {
                    c = c.getSuperclass();
                    if (c == null)
                        break;
                }
            } while (m == null);
            if (m != null) {
                m.setAccessible(true);
            } else {
                Log.e(AndHook.LOG_TAG, "failed to find method " + name
                        + " of class " + clazz.getName());
            }
            return m;
        }

        @SuppressWarnings("ConstantConditions")
        static String getConstructorSignature(final Class<?> clazz, final Class<?>[] parameterTypes) {
            try {
                if (Build.VERSION.SDK_INT <= 25) {
                    return (String) getSignature.invoke(clazz
                            .getConstructor(parameterTypes));
                } else {
                    final StringBuilder result = new StringBuilder();

                    result.append('(');
                    for (final Class<?> parameterType : parameterTypes) {
                        result.append(getSignature.invoke(null, parameterType));
                    }
                    result.append(")V");

                    return result.toString();
                }
            } catch (final Exception e) {
                Log.e(AndHook.LOG_TAG,
                        "failed to get signature of constructor!", e);
            }
            return null;
        }

        private static String asConstructor(final Class<?> clazz, final String method,
                                            final Class<?>[] parameterTypes) {
            final String clsname = clazz.getName();
            if (!method.equals(constructorName) && !clsname.endsWith("." + method) &&
                    !clsname.endsWith("$" + method)) {
                return null;
            }
            return getConstructorSignature(clazz, parameterTypes);
        }

        @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
        @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
        public @interface Hook {
            String value() default ""; // class name

            Class<?> clazz() default Hook.class; // class

            String name() default ""; // target method name

            boolean need_origin() default true;
        }

        public static void applyHooks(final Class<?> holdClass) {
            applyHooks(holdClass, holdClass.getClassLoader());
        }

        public static void applyHooks(final Class<?> holdClass,
                                      final ClassLoader loader) {
            AndHook.ensureClassInitialized(holdClass);
            AndHook.suspendAll();
            for (final Method hookMethod : holdClass.getDeclaredMethods()) {
                final Hook hookInfo = hookMethod.getAnnotation(Hook.class);
                if (hookInfo != null) {
                    String name = hookInfo.name();
                    if (name.isEmpty())
                        name = hookMethod.getName();

                    Class<?> clazz = hookInfo.clazz();
                    Method origin;
                    try {
                        if (clazz == Hook.class)
                            clazz = loader.loadClass(hookInfo.value());
                        final Class<?>[] parameterTypes = hookMethod
                                .getParameterTypes();
                        final String sig = asConstructor(clazz, name,
                                parameterTypes);
                        if (sig != null) {
                            if (hookInfo.need_origin()) {
                                hook(clazz, constructorName, sig, hookMethod);
                            } else {
                                hookNoBackup(clazz, constructorName, sig, hookMethod);
                            }
                        } else {
                            origin = findMethodHierarchically(clazz, name, parameterTypes);
                            if (origin != null) {
                                AndHook.ensureClassInitialized(clazz);
                                if (hookInfo.need_origin()) {
                                    hook(origin, hookMethod);
                                } else {
                                    hookNoBackup(origin, hookMethod);
                                }
                            }
                        }
                    } catch (final Exception e) {
                        Log.e(AndHook.LOG_TAG, e.getMessage(), e);
                    }
                }
            }
            AndHook.resumeAll();
        }
    }

    private static final class Dalvik {
        public static native void invokeVoidMethod(final int slot,
                                                   final Object receiver, final Object... params);

        public static native boolean invokeBooleanMethod(final int slot,
                                                         final Object receiver, final Object... params);

        public static native byte invokeByteMethod(final int slot,
                                                   final Object receiver, final Object... params);

        public static native short invokeShortMethod(final int slot,
                                                     final Object receiver, final Object... params);

        public static native char invokeCharMethod(final int slot,
                                                   final Object receiver, final Object... params);

        public static native int invokeIntMethod(final int slot,
                                                 final Object receiver, final Object... params);

        public static native long invokeLongMethod(final int slot,
                                                   final Object receiver, final Object... params);

        public static native float invokeFloatMethod(final int slot,
                                                     final Object receiver, final Object... params);

        public static native double invokeDoubleMethod(final int slot,
                                                       final Object receiver, final Object... params);

        @SuppressWarnings("unchecked")
        public static <T> T invokeObjectMethod(final int slot,
                                               final Object receiver, final Object... params) {
            return (T) AndHook.invokeMethod(slot, receiver, params);
        }
    }
}
