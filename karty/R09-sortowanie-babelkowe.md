# Rozdział 9. Sortowanie bąbelkowe

**Po co ci to w projekcie:** opcja [4] w menu sortuje notatki po tytule, a wersja na dwóch listach musi przesuwać tytuły i treści razem (rozdział 6).

## Teoria

### Bąbelki w wodzie

Wyobraź sobie wodę z bąbelkami. Największy bąbelek wypływa na sam koniec. Algorytm działa tak samo:

1. Bierzesz pary sąsiadów: element `j-1` i element `j`.
2. Jeśli stoją w złej kolejności (lewy jest większy), zamieniasz je miejscami.
3. Po jednym pełnym przejściu największa liczba jest już na końcu.
4. Powtarzasz przejścia, aż wszystko stanie na swoim miejscu.

Zamiana dwóch szuflad ma zawsze ten sam kształt (trzy kubki z rozdziału 1):

```java
int tmp = tab[j - 1];
tab[j - 1] = tab[j];
tab[j] = tmp;
```

Wzór pętli (bez gotowca do zadania, to tylko kształt):

```java
for (int i = 0; i < n; i++) {           // przejscia
    for (int j = 1; j < n - i; j++) {   // pary sasiadow
        if (tab[j - 1] > tab[j]) {
            // zamiana jak wyzej
        }
    }
}
```

### Ślad krok po kroku: tablica {5, 1, 4, 2}

Poniżej każdy krok z prawdziwego uruchomienia programu. Liczby w nawiasach to stan tablicy PO kroku.

```
start                            [5 1 4 2]

przejscie i=0
  j=1: 5 > 1 -> zamiana          [1 5 4 2]
  j=2: 5 > 4 -> zamiana          [1 4 5 2]
  j=3: 5 > 2 -> zamiana          [1 4 2 5]
  koniec przejscia 0:            [1 4 2 5]   (5 jest juz na koncu)

przejscie i=1
  j=1: 1 > 4 -> bez zmian        [1 4 2 5]
  j=2: 4 > 2 -> zamiana          [1 2 4 5]
  koniec przejscia 1:            [1 2 4 5]   (4 jest juz na swoim miejscu)

przejscie i=2
  j=1: 1 > 2 -> bez zmian        [1 2 4 5]
  koniec przejscia 2:            [1 2 4 5]

przejscie i=3
  (brak porownan, bo j=1 nie jest mniejsze od n-i = 1)
  koniec przejscia 3:            [1 2 4 5]
```

Trzeba zauważyć jedną rzecz: po przejściu `i=0` na końcu jest już 5, więc przy przejściu `i=1` nie ma sensu sprawdzać ostatniego miejsca. Dlatego wewnętrzna pętla idzie tylko do `n - i`. Ogólnie: po przejściu `i` ostatnie `i` elementów stoi już na swoim miejscu.

Dlaczego `j < n - i`, a nie `j < n`? Obie wersje działają, ale ta druga robi zbędne porównania. Uważaj tylko, żeby nie pisać `tab[j + 1]` przy `j < n`: wtedy ostatni obieg wchodzi poza tablicę.

### O(N^2): dwie pętle jedna w drugiej

Złożoność mówi, ile pracy rośnie, gdy tablica rośnie. Tu są dwie pętle jedna w drugiej: zewnętrzna robi około `n` przejść, a każde przejście ma do `n` porównań. Razem to około `n * n`, stąd zapis O(N^2).

Przykład dla `n = 5`: z `j < n - i` wewnętrzna pętla robi łącznie `4 + 3 + 2 + 1 = 10` porównań. Wersja bez skracania (`j < n` w każdym przejściu) robi `5 * 4 = 20`, czyli dwa razy więcej. Oba wyniki rosną jak `n * n`.

## Ćwiczenia

Sortowanie przez selekcję i przez wstawianie POMIJAMY na 3.0. Ich nie uczymy.

### Ćwiczenie 1 (kartka, bez pliku)

Weź 4 karteczki z liczbami `3`, `6`, `2`, `1`. Sortuj je tak, żeby zamieniać miejscami TYLKO sąsiadów. Zapisz każdy ruch (która para została porównana i czy była zamiana) i stan po każdym przejściu.

Oczekiwany wynik na kartce na koniec: `1 2 3 6`.

Ślad z teorii ma inne liczby. Użyj go tylko jako wzoru, jak zapisywać ruchy.

<details>
<summary>Podpowiedź</summary>

Pierwsze przejście zaczyna się od pierwszej pary od lewej. Po pierwszym przejściu na końcu musi stać największa liczba.

</details>

### Ćwiczenie 2 (plik `R9_cw2.java`)

Napisz metodę `public static void sortuj(int tab[])`, która sortuje tablicę bąbelkiem. W metodzie, po każdym przejściu, wypisz linię w formacie `po przejsciu NR: liczby`, z jedną spacją przed każdą liczbą.

W `main` użyj tablicy `int oceny[] = {4, 2, 5, 3, 1};` i wywołaj `sortuj(oceny)`.

Oczekiwany wynik na ekranie:

```
po przejsciu 0: 2 4 3 1 5
po przejsciu 1: 2 3 1 4 5
po przejsciu 2: 2 1 3 4 5
po przejsciu 3: 1 2 3 4 5
po przejsciu 4: 1 2 3 4 5
```

<details>
<summary>Podpowiedź</summary>

