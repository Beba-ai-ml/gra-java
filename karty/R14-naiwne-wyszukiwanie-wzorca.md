# Rozdział 14. Naiwne wyszukiwanie wzorca

**Po co ci to w projekcie:** na ocenę 4.0 menu dostaje opcję „Szukaj słowa w treściach notatek”. Program odszyfrowuje treść każdej notatki i sprawdza, czy stoi w niej szukane słowo. Prowadzący zabrania `contains` i `indexOf`, więc szukanie robisz SAM, dwiema pętlami jedna w drugiej. W ćwiczeniach tej karty szukasz w zwykłych napisach, a w projekcie zrobisz to samo na odszyfrowanych treściach.

## Teoria

### Okno pod tekstem

Wyobraź sobie tekst `kotek` napisany na długim pasku papieru. Wzór `tek` jest na małym pasku. Przykładasz mały pasek pod początek dużego i patrzysz: czy wszystkie litery się zgadzają? Jeśli nie, przesuwasz mały pasek o JEDNO miejsce w prawo i patrzysz od nowa.

Mały pasek to „okno”. Numer `i` mówi, na której literze tekstu okno się zaczyna. Numer `j` mówi, którą literę wzoru porównujesz. Litera tekstu pod literą `j` wzoru ma numer `i + j`.

Dlatego potrzebne są dwie pętle:

- zewnętrzna `i` - przesuwa okno po tekście,
- wewnętrzna `j` - porównuje litery wzoru z literami pod oknem.

Przykład z innymi danymi (nie z zadania):

```java
String text = "kotek";
String wzor = "tek";
int n = text.length();
int m = wzor.length();
int wynik = -1;                       // na start: nie znaleziono
for (int i = 0; i <= n - m; i++) {
    boolean pasuje = true;
    for (int j = 0; j < m; j++) {
        if (text.charAt(i + j) != wzor.charAt(j)) {
            pasuje = false;
            break;                    // jedna zła litera - to okno odpada
        }
    }
    if (pasuje) {
        wynik = i;
        break;
    }
}
System.out.println(wynik);            // wypisze 2
```

Gdy litera się nie zgadza, `break` kończy pętlę `j`, nie ma sensu sprawdzać reszty. Pętla `i` idzie do następnego okna. Gdy pętla `j` przejdzie wszystkie litery wzoru bez `break`, `pasuje` zostaje `true` i znaleziony numer to `i`.

### Dlaczego n - m

Tekst ma `n` liter, wzór ma `m` liter. Okno wystaje na `m` liter w prawo od miejsca `i`. Ostatnie okno, które jeszcze mieści się w tekście, zaczyna się na numerze `n - m`. Dla `kotek` (`n` = 5) i `tek` (`m` = 3) to numer 2.

Gdy okno zaczyna się dalej, wystaje za koniec tekstu i `charAt` wywala `StringIndexOutOfBoundsException`. Dlatego pętla `i` ma warunek `i <= n - m`. Z `<` zamiast `<=` pominiesz ostatnie okno, a wzór stojący na samym końcu tekstu nie zostanie znaleziony.

### Przypadki brzegowe

- **Wzór dłuższy niż tekst:** `n - m` jest ujemne, pętla `i` nie robi ani jednego obiegu. Wynik `-1` - i tak ma być.
- **Pusty wzór:** `m` = 0, więc `j` nie robi obiegu i `pasuje` zostaje `true` już w oknie 0. Wynik `0`. Tak działa też prawdziwe `indexOf("")`.
- **Powtarzające się litery:** szukasz `aab` w `aaab`. Okno 0 to `aaa`: dwie pierwsze litery pasują, trzecia nie. Nie wolno przeskoczyć za daleko. Idziesz o JEDNO miejsce, okno 1 to `aab` i pasuje. Wynik `1`.
- **Wielkie i małe litery:** `A` i `a` to dwa różne znaki (65 i 97). Szukanie `Ala` w `ala` zwraca `-1`.

### Szukanie w odszyfrowanych treściach

W projekcie treść w liście jest zaszyfrowana. Nie wolno jej odszyfrować na stałe i zapisać z powrotem (w liście ma zostać szyfrogram). Dlatego dla każdej notatki robisz tak:

1. bierzesz zaszyfrowaną treść z listy (`get(i)`),
2. odszyfrowujesz ją do zwykłej zmiennej (`String tekst = odszyfruj(...)`),
3. szukasz w tej zmiennej,
4. zmienna ginie, a lista zostaje nietknięta.

## Ćwiczenia

### Ćwiczenie 1 (plik `R14_cw1.java`)

Napisz metodę `public static int szukajWzorca(String text, String wzor)`. Zwraca numer, od którego wzór stoi w tekście (pierwsze trafienie), a gdy go nie ma - `-1`. Nie wolno użyć `contains`, `indexOf` ani `substring`. Masz dwie pętle.

W `main` użyj tekstu `ananas` i wypisz trzy linie w formacie `szukam WZOR: pozycja N` dla wzorów `nas`, `ana` i `xyz`.

Oczekiwany wynik na ekranie:

```
szukam nas: pozycja 3
szukam ana: pozycja 0
szukam xyz: pozycja -1
```

<details>
<summary>Podpowiedź</summary>

