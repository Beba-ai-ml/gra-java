# Rozdział 11. Składamy projekt (kolejność budowy)

**Czas:** widełki 3-5 h. Budujesz krok po kroku w jednym pliku `Main.java` z klasą `public class Main`.
To jest Twój plik z programem w Twoim projekcie IntelliJ `pierwszy` (`~/IdeaProjects/pierwszy`), np. w nowym pakiecie `projekt`.
Nie idziesz dalej, dopóki poprzedni krok nie przechodzi testu.

## Po co ci to w projekcie

To jest właściwy projekt. Rozdziały 1-10 to były klocki. Tutaj je składasz w jeden program
i sprawdzasz każdy krok ręcznie. Żadnego kroku nie przepisujesz z gotowca. Jeśli utkniesz,
wracasz do karty R1-R10, a nie do cudzego pliku.

## Zasady budowy

- Jeden krok = jedno uruchomienie = jeden test ręczny. Test ma podany wpis z klawiatury
  i ekran, który MUSI się pojawić.
- Wpisuj testowe dane BEZ polskich znaków (`Haslo`, nie `Hasło`), żeby nie walczyć z kodowaniem konsoli.
- Napisy w programie: jak w sekcji „Uwaga o napisach” na końcu karty.
- Nie kasuj działającego kroku, zanim następny nie przejdzie testu.

Gdzie sprawdzić wynik: uruchom `Main` w IntelliJ (zielona strzałka) i wpisuj w konsoli.

## Krok 1. Szkielet: menu i `switch`

**Cel:** program wyświetla menu, czyta numer i kończy się na `0`. Opcje `1-4` na razie nic nie robią.

**Co dodać:**
- `Scanner skan` w `main` i pętla `do-while` z metodą `public static void menu()` (z rozdziału 10),
- `switch (wybor)` z `case` 1, 2, 3, 4 (puste, z `break`), `case 0` i `default`.
- Opcje w menu: `[1] Dodaj nowa notatke`, `[2] Wyswietl wszystkie notatki`, `[3] Wyszukaj po tytule`, `[4] Posortuj notatki`, `[0] Wyjscie`.

**Test ręczny:**
- Wpisz: `9`, potem `0`.
- Ekran MUSI pokazać: `Nie ma takiej opcji.` i na końcu `Koniec programu.`.

**Kontrola:**
- [ ] Menu pojawia się ponownie po każdej opcji.
- [ ] Program kończy się dopiero na `0`.
- [ ] Po `nextInt()` jest lekarstwo `skan.nextLine();`.

## Krok 2. Dwie listy i dodawanie/wyświetlanie BEZ szyfru

**Cel:** notatka trafia do dwóch list i wraca na ekran z numerem.

**Co dodać:**
- W `main`: `ArrayList<String> tytuly` i `ArrayList<String> tresci` (dokładnie te nazwy, PDF ich wymaga).
- `case 1`: pytasz o tytuł i treść przez `nextLine()`, dodajesz do OBU list. Na razie treść bez szyfru.
- Metodę `public static void wyswietl(ArrayList<String> tytuly, ArrayList<String> tresci)`.
  W środku pętla po `i` od `0` do `size()-1`, która wypisuje `[i] Tytul: ... | Tresc: ...`.
  Nie używaj for-each, bo potrzebujesz numeru `i`.
- Lista pusta: metoda wypisuje `Brak notatek.` zamiast pustego ekranu.
- `case 2`: tylko wywołanie `wyswietl(tytuly, tresci);`.

**Test ręczny:**
- Wpisz: `1`, `Haslo WiFi`, `kot123`, potem `2`, potem `0`.
- Ekran MUSI pokazać (lista bez Twoich wpisów; w konsoli IntelliJ zobaczysz je dodatkowo):
  - `Dodano notatke.` (albo Twój napis potwierdzenia)
  - `[0] Tytul: Haslo WiFi | Tresc: kot123`
  - `Koniec programu.`

