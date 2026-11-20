# Rozdział 16. Pliki i wyjątki

**Po co ci to w projekcie:** na ocenę 5.0 archiwum musi przetrwać zamknięcie programu. Zapisujesz obie listy do pliku `archiwum.txt` (linia `tytul|zaszyfrowana_tresc`) i wczytujesz je na starcie `main`. Gdy pliku nie ma albo nie da się go zapisać, program ma napisać po ludzku, co się stało, a nie wywalić czerwoną ścianę błędów. W ćwiczeniach tej karty zapisujesz oceny, liczby i imiona, a w projekcie zrobisz to samo dla notatek.

## Teoria

### Co to jest wyjątek

Wyjątek to awaria w trakcie działania programu. Znasz już jeden: `ArrayIndexOutOfBoundsException` z rozdziału 1. Kilka innych:

- dzielenie liczb całkowitych przez zero: `ArithmeticException`,
- zamiana tekstu na liczbę, gdy w tekście są litery: `Integer.parseInt("abc")` rzuca `NumberFormatException`,
- szuflada, której nie ma: `tab[5]` przy trzech szufladach.

Gdy nikt wyjątku nie złapie, program umiera w tym miejscu. Java wypisuje na czerwono nazwę wyjątku i numer linii, a linijki niżej już się nie wykonują:

```java
int ile = 0;
System.out.println("start");        // wypisze sie
System.out.println(10 / ile);       // tu program pada
System.out.println("koniec");       // tego juz nie bedzie
```

### try i catch: program żyje dalej

`try` znaczy „spróbuj”, `catch` znaczy „jeśli się nie uda, zrób to”. Kod z `try` wykonuje się normalnie. Gdy któraś linia rzuci wyjątek tego typu, co w nawiasie `catch`, Java skacze od razu do `catch` (reszta `try` jest pominięta), a po `catch` program idzie dalej.

```java
int ludzie = 0;
try {
    int kawalek = 12 / ludzie;           // tu poleci wyjatek
    System.out.println(kawalek);         // pominiete
} catch (ArithmeticException e) {
    System.out.println("zero ludzi!");   // wypisze sie
}
System.out.println("dalej");             // wypisze sie zawsze
```

Gdy w `try` nie ma błędu, `catch` jest po prostu pomijany. Typ wyjątku w nawiasie ma pasować do błędu: `catch (NumberFormatException e)` nie złapie dzielenia przez zero. Szeroki `catch (Exception e)` łapie prawie wszystko, ale lepiej nazwać konkretny błąd.

### Wyjątki, które Java każe obsłużyć

Przy plikach Java robi coś, czego nie robi przy dzieleniu: **nie skompiluje kodu, dopóki nie przewidzisz błędu**. Pliku może nie być, folder może mieć taką nazwę, może zabraknąć miejsca. Taki wyjątek nazywa się „sprawdzany”. Dostajesz błąd `unreported exception FileNotFoundException; must be caught or declared to be thrown`, który znaczy: „otocz to try i catch”.

```java
try {
    Scanner s = new Scanner(new File("dane.txt"));
} catch (FileNotFoundException e) {
    System.out.println("nie ma dane.txt");
}
```

`FileNotFoundException` jest szczególnym przypadkiem `IOException` („coś nie tak z plikiem”). Jeden `catch (IOException e)` łapie oba.

### Zapis do pliku

`new PrintWriter("lista.txt")` tworzy plik (albo czyści stary, jeśli już jest) i daje `println`, takie samo jak na ekran, tylko do pliku. Ważne: tekst nie idzie od razu na dysk, tylko czeka w pamięci. **Na dysk trafia dopiero przy zamknięciu pliku (`close()`).** Bez `close()` plik zostaje pusty albo urwany.

```java
try {
    PrintWriter p = new PrintWriter("wynik.txt");
    p.println("gra: 10");
    p.println("nauka: 8");
    p.close();                           // dopiero teraz sa w pliku
} catch (FileNotFoundException e) {
    System.out.println("blad zapisu");
}
```

Wygodniejszy sposób to **try z zasobem**: plik otwierasz w nawiasie po `try`, a Java sama zamknie go na końcu bloku (nawet gdy coś pójdzie źle):

```java
try (PrintWriter p = new PrintWriter("wynik.txt")) {
    p.println("gra: 10");
} catch (FileNotFoundException e) {
    System.out.println("blad zapisu");
}
```

Znak `|` w pliku to zwykły znak. Linię z dwóch list robisz tak: `p.println(imie + "|" + ocena);`.

### Czy plik istnieje

`new File("nazwa.txt")` to tylko „adres” pliku, nie sam plik. Pytanie `exists()` zwraca `true` albo `false` i nie rzuca wyjątku:

