# Rozdział 1. Komórki i numery (tablica `int[]`)

**Po co ci to w projekcie:** wszystko w projekcie to „która komórka". Sortowanie, szukanie i szyfr działają na numerach, a numery trzyma tablica.

**Czas:** 60-120 min

---

## Teoria

### Tablica to rząd szuflad

Wyobraź sobie komodę z trzema szufladami. Każda szuflada ma numer. W Javie numer szuflady to **indeks**. Numerujemy od **0**, nie od 1.

```
 tab[0]       tab[1]       tab[2]
+-------+    +-------+    +-------+
|   4   |    |   7   |    |   1   |
+-------+    +-------+    +-------+
    0            1            2
```

Deklaracja i odczyt:

```java
int tab[] = {4, 7, 1};

// tab[0] to 4        (pierwsza szuflada)
// tab[2] to 1        (ostatnia szuflada)
// tab.length to 3    (ile jest szuflad)
// tab[3] NIE istnieje (numery to tylko 0, 1, 2)
```

Reguła: ostatnia szuflada ma numer `tab.length - 1`. Tu: 3 - 1 = 2.

### Pętla po wszystkich szufladach

```java
for (int i = 0; i < tab.length; i++) {
    System.out.println(tab[i]);
}
```

Co się dzieje, krok po kroku:

```
i = 0   ->  tab[0] = 4    ok
i = 1   ->  tab[1] = 7    ok
i = 2   ->  tab[2] = 1    ok
i = 3   ->  3 < 3 to NIE, pętla się kończy
```

Dlaczego `<`, a nie `<=`? Przy `<=` pętla dojdzie do `i = 3` i wejdzie w szufladę, której nie ma. Java wtedy przerywa program.

### Zamiana dwóch szuflad: potrzebny trzeci kubek

Chcesz zamienić `tab[0]` i `tab[2]`. Bez trzeciego kubka jedna wartość zginie:

```java
int tmp = tab[0];   // 1. odloz 4 do kubka tmp
tab[0] = tab[2];    // 2. do szuflady 0 wsyp 1
tab[2] = tmp;       // 3. do szuflady 2 wsyp 4 z kubka
```

Co się dzieje z wartościami:

```
                tab[0]   tab[2]   tmp
na początku       4        1       -
krok 1            4        1       4    tmp = tab[0]
krok 2            1        1       4    tab[0] = tab[2]
krok 3            1        4       4    tab[2] = tmp
```

Po zamianie tablica to `{1, 7, 4}`. Szuflada `tab[1]` nie ruszona.

### Losowe liczby

```java
import java.util.Random;

Random rnd = new Random(7);
int x = rnd.nextInt(10);
```

- `rnd.nextInt(10)` daje liczbę od **0 do 9**. Liczba 10 nigdy nie wypadnie. Liczba w nawiasie to granica, której NIE dostaniesz.
- `new Random(7)`: liczba 7 to **ziarno**. Przy tym samym ziarnie wypadają zawsze te same liczby. Dzięki temu twój wynik da się porównać z podanym.

---

## Ćwiczenia

Każde ćwiczenie to osobny plik w pakiecie `nauka` w IntelliJ. Najpierw zrób, potem zajrzyj do podpowiedzi. Wynik na ekranie ma być **dokładnie** taki jak podany.

### Ćwiczenie 1.1: wypisz tablicę z numerami

Plik: `R1_cw1.java`

Zadeklaruj `int tab[] = {4, 7, 1};`. Pętlą `for` po `i` wypisz każdą szufladę w postaci `[numer] wartosc`.

**Oczekiwany wynik:**

```text
[0] 4
[1] 7
[2] 1
```

<details><summary>Podpowiedź</summary>

Pętla idzie od `0` do `tab.length` (z `<`). W środku: `"[" + i + "] " + tab[i]`.

</details>

---

### Ćwiczenie 1.2: to samo od końca

Plik: `R1_cw2.java`

Ta sama tablica `{4, 7, 1}`. Wypisz ją od ostatniej szuflady do pierwszej. Numery zostają takie jak w tablicy.

**Oczekiwany wynik:**

```text
[2] 1
[1] 7
[0] 4
```

<details><summary>Podpowiedź</summary>

Start: `tab.length - 1`. Warunek: `i >= 0`. Krok: `i--`.

</details>

---

### Ćwiczenie 1.3: numer największego elementu

Plik: `R1_cw3.java`

Zadeklaruj `int tab[] = {4, 7, 1, 9, 2};`. Znajdź **numer** największego elementu, nie jego wartość. Wypisz:

`najwiekszy jest pod numerem 3, wartosc 9`

Nie wpisuj wyniku na sztywno. Program ma go policzyć.

**Oczekiwany wynik:**

```text
najwiekszy jest pod numerem 3, wartosc 9
```

<details><summary>Podpowiedź</summary>

Trzymaj w osobnej zmiennej NUMER najlepszej dotąd szuflady, a nie jej wartość. Pomyśl, od którego numeru zacząć i kiedy ten numer zmienić.

</details>

---

### Ćwiczenie 1.4: zamień pierwszy i ostatni

Plik: `R1_cw4.java`

Zadeklaruj `int tab[] = {9, 3, 6, 2};`. Zamień miejscami pierwszy i ostatni element (użyj `tmp`). Potem wypisz tablicę tak jak w ćwiczeniu 1.1.

