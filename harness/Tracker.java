import java.util.ArrayList;
import java.util.List;

/**
 * Records every access to the tracked int[] (the one passed to zadanie).
 * Instrumented student bytecode calls these instead of IALOAD / IASTORE / ARRAYLENGTH.
 * The real array operation is performed here, so Java throws its own genuine exception.
 */
public final class Tracker {
    public static final int MAX_EVENTS = 3000;
    public static int lineOffset = 0;

    static int[] tracked;
    static boolean recording;
    /** Class mode: the first int[] the student's code touches becomes the animated array. */
    static boolean autoTrack;
    static final List<String> events = new ArrayList<>();
    static int steps;
    static int maxSteps = 200000;

    /** Thrown when the student's code runs too many array steps (probably endless loop). */
    public static final class TooManySteps extends Error {
        TooManySteps() { super("za duzo krokow"); }
    }

    /** Names of the student's methods entered during the current test (not capped like events). */
    static final java.util.LinkedHashSet<String> called = new java.util.LinkedHashSet<>();

    /** Things the student's code really used during the current test ("==", "equals", "compareTo"). */
    static final java.util.LinkedHashSet<String> used = new java.util.LinkedHashSet<>();
    /** Each distinct String object ("kartka") gets a small number, so == can be shown as "same card or not". */
    static final java.util.IdentityHashMap<Object, Integer> objIds = new java.util.IdentityHashMap<>();
    /** Chapter 3+: the char[] shown as drawers with letters (from toCharArray or a char[] argument). */
    static char[] ctracked;

    static void reset(int[] arr, boolean record) {
        called.clear();
        used.clear();
        objIds.clear();
        tracked = arr;
        ctracked = null;
        autoTrack = false;
        recording = record;
        steps = 0;
        listIds.clear();
        listSnap.clear();
        ops.clear();
        listObjs.clear();
        listNames.clear();
        scanBufs.clear();
        promptShown = 0;
        intent = null;
        curW = null;
        curR = null;
        openFiles.clear();
        fileScanners.clear();
    }

    static StackWalker.StackFrame frame() {
        return StackWalker.getInstance().walk(s -> s
                .filter(f -> f.getClassName().equals("Main") || f.getClassName().startsWith("Main$"))
                .findFirst()
                .orElse(null));
    }

    /** Methods whose events are not reported (-Dgra.quiet=a,b): a ready-made helper like szyfruj would only add noise. */
    static final java.util.Set<String> QUIET = java.util.Set.of(System.getProperty("gra.quiet", "").split(","));

    static void ev(String json) {
        if (!recording) return;
        if (events.size() >= MAX_EVENTS) {
            if (events.size() == MAX_EVENTS) events.add("{\"t\":\"cut\"}");
            return;
        }
        StackWalker.StackFrame f = frame();
        int l = f == null ? -1 : f.getLineNumber() - lineOffset;
        String m = f == null ? "" : f.getMethodName();
        if (!m.isEmpty() && QUIET.contains(m)) return;
        events.add(json.substring(0, json.length() - 1) + ",\"l\":" + l + ",\"m\":" + Runner.q(m) + "}");
    }

    static void tick() {
        if (++steps > maxSteps) throw new TooManySteps();
    }

    static void adopt(Object a) {
        if (autoTrack && tracked == null && a instanceof int[] arr) {
            tracked = arr;
            ev("{\"t\":\"arr\",\"vals\":" + Runner.arr(arr) + "}");
        }
    }

    public static int load(int[] a, int i) {
        tick();
        adopt(a);
        if (a == tracked && a != null) {
            if (i < 0 || i >= a.length) ev("{\"t\":\"oob\",\"op\":\"r\",\"i\":" + i + ",\"n\":" + a.length + "}");
            else ev("{\"t\":\"r\",\"i\":" + i + ",\"v\":" + a[i] + "}");
        }
        return a[i];
    }

    public static void store(int[] a, int i, int v) {
        tick();
        adopt(a);
        if (a == tracked && a != null) {
            if (i < 0 || i >= a.length) ev("{\"t\":\"oob\",\"op\":\"w\",\"i\":" + i + ",\"n\":" + a.length + ",\"v\":" + v + "}");
            else ev("{\"t\":\"w\",\"i\":" + i + ",\"o\":" + a[i] + ",\"v\":" + v + "}");
        }
        a[i] = v;
    }

    public static int len(Object a) {
        if (a == null) throw new NullPointerException("Cannot read the array length because the array is null");
        tick();
        adopt(a);
        if (a == tracked && a != null) ev("{\"t\":\"len\",\"n\":" + ((int[]) a).length + "}");
        else if (a == ctracked) ev("{\"t\":\"len\",\"n\":" + ((char[]) a).length + "}");
        return java.lang.reflect.Array.getLength(a);
    }

    public static void var(int v, String name) {
        tick();
        ev("{\"t\":\"v\",\"name\":" + Runner.q(name) + ",\"v\":" + v + "}");
    }

    public static void varD(double v, String name) {
        tick();
        ev("{\"t\":\"v\",\"name\":" + Runner.q(name) + ",\"v\":" + Runner.q(String.valueOf(v)) + "}");
    }

    /** Method entry / parameters / exit (inserted into every static method except main). */
    public static void enter(String name) {
        called.add(name);
        tick();
        ev("{\"t\":\"call\",\"name\":" + Runner.q(name) + "}");
    }

    public static void arg(int v, String name) {
        ev("{\"t\":\"arg\",\"name\":" + Runner.q(name) + ",\"v\":" + v + "}");
    }

