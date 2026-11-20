# Rozdział 13. Wyszukiwanie binarne

**Po co ci to w projekcie:** na ocenę 4.0 opcja szukania notatki po tytule ma działać w O(log N), czyli wyszukiwaniem binarnym. Prowadzący sprawdza jeszcze jedno: program musi PILNOWAĆ, że tytuły są posortowane. Po dodaniu nowej notatki lista przestaje być posortowana, więc przed szukaniem binarnym trzeba znowu ją posortować (bąbelkiem z rozdziału 9). W ćwiczeniach tej karty szukasz po liczbach i imionach, a w projekcie zrobisz to samo dla tytułów.

## Teoria

### Zgadywanka: połowa odpada

Ktoś myśli o liczbie od 1 do 100. Ty zgadujesz. Możesz pytać po kolei: „1? 2? 3?...” i w najgorszym razie zapytasz 100 razy. Mądrzej zapytać o środek: „50?”. Dostajesz odpowiedź „za dużo”. Teraz wiesz, że liczba jest między 1 a 49, więc połowa liczb odpadła jednym pytaniem. Pytasz o środek tego, co zostało: „25?”. „Za mało.” Zostaje 26..49. I tak dalej.

Tak działa wyszukiwanie binarne: w każdym kroku patrzysz na ŚRODEK, porównujesz go z tym, czego szukasz, i wyrzucasz połowę, w której na pewno tego nie ma.

To działa TYLKO wtedy, gdy dane są posortowane. „Za dużo” znaczy „wszystko na prawo od środka też jest za duże” tylko wtedy, gdy większe liczby stoją po prawej.

### Trzy zmienne: p, k, mid

Zapamiętujesz, gdzie jeszcze wolno szukać:

- `p` - numer pierwszej szuflady, która jeszcze się liczy (na start `0`),
- `k` - numer ostatniej szuflady, która jeszcze się liczy (na start `t.length - 1`, NIE `t.length`!),
- `mid` - środek: `(p + k) / 2`. Dzielenie liczb całkowitych odrzuca resztę, więc `(0 + 6) / 2 = 3`, a `(4 + 5) / 2 = 4`.

W każdym obiegu pętli:

1. Liczysz `mid`.
2. Jeśli `t[mid]` jest tym, czego szukasz, oddajesz `mid` (`return mid`).
3. Jeśli `t[mid]` jest MNIEJSZE od szukanego, lewa połowa (razem ze środkiem) odpada: `p = mid + 1`.
4. W przeciwnym razie prawa połowa (razem ze środkiem) odpada: `k = mid - 1`.

Pętla trwa, dopóki zostaje choć jedna szuflada: `while (p <= k)`. Gdy `p` przeskoczy za `k`, nie ma już gdzie szukać i po pętli oddajesz `-1`.

Przykład z innymi danymi (nie z zadań):

```java
int oceny[] = {1, 3, 5, 7, 9, 11, 13};   // POSORTOWANE
int szukana = 9;
int p = 0;
int k = oceny.length - 1;
int wynik = -1;
while (p <= k) {
    int mid = (p + k) / 2;
    if (oceny[mid] == szukana) {
        wynik = mid;
        break;
    } else if (oceny[mid] < szukana) {
        p = mid + 1;
    } else {
        k = mid - 1;
    }
}
System.out.println(wynik);  // wypisze 4
```

Ślad krok po kroku z prawdziwego uruchomienia:

```
krok 1: p=0 k=6 mid=3   oceny[3]=7   7 < 9  -> p = 4   (odpada 1 3 5 7)
krok 2: p=4 k=6 mid=5   oceny[5]=11  11 > 9 -> k = 4   (odpada 11 13)
krok 3: p=4 k=4 mid=4   oceny[4]=9   9 = 9  -> znaleziono pod numerem 4
```

A tak wygląda szukanie liczby, której nie ma (4 w tej samej tablicy):

