#!/usr/bin/env python3
"""Gra R1 "Komorki i numery" - local server.

Takes the student's Java method body, compiles it with javac, runs it under
an instrumented harness (harness/Runner.java + Tracker.java) and returns the
trace of array accesses plus a verdict per test. Binds 127.0.0.1 only.
"""
import base64
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import threading
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST, PORT = "127.0.0.1", int(os.environ.get("GRA_PORT", "8731"))
ROOT = os.path.dirname(os.path.abspath(__file__))


def find_jdk():
    """javac + java from GRA_JDK (a bin folder), JAVA_HOME or PATH; (None, None) when there is no JDK."""
    dirs = [os.environ.get("GRA_JDK")]
    if os.environ.get("JAVA_HOME"):
        dirs.append(os.path.join(os.environ["JAVA_HOME"], "bin"))
    for d in dirs:
        if d and shutil.which("javac", path=d) and shutil.which("java", path=d):
            return shutil.which("javac", path=d), shutil.which("java", path=d)
    javac = shutil.which("javac")
    if javac:
        # javac on PATH is often a symlink (/usr/bin/javac -> .../jdk/bin/javac): take java from the same JDK
        d = os.path.dirname(os.path.realpath(javac))
        return javac, shutil.which("java", path=d) or shutil.which("java")
    return None, None


JAVAC, JAVA = find_jdk()
LIB = os.path.join(ROOT, "lib")
TMP = os.path.join(ROOT, ".tmp")
EXPORTS = [
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
]
RUN_TIMEOUT = 5.0
COMPILE_TIMEOUT = 20.0
OUT_CAP = 64 * 1024
CODE_CAP = 20000
LINE_OFFSET = 2  # Main.java line 1 = class, line 2 = method header
RUN_SLOTS = threading.Semaphore(2)

# chapter -> levels file, card with the theory, title; "class" mode = student writes whole methods + main
CHAPTERS = [
    {"id": 1, "title": "Komórki i numery", "file": "levels.json", "card": "R01-komorki-i-numery.md", "mode": "body"},
    {"id": 2, "title": "Własne metody statyczne", "file": "levels-r2.json", "card": "R02-metody-statyczne.md", "mode": "class"},
    {"id": 3, "title": "Znaki to liczby", "file": "levels-r3.json", "card": "R03-znaki-to-liczby.md", "mode": "class"},
    {"id": 4, "title": "Szyfr Cezara jako dwie metody", "file": "levels-r4.json", "card": "R04-szyfr-cezara.md", "mode": "class"},
    {"id": 5, "title": "ArrayList po numerach", "file": "levels-r5.json", "card": "R05-arraylist-po-numerach.md", "mode": "class"},
    {"id": 6, "title": "Listy równoległe", "file": "levels-r6.json", "card": "R06-listy-rownolegle.md", "mode": "class"},
    {"id": 7, "title": "Porównywanie tekstów", "file": "levels-r7.json", "card": "R07-porownywanie-tekstow.md", "mode": "class"},
    # "icmp": int == comparisons are animated (t[i] == x), see Runner.ICMP
    {"id": 8, "title": "Wyszukiwanie liniowe", "file": "levels-r8.json", "card": "R08-wyszukiwanie-liniowe.md", "mode": "class", "icmp": True},
    # "sort": the harness also reports int comparisons of array/list elements and compareTo signs (bubble sort)
    {"id": 9, "title": "Sortowanie bąbelkowe", "file": "levels-r9.json", "card": "R09-sortowanie-babelkowe.md", "mode": "class", "sort": True},
    {"id": 10, "title": "Menu w pętli i Scanner", "file": "levels-r10.json", "card": "R10-menu-i-scanner.md", "mode": "class", "menu": True},
    {"id": 11, "title": "Składamy projekt", "file": "levels-r11.json", "card": "R11-skladamy-projekt.md", "mode": "class", "menu": True},
    {"id": 12, "title": "Obrona przed prowadzącym", "file": "levels-r12.json", "card": "R12-obrona.md", "mode": "class"},
]
CH = {c["id"]: c for c in CHAPTERS}
LEVELS = {}
for c in CHAPTERS:
    with open(os.path.join(ROOT, c["file"]), encoding="utf-8") as f:
        for lv in json.load(f):
            LEVELS[(c["id"], lv["id"])] = lv

# Polish explanations for common javac messages. The original message is always kept.

