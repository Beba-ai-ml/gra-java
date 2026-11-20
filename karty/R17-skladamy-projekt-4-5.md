# Rozdział 17. Składamy projekt na 4.0 i 5.0 + obrona

**Czas:** widełki 4-6 h. Bierzesz gotowy projekt na 3.0 (rozdział 11) i dokładasz do niego po jednym kawałku z rozdziałów 13-16. Obrona na końcu: 1-2 h.
Pracujesz w tym samym pliku `Main.java` w IntelliJ (`~/IdeaProjects/pierwszy`), w tym samym pakiecie co rozdział 11.
Nie idziesz dalej, dopóki poprzedni krok nie przechodzi testu.

## Po co ci to w projekcie

Na 3.0 masz menu, dwie listy, szyfr Cezara, szukanie liniowe i sortowanie bąbelkowe. Na 4.0 dokładasz szukanie binarne ze strażnikiem, szukanie słowa w treściach notatek i szyfr Vigenère'a. Na 5.0 dokładasz plik `archiwum.txt` (zapis i odczyt) i obsługę wyjątków. Tu składasz wszystko w jeden program i uczysz się to obronić.

## Zasady budowy

- Jeden krok = jedno uruchomienie = jeden test ręczny z kartki. Test ma podane wpisy z klawiatury i ekran, który MUSI się pojawić.
- Wpisuj dane testowe BEZ polskich znaków (`Haslo`, nie `Hasło`).
- Napisy w programie: dokładnie jak w tej karcie (bez polskich znaków).
- Zakazane w całym projekcie: `Collections.sort`, `Arrays.sort`, `contains`, `replace`, `indexOf` (i `binarySearch`). Szukasz i sortujesz własnymi pętlami.
- Nie kasuj działającego kroku, zanim następny nie przejdzie testu.

Menu rośnie razem z programem:

```
[1] Dodaj nowa notatke
[2] Wyswietl wszystkie notatki
[3] Wyszukaj po tytule
[4] Posortuj notatki
[5] Wyszukaj binarnie po tytule
[6] Szukaj slowa w tresciach notatek
[7] Zapisz notatki do pliku
[0] Wyjscie
```

## Krok 1. Opcja [5]: szukanie binarne po tytule i strażnik posortowania

**Cel:** szukasz tytułu binarnie, a program sam dba o to, żeby dane były posortowane.

**Co dodać:**
- Metodę `public static int szukajBinarnie(ArrayList<String> tytuly, String szukam)`: `p = 0`, `k = tytuly.size() - 1`, pętla `while (p <= k)`, `mid = (p + k) / 2`, porównanie przez `compareTo`. Zwraca numer albo `-1`.
- W `main` flagę `boolean posortowane = false;`. Po dodaniu notatki (`case 1`) ustaw `false`, po sortowaniu (`case 4`) ustaw `true`.
- `case 5`: pytasz o tytuł. Jeśli flaga jest `false`, NAJPIERW wołasz `sortujBabelkowo(tytuly, tresci)` (obie listy) i ustawiasz flagę na `true`. Dopiero potem `szukajBinarnie`.
- Wynik jak w `case 3`: `[poz] Tytul: ... | Tresc: ...` (treść odszyfrowana) albo `Nie znalazlem notatki o tytule X`.

**Test ręczny:**
- Wpisz: `1`, `Zeta`, `z1`, `1`, `Alfa`, `a1`, `1`, `Beta`, `b1`, `1`, `Cyk`, `c1`, `5`, `Zeta`, `2`, `0`.
- Ekran MUSI pokazać po `5`: `[3] Tytul: Zeta | Tresc: z1`, a po `2` listę w kolejności Alfa, Beta, Cyk, Zeta.
- Bez strażnika szukanie w liście Zeta, Alfa, Beta, Cyk dałoby `Nie znalazlem notatki o tytule Zeta`, choć Zeta jest.

**Kontrola:**
- [ ] `szukajBinarnie` nie ma `Collections.binarySearch` ani `Arrays.binarySearch`.
- [ ] Dodanie notatki gasi flagę.
- [ ] Sortowanie rusza OBIE listy.

## Krok 2. Opcja [6]: szukanie słowa w treściach notatek (własne pętle)

**Cel:** wpisujesz słowo i dostajesz notatki, w których treści to słowo stoi.