```
krok 1: p=0 k=6 mid=3   oceny[3]=7   7 > 4  -> k = 2
krok 2: p=0 k=2 mid=1   oceny[1]=3   3 < 4  -> p = 2
krok 3: p=2 k=2 mid=2   oceny[2]=5   5 > 4  -> k = 1
p = 2, k = 1: p jest większe od k, nie ma już szuflad. Wynik -1.
```

Dla tekstów w `ArrayList<String>` robisz to samo, tylko zamiast `<` używasz `compareTo` z rozdziału 7: `lista.get(mid).compareTo(x)` daje liczbę ujemną, gdy środek stoi PRZED szukanym, `0`, gdy są równe, i dodatnią, gdy środek stoi PO szukanym.

### Ile kroków: O(log N)

Każdy krok wyrzuca połowę. Pytanie: ile razy da się połowić, zanim nic nie zostanie? Dla 7 szuflad: 7, 3, 1, 0 - to 3 połowienia, więc najwyżej 3 sprawdzenia. Dla 1000 szuflad najwyżej 10 sprawdzeń. Dla miliona - 20.

| ile szuflad | po kolei (najgorzej) | binarnie (najgorzej) |
|---|---|---|
| 7 | 7 | 3 |
| 1000 | 1000 | 10 |
| 1 000 000 | 1 000 000 | 20 |

Zapis O(log N) czytasz: „liczba kroków rośnie tylko o jeden, gdy dane podwoisz”. Porównaj z O(N) z rozdziału 8, gdzie dane podwojone to dwa razy więcej kroków.

### Pilnowanie posortowania

Wyszukiwanie binarne działa tylko na posortowanych danych. W projekcie dodajesz nowe notatki na koniec listy, więc po dodaniu lista przestaje być posortowana. Jak to pilnować? Zmienna-flaga:

```java
static boolean posortowane = true;
```

- Po KAŻDYM dodaniu na koniec ustawiasz `posortowane = false;`.
- Po sortowaniu ustawiasz `posortowane = true;`.
- Metoda szukająca na samym początku sprawdza flagę: `if (!posortowane) { sortuj(); }`. Dopiero potem szuka binarnie.

`static` przy zmiennej poza metodami znaczy, że widzą ją wszystkie metody (tak jak listy z rozdziału 11). Gdy są dwie listy równoległe, sortowanie rusza OBIE razem (rozdział 9), inaczej imiona rozjadą się z ocenami.

Przykład z innymi danymi (owoce i ceny):

```java
static ArrayList<String> owoce = new ArrayList<>();
static ArrayList<String> ceny = new ArrayList<>();
static boolean posortowane = true;

static void dodajOwoc(String nazwa, String cena) {
    owoce.add(nazwa);
    ceny.add(cena);
    posortowane = false;      // nowy owoc stoi na koncu, lista moze byc w zlej kolejnosci
}

static String cenaOwocu(String nazwa) {
    if (!posortowane) {
        sortujOwoce();        // ta metoda na koncu ustawia posortowane = true
    }
    // ...dopiero tu szukanie binarne po owoce, wynik z listy ceny
    return "";
}
```

## Ćwiczenia

### Ćwiczenie 1 (plik `R13_cw1.java`)

W tablicy `int oceny[] = {1, 3, 5, 7, 9, 11, 13};` jest szukanie binarne z teorii (szuka `9`). Uruchom je i patrz, jak odpadają połowy. Potem zmień szukaną liczbę na `11`. Na ekranie ma być numer `5`.

Oczekiwany wynik na ekranie:

```
5
```

<details>
<summary>Podpowiedź</summary>

Zmieniasz tylko jedną liczbę: tę, z którą `oceny[mid]` jest porównywane w trzech miejscach. Najprościej mieć ją w zmiennej `szukana` i zmienić w jednym miejscu.

</details>

### Ćwiczenie 2 (plik `R13_cw2.java`)

Napisz metodę `public static int binarnie(int t[], int x)`, która szuka `x` w POSORTOWANEJ tablicy i zwraca numer szuflady albo `-1`, gdy tego nie ma.