HINTS = [
    # chapter 3 (chars) - more specific than the general patterns below, so they go first
    (r"possible lossy conversion from int to char", "Działanie na znakach (np. odejmowanie albo dodawanie) daje liczbę int, a nie char. Żeby wpisać wynik do char, zamień go na znak: (char) przed działaniem w nawiasie."),
    (r"unexpected type", "Po lewej stronie = musi stać zmienna albo szuflada, a nie wynik działania ani metody (np. text.charAt(0) albo suma(a, b))."),
    (r"String cannot be converted to char", "Jeden znak piszesz w apostrofach: 'A'. Cudzysłów \"A\" to napis (String), nawet z jedną literą."),
    (r"char\[\] cannot be converted to String", "Tablicy znaków nie oddasz jako napisu. Złóż ją z powrotem w napis: new String(tab)."),
    (r"array required, but (java\.lang\.)?String found", "String to nie tablica, nawias [ ] na nim nie działa. Literę czytasz przez text.charAt(i) albo najpierw robisz char[] znaki = text.toCharArray()."),
    (r"char cannot be dereferenced", "char to jeden znak (liczba) - nie ma kropki ani metod. Metody, np. length(), ma napis, a .length tablica."),
    (r"unclosed character literal|empty character literal", "W apostrofach mieści się dokładnie jeden znak, np. 'A'. Kilka liter to napis - pisz go w cudzysłowie."),
    # chapter 10 (menu: do-while, switch)
    (r"while expected", "Pętla do { } musi się kończyć linijką while (warunek); zaraz po klamrze }. Bez while Java nie wie, kiedy wrócić na górę."),
    (r"duplicate case label", "Dwa case mają tę samą liczbę. Każda liczba może stać tylko w jednym case."),
    (r"orphaned (case|default)", "case i default stoją tylko w środku switch (wybor) { ... }. Sprawdź klamry - może switch zamknął się za wcześnie."),
    (r"break outside switch or loop", "break działa tylko w switch albo w pętli. Ten stoi poza nimi - sprawdź klamry { }."),
    (r"';' expected", "Brakuje średnika ; na końcu instrukcji (często w linii wyżej)."),
    (r"cannot find symbol", "Java nie zna tej nazwy. Literówka? Zmienna niezadeklarowana? Tablica ma length bez nawiasów, napis ma length()."),
    (r"incompatible types", "Typy się nie zgadzają, np. wpisujesz tekst do int albo int[] do int."),
    (r"missing return statement", "Metoda ma oddać wynik (jest int albo double, nie void), a jest droga, na której nie dochodzi do return. Np. if bez else: gdy warunek nie wejdzie, metoda nie ma czego oddać. Dopisz return na końcu."),
    (r"might not have been initialized", "Zmienna nie dostała wartości przed użyciem. Nadaj jej wartość przy deklaracji."),
    (r"method .* is already defined", "Ta metoda jest w kodzie dwa razy (ta sama nazwa i te same parametry). Zostaw jedną."),
    (r"is already defined", "Ta zmienna już istnieje w tym miejscu. Nie deklaruj jej drugi raz (bez int przed nazwą)."),
    (r"not a statement", "To nie jest pełna instrukcja. Może brakuje = albo wywołania?"),
    (r"'\)' expected|'\(' expected|'\]' expected|'\[' expected", "Nawiasy się nie domykają. Policz ( ) i [ ]."),
    (r"reached end of file while parsing|class, interface, enum, or record expected|illegal start of type", "Nawiasy klamrowe { } się nie zgadzają. Policz otwierające i zamykające."),
    (r"'else' without 'if'", "else musi stać zaraz po klamrze } od if."),
    (r"unreachable statement", "Ta linia nigdy się nie wykona (np. stoi po return)."),
    (r"possible lossy conversion", "Liczba z przecinkiem (double) nie wejdzie sama do int. Tablica przechowuje int."),
    (r"bad operand types", "Ten operator nie działa dla tych typów, np. porównujesz int z int[]."),
    (r"array required, but", "Nawias [ ] stoi przy czymś, co nie jest tablicą."),
    (r"unexpected return value", "Ta metoda jest void - nie zwraca wyniku, więc return bez wartości albo wcale."),
    (r"'void' type not allowed here", "Ta metoda jest void - nic nie oddaje, więc nie da się jej wypisać ani zapisać do zmiennej. Wywołaj ją osobno, w swojej linijce."),
    (r"non-static method .* cannot be referenced from a static context", "W nagłówku metody brakuje static. main jest static, więc woła tylko metody static."),
    (r"cannot be applied to given types", "Wywołanie nie pasuje do nagłówka metody: inna liczba albo inne typy parametrów."),
    (r"invalid method declaration; return type required", "W nagłówku metody brakuje typu wyniku (int, double, void...) przed nazwą."),
    (r"(int|boolean) cannot be dereferenced", "Kropka (np. .equals, .compareTo) działa na tekście, nie na liczbie ani na true/false. Sprawdź, na czym ją wołasz."),
    (r"illegal start of expression", "Coś stoi w złym miejscu. Często brakuje klamry { } albo jest o jedną za dużo, albo metoda jest wpisana w środek innej metody (metody stoją obok siebie)."),
]