**Kontrola:**
- [ ] Dwie listy, każda z `add` w tym samym `case`.
- [ ] Wypisanie siedzi w metodzie `wyswietl` i idzie pętlą po numerze `i`, nie po wartości.
- [ ] Pusta lista nie wywala programu.

## Krok 3. Szyfr Cezara jako dwie metody (rozdział 4)

**Cel:** treść jest szyfrowana PRZED `add` i odszyfrowywana w locie przy wypisywaniu.

**Co dodać:**
- Wklej swoje metody `public static String szyfruj(String text, int szyfr)` i
  `public static String deszyfruj(String text, int szyfr)` z rozdziału 4
  (na `char[]`, z `% 26`, osobno wielkie i małe litery).
- Stała `int szyfr = 3;` w `main`, przekazywana do metod.
- `case 1`: `tresci.add(szyfruj(tresc, szyfr))`. Do `tytuly` idzie tytuł JAWNY.
- `wyswietl` dostaje trzeci parametr: `wyswietl(ArrayList<String> tytuly, ArrayList<String> tresci, int szyfr)`.
  W wypisaniu pokazujesz `deszyfruj(tresci.get(i), szyfr)`. W `case 2` wywołanie to teraz `wyswietl(tytuly, tresci, szyfr);`.

**Test ręczny (roundtrip):**
- Wpisz: `1`, `Ala ma kota xyz XYZ 123`, `Ala ma kota xyz XYZ 123`, potem `2`, potem `0`.
- Ekran MUSI pokazać wypisanie: `[0] Tytul: Ala ma kota xyz XYZ 123 | Tresc: Ala ma kota xyz XYZ 123`
  (treść po odszyfrowaniu jest taka sama jak wpisana).

**Kontrola (tymczasowa, potem usuń):**
- Dorzuć TYMCZASOWO w `case 1` zaraz po `add`: `System.out.println("RAW tresci: " + tresci);`
- Ta sama próba (wpis jak wyżej) MUSI pokazać zaszyfrowany tekst:
  `RAW tresci: [Dod pd nrwd abc ABC 123]`.
  Jeśli zobaczysz `Ala ma kota...` w tej linii, szyfr nie działa przed `add`. Napraw, zanim pójdziesz dalej.
- Usuń tę linię po kontroli.

**Kontrola:**
- [ ] `tresci` przechowuje tekst zaszyfrowany, a w wypisaniu jest odszyfrowany.
- [ ] `% 26` jest w OBU metodach, a deszyfrowanie ma `+ 26` przed `% 26`.
- [ ] Wielkie i małe litery liczone osobno, cyfry i spacje zostają bez zmian.

## Krok 4. Opcja [3]: wyszukiwanie liniowe (rozdział 8)

**Cel:** wpisujesz tytuł, dostajesz notatkę albo komunikat o braku.

**Co dodać:**
- Metodę `public static int szukajLiniowo(ArrayList<String> tytuly, String szukam)`. To Twoja metoda `szukaj`
  z rozdziału 8 (ćwiczenie 2) pod nową nazwą. Nazwy parametrów też zmień na `tytuly` i `szukam`. Zwraca numer albo `-1`.
- `case 3`: pytasz o szukany tytuł, wywołujesz metodę i wynik zapisujesz w `poz`.
  Zmienną na wpisany tytuł nazwij inaczej niż w `case 1` (np. `szukam`). Wszystkie `case` w jednym `switch`
  dzielą te same nawiasy, więc drugi `String tytul` da błąd `variable tytul is already defined`.
- Dla `-1` wypisz dokładnie `Nie znalazlem notatki o tytule ` i doklej szukany tytuł
  (np. `Nie znalazlem notatki o tytule Brak`).
- Dla numeru `poz` wypisz `[poz] Tytul: ... | Tresc: ...`, a treść bierz z DRUGIEJ listy po tym samym `poz`
  (i odszyfruj ją przez `deszyfruj`).