W `main` użyj tablicy `int numery[] = {2, 5, 8, 12, 16, 23, 38, 56};` i wypisz trzy linie w formacie `szukam X: pozycja N` dla `X` = 23, 5 i 7.

Oczekiwany wynik na ekranie:

```
szukam 23: pozycja 5
szukam 5: pozycja 1
szukam 7: pozycja -1
```

<details>
<summary>Podpowiedź</summary>

Trzy zmienne `p = 0`, `k = t.length - 1` i `mid = (p + k) / 2` liczone w każdym obiegu. Pętla `while (p <= k)`. W środku trzy przypadki: równe (`return mid`), za małe (`p = mid + 1`), za duże (`k = mid - 1`). `return -1` stoi PO pętli.

</details>

### Ćwiczenie 3 (plik `R13_cw3.java`)

Zmień metodę z ćwiczenia 2 tak, żeby nie zwracała numeru, tylko LICZBĘ OBIEGÓW pętli (ile razy liczyłeś `mid`). Nazwij ją `kroki`. W `main` na tej samej tablicy wypisz `kroki` dla 23, 7 i 100.

Oczekiwany wynik na ekranie:

```
szukam 23: 2 kroki
szukam 7: 3 kroki
szukam 100: 4 kroki
```

Zauważ: tablica ma 8 elementów, a szukanie po kolei sprawdziłoby je wszystkie (8 kroków dla liczby, której nie ma). Binarnie wystarczyły 4.

<details>
<summary>Podpowiedź</summary>

Zmienna `licznik` na `0` przed pętlą i `licznik++` jako pierwsza linijka w pętli (każdy obieg = jedno policzenie `mid`). Po pętli `return licznik`.

</details>

### Ćwiczenie 4 (plik `R13_cw4.java`)

Napisz metodę `public static int binarnie(ArrayList<String> lista, String x)` dla POSORTOWANEJ listy imion. Zamiast `<` użyj `compareTo`. W `main` dodaj do listy `imiona`: `Ania`, `Basia`, `Kasia`, `Ola`, `Zosia`. Szukaj `Kasia`, `Zosia` i `Ewa`.

Oczekiwany wynik na ekranie:

```
szukam Kasia: pozycja 2
szukam Zosia: pozycja 4
szukam Ewa: pozycja -1
```

<details>
<summary>Podpowiedź</summary>

Wynik `lista.get(mid).compareTo(x)` zapisz w zmiennej `wynik`. `0` to trafienie. Ujemne znaczy, że środek stoi przed szukanym, więc `p = mid + 1`. Dodatnie: `k = mid - 1`. Długość listy to `size()`.

</details>

### Ćwiczenie 5 (plik `R13_cw5.java`)

Dwie listy równoległe `imiona` i `oceny` są `static`, jest też flaga `static boolean posortowane = true;`. Metoda `sortuj()` (bąbelek na obu listach, z rozdziału 9) już jest w pliku i na końcu ustawia `posortowane = true`. Napisz metodę `public static void dodaj(String imie, String ocena)`: dodaje imię do `imiona`, ocenę do `oceny` i ustawia flagę na `false`.

W `main` (jest gotowy) są trzy dodania i wypisanie flagi, potem sortowanie i znowu flaga.

Oczekiwany wynik na ekranie:

```
posortowane: false
posortowane: true
```

<details>
<summary>Podpowiedź</summary>

Dwa `add` (po jednym na listę) i jedna linijka z flagą. Flaga ma być `false`, bo nowy element leży na końcu, a nie na swoim miejscu.

</details>

### Ćwiczenie 6 (plik `R13_cw6.java`)

W pliku są już `static` listy, `dodaj`, `sortuj` i `binarnie` (szuka po `imiona`). Napisz metodę `public static String znajdz(String imie)`, która:

1. jeśli flaga `posortowane` jest `false`, najpierw woła `sortuj()`,
2. szuka `imie` przez `binarnie`,
3. zwraca ocenę z DRUGIEJ listy (pod tym samym numerem) albo tekst `nie ma`.

