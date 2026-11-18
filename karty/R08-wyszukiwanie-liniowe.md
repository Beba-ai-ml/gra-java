# Rozdział 8. Wyszukiwanie liniowe

**Po co ci to w projekcie:** opcja [3] w menu szuka notatki po tytule i z wyniku bierze numer do pobrania danych z drugiej listy. W ćwiczeniach tej karty szukasz po imieniu, a w projekcie zrobisz to samo dla tytułów.

## Teoria

### Idziesz po kolei

Wyobraź sobie szukanie skarpetki w szufladzie. Zaczynasz od pierwszej szuflady, potem druga, potem trzecia, aż znajdziesz albo skończą się szuflady. Tak działa wyszukiwanie liniowe: od numeru `0` do końca, każdy element sprawdzasz po kolei.

Gdy trafisz, zwracasz numer od razu przez `return i`. `return` kończy metodę, więc zwraca PIERWSZE pasujące miejsce.

Przykład z różnymi danymi (nie z zadania):

```java
int oceny[] = {3, 5, 4};
int pozycja = -1;          // na start: nie znaleziono
for (int i = 0; i < oceny.length; i++) {
    if (oceny[i] == 4) {
        pozycja = i;
        break;
    }
}
System.out.println(pozycja);  // wypisze 2
```

Dla liczb porównujesz `==`. Dla tekstów (rozdział 7) używasz `equals`.

### Dlaczego -1

Numery szuflad to `0, 1, ..., n-1`. Każdy prawdziwy wynik jest liczbą od `0` do `n-1`. Stąd dwa wnioski:

- `0` NIE może znaczyć „nie ma”, bo `0` to prawidłowe miejsce (pierwsza szuflada).
- `-1` nie jest prawidłowym numerem w tablicy ani w liście, więc nadaje się jako sygnał „nie znaleziono”.

Ten, kto wywołuje metodę, sprawdza `if (wynik == -1)` i wie, że nic nie znaleziono.

## Ćwiczenia

### Ćwiczenie 1 (plik `R8_cw1.java`)

Napisz metodę `public static int szukaj(int t[], int x)`, która przechodzi po tablicy i zwraca numer pierwszego elementu równego `x`. Jeśli takiego nie ma, zwraca `-1`.

W `main` użyj tablicy `int oceny[] = {3, 5, 4, 2, 5};` i wypisz trzy linie w formacie `szukam X: pozycja N` dla `X` = 2, 5 i 6.

Oczekiwany wynik na ekranie:

```
szukam 2: pozycja 3
szukam 5: pozycja 1
szukam 6: pozycja -1
```

Zauważ, że dla `5` wypisało `1`, a nie `4`. To pierwsze trafienie.

<details>
<summary>Podpowiedź</summary>

Pętla `for` przechodzi od `0` do `t.length - 1`. W `if` porównujesz `t[i] == x` i w środku robisz `return i`. Wynik `return -1` stawiasz PO pętli, nie w jej środku.

</details>

### Ćwiczenie 2 (plik `R8_cw2.java`)

To samo co ćwiczenie 1, ale na liście `ArrayList<String>`. Metoda: `public static int szukaj(ArrayList<String> lista, String x)`. W `main` dodaj do listy `imiona`: `Ania`, `Basia`, `Kasia`, `Ola`. Szukaj `Kasia` i `Zosia`. Porównuj przez `equals`, a długość listy bierz z `size()`.

Oczekiwany wynik na ekranie:

```
szukam Kasia: pozycja 2
szukam Zosia: pozycja -1
```

<details>
<summary>Podpowiedź</summary>

Zamiast `t[i]` masz `lista.get(i)`, a zamiast `t.length` masz `lista.size()`. Dla tekstu `lista.get(i).equals(x)`, nie `==`.

</details>

### Ćwiczenie 3 (plik `R8_cw3.java`)

Zbuduj program z dwiema listami równoległymi `ArrayList<String>`: `imiona` (`Ania`, `Basia`, `Kasia`, `Ola`) i `oceny` (`3`, `5`, `4`, `2`). Ten sam numer = ta sama osoba. W pliku ma być metoda `szukaj` z ćwiczenia 2.

W `main` wczytaj imię przez `Scanner` (`nextLine()`):

- jeśli `szukaj` zwróci `-1`, wypisz `nie znaleziono`,
- w przeciwnym razie wypisz `IMIE ma ocene OCENA`, czyli dane z DRUGIEJ listy pod tym samym numerem.

Sprawdź dwa wejścia.

Wejście `Kasia`, oczekiwany wynik:

```
Kasia ma ocene 4
```

Wejście `Zosia`, oczekiwany wynik:

```
nie znaleziono
```

<details>
<summary>Podpowiedź</summary>

Najpierw zapisz wynik w zmiennej `nr`. Potem `imiona.get(nr)` i `oceny.get(nr)`. Dwie listy ruszasz tym samym numerem.

</details>

## Sprawdzian

Z pamięci, bez podglądania:

1. Napisz metodę `szukaj` dla tablicy `int` (ćwiczenie 1).
2. Powiedz na głos, czemu zwraca `-1`, a nie `0`.

Co sprawdza prowadzący (Claude):

- Pętla `for` zaczyna od `0` i sprawdza wszystkie elementy (nie `<=`, bo to wyjście poza tablicę).
- `return i` jest w środku pętli, po pierwszym trafieniu.
- `return -1` jest PO pętli, nie w `else` wewnątrz niej (inaczej szukanie kończy się po pierwszym elemencie).
- Wyjaśnienie: `-1` to poza zakresem numerów `0..n-1`, a `0` jest prawidłową pozycją.
- Dla tekstów używane jest `equals`, nie `==`.

## Typowe błędy

- **`return 0` zamiast `-1`.** Gdy nie ma wyniku, `0` wygląda jak „pierwsza szuflada”. Program wypisze pozycję 0 dla każdego braku.
- **`return -1` w `else` wewnątrz pętli.** Szukanie kończy się po pierwszym elemencie, który nie pasuje.
- **`==` na tekstach.** `lista.get(i) == x` dla wczytanego tekstu zwykle daje `false`, nawet przy tych samych literach (rozdział 7).
- **`i <= t.length`.** Ostatni obieg wchodzi poza tablicę i wywala `ArrayIndexOutOfBoundsException` (rozdział 1).

**Czas na rozdział:** 45-90 min.

---
