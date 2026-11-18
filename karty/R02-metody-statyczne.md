# Rozdział 2. Własne metody statyczne

**Po co ci to w projekcie:** prowadzący wymaga podziału programu na metody (np. `public static String szyfruj(...)`). Każda czynność w projekcie ma być osobną metodą z parametrami i `return`.

**Czas:** 45-90 min

---

## Teoria

### Metoda to nazwany przepis

Metoda to kawałek kodu, który dostał nazwę. Zamiast pisać wszystko w `main`, wołasz go po nazwie. Działa jak przepis w kuchni:

- **parametry** to składniki, które wchodzą do metody,
- **kroki** to kod w środku,
- **`return`** to danie, które metoda oddaje z powrotem.

```java
public static int wieksza(int a, int b) {   // int = jaki wynik oddaje
    if (a > b) {
        return a;                           // oddaj a i wyjdz z metody
    }
    return b;                               // inaczej oddaj b
}
```

Wywołanie z `main` (w tym samym pliku):

```java
int w = wieksza(3, 8);             // w dostaje 8
System.out.println(wieksza(3, 8)); // wypisze 8
```

Co się dzieje przy wywołaniu:

```
main:   int w = wieksza(3, 8);
                      |  |
                      |  a = 3, b = 8      <- wartosci wchodza do metody
                      v
        wieksza:  3 > 8 ? nie, wiec return b
                      |
                      v
        main:   w = 8                      <- return oddaje wynik do main
```

### Zmienne z `main` nie wchodzą do metody

Metoda ma własne szuflady. Zmienna zadeklarowana w `main` jest w `main`, a nie w metodzie:

```
+------------- main -------------+      +------ metoda -------+
|  int x = 5;                    | ---> |  dostaje tylko to,   |
|                                |      |  co dostanie jako    |
|                                | <--- |  parametr; return    |
+--------------------------------+      +----------------------+
```

Dlatego wartość przekazujesz jako parametr:

```java
// NIE skompiluje sie: x nie istnieje w tej metodzie
public static void pokaz() {
    System.out.println(x);
}

// dobrze: wartosc przychodzi jako parametr
public static void pokaz(int x) {
    System.out.println(x);
}
```

Tablica jako parametr to ten sam zestaw szuflad, a nie kopia. Zmiana w metodzie jest widoczna w `main`:

```java
public static void zeruj(int tab[]) {
    tab[0] = 0;         // to ta sama szuflada, co w main
}
```

### `int` vs `void`

| | `int` | `void` |
|---|---|---|
| Co oddaje | liczbę | nic |
| Jak wrócić | `return wynik;` | `return;` albo nic |
| Wywołanie | `int w = f(3);` albo `System.out.println(f(3));` | `f(3);` |
| `System.out.println(f(3))` | działa | błąd `'void' type not allowed here` |

Na razie każdą metodę piszesz jako `public static`. Powód: `main` jest `static`, a z `static` nie wołasz zwykłych metod.

---

## Ćwiczenia

Każde ćwiczenie to osobny plik w pakiecie `nauka`. Najpierw zrób, potem zajrzyj do podpowiedzi. Wynik ma być **dokładnie** taki jak podany.

### Ćwiczenie 2.1: większa z dwóch

Plik: `R2_cw1.java`

Napisz metodę `public static int wieksza(int a, int b)`, która zwraca większą z dwóch liczb. W `main`:

1. wypisz `wieksza(3, 8)`,
2. wypisz `wieksza(-2, -7)`,
3. zapisz `wieksza(10, 4)` do zmiennej `w` i wypisz `w = ...`.

**Oczekiwany wynik:**

```text
8
-2
w = 10
```

<details><summary>Podpowiedź</summary>

Metoda ma `if (a > b)`, w środku `return a;`, a na końcu `return b;`. Wypisywanie robisz w `main`, nie w metodzie.

</details>

---

### Ćwiczenie 2.2: wypisz tablicę metodą

Plik: `R2_cw2.java`

Przenieś ćwiczenie 1.1 do metody. Napisz `public static void wypiszTablice(int tab[])`, która wypisuje każdą szufladę jako `[numer] wartosc`. W `main` zadeklaruj `int tab[] = {4, 7, 1};` i wywołaj metodę.

**Oczekiwany wynik** (taki sam jak w ćwiczeniu 1.1):