```java
File f = new File("ustawienia.txt");
if (f.exists()) {
    System.out.println("jest");
} else {
    System.out.println("pierwszy start");
}
```

Dzięki temu program przy pierwszym uruchomieniu (kiedy archiwum jeszcze nie istnieje) wita użytkownika zamiast padać.

### Czytanie linia po linii

Do czytania służy `BufferedReader` z metodą `readLine()`. Zwraca kolejną linię (bez Entera), a gdy linii już nie ma, zwraca `null`. Pętla bierze linię, jeśli to nie `null` robi coś z nią i bierze następną:

```java
try {
    BufferedReader r = new BufferedReader(new FileReader("plan.txt"));
    String linia = r.readLine();
    while (linia != null) {
        System.out.println("linia: " + linia);
        linia = r.readLine();
    }
    r.close();
} catch (IOException e) {
    System.out.println("nie moge czytac pliku");
}
```

Możesz też użyć `Scanner` z plikiem zamiast klawiatury: `new Scanner(new File("plan.txt"))`, a dalej `hasNextLine()`, `nextLine()`, `hasNextInt()`, `nextInt()`, tak jak znasz.

### Jedna linia, dwa kawałki

W pliku jest linia `Ania|5`. Do pamięci chcesz wrzucić `Ania` do jednej listy, a `5` do drugiej, więc linię trzeba rozciąć na kreskę. Dwa sposoby:

**1. Własna pętla.** Szukasz numeru znaku `|` i wycinasz `substring`:

```java
int i = 0;
while (i < s.length() && s.charAt(i) != ',') {
    i++;
}
String przed = s.substring(0, i);       // wszystko przed przecinkiem
String po = s.substring(i + 1);         // wszystko po przecinku
```

**2. `split`.** Zwraca tablicę kawałków:

```java
String[] k = "stol;120".split(";");
String nazwa = k[0];                    // stol
String cena = k[1];                     // 120
```

**Pułapka:** `split` bierze wzorzec (tzw. regex), a w nim `|` znaczy „albo” i tnie tekst po KAŻDEJ literze: `"Ania|5".split("|")` daje `{"A", "n", "i", "a", "|", "5"}`. Znak `|` trzeba osłonić ukośnikiem, a w kodzie Javy ukośnik piszesz podwójnie: `split("\\|")`.

**Druga pułapka:** `split` wyrzuca puste kawałki z końca. `"Dom|".split("\\|")` daje tablicę z JEDNYM elementem (`{"Dom"}`), więc `k[1]` rzuci `ArrayIndexOutOfBoundsException`, a `catch (IOException e)` jej nie złapie (to inny wyjątek). Notatka z pustą treścią to prawdziwy przypadek, więc dopisz limit: `split("\\|", 2)` tnie najwyżej na dwa kawałki i zostawia pustą końcówkę (`{"Dom", ""}`). Możesz też sprawdzić `k.length` przed użyciem `k[1]`.

### Dwie listy w jednym pliku

Zapis: dla każdego numeru `i` jedna linia `tytuly.get(i) + "|" + zaszyfrowana tresc`. Do pliku idzie **tylko zaszyfrowana treść**, nigdy jawna.
Wczytanie na starcie: `exists()`, jeśli pliku nie ma, komunikat i puste listy; jeśli jest, `readLine()` w pętli, `split` na dwie części, `tytuly.add(...)` i `tresci.add(...)`. Treść zostaje zaszyfrowana, odszyfrowujesz ją dopiero przy wyświetlaniu.

## Ćwiczenia

### Ćwiczenie 1 (plik `R16_cw1.java`)

Napisz metodę `public static int podziel(int a, int b)`, która oddaje `a / b`. Gdy `b` to zero, ma wypisać `Nie dziel przez zero` i oddać `0`. Użyj `try` i `catch (ArithmeticException e)`, nie `if`. W `main` wypisz `podziel(10, 2)`, `podziel(7, 0)` i na końcu `koniec`.

Oczekiwany wynik na ekranie:

```text
5
Nie dziel przez zero
0
koniec
```

<details>
<summary>Podpowiedź</summary>

`return a / b;` wsadź do `try`. W `catch` wypisz komunikat i zrób `return 0;`.

</details>

### Ćwiczenie 2 (plik `R16_cw2.java`)

Napisz `public static int naLiczbe(String s)`: zamienia tekst na liczbę przez `Integer.parseInt`. Gdy to nie jest liczba, wypisz `To nie jest liczba` i oddaj `-1`. W `main` wypisz `naLiczbe("12")`, `naLiczbe("pies")` i `koniec`.