**Test ręczny:**
- Wpisz: `1`, `Haslo WiFi`, `kot123`, `3`, `Haslo WiFi`, `3`, `Brak`, `0`.
- Ekran MUSI pokazać:
  - po pierwszym `3`: `[0] Tytul: Haslo WiFi | Tresc: kot123`,
  - po drugim `3`: `Nie znalazlem notatki o tytule Brak`.

**Kontrola:**
- [ ] Porównanie tytułów przez `equals`, a nie `==`.
- [ ] Zwracasz `-1`, gdy nie ma dopasowania.
- [ ] Treść pobierasz z `tresci` po numerze z `tytuly`.

## Krok 5. Opcja [4]: sortowanie bąbelkowe na dwóch listach (rozdział 9)

**Cel:** sortujesz po tytułach, a treści jadą razem z nimi.

**Co dodać:**
- Metodę `public static void sortujBabelkowo(ArrayList<String> tytuly, ArrayList<String> tresci)`.
  To Twoja metoda `sortuj` z rozdziału 9 (ćwiczenie 4, wersja na dwóch listach) pod nową nazwą. Nazwy parametrów zmień na `tytuly` i `tresci`.
  Porównanie przez `compareTo`, zamiana w OBU listach.
- Porównanie `> 0` jest po to, żeby zamienić, gdy lewy tytuł jest „większy” (później w alfabecie).
- `case 4`: wywołaj metodę i wypisz `Posortowano notatki po tytule.`

**Test ręczny:**
- Wpisz: `1`, `Zeta`, `z1`, `1`, `Alfa`, `a1`, `1`, `Beta`, `b1`, `4`, `2`, `0`.
- Ekran MUSI pokazać `Posortowano notatki po tytule.`, a potem po `[2]`:
  ```
  [0] Tytul: Alfa | Tresc: a1
  [1] Tytul: Beta | Tresc: b1
  [2] Tytul: Zeta | Tresc: z1
  ```
  Każda treść stoi przy swoim tytule. Gdyby treści nie jechały razem z tytułami, zobaczyłbyś `Alfa | Tresc: z1`.

**Kontrola:**
- [ ] Zamiana tytułów i treści jest w tym samym `if`, jeden pod drugim.
- [ ] Wewnętrzna pętla idzie do `n - i` (albo podobnego skrócenia), nie do `n` w kółko.
- [ ] Po sortowaniu dla każdego tytułu `Tresc` nadal pasuje do swojej notatki.

## Krok 6. Przegląd wymagań i zakazanych metod

### 6a. Zakazane metody (grep)

Zakazane w 3.0 (z PDF): `Collections.sort()`, `Arrays.sort()`, `String.contains()`, `String.replace()`.

Jak to sprawdzi Claude: w katalogu Twojego projektu uruchamia poniższą komendę.
`SCIEZKA/Main.java` zamień na ścieżkę do Twojego pliku z programem (np. `src/projekt/Main.java`).

```
grep -nE 'Collections\.sort|Arrays\.sort|\.contains\(|\.replace' SCIEZKA/Main.java
```

- Wynik ma być PUSTY. Każdy trafiony wiersz trzeba wyjaśnić albo usunąć.
- `\.contains\(` trafi też w `ArrayList.contains`. PDF zakazuje `String.contains`,
  ale w tym projekcie zakazujemy obu, bo tak jest bezpieczniej.
- `\.replace` łapie też `replaceAll`. Komentarz z tymi słowami też trafi, więc go usuń.

### 6b. Wymagania z PDF 3.0 - mapa 1:1

Każda linia: wymaganie -> gdzie w Twoim kodzie ma być widoczne.