```text
[0] 4
[1] 7
[2] 1
```

<details><summary>Podpowiedź</summary>

Metoda nie zwraca nic, więc jest `void`. Wewnątrz masz pętlę po `tab.length`, jak w rozdziale 1. Wywołanie w `main` to po prostu `wypiszTablice(tab);`.

</details>

---

### Ćwiczenie 2.3: numer największego w metodzie

Plik: `R2_cw3.java`

Napisz `public static int indeksMax(int tab[])`, która zwraca **numer** największego elementu. W `main` zadeklaruj `int tab[] = {4, 7, 1, 9, 2};`, zapisz wynik do zmiennej `nr` i wypisz:

- `indeks max: ` i numer,
- `wartosc: ` i wartość pod tym numerem.

**Oczekiwany wynik:**

```text
indeks max: 3
wartosc: 9
```

<details><summary>Podpowiedź</summary>

Ta sama pętla co w ćwiczeniu 1.3, tylko w metodzie. Na końcu `return nr;`. W `main` wartość to `tab[nr]`.

</details>

---

### Ćwiczenie 2.4: pierwiastek w metodzie (Heron)

Plik: `R2_cw4.java`

W `aisd_lab02` cały algorytm siedzi w `main` i czyta liczbę przez `skan.nextDouble()`. Przerób go na metodę:

- `public static double pierwiastek(double l)`,
- ten sam algorytm: `a = l`, `b = 1`, `eps = 0.0000001`, pętla `while (|a - b| > eps)` z `a = (a + b) / 2` i `b = l / a`, na końcu wynik `(a + b) / 2`,
- bez `Scanner`. Liczba przychodzi jako parametr `l`.

W `main` wypisz `pierwiastek(16)`, a potem `pierwiastek(2)`.

**Oczekiwany wynik** (jeśli ostatnie cyfry się różnią, sprawdź, czy wzór i `eps` są takie same jak w `aisd_lab02`):

```text
4.0
1.414213562373095
```

<details><summary>Podpowiedź</summary>

Skopiuj pętlę z `aisd_lab02` do metody. Zamiast `skan.nextDouble()` masz parametr `l`. Zamiast `println` na końcu - `return`. `abs` w `aisd_lab02` pochodzi z `import static java.lang.Math.*`, tu pisz `Math.abs`.

</details>

---

## Sprawdzian

Napisz z pamięci w nowym pliku `R2_spr.java`:

1. metodę `public static int potroj(int x)`, która zwraca trzykrotność `x`,
2. metodę `public static void witaj(String imie)`, która wypisuje `witaj ` i imię,
3. w `main`: `potroj(5)` wypisane na ekranie (ma wyjść `15`) i `witaj("Ola")` (ma wyjść `witaj Ola`),
4. ustnie: czym różni się `void` od `int`?

**Lista, którą sprawdzający (Claude) sprawdza w kodzie:**

- [ ] Obie metody są `public static`, a typ zwracany zgadza się z tym, co naprawdę zwracają (`int` ma `return` z liczbą, `void` nie zwraca wartości).
- [ ] `potroj` liczy z parametru `x`, a nie ze zmiennej spoza metody.
- [ ] W `main` wynik `potroj` idzie do `println` albo do zmiennej, a wywołanie `witaj` nie ma `int ... =`.
- [ ] Program się kompiluje i wypisuje `15` i `witaj Ola`.
- [ ] Odpowiedź ustna: `int` oddaje wartość, którą można wypisać albo przypisać. `void` nic nie oddaje, więc nie da się go wypisać.

---

## Typowe błędy

- **Brak `return` w metodzie `int`** - kompilator: `missing return statement`. Dotyczy też `if` bez `else`: jeśli warunek nie wejdzie, nie ma czym oddać wyniku.
- **`System.out.println(witaj("Ola"));`** - metoda `void` nic nie oddaje. Kompilator: `'void' type not allowed here`.
- **Zmienna z `main` użyta w metodzie** bez parametru - kompilator: `cannot find symbol`.
- **Brak `static`** przy metodzie wołanej z `main` - kompilator dla metody `int wieksza(int a, int b)` bez `static`: `non-static method wieksza(int,int) cannot be referenced from a static context`.
- **Wypisywanie zamiast `return`:** metoda drukuje wynik, ale nic nie oddaje, więc `int w = f(...)` nie dostanie liczby.

---