**Co dodać:**
- Metodę `public static int szukajWzorca(String text, String wzor)`: pętla `i` po początkach okna od `0` do `n - m` włącznie i pętla `j` po literach wzoru, `break` przy pierwszej różnicy, `return i` przy pełnym trafieniu, `return -1` po pętli. `contains`, `indexOf`, `matches`, `lastIndexOf`, `regionMatches` i `startsWith` są zakazane.
- `case 6`: pytasz `Podaj szukane slowo:`. W pętli po wszystkich notatkach odszyfrowujesz treść w locie (`deszyfruj(tresci.get(i), szyfr)`), szukasz w niej słowa i wypisujesz trafione notatki jako `[i] Tytul: ... | Tresc: ...` (treść jawna). Jeśli żadna nie pasuje: `Nie znalazlem slowa X w zadnej notatce.`
- Lista `tresci` nadal trzyma TYLKO szyfry. Jawny tekst żyje chwilę w zmiennej.

**Test ręczny:**
- Dodaj notatki: `Zakupy` / `mleko chleb ser`, `Dom` / `klucz do domu`, `Plan` / `kupic mleko`, `Ok` / `ok`.
- `6`, `mleko` MUSI pokazać `[0] Zakupy` i `[2] Plan`. `6`, `ser` pokaże `[0]` (trafienie na samym końcu treści). `6`, `Mleko` oraz `6`, `xyz` pokażą `Nie znalazlem slowa ... w zadnej notatce.`

**Kontrola:**
- [ ] Wielkość liter ma znaczenie (`Mleko` to nie `mleko`).
- [ ] Notatka krótsza od słowa (`ok`) nie wywala programu.
- [ ] Brak `contains` i `indexOf`.

## Krok 3. Szyfr Vigenère'a z kluczem od użytkownika

**Cel:** treść jest szyfrowana kluczem tekstowym wpisanym na starcie programu.

**Co dodać:**
- Swoje metody z rozdziału 15: `public static String szyfrujV(String text, String klucz)` i `deszyfrujV(String text, String klucz)`: osobny licznik `j`, który rośnie tylko po literach, numer litery klucza `j % klucz.length()`, małe litery klucza, wielkie i małe litery tekstu osobno.
- Usuń `szyfruj`, `deszyfruj` i stałą `szyfr`.
- Na początku `main` (po `Scanner`) wypisz `Podaj klucz:` i wczytaj `String klucz = skan.nextLine();`.
- Wszędzie przekazuj `klucz` zamiast `szyfr` (`case 1`, `wyswietl`, `case 3`, `case 5`, `case 6`). `wyswietl` dostaje trzeci parametr `String klucz`.

**Test ręczny (roundtrip):**
- Wpisz: `kot`, `1`, `Ala ma kota xyz XYZ 123`, `Ala ma kota xyz XYZ 123`, `2`, `0`.
- Ekran MUSI pokazać po `2`: `[0] Tytul: Ala ma kota xyz XYZ 123 | Tresc: Ala ma kota xyz XYZ 123`.
- Lista `tresci` MUSI trzymać `Kzt wo dyht hms HMS 123`.

**Kontrola:**
- [ ] Lista trzyma szyfr, ekran odszyfrowany tekst.
- [ ] `% klucz.length()` jest w obu metodach.
- [ ] Spacje i cyfry nie ruszają licznika `j`.

## Krok 4. Zapis do pliku archiwum.txt (5.0)

**Cel:** notatki przeżywają zamknięcie programu.

**Co dodać:**
- W menu linia `[7] Zapisz notatki do pliku`.
- Metodę `public static void zapisz(ArrayList<String> tytuly, ArrayList<String> tresci)`. W `try (PrintWriter pw = new PrintWriter("archiwum.txt"))` zapisujesz jedną linię na notatkę: `tytul|tresc`, gdzie `tresc` to dokładnie to, co leży w liście `tresci` (szyfr, bez szyfrowania po raz drugi). Na końcu `Zapisano notatki do archiwum.txt: ` i liczba notatek.
- `catch (FileNotFoundException e)` wypisuje `Nie mozna zapisac pliku archiwum.txt`, a program działa dalej.
- `case 7`: `zapisz(tytuly, tresci);`.

**Test ręczny:**
- Wpisz: `kot`, `1`, `Zakupy`, `mleko chleb`, `1`, `Dom`, `klucz`, `7`, `0`.
- Plik `archiwum.txt` MUSI mieć dwie linie: `Zakupy|<szyfr treści>` i `Dom|<szyfr treści>`. Jawnego `mleko chleb` w pliku NIE MA.
- Gdy w katalogu programu jest KATALOG o nazwie `archiwum.txt`: ekran pokazuje `Nie mozna zapisac pliku archiwum.txt` i program żyje.

**Kontrola:**
- [ ] W pliku tylko szyfr.
- [ ] `try-catch` albo `try-with-resources`, nie gołe `new PrintWriter`.
- [ ] Linie są w kolejności listy.