| Wymaganie z PDF (3.0) | Gdzie to widać w Twoim kodzie |
|---|---|
| Cały kod w jednej klasie `Main`, bez własnych klas i obiektów | Jeden plik, `public class Main`, brak `class` poza nim |
| Logika podzielona na statyczne metody | Co najmniej: `menu`, `szyfruj`, `deszyfruj`, `wyswietl`, `szukajLiniowo`, `sortujBabelkowo`, wszystkie `public static` |
| Kod nie jest w całości w `main` | `main` tylko steruje pętlą i wywołuje metody |
| Zakaz gotowców: `Collections.sort`, `Arrays.sort`, `contains`, `replace` | Grep z kroku 6a: pusty wynik |
| Struktura: `ArrayList<String> tytuly` i `ArrayList<String> tresci` w `main` | Pierwsze deklaracje w `main` z tymi nazwami |
| Element 0 w `tytuly` odpowiada elementowi 0 w `tresci` | Każda operacja (`add`, zamiana) rusza OBIE listy |
| Menu w pętli, opcje `[1]-[4]`, `[0]` wyjście | Pętla `do-while`, `switch` z opcjami i `case 0` |
| [1] Dodaj: pyta o tytuł i treść, treść szyfrowana przed `add` | `case 1`: `tresci.add(szyfruj(...))` |
| [2] Wyświetl: `[Indeks] Tytuł: ... \| Treść: ...`, treść odszyfrowana w locie (u nas napisy `Tytul`/`Tresc`, patrz 6c) | `case 2` woła `wyswietl`, w niej pętla po `i` i `deszyfruj` w wypisywaniu |
| [3] Wyszukaj po tytule, pętla przez listę tytułów, dokładne dopasowanie | `szukajLiniowo` z `equals` i `return -1` |
| [4] Posortuj: bąbelkowo, alfabetycznie po tytułach, treści zamieniane razem | `sortujBabelkowo` z `compareTo` i zamianą w obu listach |
| [0] Wyjście z programu | `case 0`, a `while (wybor != 0)` kończy pętlę |
| Szyfr Cezara: własna metoda, zamiana na `char[]`, operacje na ASCII | `toCharArray()`, `new String(...)`, `- 'A'` / `- 'a'`, `% 26` |
| Bąbelkowy O(N²) na `ArrayList`, `compareTo` dozwolone | Dwie pętle, `tytuly.get(j - 1).compareTo(...)` |

### 6c. Uwaga o napisach (format wypisania)

PDF pisze format z polskimi znakami: `[Indeks] Tytuł: ... | Treść: ...`.
W całym kursie piszemy napisy w kodzie bez polskich znaków (Twój styl): `Tytul:` i `Tresc:`.
Wszystkie testy w tej karcie oczekują właśnie tej wersji.
Jeśli prowadzący wymaga dokładnie „Tytuł” i „Treść”, zmień tylko napisy w `println`. Logika się nie zmienia.
Nie wiadomo jeszcze, którą wersję sprawdzi prowadzący. Zapytaj go przed oddaniem.

## Sprawdzian końcowy

Otwórz pusty plik i napisz cały program bez podglądania, w max 60 minut. Potem uruchom testy z kroków 1-5
po kolei. Każdy test ma przejść z ekranem z karty.

**Kontrola (Claude sprawdzi):**
- [ ] Cały program uruchamia się i przechodzi testy kroków 1-5.
- [ ] Grep z kroku 6a daje pusty wynik.
- [ ] Mapa z kroku 6b: każdy wiersz ma miejsce w kodzie.
- [ ] Brak zostawionych linii `RAW tresci` z kroku 3.

## Typowe błędy w składaniu

- Dodanie tytułu do `tytuly`, a treści do `tresci` w innym `case` albo w innej kolejności: listy się rozjeżdżają.
- Zamiana tylko `tytuly` w sortowaniu (ćwiczenie „zobacz, co się stanie” z rozdziału 6).
- Szyfrowanie już zaszyfrowanego tekstu (dwa razy `szyfruj`).
- Porównanie `==` w wyszukiwaniu zamiast `equals`.
- Zostawiona linia debugująca po kroku 3.
- Zapomniane lekarstwo `skan.nextLine()` po `nextInt()`.