    public static void arg(double v, String name) {
        ev("{\"t\":\"arg\",\"name\":" + Runner.q(name) + ",\"v\":" + Runner.q(String.valueOf(v)) + "}");
    }

    public static void arg(Object v, String name) {
        adopt(v);
        if (v instanceof java.util.List<?> l) lid(l, name, null);
        if (v instanceof char[] c) adoptC(c);
        ev("{\"t\":\"arg\",\"name\":" + Runner.q(name) + ",\"v\":" + Runner.q(show(v)) + obj(v) + "}");
    }

    // ---------- Strings (chapter 7): String locals, ==, equals, compareTo, Scanner input ----------

    static int objId(Object o) {
        Integer id = objIds.get(o);
        if (id == null) { id = objIds.size() + 1; objIds.put(o, id); }
        return id;
    }

    /** ,"obj":N for a String (its card number), empty otherwise. */
    static String obj(Object o) {
        return o instanceof String ? ",\"obj\":" + objId(o) : "";
    }

    static String sv(String s) { return s == null ? "null" : Runner.q(s); }

    /** Store to a named String local. */
    public static void varS(Object v, String name) {
        tick();
        ev("{\"t\":\"v\",\"name\":" + Runner.q(name) + ",\"v\":" + Runner.q(show(v)) + obj(v) + "}");
    }

    /** Replaces IF_ACMPEQ/IF_ACMPNE: reference comparison, reported when a String takes part. */
    public static boolean refEq(Object a, Object b, String labels) {
        tick();
        boolean same = a == b;
        if (a instanceof String || b instanceof String) {
            used.add("==");
            ev("{\"t\":\"same\",\"a\":" + sv((String) (a instanceof String ? a : null)) + ",\"b\":" + sv(b instanceof String ? (String) b : null)
                    + ",\"ia\":" + (a == null ? 0 : objId(a)) + ",\"ib\":" + (b == null ? 0 : objId(b))
                    + ",\"v\":" + same + ",\"lab\":" + Runner.q(labels) + "}");
        }
        return same;
    }

    /** Replaces String.equals(Object). */
    public static boolean strEq(String a, Object b, String labels) {
        tick();
        nullCheck(a, "String.equals(Object)", labels);
        boolean r = a.equals(b);
        used.add("equals");
        String bs = b instanceof String ? (String) b : null;
        ev("{\"t\":\"eq\",\"a\":" + sv(a) + ",\"b\":" + (b == null ? "null" : bs != null ? Runner.q(bs) : Runner.q(String.valueOf(b)))
                + (b != null && bs == null ? ",\"notStr\":true" : "") + ",\"i\":" + firstDiff(a, bs) + ",\"v\":" + r + ",\"lab\":" + Runner.q(labels) + "}");
        return r;
    }

    /** Replaces String.compareTo(String). */
    public static int strCmp(String a, String b, String labels) {
        tick();
        nullCheck(a, "String.compareTo(String)", labels);
        int r = a.compareTo(b);
        used.add("compareTo");
        ev("{\"t\":\"cmp\",\"a\":" + sv(a) + ",\"b\":" + sv(b) + ",\"i\":" + firstDiff(a, b)
                + ",\"v\":" + r + ",\"lab\":" + Runner.q(labels) + "}");
        return r;
    }

    /** Same message as real Java for null.equals(..), with the student's name instead of Tracker's parameter. */
    static void nullCheck(String a, String method, String labels) {
        if (a != null) return;
        String name = labels.substring(0, labels.indexOf('|'));
        throw new NullPointerException("Cannot invoke \"" + method + "\" because "
                + (name.isEmpty() || name.startsWith("\"") ? "the value before the dot" : "\"" + name + "\"") + " is null");
    }

    /** Index of the first differing letter; min length when one is the start of the other; -1 when equal. */
    static int firstDiff(String a, String b) {
        if (a == null || b == null) return -1;
        int n = Math.min(a.length(), b.length());
        for (int k = 0; k < n; k++) if (a.charAt(k) != b.charAt(k)) return k;
        return a.length() == b.length() ? -1 : n;
    }

    /**
     * Scanner reads (every next.../hasNext... the course uses) go through here: the line typed in the console is
     * echoed like IntelliJ does. Chapter 10 also gets what was newly typed during this call ("typed", "" = the call
     * took what already waited in the Scanner's buffer), the buffer before ("pre") and after ("buf"), so the
     * leftover Enter after nextInt() can be drawn.
     */
    public static String nextLine(java.util.Scanner s) {
        if (isFileScanner(s)) return (String) fileRead(s, "nextLine");
        int from = readStart();
        String line;
        try {
            line = s.nextLine();
        } catch (RuntimeException e) {
            readFail(s, "nextLine", from, e);
            throw e;
        }
        StringBuilder b = sbuf(s, from);
        String pre = b.toString();
        int nl = b.indexOf("\n");
        if (nl < 0) b.setLength(0); else b.delete(0, nl + 1);
        ev("{\"t\":\"in\",\"k\":\"nextLine\",\"s\":" + Runner.q(line) + typedJson(from) + ",\"pre\":" + Runner.q(pre) + ",\"buf\":" + Runner.q(b.toString()) + "}");
        return line;
    }

    public static int nextInt(java.util.Scanner s) {
        if (isFileScanner(s)) return (Integer) fileRead(s, "nextInt");
        int from = readStart();
        int v;
        try {
            v = s.nextInt();
        } catch (RuntimeException e) {
            readFail(s, "nextInt", from, e);
            throw e;
        }
        token(s, "nextInt", String.valueOf(v), from);
        return v;
    }