# Hints that need the lines under the javac message (operand types etc.); matched on message + those lines, checked before HINTS.
DETAIL_HINTS = [
    # chapter 12: trying to change a letter of a String in place
    (r"unexpected type[\s\S]*\.charAt\([^)]*\)\s*=[^=]",
     "Po lewej stronie = musi stać zmienna albo szuflada, a nie wynik metody. String jest niezmienny: charAt(i) tylko czyta literę, nie da się do niej nic wpisać. Zrób z napisu tablicę znaków (toCharArray()), zmieniaj jej szuflady, a na końcu złóż ją w napis przez new String(...)."),
    # chapter 5 (ArrayList)
    (r"cannot find symbol[\s\S]*symbol:\s+variable length[\s\S]*(ArrayList|List)<",
     "ArrayList nie ma length. Liczbę elementów listy daje metoda size() z nawiasami: lista.size()."),
    (r"array required, but (java\.util\.)?(ArrayList|List)<",
     "Nawiasy kwadratowe [ ] działają tylko na tablicach. Element listy bierzesz przez get: lista.get(i), a zmieniasz przez set: lista.set(i, x)."),
    (r"size has private access in (java\.util\.)?ArrayList",
     "size to metoda, więc potrzebuje nawiasów: lista.size()."),
    (r"unexpected type[\s\S]*required: reference[\s\S]*found:\s+(int|double|char|boolean|long)",
     "W nawiasach < > nie może stać typ prosty (int, double, char...). Piszesz wersję z wielkiej litery: Integer, Double, Character, Boolean - np. ArrayList<Integer>."),
    # chapter 8 (search): returning the found text instead of its number; the loop counter used after the loop
    # (@caret: = the code from the javac caret on; lista.get(imie) points INSIDE get( and keeps the chapter 5 hint)
    (r"incompatible types: (java\.lang\.)?String cannot be converted to int[\s\S]*\breturn\b[\s\S]*@caret: (?![\w\"]+\s*\))",
     "Metoda ma oddać liczbę (int), a return oddaje tekst (String). Przy szukaniu oddajesz NUMER miejsca, np. return i, a nie to, co w nim leży."),
    (r"cannot find symbol[\s\S]*symbol:\s+variable (i|j|k)\b",
     "Java nie zna tej zmiennej w tym miejscu. Brakuje int przy pierwszym użyciu (for (int i = 0; ...)) albo używasz licznika za klamrą pętli - licznik z for żyje tylko w środku pętli."),
    (r"incompatible types: (java\.lang\.)?String cannot be converted to int[\s\S]*\.(get|set|remove)\(",
     "W get(...), set(...) i remove(...) pierwsza rzecz to NUMER pudełka (int), a nie tekst. Np. lista.get(0)."),
    # chapter 6
    (r"incompatible types: int cannot be converted to (java\.lang\.)?String[\s\S]*\.(add|set)\(",
     "Ta lista trzyma tekst (String). Liczbę do takiej listy wpisz w cudzysłowie: \"5\", nie 5."),
    (r"incompatible types: (java\.lang\.)?Object cannot be converted to",
     "Lista bez typu w < > oddaje Object. Napisz typ elementów przy deklaracji i w parametrze: ArrayList<String>."),
    (r"cannot find symbol[\s\S]*symbol:\s+method (get|set|add|size|remove)\([\s\S]*location: .*(int|String)\[\]",
     "To jest tablica, nie lista. Tablica ma tab[i] i tab.length, lista ma get(i) i size()."),
    (r"first type:\s+char\s+second type:\s+String|first type:\s+String\s+second type:\s+char",
     "Tu spotyka się znak (char) z napisem (String). Jeden znak piszesz w apostrofach: 'A', a nie w cudzysłowie \"A\"."),
    (r"bad operand types for binary operator '(==|!=)'[\s\S]*first type:\s+(int|boolean)\s+second type:\s+String|bad operand types for binary operator '(==|!=)'[\s\S]*first type:\s+String\s+second type:\s+(int|boolean)", "Porównujesz tekst (String) z liczbą albo z true/false. compareTo oddaje liczbę - porównaj ją z 0, np. a.compareTo(b) < 0."),
    (r"int cannot be converted to boolean[\s\S]*while\s*\(", "W warunku pętli stoi = (wpisanie liczby do zmiennej), a nie porównanie. Pętla trwa, dopóki wybór NIE jest zerem: while (wybor != 0);"),
    (r"int cannot be converted to boolean", "if potrzebuje true albo false, a tu stoi liczba (int). Porównaj ją z czymś, np. if (x > 0) albo if (a.compareTo(b) < 0)."),
    (r"bad operand types for binary operator '(<|>|<=|>=)'[\s\S]*first type:\s+String", "Tekstów nie porównasz przez < albo >. Użyj a.compareTo(b) i sprawdź znak wyniku: a.compareTo(b) < 0."),
    # chapter 9: the second swap (oceny) declares the cup again
    (r"variable (tmp\w*) is already defined", "Kubek o tej nazwie już jest zadeklarowany wyżej w tej samej klamrze. Drugi kubek nazwij inaczej, np. tmp2, albo użyj starego bez typu przed nazwą (tmp = ...)."),
]