Oczekiwany wynik na ekranie:

```text
12
To nie jest liczba
-1
koniec
```

<details>
<summary>Podpowiedź</summary>

Łapiesz `NumberFormatException`.

</details>

### Ćwiczenie 3 (plik `R16_cw3.java`)

Napisz `public static int bezpiecznie(int t[], int i)`: oddaje `t[i]`, a gdy takiej szuflady nie ma (też dla numeru ujemnego), wypisuje `Nie ma takiej szuflady` i oddaje `-1`. W `main` użyj tablicy `{4, 8, 2}` i numerów `1` oraz `3`.

Oczekiwany wynik na ekranie:

```text
8
Nie ma takiej szuflady
-1
koniec
```

<details>
<summary>Podpowiedź</summary>

`ArrayIndexOutOfBoundsException`. Sprawdź też numer `-1`: `if (i >= t.length)` by go nie złapał, a `catch` tak.

</details>

### Ćwiczenie 4 (plik `R16_cw4.java`)

Kod z `PrintWriter` bez obsługi błędu się nie skompiluje. Zapisz do pliku `notatki.txt` jedną linię `pierwsza`. Napis `zapisano` ma się wypisać tylko wtedy, gdy zapis się udał, a przy błędzie wypisz `Nie mozna zapisac pliku`.

Oczekiwany wynik na ekranie (gdy się uda):

```text
zapisano
```

<details>
<summary>Podpowiedź</summary>

Wszystko, łącznie z `println("zapisano")`, wsadź do `try`. `catch (FileNotFoundException e)` wypisze komunikat błędu.

</details>

### Ćwiczenie 5 (plik `R16_cw5.java`)

Napisz `public static void zapiszOceny(ArrayList<String> imiona, ArrayList<String> oceny)`: zapisuje do pliku `oceny.txt` jedną linię na osobę w formacie `imie|ocena`. Przy błędzie zapisu wypisz `Nie mozna zapisac pliku`. W `main` dodaj do list `Ania/5`, `Basia/4`, `Kasia/3` i wywołaj metodę.

Plik `oceny.txt` po programie:

```text
Ania|5
Basia|4
Kasia|3
```

<details>
<summary>Podpowiedź</summary>

`try (PrintWriter pw = new PrintWriter("oceny.txt"))` i `pw.println(imiona.get(i) + "|" + oceny.get(i));` w pętli po numerach. Ten `try` sam zamknie plik.

</details>

### Ćwiczenie 6 (plik `R16_cw6.java`)

Zapisz imiona do pliku `lista.txt`, ale w wersji z `PrintWriter` bez `try` z zasobem. Sprawdź, co dzieje się, gdy nie wywołasz `close()`: plik jest pusty. Potem dopisz `close()` i popraw.

<details>
<summary>Podpowiedź</summary>

`pw.close();` po pętli, jeszcze w bloku `try`.

</details>

### Ćwiczenie 7 (plik `R16_cw7.java`)

Napisz `public static boolean czyJest(String nazwa)`: oddaje `true`, gdy plik o tej nazwie leży w katalogu roboczym programu. Ścieżka bez folderu, jak `"oceny.txt"`, jest liczona od katalogu roboczego: w IntelliJ to korzeń projektu, a nie folder, w którym leży plik `.java`. W `main` użyj plików `oceny.txt` (jest) i `nic.txt` (nie ma).

Oczekiwany wynik na ekranie:

```text
true
false
```

<details>
<summary>Podpowiedź</summary>

`new File(nazwa).exists()`.

</details>

### Ćwiczenie 8 (plik `R16_cw8.java`)

Napisz `public static int ileLinii(String nazwa)`: otwiera plik przez `BufferedReader`, czyta linie `readLine()` aż do `null` i oddaje ich liczbę. Gdy pliku nie ma, wypisz `Brak pliku` i oddaj `-1`.

<details>
<summary>Podpowiedź</summary>

Pierwszą linię czytasz przed pętlą. W pętli: `ile++;` i czytasz następną. Całość w `try`, `catch (IOException e)`.

</details>

### Ćwiczenie 9 (plik `R16_cw9.java`)

Napisz `public static int sumaLiczb(String nazwa)`: Scannerem z pliku (`new Scanner(new File(nazwa))`) czyta wszystkie liczby i je sumuje. Gdy pliku nie ma, wypisz `Brak pliku` i oddaj `0`.

<details>
<summary>Podpowiedź</summary>

`while (skan.hasNextInt()) { suma = suma + skan.nextInt(); }`

</details>

### Ćwiczenie 10 (plik `R16_cw10.java`)