**Oczekiwany wynik:**

```text
[0] 2
[1] 3
[2] 6
[3] 9
```

<details><summary>Podpowiedź</summary>

Trzy linijki jak na rysunku z teorii. Ostatni numer to `tab.length - 1`, nie `tab.length`.

</details>

---

### Ćwiczenie 1.5: losowe liczby

Plik: `R1_cw5.java`

- `Random rnd = new Random(7);`
- `int tab[] = new int[8];` (8 pustych szuflad)
- W pętli wsyp do każdej szuflady `rnd.nextInt(10)`.
- Potem wypisz `[numer] wartosc` dla wszystkich 8 szuflad.

Odpowiedz sobie na kartce: czy może wypaść 10? Dlaczego?

**Oczekiwany wynik:**

```text
[0] 6
[1] 4
[2] 5
[3] 4
[4] 0
[5] 4
[6] 8
[7] 9
```

<details><summary>Podpowiedź</summary>

Dwie pętle: pierwsza wypełnia szuflady, druga je wypisuje. Każda z nich idzie od `0` do `tab.length` (z `<`).

</details>

---

### Ćwiczenie 1.6: czytanie starego kodu (aisd_lab03)

Plik: `R1_cw6.java`

Tu nie piszesz nic od zera. Czytasz swój stary kod i opisujesz go własnymi słowami.

1. Otwórz `~/Downloads/aisd_lab03.java`. Skopiuj do `R1_cw6.java` linie od `Random rnd` do końca drugiej pętli, czyli te, które liczą min i max (linie 7-25 oryginału). Dopisz `public class R1_cw6` i `main`. Na samej górze pliku (pod `package nauka;`) dopisz też `import java.util.Random;` (linia 1 oryginału), inaczej kompilator nie zna `Random`.
2. Odkomentuj `rnd.setSeed(111);`. Ustala ziarno, więc wynik jest zawsze taki sam.
3. Nad każdą linijką kodu dopisz komentarz `//`, który mówi, co ona robi. Krótko. W komentarzach kodu bez polskich znaków.
4. Na końcu `main` dopisz dwie linie: `System.out.println("dlugosc: " + a);` i `System.out.println("min: " + minimum + ", max: " + maximum);`.

**Oczekiwany wynik (`R1_cw6`):**

```text
dlugosc: 12
min: -2707, max: 4979
```

<details><summary>Podpowiedź</summary>

`rnd.nextInt(11)` daje 0-10, a po `+ 10` wychodzi 10-20. To trafia do zmiennej `a`. Pomyśl, do czego `a` służy w `new int[a]`.

</details>

**Krok 5 (osobno, nie w pliku do oddania):** w KOPII całego `aisd_lab03.java` zmień `int szukam = 2;` na `int szukam = 100;` i uruchom. Wklej pierwszą linijkę błędu. Potem wyjaśnij, dlaczego program pada, w komentarzu pod tym wpisem. **Nie naprawiaj** wyszukiwania binarnego. Na tym etapie tylko znajdujesz błąd i go opisujesz (naprawa to rozdział na 4.0).

Pierwsza linijka błędu:

```text
Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 10 out of bounds for length 10
```

<details><summary>Podpowiedź</summary>

Patrz na wiersz `int p = 0, k = tablica.length;`. `k` dostaje 10, ale ostatnia szuflada ma numer 9. Dla `100` pętla przesuwa `p` w prawo: `p = 0, k = 10` daje `mid = 5`, potem `p = 6` i `mid = 8`, potem `p = 9` i `mid = 9`, potem `p = 10` i `mid = 10`. A `tablica[10]` nie istnieje.

</details>

---

## Sprawdzian

Napisz z pamięci w nowym pliku `R1_spr.java`:

1. Pętlę po tablicy `int tab[] = {4, 7, 1};`, która wypisuje każdą szufladę.
2. Zamianę pierwszego i ostatniego elementu przez `tmp` (bez podglądania).
3. Powiedz na głos, dlaczego ostatni numer to `length - 1`.

**Lista, którą sprawdzający (Claude) sprawdza w kodzie:**

- [ ] Pętla ma `i < tab.length`, a nie `<=`.
- [ ] Zamiana idzie przez `tmp` w trzech linijkach, w dobrej kolejności.
- [ ] Ostatni element to `tab[tab.length - 1]`, nie `tab[tab.length]`.
- [ ] Program się kompiluje i po zamianie wypisuje `[0] 1`, `[1] 7`, `[2] 4`.
- [ ] Odpowiedź ustna: numeracja zaczyna się od 0, więc n szuflad ma numery od 0 do n-1.

---

## Typowe błędy

- **`i <= tab.length`** - pętla wchodzi w szufladę, której nie ma. Program pada: `ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3`.
- **`tab[tab.length]`** zamiast `tab[tab.length - 1]` - ten sam błąd, tylko w jednej linijce.
- **Zamiana bez `tmp`:** `tab[0] = tab[2]; tab[2] = tab[0];` - obie szuflady mają teraz wartość 1, a 4 przepadło.
- **`tab.length()`** - tablica nie ma nawiasów. Napis (`String`) ma `length()`. Kompilator: `cannot find symbol`.
- **`rnd.nextInt(10)`** daje 0-9, a nie 1-10. Liczba w nawiasie to granica, której nie dostaniesz.

---