ERR_RE = re.compile(r"^Main\.java:(\d+): (error|warning): (.*)$")


def explain(msg):
    for pat, txt in HINTS:
        if re.search(pat, msg):
            return txt
    return ""


def run_proc(cmd, timeout, cwd):
    """Run in its own process group; on timeout kill that group by its PID."""
    p = subprocess.Popen(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                         stdin=subprocess.DEVNULL, start_new_session=True)
    try:
        out, _ = p.communicate(timeout=timeout)
        timed_out = False
    except subprocess.TimeoutExpired:
        try:
            if os.name == "nt":
                p.kill()  # Windows has no process groups; javac/java do not start children
            else:
                os.killpg(p.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        out, _ = p.communicate()
        timed_out = True
    return p.returncode, out[:OUT_CAP].decode("utf-8", "replace"), timed_out


def parse_compile_errors(text, user_lines, offset=LINE_OFFSET):
    errors = []
    lines = text.splitlines()
    i = 0
    while i < len(lines):
        m = ERR_RE.match(lines[i])
        if m and m.group(2) == "error":
            jl = int(m.group(1))
            ul = jl - offset
            where = ul if 1 <= ul <= user_lines else None
            note = ""
            if ul > user_lines:
                note = "Błąd wskazuje za Twoim kodem (na klamrę zamykającą)."
            elif ul < 1:
                note = "Błąd wskazuje nad Twój kod (na nagłówek)."
            snippet = []
            j = i + 1
            while j < len(lines) and not ERR_RE.match(lines[j]) and not re.match(r"^\d+ errors?$", lines[j]):
                snippet.append(lines[j])
                j += 1
            full = m.group(3) + "\n" + "\n".join(snippet) + caret_text(snippet)
            pl = next((txt for pat, txt in DETAIL_HINTS if re.search(pat, full)), "") or explain(m.group(3))
            errors.append({"line": where, "javaLine": jl, "msg": m.group(3),
                           "pl": pl, "note": note,
                           "detail": "\n".join(snippet[:6])})
            i = j
        else:
            i += 1
    return errors


def caret_text(snippet):
    """'\n@caret: <code from the ^ on>' when javac drew a caret under the code line, else ''."""
    for k in range(1, len(snippet)):
        if re.fullmatch(r" *\^", snippet[k]):
            return "\n@caret: " + snippet[k - 1][len(snippet[k]) - 1:]
    return ""


def norm_out(s):
    # "[0] 4" and "0 4" count as the same line - the brackets are only decoration
    # spaces around "=" and ":" and doubled spaces do not matter either ("w= 10" == "w = 10")
    rows = [re.sub(r"^\[(-?\d+)\]", r"\1", r.strip()) for r in s.replace("\r\n", "\n").split("\n")]
    rows = [re.sub(r" +", " ", re.sub(r" *([=:]) *", r" \1 ", r)).strip() for r in rows]
    while rows and rows[-1] == "":
        rows.pop()
    return rows


# Polish explanation added after the name of a runtime exception (chapter 10+ only: Scanner menus)
EXC_PL = {
    "java.util.InputMismatchException": " - nextInt() czekał na liczbę, a w buforze był tekst.",
    "java.util.NoSuchElementException": " - program chciał czytać z klawiatury, a gra wpisała już wszystko. "
                                        "Czyta za dużo razy albo pętla nie kończy się na 0?",
}

ALMOST = (" Różnią się tylko ostatnie cyfry - wynik jest bardzo blisko. "
          "Sprawdź wzór w return (np. czy oddajesz to, co każe zadanie).")


def almost(w, g):
    # two numbers that differ only in the last digits (e.g. 4.0 vs 4.000000000000051)
    try:
        a, b = float(w), float(g)
    except (TypeError, ValueError):
        return False
    return a != b and abs(a - b) <= 1e-6 * max(1.0, abs(a))


def judge(level, results, ch=1):
    verdicts = []
    for k, (test, res) in enumerate(zip(level["tests"], results)):
        ok, why = True, []
        if "exc" in test:
            if res["exc"] != test["exc"]:
                ok = False
                why.append(test.get("exc_msg", "Na tym poziomie nic nie naprawiasz - kod ma się wywrócić tak jak w aisd_lab03. "
                           "Kliknij „Wróć do kodu startowego”."))
        elif res["exc"]:
            ok = False
            line = res["excLine"]
            where = f" (linia {line})" if line and line > 0 else ""
            if res["exc"] == "NoSuchMethod":
                why.append(res["excMsg"])
            elif res["exc"] == "OutputCap":
                why.append("Program wypisał za dużo tekstu - pewnie pętla bez końca z println.")
            elif res["exc"] == "TooManySteps":
                why.append("Program wykonał za dużo kroków - pewnie pętla bez końca.")
            else:
                # chapters 10-11 explain Scanner exceptions in their own words, chapter 12 keeps its nextInt note
                if ch in (10, 11):
                    msg = f": {res['excMsg']}" if res["excMsg"] is not None else ""
                    why.append(f"Program się wywrócił{where}: {res['exc']}{msg}" + EXC_PL.get(res["exc"], ""))
                else:
                    msg = f": {res['excMsg']}" if res.get("excMsg") else ""
                    extra = (" nextInt() dostał tekst zamiast liczby - zwykle przez Enter zostawiony po poprzedniej liczbie albo przesunięte wpisy."
                             if res["exc"].endswith("InputMismatchException") else "")
                    why.append(f"Program się wywrócił{where}: {res['exc']}{msg}.{extra}")
        unused = [u for u in test.get("uses", []) if u not in res.get("used", [])]
        if unused and not res["exc"]:
            ok = False
            why.append("Na tym poziomie porównaj teksty przez " + " i ".join(unused)
                       + " (gra nie widzi " + " ani ".join(unused) + " w Twoim kodzie).")
        missing = [m for m in test.get("calls", []) if m not in res.get("called", [])]
        if missing and (not res["exc"] or res["exc"] == test.get("exc")):
            ok = False
            why.append("main wypisuje dobry tekst, ale nie woła metody " + ", ".join(missing)
                       + ". Wynik ma przyjść z Twojej metody, nie z gotowego napisu.")
        # a level that must crash (chapter 10) still checks the screen printed before the crash
        if "out" in test and (not res["exc"] or res["exc"] == test.get("exc")):
            want, got = norm_out(test["out"]), norm_out(res["out"])
            if want != got:
                ok = False
                diff = next((n for n in range(max(len(want), len(got)))
                             if n >= len(want) or n >= len(got) or want[n] != got[n]), 0)
                raw_w = [r.rstrip() for r in test["out"].split("\n")]
                raw_g = [r.rstrip() for r in res["out"].replace("\r\n", "\n").split("\n")]
                w = raw_w[diff] if diff < len(want) else "(nic więcej)"
                g = raw_g[diff] if diff < len(got) else "(nic więcej)"
                why.append(f"Ekran różni się w wierszu {diff + 1}: oczekiwano «{w}», jest «{g}»."
                           + (ALMOST if almost(w, g) else ""))
        if "list_ops" in test and not res["exc"]:
            unused = [u for u in test["list_ops"] if u not in res.get("ops", [])]
            if unused:
                ok = False
                why.append("Twój kod ani razu nie wywołał na liście: " + ", ".join(u + "()" for u in unused)
                           + ". Na tym poziomie ćwiczysz właśnie te metody ArrayList.")
        if "list_end" in test and not res["exc"]:
            if res.get("listEnd") != test["list_end"]:
                ok = False
                why.append(f"Lista na końcu: oczekiwano {show_list(test['list_end'])}, jest {show_list(res.get('listEnd') or [])}.")
        if "lists_end" in test and not res["exc"]:
            names = test.get("list_names", [])
            got_all = res.get("lists") or []
            for k, want_l in enumerate(test["lists_end"]):
                got_l = got_all[k] if k < len(got_all) else []
                if got_l != want_l:
                    ok = False
                    nm = names[k] if k < len(names) else f"nr {k + 1}"
                    why.append(f"Lista {nm} po wywołaniu: oczekiwano {show_list(want_l)}, jest {show_list(got_l)}.")
        if "named_lists_end" in test and not res["exc"]:
            # chapter 11: final contents of the lists the student's own main made, found by variable name
            got_named = {t["name"]: t["vals"] for t in (res.get("tracked") or [])}
            for nm, want_l in test["named_lists_end"].items():
                if nm not in got_named:
                    ok = False
                    why.append(f"Gra nie widzi w main listy o nazwie {nm}. Nazwij ją dokładnie {nm} (ArrayList<String> {nm}).")
                elif got_named[nm] != want_l:
                    ok = False
                    why.append(f"Lista {nm} na końcu programu: oczekiwano {show_list(want_l)}, jest {show_list(got_named[nm])}.")
        if "arr_end" in test and not res["exc"]:
            if res["end"] != test["arr_end"]:
                ok = False
                why.append(f"Szuflady na końcu: oczekiwano {test['arr_end']}, jest {res['end']}.")
        if "ret" in test and not res["exc"]:
            got = res["ret"]
            want_ret = test["ret"]
            try:
                same = got is not None and (str(got) == want_ret if isinstance(want_ret, str) else int(got) == want_ret)
            except ValueError:
                same = False
            if not same:
                ok = False
                if got is not None and not isinstance(want_ret, str) and re.fullmatch(r"-?\d+\.\d+", str(got)):
                    why.append(f"return: oczekiwano liczby całkowitej {want_ret}, jest {got} - metoda ma zwracać int, a zwraca double.")
                else:
                    why.append(f"return: oczekiwano {test['ret']}, jest {got}." + (ALMOST if almost(want_ret, got) else ""))
        verdicts.append({"ok": ok, "why": why, "arr": test.get("arr", []), "visible": k == 0, "label": test_label(test)})
    return verdicts


def show_list(vals):
    return "[" + ", ".join('"' + v + '"' if isinstance(v, str) else str(v) for v in vals) + "]"
def code_checks(level, code):
    """Optional checks on the student's code text (e.g. the Sprawdzian checklist): one verdict each."""
    clean = re.sub(r"//.*", "", re.sub(r"/\*.*?\*/", "", code, flags=re.S))
    clean = re.sub(r'"(?:[^"\\\n]|\\.)*"', '""', clean)
    out = []
    for c in level.get("code_checks", []):
        # "raw": the check sees comments and strings too (like grep on the file)
        found = re.search(c["re"], code if c.get("raw") else clean) is not None
        ok = found == c.get("want", True)
        out.append({"ok": ok, "why": [] if ok else [c["msg"]], "arr": [], "visible": False, "label": c["label"]})
    return out


def test_label(test):
    if "call" not in test and not test.get("main"):
        return None
    if test.get("main"):
        if "in" in test:
            return "Twój program (main), w konsoli wpisano: " + ", ".join(test["in"].rstrip("\n").split("\n"))
        return "Twój program (main)"
    shown = []
    for t, a in zip(test["types"], test["args"]):
        if t == "int[]":
            shown.append("{" + ", ".join(str(x) for x in a) + "}")
        elif t == "char":
            shown.append("'" + a + "'")
        elif t == "char[]":
            shown.append('"' + a + '".toCharArray()')
        elif t == "String":
            shown.append('"' + a + '"')
        elif t.startswith("ArrayList<"):
            shown.append(show_list(a))
        else:
            shown.append(str(a))
    return f"{test['call']}({', '.join(shown)})"


def spec(test):
    if test.get("main"):
        if "in" in test:
            return "main|in|" + base64.b64encode(test["in"].encode("utf-8")).decode("ascii")
        return "main"
    vals = []
    for t, a in zip(test["types"], test["args"]):
        if t.startswith("ArrayList<"):
            vals.append(",".join(urllib.parse.quote(str(x), safe="") for x in a))
        else:
            vals.append(",".join(str(x) for x in a) if t == "int[]" else str(a))
    return f"call|{test['call']}|{','.join(test['types'])}|{';'.join(vals)}"


def run_code(level, code, mode="body", icmp=False, sort=False, ch=1):
    user_lines = code.count("\n") + 1
    if mode == "class":
        m = re.search(r"^\s*(package\s|import\s|(public\s+)?class\s)", code, re.M)
        if m:
            line = code[:m.start(1)].count("\n") + 1
            head = m.group(1).split()[0]
            return {"stage": "compile", "ok": False, "raw": "", "errors": [{
                "line": line, "javaLine": line + 1, "msg": f"{head} ...", "note": "", "detail": "",
                "pl": "Tu nie wklejaj całego pliku z IntelliJ. Gra sama dopisuje public class Main { } i import java.util.* (Random, Scanner, ArrayList). "
                      "Wklej tylko metody (public static ...) i main, bez package, import i class."}]}
        offset = 1
        src = "import java.util.*; public class Main {\n" + code + "\n}\n"
    else:
        offset = LINE_OFFSET
        # import shares line 1 with the class header, so LINE_OFFSET stays 2
        src = ("import java.util.*; public class Main {\n"
               f"static {level['ret']} zadanie(int tab[]) {{\n"
               f"{code}\n"
               "}\n"
               "}\n")
    os.makedirs(TMP, exist_ok=True)
    work = tempfile.mkdtemp(prefix="run-", dir=TMP)
    try:
        with open(os.path.join(work, "Main.java"), "w", encoding="utf-8") as f:
            f.write(src)
        rc, out, to = run_proc([JAVAC, "-J-Duser.language=en", "-J-Dstdout.encoding=UTF-8", "-J-Dstderr.encoding=UTF-8", "-J-Xmx256m", "-encoding", "UTF-8",
                                "-g", "-Xmaxerrs", "8", "-nowarn", "-d", work, "Main.java"],
                               COMPILE_TIMEOUT, work)
        if to:
            return {"stage": "compile", "ok": False, "errors": [], "raw": "Kompilacja trwała za długo."}
        if rc != 0:
            return {"stage": "compile", "ok": False,
                    "errors": parse_compile_errors(out, user_lines, offset), "raw": out}
        if mode == "class":
            tests = ["--class", *[spec(t) for t in level["tests"]]]
        else:
            tests = [",".join(str(x) for x in t["arr"]) or "-" for t in level["tests"]]
        cmd = [JAVA, "-Xmx128m", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC",
               "-Duser.language=en", *(["-Dgra.icmp=true"] if icmp else []), *(["-Dgra.sort=true"] if sort else []), *(["-Dgra.menu=true"] if CH.get(ch, {}).get("menu") else []), *EXPORTS, "-cp", LIB, "Runner", work, str(offset), *tests]
        rc, out, to = run_proc(cmd, RUN_TIMEOUT, work)
        res_path = os.path.join(work, "wynik.json")
        if to:
            return {"stage": "run", "ok": False, "timeout": True,
                    "message": f"Program działał dłużej niż {RUN_TIMEOUT:.0f} s i został zatrzymany. "
                               "Pewnie pętla bez końca (warunek nigdy nie robi się fałszywy?).",
                    "raw": out}
        if not os.path.exists(res_path):
            return {"stage": "run", "ok": False, "timeout": False,
                    "message": "Program skończył się bez wyniku. Jeśli masz System.exit, usuń je - gra musi dojść do końca programu.", "raw": out}
        with open(res_path, encoding="utf-8") as f:
            data = json.load(f)
        verdicts = judge(level, data["tests"], ch) + code_checks(level, code)
        return {"stage": "run", "ok": all(v["ok"] for v in verdicts), "verdicts": verdicts,
                "tests": data["tests"], "events": data["events"], "raw": out}
    finally:
        shutil.rmtree(work, ignore_errors=True)


def material(ch):
    """Chapter card without the solutions section (read fresh, the card may change)."""
    with open(os.path.join(ROOT, "karty", CH[ch]["card"]), encoding="utf-8") as f:
        text = f.read()
    return text.split("\n## Rozwiązania", 1)[0]


class Handler(BaseHTTPRequestHandler):
    server_version = "GraR1"

    def log_message(self, fmt, *args):
        sys.stderr.write("[gra-r1] " + (fmt % args) + "\n")

    def send(self, code, body, ctype):
        data = body if isinstance(body, bytes) else body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        path, _, query = self.path.partition("?")
        params = dict(kv.split("=", 1) for kv in query.split("&") if "=" in kv)
        try:
            ch = int(params.get("ch", "1"))
        except ValueError:
            ch = 1
        if ch not in CH:
            ch = 1
        if path in ("/", "/index.html"):
            with open(os.path.join(ROOT, "web", "index.html"), "rb") as f:
                return self.send(200, f.read(), "text/html; charset=utf-8")
        if path == "/levels.json":
            pub = [{"id": c["id"], "title": c["title"], "mode": c["mode"],
                    "levels": [lv for (cid, _), lv in LEVELS.items() if cid == c["id"]]} for c in CHAPTERS]
            return self.send(200, json.dumps(pub, ensure_ascii=False), "application/json; charset=utf-8")
        if path == "/material.md":
            return self.send(200, material(ch), "text/markdown; charset=utf-8")
        if path == "/favicon.ico":
            return self.send(204, b"", "image/x-icon")
        self.send(404, "nie ma", "text/plain; charset=utf-8")

    def do_POST(self):
        if self.path != "/run":
            return self.send(404, "nie ma", "text/plain; charset=utf-8")
        try:
            n = int(self.headers.get("Content-Length", "0"))
            if n > CODE_CAP * 2:
                return self.send(413, json.dumps({"error": "Za długi kod."}), "application/json")
            body = json.loads(self.rfile.read(n).decode("utf-8"))
            ch = int(body.get("chapter", 1))
            level = LEVELS[(ch, int(body["level"]))]
            code = str(body["code"])
            if len(code) > CODE_CAP:
                return self.send(413, json.dumps({"error": "Za długi kod."}), "application/json")
        except Exception as e:  # bad request shape
            return self.send(400, json.dumps({"error": f"Złe zapytanie: {e}"}), "application/json")
        # a page loaded before the level list changed would judge the code against the wrong task
        if body.get("title", level["title"] if ch == 1 else None) != level["title"]:
            return self.send(200, json.dumps({"stage": "stale", "ok": False, "message":
                "Gra się zaktualizowała i ta strona ma starą listę poziomów. Odśwież ją (Ctrl+F5) - "
                "Twój kod i postęp zostaną."}, ensure_ascii=False), "application/json; charset=utf-8")
        with RUN_SLOTS:
            result = run_code(level, code, CH[ch]["mode"], CH[ch].get("icmp", False), CH[ch].get("sort", False), ch)
        self.send(200, json.dumps(result, ensure_ascii=False), "application/json; charset=utf-8")


def build_lib():
    srcs = [os.path.join(ROOT, "harness", n) for n in ("Tracker.java", "Runner.java")]
    stamp = os.path.join(LIB, "Runner.class")
    if os.path.exists(stamp) and all(os.path.getmtime(stamp) >= os.path.getmtime(s) for s in srcs):
        return
    os.makedirs(LIB, exist_ok=True)
    r = subprocess.run([JAVAC, *EXPORTS, "-d", LIB, *srcs], capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit("Nie udało się zbudować harnessu:\n" + r.stderr)


def main():
    build_lib()
    try:
        httpd = ThreadingHTTPServer((HOST, PORT), Handler)
    except OSError as e:
        sys.exit(f"BŁĄD: nie mogę zająć {HOST}:{PORT} ({e.strerror}). Port zajęty? Zamknij stare okno z grą "
                 "albo uruchom na innym porcie: GRA_PORT=8732")
    print(f"Gra działa: http://{HOST}:{PORT}/", flush=True)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