Napisz `public static String przedKreska(String linia)`: własną pętlą (`charAt`) znajdź numer znaku `|` i oddaj wszystko przed nim (`substring`). Gdy kreski nie ma, oddaj całą linię. Bez `split` i `indexOf`.

<details>
<summary>Podpowiedź</summary>

`while (i < linia.length() && linia.charAt(i) != '|') { i++; }`, potem `substring(0, i)`.

</details>

### Ćwiczenie 11 (plik `R16_cw11.java`)

Napisz `public static String czesc(String linia, int nr)`: tnie linię na kreskę i oddaje kawałek o numerze `nr`. `czesc("Ania|5", 0)` to `Ania`, `czesc("Ania|5", 1)` to `5`. Sprawdź, co daje `split("|")`.

<details>
<summary>Podpowiedź</summary>

`linia.split("\\|")`.

</details>

### Ćwiczenie 12 (plik `R16_cw12.java`)

Masz dwie listy `tytuly` i `tresci`. Metoda `zapisz` zapisuje je do `archiwum.txt` jako `tytul|zaszyfrowana tresc`. Szyfr (metoda `szyfruj` z rozdziału 4) jest gotowy. Treść w pliku musi być zaszyfrowana, tytuł jawny.

Plik `archiwum.txt` po zapisie list `Zakupy/mleko` i `Dom/klucz` (szyfr 3):

```text
Zakupy|pohnr
Dom|noxfc
```

<details>
<summary>Podpowiedź</summary>

`pw.println(tytuly.get(i) + "|" + szyfruj(tresci.get(i), 3));`

</details>

### Ćwiczenie 13 (plik `R16_cw13.java`)

Napisz `public static void wczytaj(String nazwa, ArrayList<String> tytuly, ArrayList<String> tresci)`. Gdy pliku nie ma: wypisz `Brak pliku archiwum.txt - zaczynamy od pustej listy` i wyjdź. Gdy jest: wczytaj linie, rozetnij każdą na tytuł i treść i dodaj do list. Treść zostaje zaszyfrowana.

<details>
<summary>Podpowiedź</summary>

`exists()`, potem `BufferedReader`, `readLine()` w pętli, `split("\\|", 2)` (dwójka, żeby linia `Dom|` z pustą treścią nadal dała dwa kawałki), dwa `add`.

</details>

## Sprawdzian

Z pamięci, bez podglądania, napisz program archiwum w małej wersji:

1. Na starcie `main` wczytuje `archiwum.txt` (brak pliku to komunikat, nie awaria).
2. Czyta od użytkownika tytuł i treść, szyfruje treść i dodaje do list.
3. Zapisuje obie listy do `archiwum.txt` jako `tytul|zaszyfrowana_tresc`.

Co sprawdza prowadzący (Claude):

- Operacje na plikach są w `try-catch` (albo `try` z zasobem), a komunikat błędu jest po ludzku.
- Plik jest zamykany (`close()` albo `try` z zasobem).
- Brak pliku sprawdzony przez `exists()`, program z pustymi listami działa dalej.
- W pliku jest tylko zaszyfrowana treść, tytuł jawny, kreska jako rozdzielacz.
- Linia jest cięta na dwie części poprawnie: `split("\\|", 2)` albo własna pętla, nie `split("|")`. Notatka z pustą treścią (`Dom|`) też się wczytuje.
- Żadnego `contains` ani `indexOf` (zakazane w projekcie).

## Typowe błędy

- **Brak `close()`.** Plik zostaje pusty, bo tekst nie dotarł z pamięci na dysk. Zawsze `close()` albo `try` z zasobem.
- **`split("|")`.** Tnie po każdej literze. Ma być `split("\\|")`.
- **`split("\\|")` bez limitu przy pustej treści.** Z linii `Dom|` wychodzi tablica z jednym elementem, `k[1]` rzuca `ArrayIndexOutOfBoundsException` i `catch (IOException e)` jej nie złapie. Pisz `split("\\|", 2)` albo sprawdź `k.length`.
- **Wczytywanie bez `exists()`.** Przy pierwszym uruchomieniu archiwum nie ma, program pada czerwoną ścianą zamiast zacząć od pustej listy.
- **Jawna treść w pliku.** Do `archiwum.txt` idzie tylko zaszyfrowana treść. Szyfrujesz tuż przed zapisem.
- **Pusty `catch`.** Błąd znika bez śladu i nie wiadomo, czemu plik się nie zapisał. W `catch` napisz, co się stało.
- **Zbyt wąski typ w `catch`.** `catch (ArithmeticException e)` nie złapie `NumberFormatException`. Typ wyjątku ma pasować do błędu.

**Czas na rozdział:** 90-150 min.

---