`main` jest gotowy: dodaje `Zosia`, `Adam`, `Ola`, szuka `Ola`, potem dodaje `Basia` i szuka `Basia`.

Oczekiwany wynik na ekranie:

```
Ola: 3
Basia: 5
```

<details>
<summary>Podpowiedź</summary>

Na początku metody `if (!posortowane) { sortuj(); }`. Potem `int nr = binarnie(imiona, imie);` i sprawdzasz, czy `nr` to `-1`. Gdy nie, `oceny.get(nr)`.

</details>

### Ćwiczenie 7 (plik `R13_cw7.java`)

Najpierw na kartce: tablica ma 1000 posortowanych liczb. Ile najwyżej sprawdzeń zrobi szukanie po kolei, a ile binarne? A gdy tablica urośnie do 2000? Potem napisz metodę `public static int ilePolowien(int n)`: zwraca, ile razy da się podzielić `n` przez 2 (dzielenie całkowite), zanim wyjdzie `0`. To dokładnie tyle sprawdzeń, ile najwyżej zrobi szukanie binarne w `n` szufladach.

W `main` wypisz `ilePolowien` dla 7, 1000 i 1000000.

Oczekiwany wynik na ekranie:

```
7: 3
1000: 10
1000000: 20
```

<details>
<summary>Podpowiedź</summary>

Pętla `while (n > 0)`: w środku `n = n / 2;` i `licznik++`. Po pętli oddajesz licznik.

</details>

## Sprawdzian

Z pamięci, bez podglądania:

1. Napisz metodę `binarnie` dla tablicy `int` (ćwiczenie 2).
2. Powiedz na głos, po co dane muszą być posortowane.
3. Powiedz na głos, po co w projekcie flaga `posortowane` i gdzie ją ustawiasz.

Co sprawdza prowadzący (Claude):

- `p = 0` i `k = t.length - 1` (nie `t.length`).
- `mid = (p + k) / 2` liczone WEWNĄTRZ pętli, w każdym obiegu od nowa.
- Warunek `while (p <= k)` (nie `<`, bo wtedy ostatnia szuflada nigdy nie jest sprawdzona).
- `p = mid + 1` i `k = mid - 1` (nie samo `mid`, bo pętla się zawiesi).
- `return -1` PO pętli.
- Wyjaśnienie: połowa odpada tylko wtedy, gdy dane są posortowane; O(log N), bo każdy krok połowi dane.
- Flaga: po dodaniu `false`, po sortowaniu `true`, a szukanie zaczyna od `if (!posortowane) sortuj();`. Przy dwóch listach sortowanie rusza obie.

## Typowe błędy

- **`k = t.length` zamiast `t.length - 1`.** Przy szukaniu liczby większej niż wszystkie `mid` dochodzi do `t.length` i program wywala `ArrayIndexOutOfBoundsException`. To ten sam błąd, co w starym ćwiczeniu z laboratorium.
- **`while (p < k)` zamiast `p <= k`.** Gdy zostaje jedna szuflada, `p` jest równe `k`, pętla się nie wykonuje i ostatnia kandydatka nigdy nie zostaje sprawdzona.
- **`p = mid` albo `k = mid` zamiast `mid + 1` i `mid - 1`.** Środek już sprawdziłeś, więc ma odpaść. Bez tego zakres przestaje się kurczyć i pętla trwa w nieskończoność.
- **Dane nieposortowane.** Szukanie binarne spokojnie zwraca `-1`, choć liczba w tablicy jest. Nikt nie zgłasza błędu, wynik jest po prostu zły.
- **Dodanie na koniec bez ustawienia flagi.** Szukanie nie wie, że lista jest do sortowania, i szuka binarnie w nieposortowanej.
- **`==` na tekstach w wersji na listach.** Do porównania użyj `compareTo` (rozdział 7), a nie `==` ani `<`.

**Czas na rozdział:** 60-90 min.

---
