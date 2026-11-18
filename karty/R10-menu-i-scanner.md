# Rozdział 10. Menu w pętli i `Scanner`

**Czas:** widełki 2-3 h (nie obietnica, idź swoim tempem).
**Pakiet w IntelliJ:** `nauka`. Każde ćwiczenie w osobnym pliku, nazwa pliku = nazwa klasy.

## Po co ci to w projekcie

Całe „Kryptograficzne Archiwum” to jedno wielkie menu. Program wypisuje opcje, czyta numer,
robi to, co wybrałeś, i wraca do menu, aż wpiszesz `0`. Bez tego szkieletu nie ma gdzie
dopisać dodawania notatek, wyświetlania, wyszukiwania i sortowania.

## Teoria

### Pętla `do-while` i `switch`

Menu musi się wypisać przynajmniej raz, zanim cokolwiek wybierzesz. Dlatego używamy
`do-while`: warunek sprawdzany jest NA KOŃCU, więc ciało wykona się zawsze przynajmniej raz.

Szkielet (bez `main`, uzupełnij sam w ćwiczeniu 1):

```java
import java.util.Scanner; // na samej gorze pliku, nad klasa

Scanner skan = new Scanner(System.in); // w main, raz na caly program
int wybor;

do {
    menu();                    // wypisuje opcje
    wybor = skan.nextInt();    // czyta numer

    switch (wybor) {
        case 1:
            // ...
            break;             // bez break program "wpadnie" w kolejny case
        case 0:
            System.out.println("Koniec programu.");
            break;
        default:
            System.out.println("Nie ma takiej opcji.");
    }
} while (wybor != 0);          // petla konczy sie, gdy wybrano 0
```

Trzy rzeczy, które musisz zapamiętać:
- `menu()` tylko WYPISUJE opcje. Nic nie wybiera. Wybór robi `skan.nextInt()`.
- Każdy `case` kończy się `break`. Bez niego wykona się też kod z następnego `case`.
- Warunek `while (wybor != 0);` ma średnik na końcu. Bez `while` pętli nie ma.

### Pułapka: `nextInt()` zostawia Enter

`nextInt()` czyta TYLKO liczbę. Enter, który wcisnąłeś po liczbie, zostaje w buforze
(to jest kolejka tekstu, który program jeszcze nie przeczytał). Następne `nextLine()`
czyta wszystko do końca linii, a koniec linii to właśnie ten zostawiony Enter.
Efekt: `nextLine()` zwraca pusty napis i nie czeka na Ciebie.

Wpisujesz `1`, Enter, potem `Ala`, Enter. Zobacz, co dzieje się w buforze:

```
Krok 1: wpisujesz "1" i Enter
   bufor: [ 1 | \n ]

Krok 2: nextInt() czyta "1"
   bufor: [   | \n ]        <- Enter NADAL tam jest

Krok 3: nextLine() czyta do konca linii
   zwraca ""  (pusty napis), bufor pusty
   program NIE czeka na "Ala" - od razu idzie dalej

Krok 4: dopiero teraz "Ala" trafia do bufora, ale juz nie do imienia
```

Lekarstwo: zaraz po `nextInt()` jedno puste `skan.nextLine()`. To zjada zostawiony Enter.

```java
wybor = skan.nextInt();
skan.nextLine(); // zjada enter po liczbie
```

Menu wypisuje osobna metoda `menu()`, a `main` tylko steruje pętlą.

## Ćwiczenia

Dla każdego ćwiczenia: najpierw napisz kod sam, potem odpal go w IntelliJ, wpisz w konsoli
po kolei to, co podaje „Wejście”, i porównaj ekran z oczekiwanym.

### Ćwiczenie 1. Menu z trzema opcjami (`R10_cw1.java`)

Napisz program z menu:
- `[1]`, `[2]`, `[3]` wypisują „wybrales 1”, „wybrales 2”, „wybrales 3”,
- `[0]` kończy program („Koniec programu.”),
- inny numer wypisuje „Nie ma takiej opcji.”.

Metoda `menu()` wypisuje cztery linie opcji i na końcu `Wybierz: ` (bez przejścia do nowej linii).
Użyj szkieletu z teorii i pamiętaj o lekarstwie po `nextInt()`.

**Wejście:** `1`, `2`, `3`, `7`, `0` (każde w osobnej linii)

**Oczekiwany ekran (dokładnie tak):**

```
=== MENU ===
[1] Opcja pierwsza
[2] Opcja druga
[3] Opcja trzecia
[0] Wyjscie
Wybierz: wybrales 1
=== MENU ===
[1] Opcja pierwsza
[2] Opcja druga
[3] Opcja trzecia
[0] Wyjscie
Wybierz: wybrales 2
=== MENU ===
[1] Opcja pierwsza
[2] Opcja druga
[3] Opcja trzecia
[0] Wyjscie
Wybierz: wybrales 3
=== MENU ===
[1] Opcja pierwsza
[2] Opcja druga
[3] Opcja trzecia
[0] Wyjscie
Wybierz: Nie ma takiej opcji.
=== MENU ===
[1] Opcja pierwsza
[2] Opcja druga
[3] Opcja trzecia
[0] Wyjscie
Wybierz: Koniec programu.
```

