# Rozdział 4. Szyfr Cezara jako dwie metody

**Po co ci to w projekcie:** wymaganie projektu. Szyfr Cezara ma być zrobiony jako dwie metody: `szyfruj` (zakodowanie tekstu) i `deszyfruj` (odkodowanie).

**Pliki:** wszystkie ćwiczenia wrzucasz do pakietu `nauka` w IntelliJ, każde w osobnym pliku. Ćwiczenia 1 i 2 robisz na kartce, bez pliku.

## Teoria

### Szyfr Cezara w jednym zdaniu

Każdą literę przesuwasz w alfabecie o stałą liczbę miejsc. Przy szyfrze 3: A zmienia się w D, B w E, C w F.

### Krok 1: litery to numery

Komputer nie widzi liter, widzi numery. Liczymy od zera, tak jak przy tablicy:

```
litera:  A  B  C  D  E  F  G  H  I  J  K  L  M
numer:   0  1  2  3  4  5  6  7  8  9  10 11 12

litera:  N  O  P  Q  R  S  T  U  V  W  X  Y  Z
numer:   13 14 15 16 17 18 19 20 21 22 23 24 25
```

Przesunięcie to dodawanie do numeru: `nowy numer = stary numer + szyfr`. Z `A` (0) plus 3 wychodzi `3`, a to `D`.

### Krok 2: zawijanie na końcu alfabetu

Co zrobić z `X`, `Y`, `Z`? Przy szyfrze 3 numer wychodzi poza 25:

| litera | numer | + 3 | `% 26` | nowa litera |
|---|---|---|---|---|
| x | 23 | 26 | 0 | a |
| y | 24 | 27 | 1 | b |
| z | 25 | 28 | 2 | c |

Dzielenie z resztą przez 26 (`% 26`) zawija numer z powrotem na początek. Bez tego `x` zamieniłoby się w `{` (kod 123), a nie w literę.

```
   ... X Y Z | A B C ...
   23 24 25 |  0  1  2
          ^
          Z + 1 wraca na A (numer 26 zamienia się w 0)
```

### Krok 3: w kodzie

`'A'` i `'a'` to znaki, a w Javie znak da się odjąć od znaku. `'x' - 'a'` daje `23`. Potem `(char)` zamienia numer z powrotem na literę:

**Plik:** `R4_przyklad.java` (do przeczytania; możesz go wkleić do pakietu `nauka` i uruchomić)

```java
package nauka;

public class R4_przyklad {
    public static void main(String[] args) {
        char c = 'x';
        int numer = c - 'a';                 // x to 23
        numer = (numer + 3) % 26;            // 26 % 26 = 0
        char nowa = (char) ('a' + numer);    // 0 to 'a'
        System.out.println(c + " -> " + nowa);
    }
}
```

**Oczekiwany wynik:**

```text
x -> a
```

### Krok 4: deszyfrowanie i pułapka minusa

Deszyfrowanie to przesunięcie w drugą stronę: `- szyfr`. Tu wpada pułapka. W Javie `%` przy ujemnej liczbie daje wynik ujemny, więc `-3 % 26` to `-3`, a nie `23`. Dlatego dodajemy `26` przed `%`.

**Plik:** `R4_przyklad_minus.java`

```java
package nauka;

public class R4_przyklad_minus {
    public static void main(String[] args) {
        int a = 0 - 3;                        // litera a (0) przesunieta o 3 w lewo
        System.out.println(a % 26);           // minus zostaje
        System.out.println((a + 26) % 26);    // poprawny wynik
    }
}
```

**Oczekiwany wynik:**

```text
-3
23
```

Numer 23 to `x`. Czyli litera `a` odszyfrowana przesunięciem 3 daje `x`.

## Ćwiczenia

### Ćwiczenie 1 (na kartce)

Zaszyfruj „Ala" przesunięciem 3. Licz numery liter ręcznie: zapisz numer każdej litery, dodaj 3, zamień wynik z powrotem na literę.

**Plik:** brak, liczysz na kartce. Sprawdzisz to w ćwiczeniu 4.

<details>
<summary>Podpowiedź</summary>

Wielkie i małe litery liczysz osobno. `A` ma numer 0 w wielkich literach, `a` ma numer 0 w małych. Zapisz tabelkę z numerami przed sobą i odczytuj z niej.

</details>

### Ćwiczenie 2 (na kartce)

Zaszyfruj „xyz" przesunięciem 3. Najpierw policz bez `% 26`: jaki znak wyjdzie? Potem z `% 26`. Zapisz oba wyniki.

**Plik:** brak, liczysz na kartce.

<details>
<summary>Podpowiedź</summary>

Bez `% 26` numer `x` plus 3 to 26. Litera o numerze 26 nie istnieje, więc wyjdzie znak z innego miejsca tablicy ASCII. Sprawdź, jaki znak ma kod 123.

</details>

### Ćwiczenie 3

Napisz metodę `public static String szyfruj(String text, int szyfr)`. Na razie przesuwa TYLKO małe litery (`a`-`z`). Wielkie litery zostają bez zmian. Użyj tablicy znaków `char[]`: `text.toCharArray()` na początku i `new String(znaki)` na końcu. Wywołaj ją w `main` dla trzech tekstów: `"ala"`, `"xyz"` i `"Ala"` (szyfr 3), każdy w osobnej linii.

**Plik:** `R4_cw3.java`

**Oczekiwany wynik:**

```text
dod
abc
Aod
```

<details>
<summary>Podpowiedź</summary>