    public static double nextDouble(java.util.Scanner s) {
        int from = readStart();
        double v;
        try {
            v = s.nextDouble();
        } catch (RuntimeException e) {
            readFail(s, "nextDouble", from, e);
            throw e;
        }
        token(s, "nextDouble", String.valueOf(v), from);
        return v;
    }

    public static String next(java.util.Scanner s) {
        if (isFileScanner(s)) return (String) fileRead(s, "next");
        int from = readStart();
        String v;
        try {
            v = s.next();
        } catch (RuntimeException e) {
            readFail(s, "next", from, e);
            throw e;
        }
        token(s, "next", v, from);
        return v;
    }

    /** hasNextInt / hasNext / hasNextLine only look into the buffer (they may wait for typing), nothing is taken. */
    public static boolean hasNextInt(java.util.Scanner s) { if (isFileScanner(s)) return filePeek(s, "hasNextInt"); int from = readStart(); boolean r = s.hasNextInt(); peek(s, "hasNextInt", r, from); return r; }
    public static boolean hasNext(java.util.Scanner s) { if (isFileScanner(s)) return filePeek(s, "hasNext"); int from = readStart(); boolean r = s.hasNext(); peek(s, "hasNext", r, from); return r; }
    public static boolean hasNextLine(java.util.Scanner s) { if (isFileScanner(s)) return filePeek(s, "hasNextLine"); int from = readStart(); boolean r = s.hasNextLine(); peek(s, "hasNextLine", r, from); return r; }

    static int readStart() {
        tick();
        prompt();
        return Runner.KEYBOARD.pos;
    }

    /** A token read (nextInt, nextDouble, next): skips spaces and Enters in front, takes the token. */
    static void token(java.util.Scanner s, String k, String shown, int from) {
        StringBuilder b = sbuf(s, from);
        String pre = b.toString();
        int i = 0;
        while (i < b.length() && Character.isWhitespace(b.charAt(i))) i++;
        int skipped = i;
        while (i < b.length() && !Character.isWhitespace(b.charAt(i))) i++;
        b.delete(0, i);
        ev("{\"t\":\"in\",\"k\":" + Runner.q(k) + ",\"s\":" + Runner.q(shown) + ",\"skip\":" + skipped
                + typedJson(from) + ",\"pre\":" + Runner.q(pre) + ",\"buf\":" + Runner.q(b.toString()) + "}");
    }

    static void peek(java.util.Scanner s, String k, boolean r, int from) {
        StringBuilder b = sbuf(s, from);
        ev("{\"t\":\"in\",\"k\":" + Runner.q(k) + ",\"s\":\"" + r + "\",\"peek\":true" + typedJson(from)
                + ",\"pre\":" + Runner.q(b.toString()) + ",\"buf\":" + Runner.q(b.toString()) + "}");
    }

    /** Wrong kind of text (InputMismatch: the token stays in the buffer) or nothing left to read (eof). */
    static void readFail(java.util.Scanner s, String k, int from, RuntimeException e) {
        StringBuilder b = sbuf(s, from);
        if (e instanceof java.util.InputMismatchException) {
            String rest = b.toString().stripLeading();
            int end = 0;
            while (end < rest.length() && !Character.isWhitespace(rest.charAt(end))) end++;
            ev("{\"t\":\"in\",\"k\":" + Runner.q(k) + ",\"s\":" + Runner.q(rest.substring(0, end)) + ",\"bad\":true" + typedJson(from) + ",\"buf\":" + Runner.q(b.toString()) + "}");
        } else if (e instanceof java.util.NoSuchElementException) {
            ev("{\"t\":\"in\",\"k\":" + Runner.q(k) + ",\"s\":\"\",\"eof\":true" + typedJson(from) + ",\"buf\":" + Runner.q(b.toString()) + "}");
        }
    }

    // ---------- chapter 10: menu loop (Scanner buffer, print prompt, switch, do-while) ----------

    /** What each Scanner holds but has not given out yet (rebuilt from the keyboard text it read). */
    static final java.util.IdentityHashMap<java.util.Scanner, StringBuilder> scanBufs = new java.util.IdentityHashMap<>();

    /** The Scanner's buffer after adding the keyboard text read since position from. */
    static StringBuilder sbuf(java.util.Scanner s, int from) {
        StringBuilder b = scanBufs.computeIfAbsent(s, x -> new StringBuilder());
        b.append(typed(from));
        return b;
    }

    static String typed(int from) {
        Runner.Keyboard kb = Runner.KEYBOARD;
        return new String(kb.data, from, Math.max(0, kb.pos - from), java.nio.charset.StandardCharsets.UTF_8);
    }

    static String typedJson(int from) { return ",\"typed\":" + Runner.q(typed(from)); }

    /** Text printed with print (no Enter yet) is shown before the program waits for the keyboard, like in IntelliJ. */
    static void prompt() {
        String p = Runner.pending();
        if (p.length() <= promptShown) return;
        ev("{\"t\":\"outp\",\"s\":" + Runner.q(p) + ",\"from\":" + promptShown + "}");
        promptShown = p.length();
    }

    /** How much of the started (unfinished) output line was already shown by prompt(); 0 after every Enter. */
    static int promptShown;

    /** Before a switch jumps: the value and the case numbers it knows ("1,2,3,0"), d = it has a default target. */
    public static void sw(int v, String cases) {
        tick();
        ev("{\"t\":\"sw\",\"v\":" + v + ",\"cases\":" + Runner.q(cases) + "}");
    }

    /** At the start of the code of a case (reached by the jump or by falling through from the case above). */
    public static void caseHit(String keys) {
        tick();
        ev("{\"t\":\"case\",\"k\":" + Runner.q(keys) + "}");
    }