## Krok 5. Odczyt archiwum.txt na starcie programu (5.0)

**Cel:** po ponownym uruchomieniu notatki wracają.

**Co dodać:**
- Metodę `public static void wczytaj(String nazwa, ArrayList<String> tytuly, ArrayList<String> tresci)`. Najpierw `new File(nazwa).exists()`: gdy pliku nie ma, wypisz `Brak pliku archiwum.txt - zaczynamy od pustej listy` i wyjdź (`return`). Gdy jest: `BufferedReader`, linia po linii, `linia.split("\\|", 2)`, `tytuly.add(kawalki[0])`, `tresci.add(kawalki[1])`. Na końcu `Wczytano notatki z archiwum.txt: ` i liczba.
- `catch (IOException e)` wypisuje `Nie mozna odczytac pliku archiwum.txt`.
- W `main` wołasz `wczytaj("archiwum.txt", tytuly, tresci);` zaraz po utworzeniu list i `Scannera`, przed pytaniem o klucz.
- `split("|")` TO PUŁAPKA: sama kreska w wyrażeniu regularnym znaczy „lub” i tnie po każdym znaku. Dosłowna kreska to `\\|`. Drugi argument `2` jest po to, żeby notatka z PUSTĄ treścią (linia `Dom|`) też dała dwa kawałki: zwykły `split` wyrzuca puste kawałki z końca, zostaje jeden i `kawalki[1]` rzuca `ArrayIndexOutOfBoundsException` (tego `catch (IOException e)` nie złapie). Tytuł nie może zawierać znaku `|` - przesunąłby dane w linii.

**Test ręczny:**
- Plik z linią `Pusta|` (pusta treść) MUSI się wczytać: tytuł `Pusta`, treść pusta, bez wyjątku.
- Z plikiem z kroku 4: wpisz `kot`, `2`, `0`. Ekran MUSI pokazać `Wczytano notatki z archiwum.txt: 2` i obie notatki jawnie. Bez pliku: `Brak pliku archiwum.txt - zaczynamy od pustej listy` i `Brak notatek.`
- Po wczytaniu lista NIE jest oznaczona jako posortowana: `[5]` posortuje ją przed szukaniem.

**Kontrola:**
- [ ] Brak pliku to zdanie, nie czerwony błąd.
- [ ] `split` z dwoma ukośnikami i limitem `2`.
- [ ] Wczytujesz do OBU list.

## Krok 6. Przegląd wymagań 4.0 i 5.0 i zakazanych metod

Grep na zakazane metody ma dać pusty wynik:

```
grep -nE 'Collections\.sort|Arrays\.sort|\.contains\(|\.replace|\.indexOf\(|binarySearch' SCIEZKA/Main.java
```

Mapa wymagań z PDF:

| Wymaganie (4.0 i 5.0) | Gdzie to widać w kodzie |
|---|---|
| Szukanie binarne po tytule, O(log N) | `szukajBinarnie` z `p`, `k`, `mid`, `compareTo` |
| Strażnik posortowania | flaga `posortowane`, gaszona w `case 1`, `case 5` woła sortowanie, gdy `false` |
| Szukanie słowa w treściach bez `contains` i `indexOf` | `szukajWzorca` z dwiema pętlami, odszyfrowanie w locie w `case 6` |
| Vigenère z kluczem od użytkownika | `szyfrujV`, `deszyfrujV`, `j % klucz.length()` |
| Zapis do pliku, tylko szyfr | `zapisz`, linia `tytul|szyfr` |
| Odczyt na starcie, `split` | `wczytaj`, `split("\\|", 2)` |
| Wyjątki, komunikat zamiast awarii | `try-catch`, `exists()` |

## Sprawdzian końcowy

Pusty plik i cały program na 5.0 od zera, bez podglądania. Potem uruchom testy kroków 1-5 po kolei, w tym brak pliku i katalog zamiast pliku. Grep z kroku 6 ma być pusty.

## Typowe błędy w składaniu 4.0/5.0

- Dodanie notatki NIE gasi flagi `posortowane`: szukanie binarne chodzi po liście w złej kolejności.
- `i < n - m` zamiast `i <= n - m`: gubisz trafienie na samym końcu.
- `klucz.charAt(j)` bez `%`: wyjątek, gdy tekst jest dłuższy od klucza.
- Do pliku idzie jawny tekst (albo szyfr zaszyfrowany drugi raz).
- `split("|")` zamiast `split("\\|", 2)`; sam `split("\\|")` bez limitu wywala program na notatce o pustej treści.
- Brak `return` po komunikacie o braku pliku.
- Zapis bez `try-catch`: javac nie skompiluje (wyjątek sprawdzany).

