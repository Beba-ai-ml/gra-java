# Rozdział 5. ArrayList po numerach

**Po co ci to w projekcie:** notatki trzymamy w `ArrayList<String>`, a nie w tablicy, bo nie wiemy z góry, ile ich będzie. Musisz więc umieć pobrać, zmienić i policzyć elementy po numerze.

**Pliki:** wszystkie ćwiczenia w pakiecie `nauka` w IntelliJ, każde w osobnym pliku.

## Teoria

### Tablica kontra ArrayList

Tablica ma stały rozmiar. `ArrayList` rośnie, gdy dokładasz elementy. Obie rzeczy działają na numerach (indeksach), ale zapisuje się je inaczej. Tę tabelę trzeba znać na pamięć:

| tablica | ArrayList |
|---|---|
| `t[i]` | `lista.get(i)` |
| `t[i] = x` | `lista.set(i, x)` |
| `t.length` | `lista.size()` |
| stały rozmiar | `lista.add(x)` dokłada na koniec |

### Jak wygląda lista

Każde pudełko to jeden element. Numer (indeks) piszemy nad nim i zaczynamy od 0:

```
 indeks:     [0]          [1]           [2]
           +-------+    +--------+    +--------+
 lista:    | "Ola" |    | "Piotr"|    | "Kasia"|
           +-------+    +--------+    +--------+
 rozmiar = 3 (size), ostatni numer = 2 (size - 1)
```

### Przykład: pobieranie, zmiana i rozmiar

**Plik:** `R5_przyklad.java` (do przeczytania; możesz wkleić do pakietu `nauka` i uruchomić)

W nawiasach `< >` piszesz, co leży w pudełkach. Dla liczb całkowitych piszesz `Integer`, a nie `int`, bo lista nie przyjmuje słowa `int`. Poza tym liczby działają tak samo: `add(3)`, `get(1)`. Dla tekstów piszesz `String`.

```java
package nauka;

import java.util.ArrayList;

public class R5_przyklad {
    public static void main(String[] args) {
        ArrayList<Integer> oceny = new ArrayList<>();
        oceny.add(3);
        oceny.add(5);
        oceny.add(4);
        System.out.println(oceny.get(1));    // element o numerze 1
        oceny.set(1, 2);                     // zmien element 1 na 2
        System.out.println(oceny.get(1));
        System.out.println(oceny.size());    // ile jest elementow
    }
}
```

**Oczekiwany wynik:**

```text
5
2
3
```

### Przekazanie listy do metody

Lista przekazana do metody to TA SAMA lista, a nie kopia. Zmiana w metodzie jest widoczna w `main`. Tablica zachowuje się tak samo.

**Plik:** `R5_przyklad_metoda.java`

```java
package nauka;

import java.util.ArrayList;

public class R5_przyklad_metoda {
    public static void dopisz(ArrayList<String> lista) {
        lista.add("Ania");
    }

    public static void main(String[] args) {
        ArrayList<String> imiona = new ArrayList<>();
        dopisz(imiona);
        System.out.println(imiona.size());
        System.out.println(imiona.get(0));
    }
}
```

**Oczekiwany wynik:**

```text
1
Ania
```

## Ćwiczenia

### Ćwiczenie 1

Zrób listę trzech imion: `"Ola"`, `"Piotr"`, `"Kasia"`. Wypisz je z numerami. Użyj pętli po `i` (od 0 do `size()`), a nie `for-each`.

**Plik:** `R5_cw1.java`

**Oczekiwany wynik:**

```text
0: Ola
1: Piotr
2: Kasia
```

<details>
<summary>Podpowiedź</summary>

Warunek pętli to `i < imiona.size()`, nie `<=`. Wypisujesz `i` i `imiona.get(i)` w jednej linijce, z napisem `": "` między nimi.

</details>

### Ćwiczenie 2

Zamień miejscami element 0 i element 2 przez `get`, `set` i pomocniczą zmienną `tmp`. Najpierw wypisz listę przed zamianą, potem po niej.

**Plik:** `R5_cw2.java`

**Oczekiwany wynik:**

```text
PRZED:
0: Ola
1: Piotr
2: Kasia
PO:
0: Kasia
1: Piotr
2: Ola
```

<details>
<summary>Podpowiedź</summary>

Najpierw zapamiętaj wartość z pozycji 0 w `tmp`. Potem wstaw na pozycję 0 wartość z pozycji 2. Na końcu wstaw `tmp` na pozycję 2. Gdy pominiesz `tmp`, obie pozycje dostaną tę samą wartość.

</details>

### Ćwiczenie 3

Napisz metodę `public static void dodajImie(ArrayList<String> lista, String imie)`, która dokłada imię na koniec listy. W `main` stwórz pustą listę, dodaj do niej dwa imiona przez tę metodę i sprawdź, że lista urosła.

**Plik:** `R5_cw3.java`

**Oczekiwany wynik:**

```text
Rozmiar: 2
0: Ola
1: Piotr
```

<details>
<summary>Podpowiedź</summary>

Zajrzyj do tabeli z teorii: która metoda dokłada element na koniec? Pętla w `main` jest taka jak w ćwiczeniu 1.

</details>

## Sprawdzian

Napisz z pamięci zamianę z ćwiczenia 1.4 (pierwszy i ostatni element), ale na `ArrayList` zamiast tablicy. Czyli: lista z trzema imionami, zamiana pierwszego i ostatniego elementu przez `get`, `set` i `tmp`, wypisanie wyniku pętlą po `i`. Bez zaglądania do kodu.

**Checklista, którą sprawdzi Claude:**

- [ ] Brak `lista[i]` i `lista.length`. Są `get(i)`, `set(i, x)` i `size()`.
- [ ] Zamiana robi się przez `tmp`, w trzech krokach: `tmp = get(0)`, `set(0, get(2))`, `set(2, tmp)` (zamiast `2` może być `size() - 1`).
- [ ] Pętla idzie `i < lista.size()`, a nie `<=`.
- [ ] Program się kompiluje, a wypisanie pokazuje imiona pierwsze i ostatnie zamienione miejscami.
- [ ] `import java.util.ArrayList;` jest na górze pliku.

## Typowe błędy

- **`lista.length` zamiast `lista.size()`.** `ArrayList` nie ma pola `length`. Kompilator pisze `cannot find symbol`.
- **`lista[i]` zamiast `lista.get(i)`.** Nawiasy kwadratowe działają tylko na tablicach.
- **`i <= lista.size()`.** Ostatni ważny numer to `size() - 1`. Przy 3 elementach `get(3)` daje `IndexOutOfBoundsException: Index 3 out of bounds for length 3`.
- **`set` na pustej liście.** `set(0, x)` podmienia istniejący element, nie dokłada nowego. Najpierw `add`, potem `set`.
- **Zamiana bez `tmp`.** `set(0, get(2))` i `set(2, get(0))` daje dwa razy tę samą wartość, bo pierwszy `set` już nadpisał element 0.

**Czas:** ok. 1-2 godziny.

---