Zewnętrzna pętla `for (int i = 0; i <= n - m; i++)`. W środku `boolean pasuje = true;` i pętla `for (int j = 0; j < m; j++)`. Gdy `text.charAt(i + j) != wzor.charAt(j)`, ustaw `pasuje = false;` i zrób `break`. Po pętli `j`: `if (pasuje) return i;`. Wynik `return -1;` stoi PO pętli `i`.

</details>

### Ćwiczenie 2 (plik `R14_cw2.java`)

Napisz metodę `public static int ileRazy(String text, String wzor)`. Liczy, w ilu miejscach wzór stoi w tekście. Trafienia mogą na siebie nachodzić: wzór `aa` w tekście `aaaa` stoi w trzech miejscach (0, 1 i 2). Pusty wzór zwraca `0`.

W `main` wypisz `ileRazy("aaaa", "aa")`, `ileRazy("banan", "an")` i `ileRazy("kot", "ptak")`, każde w osobnej linii.

Oczekiwany wynik na ekranie:

```
3
2
0
```

<details>
<summary>Podpowiedź</summary>

To prawie ćwiczenie 1, ale po trafieniu NIE robisz `return` ani `break` z pętli `i`. Zamiast tego `licznik++` i pętla idzie do następnego okna. `return licznik;` stoi po pętli `i`. Pusty wzór obsłuż osobnym `if (m == 0) return 0;` na początku.

</details>

### Ćwiczenie 3 (plik `R14_cw3.java`)

W `main` stoją dwie listy równoległe: `tytuly` i `tresci`. Treści są zaszyfrowane szyfrem Cezara z kluczem 3 (małe litery, spacje bez zmian). Metodę `odszyfruj(String coded, int klucz)` dostajesz gotową. Napisz metodę `public static boolean zawiera(String text, String wzor)` (te same dwie pętle co w ćwiczeniu 1, ale zwraca `true` albo `false`).

W `main` wczytaj słowo przez `Scanner` (`nextLine()`). Dla każdej notatki odszyfruj treść do zmiennej, sprawdź ją przez `zawiera` i wypisz TYTUŁ każdej notatki, która ma to słowo. Gdy żadna nie ma, wypisz `brak notatek`.

Dane (tytuł - zaszyfrowana treść):

- `Zakupy` - `nxs pohnr l fkohe` (czyli `kup mleko i chleb`)
- `Szkola` - `cdgdqlh c pdwhpdwbnl` (czyli `zadanie z matematyki`)
- `Dom` - `xpbf rnqr l srgohm nzldwb` (czyli `umyc okno i podlej kwiaty`)

Wejście `mleko`, oczekiwany wynik:

```
Zakupy
```

Wejście `okno`, oczekiwany wynik:

```
Dom
```

Wejście `kot`, oczekiwany wynik:

```
brak notatek
```

<details>
<summary>Podpowiedź</summary>

W pętli po `i` od `0` do `tresci.size() - 1`: `String tekst = odszyfruj(tresci.get(i), 3);`, potem `if (zawiera(tekst, szukane)) { System.out.println(tytuly.get(i)); }`. Do pamiętania, czy cokolwiek znaleziono, zrób `boolean bylo = false;` przed pętlą i ustaw `true` w `if`. Po pętli `if (!bylo)` wypisz `brak notatek`.

</details>

## Sprawdzian

Z pamięci, bez podglądania:

1. Napisz metodę `szukajWzorca(String text, String wzor)` z ćwiczenia 1.
2. Powiedz na głos, czemu pętla `i` kończy się na `n - m`, a nie na `n`.

Co sprawdza prowadzący (Claude):

- Dwie pętle: `i` po początkach okna, `j` po literach wzoru. Litera tekstu to `text.charAt(i + j)`.
- Warunek pętli `i`: `i <= n - m`. Z `<` ginie trafienie na końcu, z `<= n` jest wyjątek.
- Przy złej literze `break` (albo `pasuje = false`) i przejście do następnego okna.
- `return -1` stoi PO pętli `i`, nie w jej środku.
- Brzegi: pusty wzór daje `0`, wzór dłuższy od tekstu daje `-1`, `aab` w `aaab` daje `1`.
- Brak `contains`, `indexOf`, `substring` i `equals` na kawałku tekstu - szukanie jest własne.

## Typowe błędy

- **`i < n - m`.** Ostatnie okno jest pomijane. Wzór stojący na samym końcu tekstu nie zostaje znaleziony.
- **`i <= n`.** Okno wystaje za tekst i program wywala `StringIndexOutOfBoundsException`.
- **`j` zadeklarowane przed pętlą `i`.** Po pierwszym oknie `j` nie wraca do `0` i kolejne okna zaczynają porównywanie od środka wzoru.
- **`return -1` w środku pętli `i`.** Szukanie kończy się po pierwszym oknie, które nie pasuje.
- **Porównywanie `text.charAt(i) == wzor.charAt(j)`** (zamiast `i + j`). Zawsze patrzysz na tę samą literę tekstu, okno się nie przesuwa.
- **Odszyfrowanie treści w liście na stałe** (`tresci.set(i, odszyfruj(...))`). Po szukaniu w liście leży zwykły tekst, a nie szyfrogram.

**Czas na rozdział:** 60-90 min.

---