## Obrona: pytania do 4.0 i 5.0

Odpowiadasz własnymi słowami, krótko, i pokazujesz miejsce w kodzie (jak w rozdziale 12).

### 1. Czemu szukanie binarne wymaga posortowanych danych i co robi strażnik?

**Odpowiedź:** Szukanie binarne porównuje szukany tytuł ze środkiem i odrzuca CAŁĄ połowę. To ma sens tylko wtedy, gdy mniejsze tytuły stoją po jednej stronie, a większe po drugiej. Strażnik to flaga `posortowane`: dodanie notatki ją gasi, a `case 5` sprawdza ją przed szukaniem i, gdy jest `false`, woła `sortujBabelkowo` na obu listach.

**Pokaż w kodzie:** `boolean posortowane`, `if (!posortowane)` w `case 5`.

### 2. Ile kroków robi szukanie binarne, a ile liniowe? Czemu O(log N)?

**Odpowiedź:** Liniowe sprawdza najwyżej N elementów: O(N). Binarne za każdym razem tnie zakres na pół, więc robi najwyżej tyle kroków, ile razy da się podzielić N przez 2: dla 1000 notatek 10, dla miliona 20. Podwojenie liczby notatek dokłada jeden krok.

**Pokaż w kodzie:** `while (p <= k)` z `mid = (p + k) / 2` i `p = mid + 1` albo `k = mid - 1`.

### 3. Czemu pętla po oknach idzie do n - m, a nie do n?

**Odpowiedź:** Okno ma m liter. Ostatnie okno, które jeszcze mieści się w tekście o n literach, zaczyna się na pozycji n - m. Dalej `charAt(i + j)` wyszłoby poza tekst. Gdy wzór jest dłuższy od tekstu, n - m jest ujemne i pętla nie robi żadnego obrotu.

**Pokaż w kodzie:** `for (int i = 0; i <= n - m; i++)`.

### 4. Czemu % na numerze klucza i osobny licznik j?

**Odpowiedź:** Klucz bywa krótszy od tekstu. `j % klucz.length()` zawija numer litery klucza, więc klucz zaczyna się od nowa. Osobny licznik `j` rośnie tylko po literach, więc spacje i cyfry nie zjadają liter klucza.

**Pokaż w kodzie:** `klucz.charAt(j % klucz.length())` i `j++` w blokach liter.

### 5. Czemu do pliku trafia tylko zaszyfrowana treść?

**Odpowiedź:** Plik leży na dysku i każdy może go otworzyć. Lista `tresci` już trzyma szyfr, więc `zapisz` zapisuje to, co leży w liście, bez szyfrowania po raz drugi. Jawny tekst żyje tylko w pamięci programu i na ekranie.

**Pokaż w kodzie:** `pw.println(tytuly.get(i) + "|" + tresci.get(i))`.

### 6. Czym jest wyjątek sprawdzany i czemu javac wymusza try-catch?

**Odpowiedź:** To wyjątek, o którym kompilator wie z góry (np. `FileNotFoundException` przy `new PrintWriter(...)`). Musisz go złapać (`try-catch`) albo zadeklarować (`throws`), inaczej program się nie skompiluje. Wyjątki niesprawdzane (dzielenie przez zero, zły indeks) wychodzą dopiero w trakcie działania.

**Pokaż w kodzie:** `try (PrintWriter pw = ...) { ... } catch (FileNotFoundException e) { ... }`.

### 7. Co się stanie, gdy pliku archiwum.txt nie ma? Czemu split("|") jest pułapką?

**Odpowiedź:** `exists()` zwraca `false`, `wczytaj` wypisuje `Brak pliku archiwum.txt - zaczynamy od pustej listy` i kończy się. To normalne przy pierwszym uruchomieniu, nie awaria. `split("|")` tnie po każdym znaku, bo `split` bierze wyrażenie regularne, w którym sama kreska znaczy „lub”. Dosłowna kreska: `split("\\|", 2)`, a `2` chroni notatki z pustą treścią (bez niego `Dom|` daje jeden kawałek i `kawalki[1]` pada).

**Pokaż w kodzie:** `if (!plik.exists()) { ... return; }` i `split("\\|", 2)`.

## Quiz na 10 minut (4.0 i 5.0)

Odpowiedz na głos, bez zaglądania: (1) po co flaga `posortowane`, (2) ile kroków binarnego dla 1000 notatek, (3) czemu `n - m`, (4) czemu `%` i `j`, (5) co leży w pliku i czemu, (6) co to wyjątek sprawdzany, (7) co zrobi program bez pliku.
