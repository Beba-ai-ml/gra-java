# Rozdział 7. Porównywanie tekstów

**Po co ci to w projekcie:** wyszukiwanie (rozdział 8) porównuje teksty przez `equals`, a sortowanie (rozdział 9) przez `compareTo`.

## Teoria

### Dwa różne pytania

Java ma dwa sposoby porównania i pytają o coś innego:

- `==` pyta: czy to TA SAMA kartka? Wyobraź sobie dwie identyczne kartki z napisem „Ala”. Na każdej jest to samo, ale to dwie różne kartki. `==` patrzy na kartkę, nie na napis.
- `equals` pyta: czy napis na obu kartkach jest taki sam? To jest pytanie, którego potrzebujesz.

```java
String a = "ala";
String b = "ala";
System.out.println(a == b);       // true, bo oba napisy sa z kodu
System.out.println(a.equals(b));  // true
```

Uwaga: `"ala" == "ala"` daje `true` tylko dlatego, że Java dla napisów wpisanych w kodzie trzyma jeden egzemplarz. Tekst wczytany z klawiatury przez `nextLine()` to prawie zawsze nowa kartka. Wtedy `==` daje `false`, nawet gdy wpisałeś dokładnie to samo. Są wyjątki (np. dwa puste napisy potrafią dać `true`), więc na `==` nie możesz polegać w żadną stronę. Dlatego do tekstów zawsze używaj `equals`.

### compareTo: co znaczy wynik

`a.compareTo(b)` zwraca liczbę:

- ujemną, gdy `a` jest wcześniej w kolejności,
- `0`, gdy teksty są identyczne,
- dodatnią, gdy `a` jest później.

Java idzie po literach od lewej strony. Pierwsza litera, która się różni, rozstrzyga. Wynik to różnica kodów tych liter (kod każdej litery to jej numer z tabeli ASCII, rozdział 3).

Kody, które warto znać:

| litera | kod |
|---|---|
| `A` | 65 |
| `Z` | 90 |
| `a` | 97 |
| `b` | 98 |
| `z` | 122 |

Tabela wyników (liczby wypisał prawdziwy program):

| a | b | wynik | dlaczego |
|---|---|---|---|
| `"Zupa"` | `"bank"` | -8 | `'Z'` = 90, `'b'` = 98, 90 - 98 = -8 |
| `"bank"` | `"zupa"` | -24 | `'b'` = 98, `'z'` = 122, 98 - 122 = -24 |
| `"Ala"` | `"ala"` | -32 | `'A'` = 65, `'a'` = 97, 65 - 97 = -32 |
| `"abc"` | `"abd"` | -1 | dwie pierwsze litery są takie same, `'c'` = 99, `'d'` = 100, 99 - 100 = -1 |
| `"kot"` | `"kot"` | 0 | identyczne |
| `"kot"` | `"kotek"` | -2 | pierwsze trzy litery są takie same, a `"kot"` jest krótszy: 3 - 5 = -2 |

Dlaczego `"Zupa"` jest przed `"bank"`? Bo wielkie litery mają mniejsze kody niż małe. `'Z'` (90) jest mniejsze niż `'b'` (98), więc `"Zupa"` idzie pierwsze, mimo że `Z` jest w alfabecie na końcu. Podobnie `"Ala"` jest przed `"ala"`.

Jak używać wyniku:

```java
if (a.compareTo(b) < 0) {
    System.out.println(a + " jest przed " + b);
}
```

Nie czekaj na wynik `-1` albo `1`. Java zwraca różnicę, czyli może to być `-8` albo `-24`. Sprawdzaj znak: `< 0`, `== 0`, `> 0`.

## Ćwiczenia

### Ćwiczenie 1 (plik `R7_cw1.java`)

Napisz program, który wczyta dwa słowa przez `nextLine()` i porówna je dwa razy: przez `==` i przez `equals`. Wypisz wyniki.
Do wczytywania potrzebujesz dwóch linijek: `import java.util.Scanner;` nad klasą i `Scanner skan = new Scanner(System.in);` w `main` (tak jak w Twoim `aisd_lab02`). Więcej o `Scanner` w rozdziale 10.

Wypisz dokładnie te linie (prompty przez `println`):

- `Podaj pierwsze slowo:`
- `Podaj drugie slowo:`
- `==: wynik`
- `equals: wynik`

Wpisz dwa razy to samo słowo: `Ala`, potem `Ala`.

Oczekiwany wynik na ekranie:

```
Podaj pierwsze slowo:
Podaj drugie slowo:
==: false
equals: true
```

Blok nie zawiera tekstu, który wpisujesz w konsoli. Zobacz, że `==` daje `false`, mimo że słowa są takie same.

<details>
<summary>Podpowiedź</summary>

`==` pyta o kartkę, a nie o napis. Dwa wczytania z klawiatury to dwie różne kartki, więc `==` mówi `false`. Wynik dla `equals` zapisz w `System.out.println` jako `a.equals(b)`.

</details>

### Ćwiczenie 2 (plik `R7_cw2.java`)

W `main` masz te pary imion: `Adam` i `Zosia`, `Zosia` i `Adam`, `Ola` i `ola`, `Ola` i `Olaf`, `Ola` i `Ola`.

Najpierw na kartce przewidź, czy wynik `compareTo` będzie ujemny, zero czy dodatni. Dopiero potem uruchom program. Wypisz każde porównanie w formacie `pierwsze i drugie: wynik`.

Oczekiwany wynik na ekranie:

```
Adam i Zosia: -25
Zosia i Adam: 25
Ola i ola: -32
Ola i Olaf: -1
Ola i Ola: 0
```

<details>
<summary>Podpowiedź</summary>

Zajrzyj do tabeli z teorii. Dla `Ola` i `ola` pierwsza różniąca się litera to `O` (79) i `o` (111). Dla `Ola` i `Olaf` cały `Ola` jest taki sam, więc krótszy tekst wygrywa.

</details>

## Sprawdzian

Bez komputera, na kartce albo na głos:

1. Wyjaśnij, czemu `"Zupa"` jest przed `"bank"`. Użyj słów „kod litery”.
2. Napisz dwie linijki kodu, które sprawdzą, czy imię `a` jest przed imieniem `b`.

Co sprawdza prowadzący (Claude):

- Wyjaśnienie mówi, że Java porównuje litery od lewej i rozstrzyga pierwsza różnica (`Z` i `b`).
- Podane kody: `'Z'` = 90, `'b'` = 98, więc 90 < 98.
- Wyjaśnienie mówi, że wielkie litery mają mniejsze kody niż małe.
- Kod z punktu 2 używa `compareTo(...) < 0` albo `> 0`, a nie `== -1`.
- Jeśli kod porównuje imiona, używa `equals` lub `compareTo`, a nie `==`.

## Typowe błędy

- **`==` na tekstach.** `if (imie == "Ola")` działa dla napisu z kodu, ale po `nextLine()` zwykle daje `false`. Pisz `imie.equals("Ola")`.
- **Wynik `compareTo` jak `-1`/`1`.** Java zwraca różnicę kodów, np. `-8` albo `-24`. Sprawdzaj znak, nie konkretną wartość.
- **Zapomniane wielkie litery.** `"Ola"` i `"ola"` to różne teksty dla `equals` i dla `compareTo`.
- **Odwrócony znak.** `a.compareTo(b) > 0` znaczy, że `a` jest PO `b`, nie przed.

**Czas na rozdział:** 40-75 min.

---
