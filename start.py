#!/usr/bin/env python3
"""Starts the game: checks Python and the JDK, builds the harness, opens the browser.

Works the same on Windows, macOS and Linux. Stop with Ctrl+C or by closing the window.
"""
import os
import re
import subprocess
import sys
import threading
import webbrowser

NEED_JAVA = 21
JDK_URL = "https://adoptium.net/temurin/releases/?version=21&package=jdk"


def fail(msg):
    print("\nBŁĄD: " + msg + "\n", file=sys.stderr)
    sys.exit(1)  # start.bat pauses after an error, so the window stays open on Windows


def main():
    if sys.version_info < (3, 8):
        fail(f"Potrzebny Python 3.8 lub nowszy, masz {sys.version.split()[0]}.")
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    import server

    if not server.JAVAC or not server.JAVA:
        fail("Nie znalazłem JDK (javac). Zainstaluj JDK 21 (Temurin):\n  " + JDK_URL +
             "\nPrzy instalacji na Windows zaznacz „Set JAVA_HOME” i „Add to PATH”, potem otwórz nowe okno.")
    try:
        out = subprocess.run([server.JAVAC, "-version"], capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.TimeoutExpired) as e:
        fail(f"Nie mogę uruchomić {server.JAVAC}: {e}")
    m = re.search(r"javac (\d+)", out.stdout + out.stderr)
    if not m or int(m.group(1)) != NEED_JAVA:
        found = m.group(0) if m else (out.stdout + out.stderr).strip()
        fail(f"Gra działa na JDK {NEED_JAVA}, a znalazłem: {found} ({server.JAVAC}).\n"
             f"Zainstaluj JDK {NEED_JAVA}: {JDK_URL}\n"
             "Jeśli masz kilka JDK, wskaż właściwe: ustaw zmienną GRA_JDK na jego folder bin.")

    url = f"http://{server.HOST}:{server.PORT}/"
    print("Za chwilę otworzy się przeglądarka. Gdyby nie, wejdź na: " + url)
    print("Żeby wyłączyć grę, zamknij to okno albo naciśnij Ctrl+C.\n")
    threading.Timer(1.5, webbrowser.open, args=(url,)).start()
    try:
        server.main()
    except KeyboardInterrupt:
        print("\nGra wyłączona.")


if __name__ == "__main__":
    main()