**Uwaga o wyglądzie:** oczekiwany ekran w karcie NIE pokazuje Twoich wpisów, dlatego
„wybrales 1” stoi zaraz za „Wybierz: ”. W konsoli IntelliJ wpisane przez Ciebie `1` i Enter
pojawią się po „Wybierz: ”, więc „wybrales 1” zobaczysz w nowej linii. To jest oczekiwane.

<details>
<summary>Podpowiedź</summary>

- Jeden `switch (wybor)` z pięcioma `case` (1, 2, 3, 0 i `default`).
- Każdy `case` kończy się `break`.
- Warunek pętli: `while (wybor != 0);`.
- Zapomniałeś o `skan.nextLine()` po `nextInt()`? Wtedy zobaczysz błąd w ćwiczeniu 2a.
</details>

### Ćwiczenie 2a. Imię BEZ lekarstwa (`R10_cw2a.java`)

Najpierw zobacz błąd. Napisz program:
- opcja `[1]` pyta „Podaj imie: ” i czyta imię przez `skan.nextLine()`, wypisuje `Witaj, [imie]`,
- opcja `[0]` kończy program.

Nie dodawaj jeszcze żadnego `nextLine()` po `nextInt()`. Wypisz wynik i zanotuj, co się dzieje.

**Wejście:** `1`, `Ala`, `0` (każde w osobnej linii)

**Oczekiwany ekran (to jest BŁĄD, który masz zobaczyć):**

```
=== MENU ===
[1] Podaj imie
[0] Wyjscie
Wybierz: Podaj imie: Witaj, []
=== MENU ===
[1] Podaj imie
[0] Wyjscie
Wybierz: Exception in thread "main" java.util.InputMismatchException
	at java.base/java.util.Scanner.throwFor(Scanner.java:947)
	...
```

Co widzisz, krok po kroku:
1. „Podaj imie: ” i od razu `Witaj, []`. Imię jest puste, bo `nextLine()` zjadł zostawiony Enter.
2. Menu wraca i program czeka na numer. Dostaje `Ala`, a `nextInt()` nie umie zamienić tekstu na liczbę.
3. Program pada z `InputMismatchException`. Do `0` nigdy nie dojdzie.

Linie `at java.base...` i `at R10_cw2a.main(...)` to ślad błędu. Ważna jest pierwsza linia z nazwą wyjątku. Numer linii w nawiasie (np. `Scanner.java:947`) zależy od wersji Javy i u Ciebie może być inny.

<details>
<summary>Podpowiedź</summary>

Porównaj z pułapką z teorii: co zostaje w buforze po `nextInt()`? Kto to zjada?
</details>

### Ćwiczenie 2b. Imię Z lekarstwem (`R10_cw2b.java`)

Ta sama wersja co 2a, ale dodaj lekarstwo po `nextInt()` i sprawdź, że imię jest już poprawne.

**Wejście:** `1`, `Ala`, `0` (każde w osobnej linii)

**Oczekiwany ekran:**

```
=== MENU ===
[1] Podaj imie
[0] Wyjscie
Wybierz: Podaj imie: Witaj, [Ala]
=== MENU ===
[1] Podaj imie
[0] Wyjscie
Wybierz: Koniec programu.
```

Dopisz jeszcze: `Ala` jest pokazane w nawiasach, czyli imię nie ma spacji ani pustego napisu.

<details>
<summary>Podpowiedź</summary>

Jedna linia: `skan.nextLine();` zaraz po `wybor = skan.nextInt();`.
</details>

## Sprawdzian

Zamknij notatki. Z pamięci, w 5 minut, napisz szkielet menu:
- `do-while` z `menu()`, `nextInt()`, lekarstwem, `switch` z `break` i pętlą `while (wybor != 0);`,
- i na głos powiedz, dlaczego `nextLine()` po `nextInt()` zwraca pusty napis.

**Kontrola (Claude sprawdzi w kodzie):**
- [ ] `menu()` tylko wypisuje, nie czyta.
- [ ] Po `nextInt()` stoi `skan.nextLine();` (lekarstwo), przed czytaniem tekstu.
- [ ] Każdy `case` kończy się `break`, jest `default`.
- [ ] Pętla kończy się na `wybor != 0`, a `0` jest obsłużone.
- [ ] Program bez błędu przechodzi wejście `1`, `Ala`, `0` i pokazuje `Witaj, [Ala]`.

## Typowe błędy

- **Brak `break`** w `case`: program wykonuje kilka `case` pod rząd, bo nie zatrzymał się na `break`.
- **Średnik po `while (wybor != 0)` w złym miejscu** albo brak `while`: pętla `do` nie działa.
- **`nextLine()` zaraz po `nextInt()` bez lekarstwa**: pusty napis, program „przeskakuje” pytanie.
- **Litery wpisane tam, gdzie `nextInt()`**: `InputMismatchException`.
- **`menu()` wybiera opcję** zamiast tylko ją wypisać: mylisz wypisywanie z czytaniem.
- **Pomylenie `=` z `==`** w warunku: `while (wybor = 0)` nie kompiluje się, bo `int` nie jest `boolean`.