    /** Condition at the bottom of a do-while (a backward conditional jump): go = the loop goes round again. */
    public static void loop1(int a, String op) {
        tick();
        boolean go = switch (op) {
            case "IFEQ" -> a == 0; case "IFNE" -> a != 0; case "IFLT" -> a < 0;
            case "IFGE" -> a >= 0; case "IFGT" -> a > 0; default -> a <= 0;
        };
        ev("{\"t\":\"dw\",\"a\":" + a + ",\"go\":" + go + "}");
    }

    public static void loop2(int a, int b, String op) {
        tick();
        boolean go = switch (op) {
            case "IF_ICMPEQ" -> a == b; case "IF_ICMPNE" -> a != b; case "IF_ICMPLT" -> a < b;
            case "IF_ICMPGE" -> a >= b; case "IF_ICMPGT" -> a > b; default -> a <= b;
        };
        ev("{\"t\":\"dw\",\"a\":" + a + ",\"b\":" + b + ",\"go\":" + go + "}");
    }

    public static void retI(int v) { ev("{\"t\":\"ret\",\"v\":" + Runner.q(String.valueOf(v)) + "}"); }
    /** boolean methods: bytecode returns an int (1/0), show true/false. */
    public static void retZ(boolean v) { ev("{\"t\":\"ret\",\"v\":" + Runner.q(String.valueOf(v)) + "}"); }
    public static void argZ(boolean v, String name) {
        ev("{\"t\":\"arg\",\"name\":" + Runner.q(name) + ",\"v\":" + Runner.q(String.valueOf(v)) + "}");
    }
    public static void retD(double v) { ev("{\"t\":\"ret\",\"v\":" + Runner.q(String.valueOf(v)) + "}"); }
    public static void retO(Object v) { ev("{\"t\":\"ret\",\"v\":" + Runner.q(show(v)) + "}"); }
    public static void retV() { ev("{\"t\":\"ret\",\"v\":null}"); }

    // ---------------- chapter 3: chars are numbers ----------------

    /** JSON fields for one char: its code and the letter itself. */
    static String ch(int v) {
        return "\"v\":" + v + ",\"c\":" + Runner.q(String.valueOf((char) v));
    }

    /** "'y' (121)" - the letter and its number shown together. */
    static String showC(int v) {
        return "'" + (char) v + "' (" + v + ")";
    }

    /** Same message as plain Java would give, without naming Tracker's own parameters. */
    static void nn(Object x, String what) {
        if (x == null) throw new NullPointerException("Cannot " + what + " because the value is null");
    }

    static void adoptC(char[] a) {
        if (ctracked == null && a != null) {
            ctracked = a;
            ev("{\"t\":\"carr\",\"s\":null,\"vals\":" + codes(a) + "}");
        }
    }

