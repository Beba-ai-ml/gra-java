# Rozdział 3. Znaki to liczby (ASCII i `char[]`)

**Po co ci to w projekcie:** szyfr Cezara to dodawanie liczb do liter. Bez tego rozdziału nie napiszesz szyfrowania ani deszyfrowania.

**Czas:** 60-120 min

---

## Teoria

### Każda litera ma swój numer

Komputer zna tylko liczby. Każdy znak ma swój numer z tabeli ASCII. W Javie znak zapisujesz w apostrofach, np. `'A'`, i możesz go traktować jak liczbę.

```
A=65   B=66   C=67   ...   Z=90
a=97   b=98   c=99   ...   z=122
spacja=32      cyfra '0' = 48
```

Wielkie litery mają mniejsze numery niż małe. To się przyda w rozdziale 7.

### Różnica dwóch znaków to miejsce w alfabecie

```java
char z = 'C';
int nr = z - 'A';    // 67 - 65 = 2
```

Odejmujesz numer `'A'` od numeru litery i dostajesz jej miejsce w alfabecie, licząc od zera:

```
litera:   A   B   C   D   E   ...   Z
miejsce:  0   1   2   3   4   ...   25
```

To ten sam trik co numer szuflady z rozdziału 1: liczysz od zera.

### Z numeru z powrotem na literę

```java
int nr = 2;
char lit = (char) ('A' + nr);    // 65 + 2 = 67, czyli 'C'
```

- `(char)` znaczy: zamień ten numer na znak. Bez niego `'A' + 2` jest zwykłą liczbą 67.
- `System.out.println('A' + 2);` wypisze `67`, a nie `C`.

### Zawijanie: `%` i alfabet z 26 literami

Przesuwasz literę o 3 do przodu. Za `z` powinno wrócić `a`. Robi to `%`:

```
'x' to miejsce 23.  (23 + 3) % 26 = 26 % 26 = 0  -> 'a'
'Z' to miejsce 25.  (25 + 3) % 26 = 28 % 26 = 2  -> 'C'
```

Wzór na małą literę: `(c - 'a' + 3) % 26`, a potem dodajesz `'a'` z powrotem. Dla wielkiej to samo z `'A'`.

### Napis na tablicę znaków

Napis (`String`) nie ma szuflad `[i]`. Zamieniasz go w tablicę znaków:

```java
String text = "Ala";
char tab[] = text.toCharArray();    // tab to A, l, a
tab[0] = 'X';                       // zmieniasz szuflade jak w rozdziale 1
String wynik = new String(tab);     // skladasz z powrotem: "Xla"
```

```
text:  "Ala"
tab:   +---+ +---+ +---+
       | A | | l | | a |
       +---+ +---+ +---+
         0     1     2
```

Uwaga na długość:

- napis: `text.length()` (z nawiasami),
- tablica: `tab.length` (bez nawiasów).

Czyli to ten sam świat co w rozdziale 1, tylko w szufladach są litery.

---

## Ćwiczenia

Każde ćwiczenie to osobny plik w pakiecie `nauka`. Najpierw zrób, potem zajrzyj do podpowiedzi. Wynik ma być **dokładnie** taki jak podany.

### Ćwiczenie 3.1: każdy znak i jego numer

Plik: `R3_cw1.java`

Dla `String text = "Ala";` wypisz każdy znak i jego numer, po jednej linijce na znak. Format: znak, spacja, numer.

**Oczekiwany wynik:**

```text
A 65
l 108
a 97
```

<details><summary>Podpowiedź</summary>

Pętla po `i` do `text.length()`. Znak daje `text.charAt(i)`, a numer daje `(int) text.charAt(i)`.

</details>

---

### Ćwiczenie 3.2: małe na wielkie bez gotowych metod

Plik: `R3_cw2.java`

Dla `String text = "ala ma kota";` zamień małe litery na wielkie **bez** `toUpperCase()`. Spacje zostają. Wynik wypisz jako napis.

**Oczekiwany wynik:**

```text
ALA MA KOTA
```

<details><summary>Podpowiedź</summary>

Zamień napis na `char tab[]` przez `toCharArray()`. W pętli, gdy `tab[i]` jest w zakresie `'a'..'z'`, odejmij 32 i rzutuj na `char`. Na koniec `new String(tab)`.

</details>

---

### Ćwiczenie 3.3: policz wielkie litery

Plik: `R3_cw3.java`

Dla `String text = "Ala Ma Kota 2";` policz, ile jest wielkich liter. Spacje i cyfry się nie liczą. Wypisz:

`wielkich liter: ` i wynik.

**Oczekiwany wynik:**

```text
wielkich liter: 3
```

<details><summary>Podpowiedź</summary>

Licznik `licz` startuje z `0`. W pętli po `i`: `if` sprawdza, czy znak jest w zakresie `'A'..'Z'`, i wtedy `licz++`.

</details>

---

### Ćwiczenie 3.4: przesuń literę o 3 z zawinięciem

Plik: `R3_cw4.java`

Przesuń dwie litery o 3, z zawinięciem alfabetu:

- `'y'` (mała litera),
- `'Y'` (wielka litera).

Najpierw policz wynik na kartce, potem sprawdź programem.

Wypisz każdą parę w formacie `znak -> wynik`. Tylko jedna litera naraz, bez pętli.

**Oczekiwany wynik:**

```text
y -> b
Y -> B
```

<details><summary>Podpowiedź</summary>

Wzór z teorii („Zawijanie”) działa dla obu liter. Pamiętaj, że małą literę liczysz od `'a'`, a wielką od `'A'`.

</details>

---

## Sprawdzian

Napisz z pamięci w nowym pliku `R3_spr.java`:

Dla napisu `"Marek ma psa"` zamień na `char[]`, w pętli zamień każdą małą literę `a` na wielką `A`, złóż napis z powrotem i wypisz go.

Powinno wypisać: `MArek mA psA`

**Lista, którą sprawdzający (Claude) sprawdza w kodzie:**

- [ ] Napis zamieniony na tablicę przez `text.toCharArray()`.
- [ ] Pętla po tablicy z `i < tab.length`, bez nawiasów przy `length`.
- [ ] Zamiana w pętli warunkiem na znak, np. `if (tab[i] == 'a') tab[i] = 'A';`.
- [ ] Wynik składany przez `new String(tab)`, a nie przez `+=` w pętli.
- [ ] Program się kompiluje i wypisuje `MArek mA psA`.

---

## Typowe błędy

- **`'A' + 1` w `println`** wypisze `66`, a nie `B`. Dodawanie do znaku daje liczbę. Potrzebujesz `(char)`.
- **`tab[i] = tab[i] - 32;` bez `(char)`** - kompilator: `incompatible types: possible lossy conversion from int to char`. Wynik działania na znakach to `int`.
- **`text.length`** bez nawiasów dla napisu albo **`tab.length()`** dla tablicy - kompilator: `cannot find symbol`.
- **`System.out.println("" + tab);`** wypisze coś w rodzaju `[C@1b6d3586`, czyli adres w pamięci, a nie napis. Składaj przez `new String(tab)`.
- **Próba zmiany napisu:** `text.charAt(0) = 'X';` się nie skompiluje, bo do wyniku metody nie da się nic przypisać (kompilator: `unexpected type`). Osobna sprawa: napisu i tak nie da się zmienić w miejscu (jest niezmienny), dlatego zamieniasz go na tablicę.

---