Wypisywanie po przejściu wstaw między końcem wewnętrznej pętli `j` a końcem zewnętrznej `i`. Osobna pętla `for` z `k` wypisuje tablicę: `System.out.print(" " + tab[k])`.

</details>

### Ćwiczenie 3 (plik `R9_cw3.java`)

Zrób to samo na `ArrayList<String>`. Lista `imiona` ma kolejno: `Zosia`, `Adam`, `Ola`, `kasia`, `Basia`. Porównuj przez `compareTo` zamiast `>`. Po każdym przejściu wypisz `po przejsciu NR:` i imiona oddzielone spacją.

Oczekiwany wynik na ekranie:

```
po przejsciu 0: Adam Ola Zosia Basia kasia
po przejsciu 1: Adam Ola Basia Zosia kasia
po przejsciu 2: Adam Basia Ola Zosia kasia
po przejsciu 3: Adam Basia Ola Zosia kasia
po przejsciu 4: Adam Basia Ola Zosia kasia
```

Zobacz, że `kasia` (mała litera) stoi na końcu. Wynika to z kodów liter z rozdziału 7.

<details>
<summary>Podpowiedź</summary>

Tam, gdzie w ćwiczeniu 2 było `>`, potrzebujesz `compareTo` i sprawdzenia znaku wyniku (rozdział 7). Zamianę na liście znasz z rozdziału 5.

</details>

### Ćwiczenie 4 (plik `R9_cw4.java`)

Dwie listy równoległe: `imiona` (`Zosia`, `Adam`, `Ola`) i `oceny` (`4`, `5`, `3`). Ten sam numer = ta sama osoba.

Napisz metodę `public static void sortuj(ArrayList<String> imiona, ArrayList<String> oceny)`, która sortuje bąbelkiem po `imiona`. Każda zamiana w `imiona` ma iść RAZEM z zamianą w `oceny`. Metoda nic nie wypisuje.

W `main` zbuduj obie listy, wywołaj `sortuj(imiona, oceny)`, a potem wypisz `[NR] imie: ocena` dla każdego numeru.

Oczekiwany wynik na ekranie:

```
[0] Adam: 5
[1] Ola: 3
[2] Zosia: 4
```

<details>
<summary>Podpowiedź</summary>

W tym samym `if`, po zamianie imion, zamień oceny dla tych samych indeksów `j - 1` i `j`. Jeśli zapomnisz o drugiej liście, dane się rozjadą.

</details>

### Ćwiczenie 5 (plik `R9_cw5.java`): czytanie starego kodu

W `aisd_lab03` znajdź zakomentowany fragment `// sort babelkowe`. Przepisz go do `main` w pliku `R9_cw5.java` i odkomentuj. Przed nim zadeklaruj tablicę `int tab[] = {5, 1, 4, 2};` i dopisz `int a = tab.length;` (w starym pliku `a` było długością tablicy, tu bez tej linii kompilator zgłosi `cannot find symbol`). Na końcu wypisz liczby w jednej linii z `System.out.print(tab[i] + " ")` i nową linią.

Porównaj go z własną wersją z ćwiczenia 2. Zapisz na kartce, ile porównań robi każda. Stara wersja ma `j < a` (wszystkie pary w każdym przejściu), Twoja `j < n - i`.

Oczekiwany wynik na ekranie (po `5` jest spacja na końcu linii):

```
1 2 4 5 
```

<details>
<summary>Podpowiedź</summary>

Policz osobno, ile par (`j`) sprawdza każde przejście (`i`), i zsumuj. Zrób to raz dla `j < a`, raz dla `j < n - i`.

</details>

## Sprawdzian

Z pamięci:

1. Napisz wersję z ćwiczenia 3 (`ArrayList<String>` z `compareTo`).
2. Na kartce rozpisz jedno przejście dla `Zosia`, `Adam`, `Ola` (każdą parę, każdą zamianę).

Co sprawdza prowadzący (Claude):

- Pętla zewnętrzna to `i < n`, a wewnętrzna `j = 1` i `j < n - i`.
- Porównanie to `compareTo(...) > 0`, a zamiana idzie przez `tmp` z `get` i `set` (nie przez `=` na samej liście).
- Indeksy to `j - 1` i `j`, nie `j + 1`.
- Rozpisanie kartki zgadza się z tym, co program wypisuje po każdym przejściu.
- Wyjaśnienie, po co `n - i`: po przejściu `i` ostatnie `i` elementów jest już na miejscu.

## Typowe błędy

- **`j < n` z `tab[j + 1]`.** Ostatni obieg czyta `tab[n]` i wywala `ArrayIndexOutOfBoundsException`. Porównuj `tab[j - 1]` z `tab[j]`.
- **Zamiana tylko w jednej liście.** Imiona się posortują, oceny zostaną na miejscu. Każda ocena trafia do złej osoby (rozdział 6).
- **`>` na tekstach.** `imiona.get(j - 1) > imiona.get(j)` się nie kompiluje. Dla tekstów jest `compareTo`.
- **Zapomniany `tmp`.** Jeśli zrobisz `tab[j - 1] = tab[j]; tab[j] = tab[j - 1];`, obie liczby będą takie same i dane zginą.

**Czas na rozdział:** 2-4 h (rozłóż na dwa dni: teoria i ćwiczenia 1-3, potem 4-5 i sprawdzian).

---