Pętla po wszystkich znakach (`i < znaki.length`). Dla każdego znaku sprawdź, czy mieści się w przedziale `'a'` do `'z'`. Jeśli tak: odejmij `'a'`, dodaj szyfr, weź resztę z dzielenia przez 26, dodaj `'a'` z powrotem i rzutuj na `char`.

</details>

### Ćwiczenie 4

Dopisz do `szyfruj` obsługę wielkich liter. Ta sama logika, tylko z `'A'`. Znaki, które nie są literami (spacja, cyfry), zostają bez zmian. Wywołaj ją dla `"Ala"`, `"xyz"`, `"XYZ"` i `"Ala ma kota 123"` (szyfr 3).

**Plik:** `R4_cw4.java`

**Oczekiwany wynik:**

```text
Dod
abc
ABC
Dod pd nrwd 123
```

<details>
<summary>Podpowiedź</summary>

Dwa bloki `if` i `else if`, jeden dla małych liter, drugi dla wielkich. Każdy blok liczy numer od swojej bazy (`'a'` albo `'A'`).

</details>

### Ćwiczenie 5

Napisz metodę `public static String deszyfruj(String text, int szyfr)`. Zrób ją tak samo jak `szyfruj`, ale przesuwaj w drugą stronę. Pamiętaj o `+ 26` przed `% 26`. Wywołaj `deszyfruj` dla `"Dod"`, `"abc"`, `"ABC"` i `"Dod pd nrwd 123"` (szyfr 3).

**Plik:** `R4_cw5.java` (w tym pliku są obie metody: `szyfruj` z ćwiczenia 4 i `deszyfruj`)

**Oczekiwany wynik:**

```text
Ala
xyz
XYZ
Ala ma kota 123
```

<details>
<summary>Podpowiedź</summary>

Zmienia się jedna linijka w każdym bloku: zamiast `tmp + szyfr` piszesz `tmp - szyfr + 26`. Reszta jest taka sama.

</details>

### Ćwiczenie 6: test w obie strony

W `main` zaszyfruj tekst `"Ala ma kota xyz XYZ 123"` przesunięciem 3, potem odszyfruj wynik. Wypisz zaszyfrowany tekst, odszyfrowany tekst i czy są równe.

> **Zapowiedź: `equals`.** Dwa teksty porównujesz przez `a.equals(b)`, a nie przez `a == b`. `equals` sprawdza litery po kolei i daje `true` albo `false`.
> Dlaczego `==` tu nie pasuje, dowiesz się w rozdziale 7. Na razie po prostu użyj `equals`.

**Plik:** `R4_cw6.java` (obie metody w pliku, jak w ćwiczeniu 5)

**Oczekiwany wynik:**

```text
Dod pd nrwd abc ABC 123
Ala ma kota xyz XYZ 123
true
```

<details>
<summary>Podpowiedź</summary>

`deszyfruj(szyfruj(x, 3), 3)` musi dać z powrotem `x`. Do porównania dwóch tekstów użyj `equals`, nie `==`.

</details>

### Ćwiczenie 7: czytanie starego kodu

Skopiuj plik `szyfrcezara.java` (z `~/IdeaProjects/pierwszy/src/algorytmika/`) do pakietu `nauka` jako `R4_cw7.java` i zmień nazwę klasy. Pod każdą linijką napisz komentarz `// co robi`. Potem odpowiedz na pytanie: czym ten kod różni się od Twojej wersji z `char[]`?

**Plik:** `R4_cw7.java`

**Oczekiwany wynik:**

```text
Ala ma kota
Dod pd nrwd
```

<details>
<summary>Podpowiedź</summary>

Stary kod idzie po tekście przez `text.charAt(i)` i składa wynik przez `coded += ...`. Twoja wersja idzie po tablicy `znaki[i]` i zmienia ją w miejscu. Zauważ, że `coded += ...` tworzy za każdym razem nowy napis.

</details>

## Sprawdzian

Bez podglądania, z pamięci:

1. Napisz obie metody `szyfruj` i `deszyfruj` (`public static String`, dwa parametry).
2. Wyjaśnij na głos, po co `+ 26` w `deszyfruj`. Co by się stało bez niego dla litery `a`?

**Checklista, którą sprawdzi Claude:**

- [ ] `szyfruj` i `deszyfruj` są `public static String` z parametrami `(String text, int szyfr)`.
- [ ] Tekst jest zamieniany na `char[]` przez `toCharArray()` i składany przez `new String(...)`.
- [ ] Małe i wielkie litery są liczone osobno, od swojej bazy (`'a'` i `'A'`), a zawijanie jest przez `% 26`.
- [ ] `deszyfruj` ma `+ 26` przed `% 26`, a znaki spoza liter zostają bez zmian.
- [ ] Test `deszyfruj(szyfruj(x, 3), 3)` daje `x`, sprawdzony przez `equals`.

## Typowe błędy

- **Brak `+ 26` w deszyfrowaniu.** Dla `a` wychodzi `-3`, a `'a' + (-3)` to znak `^`, nie litera.
- **Pisanie `(char) tmp` zamiast `(char) ('a' + tmp)`.** Dostajesz znaki sterujące zamiast liter, bo numer `3` to nie litera `c`.
- **Liczenie wielkich liter od `'a'`** (albo odwrotnie). Wynik wygląda na przesunięty.
- **`znaki[i] = 'a' + tmp;` bez rzutowania.** Kompilator krzyczy `possible lossy conversion from int to char`.
- **Próba zmiany `String`.** Napisy w Javie się nie zmieniają. Zamieniaj `char[]`, a na końcu zrób z niego `String`.

**Czas:** ok. 2-4 godziny (ćwiczenia 1-2 szybko, 3-5 najwięcej czasu, 7 zależy od tempa czytania).

---