    static String codes(char[] a) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) { if (i > 0) b.append(','); b.append((int) a[i]); }
        return b.append(']').toString();
    }

    public static char cload(char[] a, int i) {
        nn(a, "load from char array");
        tick();
        adoptC(a);
        if (a == ctracked && a != null) {
            if (i < 0 || i >= a.length) ev("{\"t\":\"oob\",\"op\":\"r\",\"i\":" + i + ",\"n\":" + a.length + "}");
            else ev("{\"t\":\"cr\",\"i\":" + i + "," + ch(a[i]) + "}");
        }
        return a[i];
    }

    public static void cstore(char[] a, int i, char v) {
        nn(a, "store to char array");
        tick();
        adoptC(a);
        if (a == ctracked && a != null) {
            if (i < 0 || i >= a.length) ev("{\"t\":\"oob\",\"op\":\"w\",\"i\":" + i + ",\"n\":" + a.length + ",\"v\":" + (int) v + "}");
            else ev("{\"t\":\"cw\",\"i\":" + i + ",\"o\":" + (int) a[i] + ",\"oc\":" + Runner.q(String.valueOf(a[i])) + "," + ch(v) + "}");
        }
        a[i] = v;
    }

    /** text.charAt(i) */
    public static char charAt(String s, int i) {
        nn(s, "invoke \"String.charAt(int)\"");
        tick();
        if (s != null) {
            if (i < 0 || i >= s.length()) ev("{\"t\":\"soob\",\"s\":" + Runner.q(s) + ",\"i\":" + i + "}");
            else ev("{\"t\":\"sget\",\"s\":" + Runner.q(s) + ",\"i\":" + i + "," + ch(s.charAt(i)) + "}");
        }
        return s.charAt(i);
    }

    /** text.length() */
    public static int slen(String s) {
        nn(s, "invoke \"String.length()\"");
        tick();
        if (s != null) ev("{\"t\":\"slen\",\"s\":" + Runner.q(s) + ",\"n\":" + s.length() + "}");
        return s.length();
    }

    /** text.toCharArray(): the new array becomes the drawers with letters. */
    public static char[] toChars(String s) {
        nn(s, "invoke \"String.toCharArray()\"");
        tick();
        char[] a = s.toCharArray();
        ctracked = a;
        ev("{\"t\":\"carr\",\"s\":" + Runner.q(s) + ",\"vals\":" + codes(a) + "}");
        return a;
    }

    /** new String(tab): called with a copy of the array reference just before the constructor. */
    public static void joinC(char[] a) {
        tick();
        if (a != null) ev("{\"t\":\"cjoin\",\"same\":" + (a == ctracked) + ",\"s\":" + Runner.q(new String(a)) + "}");
    }

    /** c - 'A' / c - 'a': distance from the start of the alphabet. */
    public static int dist(int a, int b) {
        tick();
        ev("{\"t\":\"dist\",\"a\":" + a + ",\"b\":" + b + ",\"r\":" + (a - b) + "}");
        return a - b;
    }

    /** x % y (only in code that works with chars). */
    public static int mod(int a, int b) {
        tick();
        int r = a % b;
        ev("{\"t\":\"mod\",\"a\":" + a + ",\"b\":" + b + ",\"r\":" + r + "}");
        return r;
    }

    /** (char) x: a number becomes a letter. */
    public static char i2c(int v) {
        tick();
        ev("{\"t\":\"i2c\"," + ch((char) v) + ",\"from\":" + v + "}");
        return (char) v;
    }

    /** Store to a named char local. */
    public static void varC(int v, String name) {
        tick();
        ev("{\"t\":\"v\",\"name\":" + Runner.q(name) + ",\"v\":" + Runner.q(showC(v)) + ",\"ch\":" + v + "}");
    }

    public static void arg(char v, String name) {
        ev("{\"t\":\"arg\",\"name\":" + Runner.q(name) + ",\"v\":" + Runner.q(showC(v)) + ",\"ch\":" + (int) v + "}");
    }

    public static void retC(int v) { ev("{\"t\":\"ret\",\"v\":" + Runner.q(showC(v)) + ",\"ch\":" + v + "}"); }

    // ---------------- chapter 4: char[] names ----------------

    /** Store of the char[] shown as drawers into a named local: tells the animation its name (znaki). */
    public static void nameC(Object a, String name) {
        if (a instanceof char[] c && c == ctracked) ev("{\"t\":\"an\",\"name\":" + Runner.q(name) + "}");
    }

    static String show(Object v) {
        if (v == null) return "null";
        if (v instanceof int[] a) return a == tracked ? "tablica tab" : "tablica " + Runner.arr(a);
        if (v instanceof String s) return "\"" + s + "\"";
        if (v instanceof java.util.List<?> l) return "lista#" + lid(l, null, null);
        if (v instanceof char[] c) return c == ctracked ? "szuflady z literami" : "litery " + new String(c);
        return String.valueOf(v);
    }

    // =====================================================================
    // ArrayList / List tracking (chapter 5+). Generic: any number of lists,
    // identified by object identity, named after the first variable holding them.
    // Student bytecode calls these instead of List.add/get/set/remove/size/isEmpty/clear;
    // the real list operation runs here, so Java throws its own genuine exceptions.
    // Events: lst (list appears), lsync, ladd, lget, lset, lrem, lmiss, lsize, lclear, loob, lv.
    // =====================================================================
    static final java.util.IdentityHashMap<Object, Integer> listIds = new java.util.IdentityHashMap<>();
    /** What the animation last saw of each list (by id), to detect changes made outside the tracked calls. */
    static final int SYNC_MAX = 2000;
    static final java.util.HashMap<Integer, java.util.List<Object>> listSnap = new java.util.HashMap<>();
    /** List operations used during the current test (add, get, set, size, remove, ...). */
    static final java.util.LinkedHashSet<String> ops = new java.util.LinkedHashSet<>();

    /** Element as JSON: Integer -> number, String -> "text", null -> null. */
    static String jv(Object o) {
        if (o == null) return "null";
        if (o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte) return String.valueOf(o);
        if (o instanceof Double d && !d.isNaN() && !d.isInfinite()) return String.valueOf(d);
        return Runner.q(String.valueOf(o));
    }

    static String vals(java.util.List<?> l) {
        StringBuilder b = new StringBuilder("[");
        int k = 0;
        for (Object o : l) { if (k++ > 0) b.append(','); b.append(jv(o)); if (k >= 200) break; }
        return b.append(']').toString();
    }

    /** "ArrayList<String>" signature -> "String"; unknown -> guessed from the first element. */
    static String elemType(String sig, java.util.List<?> l) {
        if (sig != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<L(?:[\\w/]+/)?(\\w+);>").matcher(sig);
            if (m.find()) return m.group(1);
        }
        for (Object o : l) if (o != null) return o.getClass().getSimpleName();
        return "";
    }

    /**
     * Id of the list; registers it (event lst with its current contents) on first sight.
     * If the list changed since the last tracked call (addAll, Collections.swap/sort, removeIf,
     * iterator().remove() ...), emits lsync with the real contents so the shelf is rebuilt.
     */
    static int lid(java.util.List<?> l, String name, String sig) {
        syncAll();
        Integer id = listIds.get(l);
        if (id == null) {
            id = listIds.size() + 1;
            listIds.put(l, id);
            listObjs.add(l);
            listNames.add(name == null ? "lista" : name);
            ev("{\"t\":\"lst\",\"id\":" + id + ",\"name\":" + Runner.q(name == null ? "lista" : name)
                    + ",\"et\":" + Runner.q(elemType(sig, l)) + ",\"vals\":" + vals(l) + ",\"n\":" + l.size()
                    + (name == null ? "" : ",\"named\":true") + "}");
        }
        seen(id, l);
        return id;
    }

    /** Every tracked list that changed untracked since its last tracked call gets its lsync now - all of them
     *  together, so Collections.swap(a, ..); Collections.swap(b, ..) is reported as one moment, not one by one. */
    static void syncAll() {
        for (int k = 0; k < listObjs.size(); k++) {
            java.util.List<?> o = listObjs.get(k);
            if (o.size() <= SYNC_MAX && !o.equals(listSnap.get(k + 1))) {
                ev("{\"t\":\"lsync\",\"id\":" + (k + 1) + ",\"vals\":" + vals(o) + ",\"n\":" + o.size() + "}");
                seen(k + 1, o);
            }
        }
    }

    /** After a tracked change: remember what the animation now shows. */
    static void seen(int id, java.util.List<?> l) {
        // big lists are not compared (copying them on every call would be too slow)
        listSnap.put(id, l.size() <= SYNC_MAX ? new java.util.ArrayList<Object>(l) : null);
    }

    static void lnn(Object l, String op) {
        if (l == null) throw new NullPointerException("lista jest null - nie ma jeszcze pudełek (brakuje new ArrayList<>()), więc nie da się zrobić " + op + "()");
    }

    static void oob(int id, String op, int i, int n, Object v) {
        ev("{\"t\":\"loob\",\"id\":" + id + ",\"op\":" + Runner.q(op) + ",\"i\":" + i + ",\"n\":" + n
                + (v == null ? "" : ",\"v\":" + jv(v)) + "}");
    }

    public static boolean lAdd(java.util.List<Object> l, Object x) {
        lnn(l, "add"); tick(); ops.add("add");
        int id = lid(l, null, null);
        boolean r = l.add(x);
        seen(id, l);
        ev("{\"t\":\"ladd\",\"id\":" + id + ",\"i\":" + (l.size() - 1) + ",\"v\":" + jv(x) + ",\"n\":" + l.size() + ",\"end\":true}");
        return r;
    }

    public static void lAddAt(java.util.List<Object> l, int i, Object x) {
        lnn(l, "add"); tick(); ops.add("add");
        int id = lid(l, null, null);
        if (i < 0 || i > l.size()) oob(id, "add", i, l.size(), x);
        l.add(i, x);
        seen(id, l);
        ev("{\"t\":\"ladd\",\"id\":" + id + ",\"i\":" + i + ",\"v\":" + jv(x) + ",\"n\":" + l.size() + ",\"end\":false}");
    }

    public static Object lGet(java.util.List<?> l, int i) {
        lnn(l, "get"); tick(); ops.add("get");
        int id = lid(l, null, null);
        if (i < 0 || i >= l.size()) oob(id, "get", i, l.size(), null);
        Object v = l.get(i);
        ev("{\"t\":\"lget\",\"id\":" + id + ",\"i\":" + i + ",\"v\":" + jv(v) + "}");
        return v;
    }

    public static Object lSet(java.util.List<Object> l, int i, Object x) {
        lnn(l, "set"); tick(); ops.add("set");
        int id = lid(l, null, null);
        if (i < 0 || i >= l.size()) oob(id, "set", i, l.size(), x);
        Object o = l.set(i, x);
        seen(id, l);
        ev("{\"t\":\"lset\",\"id\":" + id + ",\"i\":" + i + ",\"o\":" + jv(o) + ",\"v\":" + jv(x) + "}");
        return o;
    }

    public static Object lRemoveAt(java.util.List<?> l, int i) {
        lnn(l, "remove"); tick(); ops.add("remove");
        int id = lid(l, null, null);
        if (i < 0 || i >= l.size()) oob(id, "remove", i, l.size(), null);
        Object v = l.remove(i);
        seen(id, l);
        ev("{\"t\":\"lrem\",\"id\":" + id + ",\"i\":" + i + ",\"v\":" + jv(v) + ",\"n\":" + l.size() + "}");
        return v;
    }

    public static boolean lRemove(java.util.List<?> l, Object x) {
        lnn(l, "remove"); tick(); ops.add("remove");
        int id = lid(l, null, null);
        int i = l.indexOf(x);
        boolean r = l.remove(x);
        seen(id, l);
        if (r) ev("{\"t\":\"lrem\",\"id\":" + id + ",\"i\":" + i + ",\"v\":" + jv(x) + ",\"n\":" + l.size() + "}");
        else ev("{\"t\":\"lmiss\",\"id\":" + id + ",\"v\":" + jv(x) + "}");
        return r;
    }

    public static int lSize(java.util.List<?> l) {
        lnn(l, "size"); tick(); ops.add("size");
        int id = lid(l, null, null);
        ev("{\"t\":\"lsize\",\"id\":" + id + ",\"n\":" + l.size() + "}");
        return l.size();
    }

    public static boolean lEmpty(java.util.List<?> l) {
        lnn(l, "isEmpty"); tick(); ops.add("isEmpty");
        int id = lid(l, null, null);
        ev("{\"t\":\"lsize\",\"id\":" + id + ",\"n\":" + l.size() + ",\"op\":\"isEmpty\"}");
        return l.isEmpty();
    }

    public static void lClear(java.util.List<?> l) {
        lnn(l, "clear"); tick(); ops.add("clear");
        int id = lid(l, null, null);
        l.clear();
        seen(id, l);
        ev("{\"t\":\"lclear\",\"id\":" + id + "}");
    }

    // ---- chapter 6 additions: indexOf/contains (lfind), for-each (lget with "it":1), final state of all lists ----
    static final List<java.util.List<?>> listObjs = new ArrayList<>();
    static final List<String> listNames = new ArrayList<>();

    public static int lIndexOf(java.util.List<?> l, Object x) {
        lnn(l, "indexOf"); tick(); ops.add("indexOf");
        int id = lid(l, null, null);
        int i = l.indexOf(x);
        ev("{\"t\":\"lfind\",\"id\":" + id + ",\"op\":\"indexOf\",\"v\":" + jv(x) + ",\"i\":" + i + "}");
        return i;
    }

    public static boolean lContains(java.util.List<?> l, Object x) {
        lnn(l, "contains"); tick(); ops.add("contains");
        int id = lid(l, null, null);
        int i = l.indexOf(x);
        ev("{\"t\":\"lfind\",\"id\":" + id + ",\"op\":\"contains\",\"v\":" + jv(x) + ",\"i\":" + i + "}");
        return i >= 0;
    }

    /** for-each over a list: every next() is reported as a read of element i. */
    public static java.util.Iterator<Object> lIter(java.util.List<?> l) {
        lnn(l, "iterator"); ops.add("iterator");
        lid(l, null, null);
        @SuppressWarnings("unchecked") java.util.Iterator<Object> it = (java.util.Iterator<Object>) l.iterator();
        return new java.util.Iterator<Object>() {
            int i = 0;
            public boolean hasNext() { return it.hasNext(); }
            public Object next() {
                tick();
                Object v = it.next();
                int id = lid(l, null, null); // emits lsync first if the loop body changed the list untracked
                ev("{\"t\":\"lget\",\"id\":" + id + ",\"i\":" + (i++) + ",\"v\":" + jv(v) + ",\"it\":1}");
                return v;
            }
            public void remove() { it.remove(); i--; }
        };
    }

    /** Final contents of every tracked list (the frontend re-syncs after the 3000-event cut). */
    static String trackedJson() {
        StringBuilder b = new StringBuilder("[");
        for (int k = 0; k < listObjs.size(); k++) {
            if (k > 0) b.append(',');
            b.append("{\"id\":").append(k + 1).append(",\"name\":").append(Runner.q(listNames.get(k)))
             .append(",\"vals\":").append(vals(listObjs.get(k))).append('}');
        }
        return b.append(']').toString();
    }

    /** Store to a local of type ArrayList/List/Integer (sig = generic signature or null). */
    public static void varO(Object v, String name, String sig) {
        tick();
        if (v instanceof java.util.List<?> l) {
            int id = lid(l, name, sig);
            ev("{\"t\":\"lv\",\"name\":" + Runner.q(name) + ",\"id\":" + id + "}");
        } else {
            // Integer local: same "v" event as an int local (String locals go through varS)
            ev("{\"t\":\"v\",\"name\":" + Runner.q(name) + ",\"v\":" + jv(v) + "}");
        }
    }

    // ---- chapter 8: int == comparisons (t[i] == x) for the linear search animation ----

    /** Replaces IF_ICMPEQ/IF_ICMPNE when the chapter asks for it: reports both values and the result. */
    public static boolean ieq(int a, int b, String labels) {
        tick();
        ev("{\"t\":\"ieq\",\"a\":" + a + ",\"b\":" + b + ",\"v\":" + (a == b) + ",\"lab\":" + Runner.q(labels) + "}");
        return a == b;
    }

    // ---------------- chapter 9: comparisons in sorting ----------------

    /** Source operator of an if whose jump opcode is the negation (javac jumps over the body when the test fails). */
    static String srcOp(int jumpOp) {
        return switch (jumpOp) {
            case 159, 153 -> "!=";   // IF_ICMPEQ / IFEQ
            case 160, 154 -> "==";   // IF_ICMPNE / IFNE
            case 161, 155 -> ">=";   // IF_ICMPLT / IFLT
            case 162, 156 -> "<";    // IF_ICMPGE / IFGE
            case 163, 157 -> "<=";   // IF_ICMPGT / IFGT
            default -> ">";          // IF_ICMPLE / IFLE
        };
    }

    static boolean jumps(int a, int b, int jumpOp) {
        return switch (jumpOp) {
            case 159 -> a == b;
            case 160 -> a != b;
            case 161 -> a < b;
            case 162 -> a >= b;
            case 163 -> a > b;
            default -> a <= b;
        };
    }

    /** Replaces IF_ICMPxx right after an array read: a and b, the operator as written in the code, true/false. */
    public static boolean icmp(int a, int b, int jumpOp) {
        tick();
        boolean j = jumps(a, b, jumpOp);
        ev("{\"t\":\"icmp\",\"a\":" + a + ",\"b\":" + b + ",\"op\":" + Runner.q(srcOp(jumpOp)) + ",\"v\":" + !j + "}");
        return j;
    }

    /** Replaces IFxx right after compareTo: the result checked against zero. */
    public static boolean zcmp(int v, int jumpOp) {
        tick();
        boolean j = jumps(v, 0, jumpOp + 6);
        ev("{\"t\":\"zcmp\",\"a\":" + v + ",\"op\":" + Runner.q(srcOp(jumpOp)) + ",\"v\":" + !j + "}");
        return j;
    }

    static void out(String text) {
        ev("{\"t\":\"out\",\"s\":" + Runner.q(text) + "}");
    }

    /** The last line of the program had no Enter (print at the very end, or a crash after print). */
    static void outRest(String text) {
        ev("{\"t\":\"out\",\"s\":" + Runner.q(text) + ",\"nonl\":true}");
    }

    // ---------- chapter 16: files and exceptions ----------

    /** What the next file open will be ("r", "w", "a"): set by Runner's hook right before the constructor. */
    static String intent;
    static String curW, curR;
    static final List<String> openFiles = new ArrayList<>();
    static final java.util.Set<Object> fileScanners = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    public static void intent(String m) { intent = m; }
    public static void intentA(boolean append) { intent = append ? "a" : "w"; }
    public static void fileScanner(java.util.Scanner s) { fileScanners.add(s); }
    static boolean isFileScanner(java.util.Scanner s) { return fileScanners.contains(s); }

    /** The SecurityManager saw the student's code open a file inside the sandbox. */
    static void touch(java.nio.file.Path p, java.nio.file.Path root, boolean write) {
        String m = intent;
        intent = null;
        if (m == null) return;
        boolean g = Runner.Sandbox.guard;
        Runner.Sandbox.guard = false;
        try {
            String rel = root.relativize(p).toString();
            boolean dir = java.nio.file.Files.isDirectory(p);
            boolean ok;
            String why = "";
            if (write) {
                java.nio.file.Path par = p.getParent();
                ok = !dir && (par == null || java.nio.file.Files.isDirectory(par));
                if (dir) why = "to jest katalog, nie plik";
            } else {
                ok = java.nio.file.Files.isRegularFile(p);
                if (!ok) why = dir ? "to jest katalog, nie plik" : "nie ma takiego pliku";
            }
            if (ok) {
                if (write) curW = rel; else curR = rel;
                openFiles.add(rel);
            }
            ev("{\"t\":\"fopen\",\"f\":" + Runner.q(rel) + ",\"md\":\"" + m + "\",\"ok\":" + ok + ",\"why\":" + Runner.q(why) + "}");
        } finally {
            Runner.Sandbox.guard = g;
        }
    }

    public static boolean fexists(java.io.File f) {
        boolean v = f.exists();
        ev("{\"t\":\"fexists\",\"f\":" + Runner.q(f.getPath()) + ",\"v\":" + v + "}");
        return v;
    }

    static void fw(String s) { ev("{\"t\":\"fw\",\"f\":" + Runner.q(curW == null ? "" : curW) + ",\"s\":" + Runner.q(s) + "}"); }

    public static void wWrite(java.io.Writer w, String s) throws java.io.IOException { tick(); w.write(s); fw(s); }
    public static void pwPrintln(java.io.PrintWriter w, String s) { tick(); w.println(s); fw(s + "\n"); }
    public static void pwPrint(java.io.PrintWriter w, String s) { tick(); w.print(s); fw(s); }
    public static void wNewLine(java.io.BufferedWriter w) throws java.io.IOException { tick(); w.newLine(); fw("\n"); }

    public static String readLine(java.io.BufferedReader r) throws java.io.IOException {
        tick();
        String l = r.readLine();
        ev("{\"t\":\"fr\",\"f\":" + Runner.q(curR == null ? "" : curR) + ",\"k\":\"readLine\",\"s\":" + (l == null ? "null" : Runner.q(l)) + "}");
        return l;
    }

    public static void fclose(Object o) throws Exception {
        tick();
        ((AutoCloseable) o).close();
        if (o instanceof java.util.Scanner sc && !fileScanners.contains(sc)) return;
        if (openFiles.isEmpty()) return;
        String f = openFiles.remove(openFiles.size() - 1);
        ev("{\"t\":\"fclose\",\"f\":" + Runner.q(f) + "}");
    }

    /** nextLine / nextInt / next on a Scanner that reads a file (not the keyboard). */
    static Object fileRead(java.util.Scanner s, String k) {
        tick();
        Object v;
        try {
            v = switch (k) { case "nextLine" -> s.nextLine(); case "nextInt" -> (Object) s.nextInt(); default -> s.next(); };
        } catch (RuntimeException e) {
            ev("{\"t\":\"fr\",\"f\":" + Runner.q(curR == null ? "" : curR) + ",\"k\":" + Runner.q(k) + ",\"s\":null}");
            throw e;
        }
        ev("{\"t\":\"fr\",\"f\":" + Runner.q(curR == null ? "" : curR) + ",\"k\":" + Runner.q(k) + ",\"s\":" + Runner.q(String.valueOf(v)) + "}");
        return v;
    }

    static boolean filePeek(java.util.Scanner s, String k) {
        tick();
        boolean r = switch (k) { case "hasNextLine" -> s.hasNextLine(); case "hasNextInt" -> s.hasNextInt(); default -> s.hasNext(); };
        ev("{\"t\":\"fhas\",\"f\":" + Runner.q(curR == null ? "" : curR) + ",\"k\":" + Runner.q(k) + ",\"v\":" + r + "}");
        return r;
    }

    public static String[] split(String s, String re) {
        String[] p = s.split(re);
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < p.length; i++) { if (i > 0) b.append(','); b.append(Runner.q(p[i])); }
        ev("{\"t\":\"fsplit\",\"s\":" + Runner.q(s) + ",\"re\":" + Runner.q(re) + ",\"p\":" + b.append(']') + "}");
        return p;
    }

    public static String sub2(String s, int a, int b) {
        String r = s.substring(a, b);
        ev("{\"t\":\"fsub\",\"s\":" + Runner.q(s) + ",\"a\":" + a + ",\"b\":" + b + ",\"r\":" + Runner.q(r) + "}");
        return r;
    }

    public static String sub1(String s, int a) {
        String r = s.substring(a);
        ev("{\"t\":\"fsub\",\"s\":" + Runner.q(s) + ",\"a\":" + a + ",\"b\":" + s.length() + ",\"r\":" + Runner.q(r) + "}");
        return r;
    }

    /** First line of a catch block: the exception is on the stack. "from" = the line of the student's code where it was thrown. */
    public static void caught(Throwable e) {
        int from = -1;
        for (StackTraceElement el : e.getStackTrace()) {
            if (el.getClassName().equals("Main") || el.getClassName().startsWith("Main$")) {
                from = el.getLineNumber() - lineOffset;
                break;
            }
        }
        String msg = e.getMessage();
        ev("{\"t\":\"caught\",\"c\":" + Runner.q(e.getClass().getName()) + ",\"msg\":" + (msg == null ? "null" : Runner.q(msg)) + ",\"from\":" + from + "}");
    }
}
