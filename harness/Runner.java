import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Usage: Runner <classDir> <lineOffset> <tests...>
 * each test = comma separated ints ("4,7,1"), "-" for empty array.
 * Class mode: Runner <classDir> <lineOffset> --class <specs...>
 * each spec = "main" or "call|name|type,type|value;value" (types: int, double, String, int[], char, char[],
 * ArrayList<String>, ArrayList<Integer>; an int[] value is "4,7,1", a char value is one letter, a char[] value is
 * the letters written together "abc", a list value is its elements joined by "," with % , ; | percent-encoded,
 * "" = empty list).
 * Loads Main*.class from classDir, rewrites array bytecode to Tracker calls,
 * runs Gra.zadanie(int[]) for each test, writes <classDir>/wynik.json.
 */
public final class Runner {

    static final int OUT_CAP = 8000;
    /** Chapter 8+: int == / != comparisons are reported (event ieq), switched on per chapter by the server. */
    static final boolean ICMP = Boolean.getBoolean("gra.icmp");
    /** Chapters 10-11 (server passes -Dgra.menu=true): switch cases and do-while conditions become events. */
    static final boolean MENU = Boolean.getBoolean("gra.menu");
    /** Chapter 16 (server passes -Dgra.files=true): own sandbox directory per test, file and exception events. */
    static final boolean FILES = Boolean.getBoolean("gra.files");

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args[0]);
        Tracker.lineOffset = Integer.parseInt(args[1]);
        PrintStream realOut = System.out;

        Class<?> gra = load(dir);
        if (args.length > 2 && args[2].equals("--class")) {
            runClass(gra, dir, args, realOut);
            return;
        }
        Method m = null;
        for (Method x : gra.getDeclaredMethods()) if (x.getName().equals("zadanie")) m = x;
        m.setAccessible(true);

        StringBuilder json = new StringBuilder("{\"tests\":[");
        for (int t = 2; t < args.length; t++) {
            int[] arr = parse(args[t]);
            int[] start = arr.clone();
            Tracker.reset(arr, t == 2);
            Capture cap = new Capture();
            PrintStream ps = new PrintStream(cap, true, StandardCharsets.UTF_8);
            System.setOut(ps);
            System.setErr(ps);
            String exc = null, excMsg = null, ret = null;
            int excLine = -1;
            try {
                Object r = m.invoke(null, (Object) arr);
                if (r != null) ret = String.valueOf(r);
            } catch (InvocationTargetException ite) {
                Throwable c = ite.getCause();
                if (c instanceof Tracker.TooManySteps) {
                    exc = "TooManySteps";
                    excMsg = "Program zrobil za duzo krokow (pewnie petla bez konca).";
                } else if (c instanceof OutputCap) {
                    exc = "OutputCap";
                    excMsg = "Program wypisal za duzo tekstu.";
                } else {
                    exc = c.getClass().getName();
                    excMsg = c.getMessage();
                }
                for (StackTraceElement el : c.getStackTrace()) {
                    if (el.getClassName().equals("Main") || el.getClassName().startsWith("Main$")) {
                        excLine = el.getLineNumber() - Tracker.lineOffset;
                        break;
                    }
                }
            } finally {
                ps.flush();
                cap.flushRest();
                System.setOut(realOut);
                System.setErr(realOut);
            }
            if (t > 2) json.append(',');
            json.append("{\"start\":").append(arr(start))
                .append(",\"end\":").append(arr(arr))
                .append(",\"out\":").append(q(cap.all.toString()))
                .append(",\"ret\":").append(ret == null ? "null" : q(ret))
                .append(",\"exc\":").append(exc == null ? "null" : q(exc))
                .append(",\"excMsg\":").append(excMsg == null ? "null" : q(excMsg))
                .append(",\"excLine\":").append(excLine)
                .append('}');
        }
        json.append("],\"events\":[").append(String.join(",", Tracker.events)).append("]}");
        Files.writeString(dir.resolve("wynik.json"), json.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }

    /** Loads (instrumented) Main*.class from dir in a fresh class loader. */
    static Class<?> load(Path dir) throws ClassNotFoundException {
        ClassLoader loader = new ClassLoader(Runner.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                Path p = dir.resolve(name.replace('.', '/') + ".class");
                if (!name.equals("Main") && !name.startsWith("Main$")) throw new ClassNotFoundException(name);
                try {
                    byte[] b = instrument(Files.readAllBytes(p));
                    return defineClass(name, b, 0, b.length);
                } catch (java.io.IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        };
        return loader.loadClass("Main");
    }

    /**
     * Class mode: run main and/or call the student's own static methods.
     * Every test gets a fresh copy of Main, so static fields (a static Scanner, a static
     * list, a counter) start from scratch like in a real new run of the program.
     */
    static void runClass(Class<?> gra, Path dir, String[] args, PrintStream realOut) throws Exception {
        System.setIn(KEYBOARD);
        if (FILES) Sandbox.install(dir);
        StringBuilder json = new StringBuilder("{\"tests\":[");
        for (int t = 3; t < args.length; t++) {
            if (FILES) Sandbox.begin(dir, t - 3);
            String spec = args[t];
            if (t > 3) gra = load(dir);
            int[] arrArg = null;
            java.util.ArrayList<Object> listArg = null;
            List<java.util.ArrayList<Object>> listArgs = new ArrayList<>();
            Method m = null;
            Object[] callArgs;
            String missing = null;
            // "main|in|<base64>" = main with this text typed on the keyboard (System.in)
            byte[] stdin = new byte[0];
            if (spec.startsWith("main|in|")) {
                stdin = java.util.Base64.getDecoder().decode(spec.substring(8));
                spec = "main";
            }
            KEYBOARD.feed(stdin);
            if (spec.equals("main")) {
                m = find(gra, "main", new Class<?>[]{String[].class});
                callArgs = new Object[]{new String[0]};
                if (m == null) missing = "Nie ma metody main(String[] args) - bez niej program nie ma od czego zacząć.";
            } else {
                String[] p = spec.split("\\|", -1);
                String[] types = p[2].isEmpty() ? new String[0] : p[2].split(",");
                String[] vals = p[3].isEmpty() ? new String[0] : p[3].split(";", -1);
                if (vals.length < types.length) vals = java.util.Arrays.copyOf(vals, types.length);
                for (int k = 0; k < vals.length; k++) if (vals[k] == null) vals[k] = "";
                Class<?>[] cls = new Class<?>[types.length];
                callArgs = new Object[types.length];
                for (int k = 0; k < types.length; k++) {
                    switch (types[k]) {
                        case "int" -> { cls[k] = int.class; callArgs[k] = Integer.parseInt(vals[k]); }
                        case "double" -> { cls[k] = double.class; callArgs[k] = Double.parseDouble(vals[k]); }
                        case "String" -> { cls[k] = String.class; callArgs[k] = dec(vals[k]); }
                        case "ArrayList<String>", "ArrayList<Integer>" -> {
                            cls[k] = java.util.ArrayList.class;
                            java.util.ArrayList<Object> l = parseList(vals[k], types[k].equals("ArrayList<Integer>"));
                            callArgs[k] = l;
                            if (listArg == null) listArg = l;
                            listArgs.add(l);
                        }
                        case "char" -> { cls[k] = char.class; callArgs[k] = dec(vals[k]).charAt(0); }
                        case "char[]" -> { cls[k] = char[].class; callArgs[k] = vals[k].toCharArray(); }
                        default -> { cls[k] = int[].class; int[] a = parse(vals[k]); callArgs[k] = a; if (arrArg == null) arrArg = a; }
                    }
                }
                m = find(gra, p[1], cls);
                if (m == null) missing = "Nie ma metody " + p[1] + "(" + String.join(", ", types)
                        + ") - sprawdź nazwę, static i typy parametrów.";
            }
            int[] start = arrArg == null ? new int[0] : arrArg.clone();
            Tracker.reset(arrArg, t == 3);
            Tracker.autoTrack = arrArg == null;
            Capture cap = new Capture();
            current = cap;
            PrintStream ps = new PrintStream(cap, true, StandardCharsets.UTF_8);
            System.setOut(ps);
            System.setErr(ps);
            String exc = null, excMsg = null, ret = null, excMethod = null;
            int excLine = -1;
            try {
                if (missing != null) {
                    exc = "NoSuchMethod";
                    excMsg = missing;
                } else {
                    Sandbox.guard = FILES;
                    Object r = m.invoke(null, callArgs);
                    if (r != null) ret = String.valueOf(r);
                }
            } catch (InvocationTargetException ite) {
                Throwable c = ite.getCause();
                if (c instanceof Tracker.TooManySteps) {
                    exc = "TooManySteps";
                    excMsg = "Program zrobil za duzo krokow (pewnie petla bez konca).";
                } else if (c instanceof OutputCap) {
                    exc = "OutputCap";
                    excMsg = "Program wypisal za duzo tekstu.";
                } else {
                    exc = c.getClass().getName();
                    excMsg = c.getMessage();
                }
                for (StackTraceElement el : c.getStackTrace()) {
                    if (el.getClassName().equals("Main") || el.getClassName().startsWith("Main$")) {
                        excLine = el.getLineNumber() - Tracker.lineOffset;
                        excMethod = el.getMethodName();
                        break;
                    }
                }
            } finally {
                Sandbox.guard = false;
                if (FILES) Sandbox.end(dir, t - 3);
                ps.flush();
                cap.flushRest();
                System.setOut(realOut);
                System.setErr(realOut);
            }
            int[] end = arrArg != null ? arrArg : (Tracker.tracked != null ? Tracker.tracked : new int[0]);
            if (t > 3) json.append(',');
            json.append("{\"start\":").append(arr(start))
                .append(",\"end\":").append(arr(end))
                .append(",\"out\":").append(q(cap.all.toString()))
                .append(",\"ret\":").append(ret == null ? "null" : q(ret))
                .append(",\"exc\":").append(exc == null ? "null" : q(exc))
                .append(",\"excMsg\":").append(excMsg == null ? "null" : q(excMsg))
                .append(",\"excLine\":").append(excLine)
                .append(",\"excMethod\":").append(excMethod == null ? "null" : q(excMethod))
                .append(",\"called\":[").append(String.join(",", Tracker.called.stream().map(Runner::q).toList())).append("]")
                .append(",\"ops\":[").append(String.join(",", Tracker.ops.stream().map(Runner::q).toList())).append("]")
                .append(",\"listEnd\":").append(listArg == null ? "null" : Tracker.vals(listArg))
                .append(",\"lists\":[").append(String.join(",", listArgs.stream().map(Tracker::vals).toList())).append("]")
                .append(",\"steps\":").append(Tracker.steps)
                .append(",\"tracked\":").append(Tracker.trackedJson())
                .append(",\"used\":[").append(String.join(",", Tracker.used.stream().map(Runner::q).toList())).append("]")
                .append('}');
        }
        json.append("],\"events\":[").append(String.join(",", Tracker.events)).append("]}");
        Files.writeString(dir.resolve("wynik.json"), json.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }


    /**
     * Chapter 16: the student's file operations only ever touch one directory (the JVM working directory, a fresh
     * temp dir per run; the files of every single test are wiped and re-seeded). A SecurityManager that is switched on
     * only while the student's code runs refuses everything else: reading the game and other runs, writing or
     * deleting outside, starting processes, the network. It also tells Tracker which file the code opens.
     * It guards against MISTAKES, not attacks: code written on purpose to escape can (/proc/self/root, symlinks,
     * reflection on `guard`). That is fine only because the game runs the student's own code on their own machine,
     * like IntelliJ does; /run accepts requests from the game page only (see server.py do_POST).
     */
    static final class Sandbox extends SecurityManager {
        static volatile boolean guard;
        static Path root, work, lib;
        static String javaHome;

        static void install(Path dir) throws java.io.IOException {
            work = dir.toAbsolutePath().normalize();
            root = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            javaHome = Paths.get(System.getProperty("java.home")).toAbsolutePath().normalize().toString();
            lib = Paths.get(System.getProperty("java.class.path").split(java.io.File.pathSeparator)[0]).toAbsolutePath().normalize();
            System.setSecurityManager(new Sandbox());
        }

        /** Empty the sandbox, then copy the files prepared for this test (work/seed/<k>) into it. */
        static void begin(Path dir, int k) throws java.io.IOException {
            wipe(root);
            Path seed = work.resolve("seed").resolve(String.valueOf(k));
            if (Files.isDirectory(seed)) copyTree(seed, root);
        }

        /** After the test: the files it left behind are copied to work/end/<k> for the server to read. */
        static void end(Path dir, int k) throws java.io.IOException {
            Path out = work.resolve("end").resolve(String.valueOf(k));
            copyTree(root, out);
        }

        static void wipe(Path r) throws java.io.IOException {
            try (var w = Files.walk(r)) {
                for (Path p : (Iterable<Path>) w.sorted(java.util.Comparator.reverseOrder())::iterator)
                    if (!p.equals(r)) Files.deleteIfExists(p);
            }
        }

        static void copyTree(Path from, Path to) throws java.io.IOException {
            Files.createDirectories(to);
            try (var w = Files.walk(from)) {
                for (Path p : (Iterable<Path>) w::iterator) {
                    Path t = to.resolve(from.relativize(p).toString());
                    if (Files.isDirectory(p)) Files.createDirectories(t);
                    else Files.copy(p, t, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        static Path abs(String f) { return Paths.get(f).toAbsolutePath().normalize(); }

        static boolean inRoot(Path p) { return p.startsWith(root); }

        static boolean readable(Path p) {
            String s = p.toString();
            return inRoot(p) || s.startsWith(javaHome) || p.startsWith(lib)
                    || (p.startsWith(work) && s.endsWith(".class"))
                    || s.startsWith("/proc/") || s.startsWith("/sys/") || s.equals("/etc/localtime") || s.startsWith("/usr/share/zoneinfo");
        }

        @Override public void checkRead(String f) {
            if (!guard) return;
            Path p = abs(f);
            if (!readable(p)) throw new SecurityException("Program moze czytac tylko pliki w swoim katalogu: " + f);
            if (inRoot(p)) Tracker.touch(p, root, false);
        }
        @Override public void checkRead(String f, Object ctx) { checkRead(f); }
        @Override public void checkWrite(String f) {
            if (!guard) return;
            Path p = abs(f);
            if (!inRoot(p)) throw new SecurityException("Program moze zapisywac tylko pliki w swoim katalogu: " + f);
            Tracker.touch(p, root, true);
        }
        @Override public void checkDelete(String f) {
            if (guard && !inRoot(abs(f))) throw new SecurityException("Program moze kasowac tylko pliki w swoim katalogu: " + f);
        }
        @Override public void checkExec(String cmd) { if (guard) throw new SecurityException("Program nie moze uruchamiac innych programow."); }
        @Override public void checkConnect(String host, int port) { if (guard) throw new SecurityException("Program nie moze laczyc sie z siecia."); }
        @Override public void checkConnect(String host, int port, Object ctx) { checkConnect(host, port); }
        @Override public void checkListen(int port) { if (guard) throw new SecurityException("Program nie moze otwierac portow."); }
        @Override public void checkPermission(java.security.Permission perm) {
            if (guard && perm instanceof RuntimePermission && perm.getName().equals("setSecurityManager"))
                throw new SecurityException("Nie wolno zmieniac zabezpieczen.");
        }
        @Override public void checkPermission(java.security.Permission perm, Object ctx) { checkPermission(perm); }
    }

    static Method find(Class<?> c, String name, Class<?>[] params) {
        for (Method x : c.getDeclaredMethods()) {
            if (x.getName().equals(name) && java.util.Arrays.equals(x.getParameterTypes(), params)
                    && java.lang.reflect.Modifier.isStatic(x.getModifiers())) {
                x.setAccessible(true);
                return x;
            }
        }
        // no exact match: accept wider parameter types (e.g. List<String> for an ArrayList argument)
        for (Method x : c.getDeclaredMethods()) {
            Class<?>[] p = x.getParameterTypes();
            if (!x.getName().equals(name) || p.length != params.length || !java.lang.reflect.Modifier.isStatic(x.getModifiers())) continue;
            boolean ok = true;
            for (int k = 0; k < p.length; k++) if (!p[k].isAssignableFrom(params[k])) ok = false;
            if (ok) {
                x.setAccessible(true);
                return x;
            }
        }
        return null;
    }

    /**
     * System.in for the whole run; each test also gets a fresh Main class, so static fields start empty.
     * Like a terminal: each read gives at most one line, so two Scanners on System.in
     * do not swallow each other's lines; the text is swapped per test.
     */
    static final Keyboard KEYBOARD = new Keyboard();

    static final class Keyboard extends java.io.InputStream {
        byte[] data = new byte[0];
        int pos;

        void feed(byte[] d) { data = d; pos = 0; }

        @Override
        public int read() { return pos < data.length ? data[pos++] & 0xff : -1; }

        @Override
        public int read(byte[] b, int off, int len) {
            if (len == 0) return 0;
            if (pos >= data.length) return -1;
            int n = 0;
            while (n < len && pos < data.length) {
                byte c = data[pos++];
                b[off + n++] = c;
                if (c == '\n') break;
            }
            return n;
        }

        @Override
        public int available() { return 0; }

        @Override
        public void close() { /* skan.close() must not end the keyboard for later tests */ }
    }

    static final class OutputCap extends Error {
        OutputCap() { super("za duzo tekstu"); }
    }

    /** Output of the test that runs now (class mode), so a Scanner read can show a print without Enter first. */
    static Capture current;

    /** The started line (print without Enter) of the running test, "" when none. */
    static String pending() {
        return current == null ? "" : current.line.toString(StandardCharsets.UTF_8);
    }

    /** Collects student output; emits one "out" event per finished line. */
    static final class Capture extends OutputStream {
        final StringBuilder all = new StringBuilder();
        final ByteArrayOutputStream line = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            if (b == '\n') { flushLine(); return; }
            line.write(b);
        }

        void flushRest() {
            try {
                if (line.size() > 0) flushLine(true);
            } catch (OutputCap ignored) {
                // cap already reached; nothing more to record
            }
        }

        void flushLine() { flushLine(false); }

        void flushLine(boolean noEnter) {
            String s = line.toString(StandardCharsets.UTF_8);
            line.reset();
            if (all.length() > OUT_CAP) return;
            all.append(s).append('\n');
            Tracker.promptShown = 0;
            if (noEnter) Tracker.outRest(s); else Tracker.out(s);
            if (all.length() > OUT_CAP) throw new OutputCap();
        }
    }

    /** String and char arguments of a call spec arrive percent-encoded (so | and ; inside a text are safe). */
    static String dec(String v) { return java.net.URLDecoder.decode(v.replace("+", "%2B"), StandardCharsets.UTF_8); }

    static int[] parse(String s) {
        if (s.equals("-") || s.isEmpty()) return new int[0];
        String[] p = s.split(",");
        int[] a = new int[p.length];
        for (int i = 0; i < p.length; i++) a[i] = Integer.parseInt(p[i].trim());
        return a;
    }

    static java.util.ArrayList<Object> parseList(String s, boolean ints) {
        java.util.ArrayList<Object> l = new java.util.ArrayList<>();
        if (s.isEmpty()) return l;
        for (String p : s.split(",", -1)) {
            String v = java.net.URLDecoder.decode(p.replace("+", "%2B"), StandardCharsets.UTF_8);
            l.add(ints ? (Object) Integer.valueOf(v.trim()) : v);
        }
        return l;
    }

    static String arr(int[] a) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) { if (i > 0) b.append(','); b.append(a[i]); }
        return b.append(']').toString();
    }

    static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }

    /** Rewrite IALOAD/IASTORE/ARRAYLENGTH and report every store to a named int local. */
    static byte[] instrument(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);
        boolean chars = usesChars(cn);
        for (MethodNode mn : cn.methods) {
            InsnList il = mn.instructions;
            List<AbstractInsnNode> all = new ArrayList<>();
            for (AbstractInsnNode n = il.getFirst(); n != null; n = n.getNext()) all.add(n);
            skipSwitch = 0;
            for (AbstractInsnNode n : all) {
                int op = n.getOpcode();
                if (FILES && n instanceof MethodInsnNode fm && fileHook(il, fm)) continue;
                if (MENU && (op == Opcodes.TABLESWITCH || op == Opcodes.LOOKUPSWITCH)) {
                    // chapter 10: which case the switch jumps to, and every case whose code starts (fall-through too)
                    if (stringSwitch(n)) { skipSwitch = 1; continue; }
                    if (skipSwitch > 0) { skipSwitch = 0; continue; }
                    instrumentSwitch(il, n);
                    continue;
                }
                if (MENU && n instanceof JumpInsnNode j && op >= Opcodes.IFEQ && op <= Opcodes.IF_ICMPLE
                        && il.indexOf(j.label) < il.indexOf(n)) {
                    // a conditional jump backwards = the condition at the bottom of a do-while
                    InsnList add = new InsnList();
                    boolean two = op >= Opcodes.IF_ICMPEQ;
                    add.add(new InsnNode(two ? Opcodes.DUP2 : Opcodes.DUP));
                    add.add(new LdcInsnNode(JUMP_NAMES[op - Opcodes.IFEQ]));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", two ? "loop2" : "loop1",
                            two ? "(IILjava/lang/String;)V" : "(ILjava/lang/String;)V", false));
                    il.insertBefore(n, add);
                    continue;
                }
                if ((op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKEINTERFACE) && n instanceof MethodInsnNode mi
                        && (mi.owner.equals("java/util/ArrayList") || mi.owner.equals("java/util/List"))) {
                    String[] to = LIST_OPS.get(mi.name + mi.desc);
                    if (to != null) il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", to[0], to[1], false));
                } else if (op == Opcodes.ASTORE) {
                    int slot = ((VarInsnNode) n).var;
                    LocalVariableNode lv = localVar(mn, il, n, slot, OBJ_DESCS);
                    if (lv != null) {
                        InsnList add = new InsnList();
                        add.add(new VarInsnNode(Opcodes.ALOAD, slot));
                        add.add(new LdcInsnNode(lv.name));
                        if (lv.desc.equals("Ljava/lang/String;")) {
                            add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "varS", "(Ljava/lang/Object;Ljava/lang/String;)V", false));
                        } else {
                            add.add(lv.signature == null ? new InsnNode(Opcodes.ACONST_NULL) : new LdcInsnNode(lv.signature));
                            add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "varO", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V", false));
                        }
                        il.insert(n, add);
                    } else {
                        // chapter 4: the name of a char[] local (znaki) for the letter drawers
                        String cname = localName(mn, il, n, slot, "[C");
                        if (cname != null) {
                            InsnList add = new InsnList();
                            add.add(new VarInsnNode(Opcodes.ALOAD, slot));
                            add.add(new LdcInsnNode(cname));
                            add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "nameC", "(Ljava/lang/Object;Ljava/lang/String;)V", false));
                            il.insert(n, add);
                        }
                    }
                } else if (op == Opcodes.CALOAD) {
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "cload", "([CI)C", false));
                } else if (op == Opcodes.CASTORE) {
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "cstore", "([CIC)V", false));
                } else if (FILES && n instanceof MethodInsnNode mi && mi.owner.equals("java/lang/String") && op == Opcodes.INVOKEVIRTUAL
                        && FILE_STRING_HOOKS.containsKey(mi.name + mi.desc)) {
                    String[] h = FILE_STRING_HOOKS.get(mi.name + mi.desc);
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", h[0], h[1], false));
                } else if (n instanceof MethodInsnNode mi && mi.owner.equals("java/lang/String") && op == Opcodes.INVOKEVIRTUAL
                        && STRING_HOOKS.containsKey(mi.name + mi.desc)) {
                    String[] h = STRING_HOOKS.get(mi.name + mi.desc);
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", h[0], h[1], false));
                } else if (n instanceof MethodInsnNode mi && mi.owner.equals("java/lang/String") && op == Opcodes.INVOKESPECIAL
                        && mi.name.equals("<init>") && mi.desc.equals("([C)V")) {
                    InsnList add = new InsnList();
                    add.add(new InsnNode(Opcodes.DUP));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "joinC", "([C)V", false));
                    il.insertBefore(n, add);
                } else if (chars && op == Opcodes.ISUB && n.getPrevious() instanceof IntInsnNode c
                        && c.getOpcode() == Opcodes.BIPUSH && (c.operand == 'A' || c.operand == 'a')) {
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "dist", "(II)I", false));
                } else if (chars && op == Opcodes.IREM) {
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "mod", "(II)I", false));
                } else if (chars && op == Opcodes.I2C) {
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "i2c", "(I)C", false));
                } else if (((SORT && afterArrayRead(n)) || BIN) && op >= Opcodes.IF_ICMPEQ && op <= Opcodes.IF_ICMPLE) {
                    // chapter 9+: tab[j - 1] > tab[j] - report both numbers, the operator and the answer, then jump as before
                    JumpInsnNode j = (JumpInsnNode) n;
                    InsnList add = new InsnList();
                    add.add(new LdcInsnNode(op));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "icmp", "(III)Z", false));
                    add.add(new JumpInsnNode(Opcodes.IFNE, j.label));
                    il.insertBefore(n, add);
                    il.remove(n);
                } else if ((SORT || BIN) && op >= Opcodes.IFEQ && op <= Opcodes.IFLE && prevReal(n.getPrevious()) instanceof MethodInsnNode pm
                        && pm.owner.equals("Tracker") && pm.name.equals("strCmp")) {
                    // chapter 9+: a.compareTo(b) > 0 - report the result checked against zero
                    JumpInsnNode j = (JumpInsnNode) n;
                    InsnList add = new InsnList();
                    add.add(new LdcInsnNode(op));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "zcmp", "(II)Z", false));
                    add.add(new JumpInsnNode(Opcodes.IFNE, j.label));
                    il.insertBefore(n, add);
                    il.remove(n);
                } else if (op == Opcodes.IALOAD) {
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "load", "([II)I", false));
                } else if (op == Opcodes.IASTORE) {
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "store", "([III)V", false));
                } else if (op == Opcodes.ARRAYLENGTH) {
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "len", "(Ljava/lang/Object;)I", false));
                } else if (ICMP && (op == Opcodes.IF_ICMPEQ || op == Opcodes.IF_ICMPNE) && !boolCompare(mn, il, n)) {
                    // a == b (or a != b, same jump) on ints (t[i] == x, nr == -1): ask Tracker (reports the pair), then jump on its boolean
                    JumpInsnNode j = (JumpInsnNode) n;
                    InsnList add = new InsnList();
                    add.add(new LdcInsnNode(intLabels(mn, il, n)));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "ieq", "(IILjava/lang/String;)Z", false));
                    add.add(new JumpInsnNode(op == Opcodes.IF_ICMPEQ ? Opcodes.IFNE : Opcodes.IFEQ, j.label));
                    il.insertBefore(n, add);
                    il.remove(n);
                } else if (ICMP && (op == Opcodes.IFEQ || op == Opcodes.IFNE) && prevReal(n.getPrevious()) instanceof MethodInsnNode ld
                        && ld.owner.equals("Tracker") && ld.name.equals("load")) {
                    // t[i] == 0 compiles to a jump on the element itself: compare it with 0 through Tracker too
                    JumpInsnNode j = (JumpInsnNode) n;
                    Object[] el = intOperand(mn, il, ld);
                    InsnList add = new InsnList();
                    add.add(new InsnNode(Opcodes.ICONST_0));
                    add.add(new LdcInsnNode((el == null ? "" : (String) el[0]) + "|0"));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "ieq", "(IILjava/lang/String;)Z", false));
                    add.add(new JumpInsnNode(op == Opcodes.IFEQ ? Opcodes.IFNE : Opcodes.IFEQ, j.label));
                    il.insertBefore(n, add);
                    il.remove(n);
                } else if (op == Opcodes.IF_ACMPEQ || op == Opcodes.IF_ACMPNE) {
                    // a == b on objects: ask Tracker (reports Strings), then jump on its boolean
                    JumpInsnNode j = (JumpInsnNode) n;
                    InsnList add = new InsnList();
                    add.add(new LdcInsnNode(labels(mn, il, n)));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "refEq", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Z", false));
                    add.add(new JumpInsnNode(op == Opcodes.IF_ACMPEQ ? Opcodes.IFNE : Opcodes.IFEQ, j.label));
                    il.insertBefore(n, add);
                    il.remove(n);
                } else if (op == Opcodes.INVOKEVIRTUAL && n instanceof MethodInsnNode mi && mi.owner.equals("java/lang/String")
                        && ((mi.name.equals("equals") && mi.desc.equals("(Ljava/lang/Object;)Z"))
                            || (mi.name.equals("compareTo") && mi.desc.equals("(Ljava/lang/String;)I")))) {
                    boolean eq = mi.name.equals("equals");
                    il.insertBefore(n, new LdcInsnNode(labels(mn, il, n)));
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", eq ? "strEq" : "strCmp",
                            eq ? "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)Z" : "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I", false));
                } else if (op == Opcodes.INVOKEVIRTUAL && n instanceof MethodInsnNode mi && mi.owner.equals("java/util/Scanner")
                        && SCANNER_READS.contains(mi.name + mi.desc)) {
                    il.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", mi.name, "(Ljava/util/Scanner;)" + mi.desc.substring(2), false));
                } else if (op == Opcodes.DSTORE) {
                    int slot = ((VarInsnNode) n).var;
                    String name = localName(mn, il, n, slot, "D");
                    if (name != null) {
                        InsnList add = new InsnList();
                        add.add(new VarInsnNode(Opcodes.DLOAD, slot));
                        add.add(new LdcInsnNode(name));
                        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "varD", "(DLjava/lang/String;)V", false));
                        il.insert(n, add);
                    }
                } else if (traced(mn) && (op == Opcodes.IRETURN || op == Opcodes.DRETURN || op == Opcodes.ARETURN || op == Opcodes.RETURN)) {
                    InsnList add = new InsnList();
                    switch (op) {
                        case Opcodes.IRETURN -> { add.add(new InsnNode(Opcodes.DUP)); add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker",
                                mn.desc.endsWith(")Z") ? "retZ" : mn.desc.endsWith(")C") ? "retC" : "retI",
                                mn.desc.endsWith(")Z") ? "(Z)V" : "(I)V", false)); }
                        case Opcodes.DRETURN -> { add.add(new InsnNode(Opcodes.DUP2)); add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "retD", "(D)V", false)); }
                        case Opcodes.ARETURN -> { add.add(new InsnNode(Opcodes.DUP)); add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "retO", "(Ljava/lang/Object;)V", false)); }
                        default -> add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "retV", "()V", false));
                    }
                    il.insertBefore(n, add);
                } else if (op == Opcodes.ISTORE || op == Opcodes.IINC) {
                    int slot = (n instanceof VarInsnNode v) ? v.var : ((IincInsnNode) n).var;
                    String name = localName(mn, il, n, slot, "I");
                    String cname = name == null ? localName(mn, il, n, slot, "C") : null;
                    if (name != null || cname != null) {
                        InsnList add = new InsnList();
                        add.add(new VarInsnNode(Opcodes.ILOAD, slot));
                        add.add(new LdcInsnNode(name != null ? name : cname));
                        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", name != null ? "var" : "varC", "(ILjava/lang/String;)V", false));
                        il.insert(n, add);
                    }
                }
            }
        }
        if (FILES) for (MethodNode mn : cn.methods) catchHooks(mn);
        for (MethodNode mn : cn.methods) if (traced(mn)) il0(mn);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return "java/lang/Object";
            }
        };
        cn.accept(cw);
        return cw.toByteArray();
    }


    // ---------- chapter 16: files and exceptions ----------

    static final java.util.Set<String> WRITER_OWNERS = java.util.Set.of(
            "java/io/FileWriter", "java/io/BufferedWriter", "java/io/Writer", "java/io/OutputStreamWriter", "java/io/PrintWriter");
    static final java.util.Set<String> CLOSE_OWNERS = java.util.Set.of(
            "java/io/FileWriter", "java/io/BufferedWriter", "java/io/Writer", "java/io/PrintWriter",
            "java/io/FileReader", "java/io/BufferedReader", "java/io/Reader", "java/util/Scanner");

    static InsnList call(String name, String desc, Object... ldc) {
        InsnList l = new InsnList();
        for (Object o : ldc) l.add(new LdcInsnNode(o));
        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", name, desc, false));
        return l;
    }

    /** File and writer/reader calls are rewritten to Tracker calls that do the same thing and report it. */
    static boolean fileHook(InsnList il, MethodInsnNode mi) {
        String o = mi.owner, d = mi.desc, nm = mi.name;
        if (mi.getOpcode() == Opcodes.INVOKESPECIAL && nm.equals("<init>")) {
            boolean strOrFile = d.equals("(Ljava/lang/String;)V") || d.equals("(Ljava/io/File;)V");
            if (o.equals("java/io/FileWriter") && d.endsWith("Z)V")) {
                il.insertBefore(mi, new InsnNode(Opcodes.DUP));
                il.insertBefore(mi, call("intentA", "(Z)V"));
            } else if ((o.equals("java/io/FileWriter") || o.equals("java/io/PrintWriter")) && strOrFile) {
                il.insertBefore(mi, call("intent", "(Ljava/lang/String;)V", "w"));
            } else if (o.equals("java/io/FileReader") && strOrFile) {
                il.insertBefore(mi, call("intent", "(Ljava/lang/String;)V", "r"));
            } else if (o.equals("java/util/Scanner") && d.equals("(Ljava/io/File;)V")) {
                il.insertBefore(mi, call("intent", "(Ljava/lang/String;)V", "r"));
                InsnList after = new InsnList();
                after.add(new InsnNode(Opcodes.DUP));
                after.add(call("fileScanner", "(Ljava/util/Scanner;)V"));
                il.insert(mi, after);
            }
            return false;
        }
        if (mi.getOpcode() != Opcodes.INVOKEVIRTUAL) return false;
        String[] to = null;
        if (WRITER_OWNERS.contains(o) && nm.equals("write") && d.equals("(Ljava/lang/String;)V")) to = new String[]{"wWrite", "(Ljava/io/Writer;Ljava/lang/String;)V"};
        else if (o.equals("java/io/PrintWriter") && nm.equals("println") && d.equals("(Ljava/lang/String;)V")) to = new String[]{"pwPrintln", "(Ljava/io/PrintWriter;Ljava/lang/String;)V"};
        else if (o.equals("java/io/PrintWriter") && nm.equals("print") && d.equals("(Ljava/lang/String;)V")) to = new String[]{"pwPrint", "(Ljava/io/PrintWriter;Ljava/lang/String;)V"};
        else if (o.equals("java/io/BufferedWriter") && nm.equals("newLine") && d.equals("()V")) to = new String[]{"wNewLine", "(Ljava/io/BufferedWriter;)V"};
        else if (o.equals("java/io/BufferedReader") && nm.equals("readLine") && d.equals("()Ljava/lang/String;")) to = new String[]{"readLine", "(Ljava/io/BufferedReader;)Ljava/lang/String;"};
        else if (CLOSE_OWNERS.contains(o) && nm.equals("close") && d.equals("()V")) to = new String[]{"fclose", "(Ljava/lang/Object;)V"};
        else if (o.equals("java/io/File") && nm.equals("exists") && d.equals("()Z")) to = new String[]{"fexists", "(Ljava/io/File;)Z"};
        if (to == null) return false;
        il.set(mi, new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", to[0], to[1], false));
        return true;
    }

    /** Start of every catch block: Tracker.caught(exception) shows the jump from the failing line to the catch. */
    static void catchHooks(MethodNode mn) {
        java.util.Set<LabelNode> done = new java.util.HashSet<>();
        for (TryCatchBlockNode tc : mn.tryCatchBlocks) {
            if (tc.type == null || !done.add(tc.handler)) continue;
            AbstractInsnNode first = tc.handler;
            while (first != null && first.getOpcode() < 0) first = first.getNext();
            if (first == null || first.getOpcode() != Opcodes.ASTORE) continue;
            boolean synthetic = false;
            AbstractInsnNode x = first;
            for (int k = 0; x != null && k < 16; k++, x = x.getNext())
                if (x instanceof MethodInsnNode m && m.name.equals("addSuppressed")) synthetic = true;
            if (synthetic) continue;
            InsnList add = new InsnList();
            add.add(new InsnNode(Opcodes.DUP));
            add.add(call("caught", "(Ljava/lang/Throwable;)V"));
            mn.instructions.insertBefore(first, add);
        }
    }

    /** Chapter 9+ (server passes -Dgra.sort=1): int comparisons of array/list elements and compareTo signs become events. */
    static final boolean SORT = Boolean.getBoolean("gra.sort");
    /** Chapter 13 (server passes -Dgra.bin=true): EVERY int comparison (t[mid] < x, p <= k) becomes an icmp event; the page picks the ones that follow an element read. */
    static final boolean BIN = Boolean.getBoolean("gra.bin");

    /** The second operand of the comparison was just read from an int[] (already rewritten to Tracker.load) or unboxed from a list. */
    static boolean afterArrayRead(AbstractInsnNode n) {
        AbstractInsnNode p = prevReal(n.getPrevious());
        if (p == null) return false;
        if (p.getOpcode() == Opcodes.IALOAD) return true;
        return p instanceof MethodInsnNode m && ((m.owner.equals("Tracker") && m.name.equals("load"))
                || (m.owner.equals("java/lang/Integer") && m.name.equals("intValue")));
    }

    /** Scanner reads replaced by Tracker (same name, the Scanner becomes the first argument). */
    static final java.util.Set<String> SCANNER_READS = java.util.Set.of("nextLine()Ljava/lang/String;", "nextInt()I",
            "nextDouble()D", "next()Ljava/lang/String;", "hasNextInt()Z", "hasNext()Z", "hasNextLine()Z");
    static final String[] JUMP_NAMES = {"IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE",
            "IF_ICMPEQ", "IF_ICMPNE", "IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT", "IF_ICMPLE"};
    /** A switch on a String compiles to two switches (hash, then number); neither is drawn. */
    static int skipSwitch;

    static boolean stringSwitch(AbstractInsnNode n) {
        AbstractInsnNode p = n.getPrevious();
        while (p != null && p.getOpcode() < 0) p = p.getPrevious();
        return p instanceof MethodInsnNode mi && mi.name.equals("hashCode") && mi.owner.equals("java/lang/String");
    }

    /** Tracker.sw(value, "1,2,3,0[|d]") before the jump, Tracker.caseHit("keys") at the start of each case's code. */
    static void instrumentSwitch(InsnList il, AbstractInsnNode n) {
        List<Integer> keys = new ArrayList<>();
        List<LabelNode> targets = new ArrayList<>();
        LabelNode dflt;
        if (n instanceof TableSwitchInsnNode ts) {
            for (int k = 0; k < ts.labels.size(); k++) {
                if (ts.labels.get(k) == ts.dflt) continue; // a hole in the table, not a real case
                keys.add(ts.min + k);
                targets.add(ts.labels.get(k));
            }
            dflt = ts.dflt;
        } else {
            LookupSwitchInsnNode ls = (LookupSwitchInsnNode) n;
            keys.addAll(ls.keys);
            targets.addAll(ls.labels);
            dflt = ls.dflt;
        }
        // without a default, javac sends other values to the end of the switch, where every break (goto) lands too
        boolean hasDefault = true;
        for (AbstractInsnNode x = n.getNext(); x != null && x != dflt; x = x.getNext())
            if (x.getOpcode() == Opcodes.GOTO && ((JumpInsnNode) x).label == dflt) { hasDefault = false; break; }
        if (il.indexOf(dflt) < il.indexOf(n)) hasDefault = false;
        java.util.LinkedHashMap<LabelNode, List<String>> at = new java.util.LinkedHashMap<>();
        for (int k = 0; k < keys.size(); k++) at.computeIfAbsent(targets.get(k), x -> new ArrayList<>()).add(String.valueOf(keys.get(k)));
        if (hasDefault) at.computeIfAbsent(dflt, x -> new ArrayList<>()).add("default");
        InsnList before = new InsnList();
        before.add(new InsnNode(Opcodes.DUP));
        before.add(new LdcInsnNode(String.join(",", keys.stream().map(String::valueOf).toList()) + (hasDefault ? "|d" : "")));
        before.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "sw", "(ILjava/lang/String;)V", false));
        il.insertBefore(n, before);
        for (var e : at.entrySet()) {
            InsnList add = new InsnList();
            add.add(new LdcInsnNode(String.join(",", e.getValue())));
            add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "caseHit", "(Ljava/lang/String;)V", false));
            il.insert(e.getKey(), add);
        }
    }

    /** List calls rewritten to Tracker (name+desc on ArrayList/List -> static replacement). */
    static final java.util.Map<String, String[]> LIST_OPS = new java.util.HashMap<>(java.util.Map.of(
            "add(Ljava/lang/Object;)Z", new String[]{"lAdd", "(Ljava/util/List;Ljava/lang/Object;)Z"},
            "add(ILjava/lang/Object;)V", new String[]{"lAddAt", "(Ljava/util/List;ILjava/lang/Object;)V"},
            "get(I)Ljava/lang/Object;", new String[]{"lGet", "(Ljava/util/List;I)Ljava/lang/Object;"},
            "set(ILjava/lang/Object;)Ljava/lang/Object;", new String[]{"lSet", "(Ljava/util/List;ILjava/lang/Object;)Ljava/lang/Object;"},
            "remove(I)Ljava/lang/Object;", new String[]{"lRemoveAt", "(Ljava/util/List;I)Ljava/lang/Object;"},
            "remove(Ljava/lang/Object;)Z", new String[]{"lRemove", "(Ljava/util/List;Ljava/lang/Object;)Z"},
            "size()I", new String[]{"lSize", "(Ljava/util/List;)I"},
            "isEmpty()Z", new String[]{"lEmpty", "(Ljava/util/List;)Z"},
            "clear()V", new String[]{"lClear", "(Ljava/util/List;)V"}));
    static {
        LIST_OPS.put("indexOf(Ljava/lang/Object;)I", new String[]{"lIndexOf", "(Ljava/util/List;Ljava/lang/Object;)I"});
        LIST_OPS.put("contains(Ljava/lang/Object;)Z", new String[]{"lContains", "(Ljava/util/List;Ljava/lang/Object;)Z"});
        LIST_OPS.put("iterator()Ljava/util/Iterator;", new String[]{"lIter", "(Ljava/util/List;)Ljava/util/Iterator;"});
    }
    /** Local variable stores reported to Tracker: String -> varS (chapter 7 cards), lists and Integer -> varO. */
    static final java.util.Set<String> OBJ_DESCS = java.util.Set.of(
            "Ljava/util/ArrayList;", "Ljava/util/List;", "Ljava/lang/String;", "Ljava/lang/Integer;");
    /** String methods replaced by Tracker calls (same stack shape: the string becomes the first argument). */
    static final java.util.Map<String, String[]> STRING_HOOKS = java.util.Map.of(
            "charAt(I)C", new String[]{"charAt", "(Ljava/lang/String;I)C"},
            "length()I", new String[]{"slen", "(Ljava/lang/String;)I"},
            "toCharArray()[C", new String[]{"toChars", "(Ljava/lang/String;)[C"});

    /** Chapter 16 only: split and substring show the line being cut. */
    static final java.util.Map<String, String[]> FILE_STRING_HOOKS = java.util.Map.of(
            "split(Ljava/lang/String;)[Ljava/lang/String;", new String[]{"split", "(Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;"},
            "substring(II)Ljava/lang/String;", new String[]{"sub2", "(Ljava/lang/String;II)Ljava/lang/String;"},
            "substring(I)Ljava/lang/String;", new String[]{"sub1", "(Ljava/lang/String;I)Ljava/lang/String;"});

    /** True when the student's code works with chars: then c - 'A', % and (char) are animated too. */
    static boolean usesChars(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if (mn.localVariables != null)
                for (LocalVariableNode lv : mn.localVariables) if (lv.desc.equals("C") || lv.desc.equals("[C")) return true;
            if (mn.desc.contains("C)") || mn.desc.endsWith(")C") || mn.desc.contains("[C")) return true;
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                int op = n.getOpcode();
                if (op == Opcodes.CALOAD || op == Opcodes.CASTORE || op == Opcodes.I2C) return true;
                if (n instanceof MethodInsnNode mi && mi.owner.equals("java/lang/String")
                        && (mi.name.equals("charAt") || mi.name.equals("toCharArray"))) return true;
            }
        }
        return false;
    }

    /** Student's own static methods (not main, not the hidden class init). */
    static boolean traced(MethodNode mn) {
        return (mn.access & Opcodes.ACC_STATIC) != 0 && !mn.name.equals("main") && !mn.name.startsWith("<");
    }

    /** Prepend Tracker.enter(name) and one Tracker.arg(value, name) per parameter. */
    static void il0(MethodNode mn) {
        InsnList add = new InsnList();
        add.add(new LdcInsnNode(mn.name));
        add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "enter", "(Ljava/lang/String;)V", false));
        int slot = 0;
        for (jdk.internal.org.objectweb.asm.Type t : jdk.internal.org.objectweb.asm.Type.getArgumentTypes(mn.desc)) {
            String pname = paramName(mn, slot);
            switch (t.getSort()) {
                case jdk.internal.org.objectweb.asm.Type.BOOLEAN -> {
                    add.add(new VarInsnNode(Opcodes.ILOAD, slot));
                    add.add(new LdcInsnNode(pname));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "argZ", "(ZLjava/lang/String;)V", false));
                }
                case jdk.internal.org.objectweb.asm.Type.CHAR -> {
                    add.add(new VarInsnNode(Opcodes.ILOAD, slot));
                    add.add(new LdcInsnNode(pname));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "arg", "(CLjava/lang/String;)V", false));
                }
                case jdk.internal.org.objectweb.asm.Type.INT, jdk.internal.org.objectweb.asm.Type.SHORT,
                     jdk.internal.org.objectweb.asm.Type.BYTE -> {
                    add.add(new VarInsnNode(Opcodes.ILOAD, slot));
                    add.add(new LdcInsnNode(pname));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "arg", "(ILjava/lang/String;)V", false));
                }
                case jdk.internal.org.objectweb.asm.Type.DOUBLE -> {
                    add.add(new VarInsnNode(Opcodes.DLOAD, slot));
                    add.add(new LdcInsnNode(pname));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "arg", "(DLjava/lang/String;)V", false));
                }
                case jdk.internal.org.objectweb.asm.Type.OBJECT, jdk.internal.org.objectweb.asm.Type.ARRAY -> {
                    add.add(new VarInsnNode(Opcodes.ALOAD, slot));
                    add.add(new LdcInsnNode(pname));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Tracker", "arg", "(Ljava/lang/Object;Ljava/lang/String;)V", false));
                }
                default -> { /* long/float: not used in the course */ }
            }
            slot += t.getSize();
        }
        mn.instructions.insert(add);
    }

    static String paramName(MethodNode mn, int slot) {
        if (mn.localVariables != null) {
            for (LocalVariableNode lv : mn.localVariables) {
                if (lv.index == slot && mn.instructions.indexOf(lv.start) <= 1) return lv.name;
            }
        }
        return "arg" + slot;
    }

    /** Source-like names of the two operands just before a comparison ("a|b", "imie|\"Ola\""); "" part when not simple. */
    static String labels(MethodNode mn, InsnList il, AbstractInsnNode n) {
        AbstractInsnNode second = prevReal(n.getPrevious());
        String b = operand(mn, il, second);
        if (b == null) return "|";
        String a = operand(mn, il, prevReal(second.getPrevious()));
        if (a == null) return "|" + b;
        return (a == null ? "" : a) + "|" + b;
    }

    /** found == true, czy(...) != false: a boolean compare is not a search step, leave it alone. */
    static boolean boolCompare(MethodNode mn, InsnList il, AbstractInsnNode n) {
        AbstractInsnNode b = prevReal(n.getPrevious());
        AbstractInsnNode a = b == null ? null : prevReal(b.getPrevious());
        return isBool(mn, il, a) || isBool(mn, il, b);
    }

    static boolean isBool(MethodNode mn, InsnList il, AbstractInsnNode x) {
        if (x instanceof VarInsnNode v && v.getOpcode() == Opcodes.ILOAD) return localName(mn, il, x, v.var, "Z") != null;
        return x instanceof MethodInsnNode mi && mi.desc.endsWith(")Z");
    }

    /** Source-like names of two int operands before a comparison ("t[i]|x", "nr|-1"); "" part when not simple. */
    static String intLabels(MethodNode mn, InsnList il, AbstractInsnNode n) {
        Object[] b = intOperand(mn, il, prevReal(n.getPrevious()));
        if (b == null) return "|";
        Object[] a = intOperand(mn, il, prevReal(((AbstractInsnNode) b[1]).getPrevious()));
        return (a == null ? "" : (String) a[0]) + "|" + b[0];
    }

    /** A simple int operand ending at x: {label, first instruction}; null when it is not a name, a number or arr[name]. */
    static Object[] intOperand(MethodNode mn, InsnList il, AbstractInsnNode x) {
        if (x == null) return null;
        if (x instanceof VarInsnNode v && v.getOpcode() == Opcodes.ILOAD) {
            String nm = localName(mn, il, x, v.var, "I");
            return nm == null ? null : new Object[]{nm, x};
        }
        int op = x.getOpcode();
        if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) return new Object[]{String.valueOf(op - Opcodes.ICONST_0), x};
        if (x instanceof IntInsnNode c && (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH)) return new Object[]{String.valueOf(c.operand), x};
        if (x instanceof LdcInsnNode l && l.cst instanceof Integer k) return new Object[]{String.valueOf(k), x};
        if (x instanceof MethodInsnNode mi && mi.owner.equals("Tracker") && mi.name.equals("load")) {
            Object[] idx = intOperand(mn, il, prevReal(x.getPrevious()));
            if (idx == null) return null;
            AbstractInsnNode arr = prevReal(((AbstractInsnNode) idx[1]).getPrevious());
            String an = operand(mn, il, arr);
            return an == null ? null : new Object[]{an + "[" + idx[0] + "]", arr};
        }
        return null;
    }

    /** Previous real instruction; null when a jump target lies in between ((c ? x : y).equals(b) has no single name). */
    static AbstractInsnNode prevReal(AbstractInsnNode x) {
        while (x != null && x.getOpcode() < 0) {
            if (x instanceof LabelNode) return null;
            x = x.getPrevious();
        }
        return x;
    }

    static String operand(MethodNode mn, InsnList il, AbstractInsnNode x) {
        if (x instanceof VarInsnNode v && v.getOpcode() == Opcodes.ALOAD && mn.localVariables != null) {
            int pos = il.indexOf(x);
            for (LocalVariableNode lv : mn.localVariables) {
                if (lv.index == v.var && pos >= il.indexOf(lv.start) && pos <= il.indexOf(lv.end)) return lv.name;
            }
        }
        if (x instanceof LdcInsnNode l && l.cst instanceof String str) return "\"" + str + "\"";
        return null;
    }

    static String localName(MethodNode mn, InsnList il, AbstractInsnNode n, int slot, String desc) {
        LocalVariableNode lv = localVar(mn, il, n, slot, java.util.Set.of(desc));
        return lv == null ? null : lv.name;
    }

    static LocalVariableNode localVar(MethodNode mn, InsnList il, AbstractInsnNode n, int slot, java.util.Set<String> descs) {
        if (mn.localVariables == null) return null;
        int pos = il.indexOf(n) + 1;
        for (LocalVariableNode lv : mn.localVariables) {
            if (lv.index != slot || !descs.contains(lv.desc)) continue;
            int s = il.indexOf(lv.start), e = il.indexOf(lv.end);
            if (pos >= s && pos <= e) return lv;
        }
        return null;
    }
}
