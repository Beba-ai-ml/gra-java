# Gra do nauki Javy

Gra w przeglądarce, która krok po kroku uczy Javy potrzebnej do projektu „Kryptograficzne Archiwum”. Ma 12 rozdziałów: od tablic, przez metody, znaki, szyfr Cezara, `ArrayList`, wyszukiwanie i sortowanie bąbelkowe, aż po menu ze `Scanner` i złożenie całego projektu.

Piszesz kod, gra go kompiluje i uruchamia, pokazuje krok po kroku, co dzieje się w tablicy, i mówi, który test nie przeszedł. Błędy kompilatora tłumaczy po polsku.

Wszystko działa u Ciebie na komputerze. Nic nie idzie do internetu, a Twój postęp zapisuje się w przeglądarce.

## Co musisz mieć

1. **Python 3.8 lub nowszy**: https://www.python.org/downloads/
   Na Windows przy instalacji zaznacz **„Add python.exe to PATH”**.
2. **JDK 21** (dokładnie 21, nowsze nie zadziałają): https://adoptium.net/temurin/releases/?version=21&package=jdk
   Na Windows przy instalacji zaznacz **„Set JAVA_HOME variable”** i **„Add to PATH”**.

## Jak uruchomić

1. Pobierz grę: zielony przycisk **Code**, potem **Download ZIP**, a na koniec rozpakuj. Możesz też użyć `git clone`. Najlepiej rozpakuj do folderu bez polskich liter w ścieżce (np. `C:\gra-java`).
2. Uruchom:
   - **Windows:** kliknij dwa razy `start.bat`.
   - **macOS / Linux:** w terminalu, w folderze gry, wpisz `./start.sh` (albo `python3 start.py`).
3. Przeglądarka otworzy się sama na `http://127.0.0.1:8731/`.

Żeby wyłączyć grę, zamknij okno z czarnym terminalem albo naciśnij w nim `Ctrl+C`.

## Gdy coś nie działa

- **„Nie znalazłem JDK”**: JDK nie jest zainstalowane albo nie ma go w PATH. Zainstaluj JDK 21 według punktu wyżej i otwórz nowe okno terminala.
- **„Gra działa na JDK 21, a znalazłem…”**: masz inną wersję Javy. Zainstaluj JDK 21. Jeśli chcesz zostawić też inną wersję, ustaw zmienną `GRA_JDK` na folder `bin` z JDK 21, np. `C:\Program Files\Eclipse Adoptium\jdk-21...\bin`.
- **macOS z Javą z Homebrew** (`brew install openjdk@21`): Homebrew nie dodaje jej do PATH. Uruchom tak: `GRA_JDK=$(brew --prefix openjdk@21)/bin ./start.sh`.
- **Windows: okno mówi o Microsoft Store**: masz tylko skrót do Pythona ze Sklepu. Zainstaluj Pythona z python.org.
- **„Port zajęty”**: gra już działa w innym oknie. Zamknij je albo uruchom na innym porcie: `GRA_PORT=8732 ./start.sh` (Windows: `set GRA_PORT=8732` i potem `start.bat`).
- **Brak Pythona na Windows**: zainstaluj go z python.org z zaznaczonym „Add python.exe to PATH”.
