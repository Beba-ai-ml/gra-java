# Rozdział 15. Szyfr Vigenère'a

**Po co ci to w projekcie:** wymaganie na ocenę 4.0. Obok szyfru Cezara program ma umieć szyfr Vigenère'a: użytkownik wpisuje tekstowy klucz, a program przesuwa każdą literę o inną liczbę miejsc. Klucz jest zwykle krótszy niż wiadomość, więc trzeba go powtarzać w kółko przez `%`.

**Pliki:** wszystkie ćwiczenia wrzucasz do pakietu `nauka` w IntelliJ, każde w osobnym pliku (`R15_cw1.java` itd.). Zakładam, że znasz rozdział 4 (Cezar), bo Vigenère to jego rozwinięcie.

## Teoria

### Cezar ma jedną liczbę, Vigenère ma słowo

W szyfrze Cezara każda litera jedzie o to samo `szyfr`. W Vigenèrze `szyfr` zmienia się z litery na literę. Bierzesz słowo (klucz), na przykład `kot`. Jego litery to liczby przesunięcia: `k` to 10, `o` to 14, `t` to 19 (liczysz od `a`, tak jak w rozdziale 4: `'k' - 'a'` daje 10).

Pierwsza litera tekstu jedzie o 10, druga o 14, trzecia o 19, czwarta znów o 10 i tak w kółko:

```
tekst:    a  l  a  m  a
klucz:    k  o  t  k  o
numer k:  10 14 19 10 14
wynik:    k  z  t  w  o
```

### Krok 1: litera klucza to liczba

```java
char k = 'o';
int szyfr = k - 'a';     // 14
```

To ta sama sztuczka co `c - 'a'` w rozdziale 4. Różnica: odejmujesz od litery klucza, nie od litery tekstu.

### Krok 2: klucz się kończy, więc `%`

Tekst ma 11 liter, klucz 3. Numer litery klucza dla litery tekstu o numerze `i` to `i % klucz.length()`. Reszta z dzielenia zawija się w kółko: `0 1 2 0 1 2 0 1 2 ...`

```java
String klucz = "kot";
for (int i = 0; i < 7; i++) {
    System.out.println(i + " " + klucz.charAt(i % klucz.length()));
}
```

Wypisze `0 k`, `1 o`, `2 t`, `3 k`, `4 o`, `5 t`, `6 k`. Dwa różne `%`: ten z rozdziału 4 zawija literę po alfabecie (`% 26`), a ten zawija numer litery po kluczu (`% klucz.length()`).

### Krok 3: spacje i cyfry nie zjadają klucza

W tekście są spacje, cyfry, wykrzykniki. Nie szyfrujemy ich. Zasada, którą przyjmujemy w całym rozdziale i którą miał Twój stary plik `vigenere`:

**Numer klucza rośnie tylko przy literach.** Do tego potrzebny jest osobny licznik `j`, bo `i` rośnie przy każdym znaku. `j` startuje od 0 i dostaje `j++` tylko wtedy, gdy zaszyfrowałeś literę.

```
tekst:   A  l  a     m  a
klucz:   k  o  t     k  o      (spacja pomija klucz)
numer j: 0  1  2     3  4  (po % 3: 0 1 2 0 1)
```

Gdybyś brał klucz przez `i % klucz.length()`, spacja zjadłaby jedną literę klucza i wynik byłby inny niż w starym pliku i u prowadzącego.

### Krok 4: deszyfrowanie

Tak samo jak w rozdziale 4: zamiast `+ szyfr` jest `- szyfr + 26` (plus 26, żeby `%` nie zwróciło minusa). Klucz, kolejność liter klucza i licznik `j` są dokładnie takie same.

### Krok 5: klucz od użytkownika

Klucz wpisuje człowiek, więc może go wpisać dużymi literami (`KOT`). Przed odejmowaniem `'a'` zamień literę klucza na małą: `Character.toLowerCase(klucz.charAt(...))`. W tym rozdziale zakładamy, że klucz składa się z samych liter, bez spacji i cyfr.

## Ćwiczenia

### Ćwiczenie 1 (na kartce)

Zaszyfruj „Ala" kluczem `bc`. Dla każdej litery zapisz: numer litery tekstu, numer litery klucza, sumę i wynik. Pamiętaj, że klucz ma dwie litery, a tekst trzy: trzecia litera tekstu bierze znów `b`. Potem w pliku powtórz obliczenie jednej litery po drugiej na zmiennych `char`.

**Plik:** `R15_cw1.java`

**Oczekiwany wynik:**

```text
A + b -> B
l + c -> n
a + b -> b
```

<details>
<summary>Podpowiedź</summary>

`A` ma numer 0, `b` ma numer 1: 0 + 1 = 1, to `B`. `l` ma numer 11, `c` ma numer 2: 13 to `n`. Trzecia litera `a` znów dostaje `b`.

</details>

### Ćwiczenie 2

Napisz pętlę, która dla tekstu `"abcdefg"` i klucza `"xyz"` wypisuje przy każdym numerze litery `i` literę klucza, która do niej pasuje: `i` i znak, oddzielone spacją. Użyj `i % klucz.length()`.

**Plik:** `R15_cw2.java`

**Oczekiwany wynik:**

```text
0 x
1 y
2 z
3 x
4 y
5 z
6 x
```

<details>
<summary>Podpowiedź</summary>

W środku pętli: `klucz.charAt(i % klucz.length())`. Dopóki `i` jest mniejsze niż 3, `%` nic nie zmienia. Od `i = 3` numer wraca do 0.

</details>

### Ćwiczenie 3

Napisz metodę `public static String szyfrujV(String text, String klucz)`. Na razie tekst ma TYLKO małe litery (bez spacji), klucz też. Użyj `char[]` z rozdziału 4 i numeru litery klucza `i % klucz.length()`. W `main` wypisz `szyfrujV("ala", "kot")`, `szyfrujV("xyz", "b")` i `szyfrujV("abc", "abc")`, każdy w osobnej linii.

**Plik:** `R15_cw3.java`

**Oczekiwany wynik:**

```text
kzt
yza
ace
```

<details>
<summary>Podpowiedź</summary>

W pętli po `znaki`: `int szyfr = klucz.charAt(i % klucz.length()) - 'a';`, potem jak w Cezarze: `tmp = znaki[i] - 'a'`, `tmp = (tmp + szyfr) % 26`, `znaki[i] = (char) ('a' + tmp)`.

</details>

### Ćwiczenie 4

Rozszerz `szyfrujV`: wielkie litery liczysz od `'A'`, a znaki, które nie są literami (spacja, cyfry), zostają bez zmian i NIE zjadają klucza. Do numeru klucza użyj osobnego licznika `j`, który rośnie tylko po zaszyfrowanej literze. W `main` wypisz `szyfrujV("Ala ma kota", "pieski")` i `szyfrujV("Ala ma kota xyz XYZ 123", "kot")`.

**Plik:** `R15_cw4.java`

**Oczekiwany wynik:**

```text
Pte ek sdbe
Kzt wo dyht hms HMS 123
```

<details>
<summary>Podpowiedź</summary>

Przed pętlą `int j = 0;`. W bloku małych liter i w bloku wielkich liter: `int szyfr = klucz.charAt(j % klucz.length()) - 'a';` i na końcu bloku `j++;`. W gałęzi znaków niebędących literami nie ma `j++`.

</details>

### Ćwiczenie 5

Napisz `public static String deszyfrujV(String text, String klucz)`. Wszystko jak w `szyfrujV`, ale `- szyfr + 26` zamiast `+ szyfr`. W `main` wypisz `deszyfrujV("Pte ek sdbe", "pieski")` i `deszyfrujV("Kzt wo dyht hms HMS 123", "kot")`.

**Plik:** `R15_cw5.java` (w pliku są obie metody)

**Oczekiwany wynik:**

```text
Ala ma kota
Ala ma kota xyz XYZ 123
```

<details>
<summary>Podpowiedź</summary>

Licznik `j` rośnie w tych samych miejscach co w `szyfrujV`. Zmienia się tylko jedna linijka w każdym bloku: `tmp = (tmp - szyfr + 26) % 26;`.

</details>

### Ćwiczenie 6: test w obie strony z kluczem od użytkownika

W `main` wczytaj przez `Scanner` dwie linie: tekst i klucz (`nextLine()` dwa razy). Zaszyfruj tekst, odszyfruj wynik i wypisz: zaszyfrowany tekst, odszyfrowany tekst i `true` albo `false` z `equals`.

**Plik:** `R15_cw6.java`

**Wpisujesz:** `Ala ma kota` i `pieski`

**Oczekiwany wynik:**

```text
Pte ek sdbe
Ala ma kota
true
```

<details>
<summary>Podpowiedź</summary>

`Scanner sc = new Scanner(System.in);`, potem `String text = sc.nextLine();` i `String klucz = sc.nextLine();`. `deszyfrujV(szyfrujV(text, klucz), klucz)` musi dać `text`.

</details>

### Ćwiczenie 7: czytanie starego kodu

Otwórz swój stary plik `vigenere.java` (z `~/IdeaProjects/pierwszy/src/algorytmika/`) i skopiuj go do `R15_cw7.java`. Pod każdą linijką napisz komentarz `// co robi`. Odpowiedz: po co jest zmienna `k` i dlaczego `k = (k + 1) % len` stoi tylko w dwóch pierwszych blokach, a nie w `else`?

**Plik:** `R15_cw7.java`

**Oczekiwany wynik:**

```text
Ala ma kota
Pte ek sdbe
```

<details>
<summary>Podpowiedź</summary>

`k` to numer litery klucza, czyli to samo co `j` z tego rozdziału. W `else` stoją znaki niebędące literami: klucz ma w nich stać w miejscu.

</details>

## Sprawdzian

Bez podglądania, z pamięci:

1. Napisz obie metody `szyfrujV` i `deszyfrujV` (`public static String`, parametry `String text, String klucz`).
2. W `main` wczytaj tekst i klucz ze `Scannera`, zaszyfruj, odszyfruj i sprawdź `equals`.
3. Wyjaśnij na głos: po co licznik `j` obok `i`? Co robi `% klucz.length()`, a co `% 26`?

**Checklista, którą sprawdzi Claude:**

- [ ] Obie metody są `public static String` z parametrami `(String text, String klucz)`.
- [ ] Tekst idzie przez `toCharArray()` i `new String(...)`.
- [ ] Numer klucza liczy osobny licznik `j`, który rośnie tylko po literze, i jest zawijany przez `% klucz.length()`.
- [ ] Przesunięcie to litera klucza minus `'a'` (po `Character.toLowerCase`).
- [ ] `deszyfrujV` ma `+ 26` przed `% 26`, a znaki niebędące literami zostają bez zmian.
- [ ] Test `deszyfrujV(szyfrujV(x, k), k)` daje `x`, sprawdzony przez `equals`.

## Typowe błędy

- **Klucz brany przez `i`, nie przez `j`.** Spacja zjada literę klucza i wynik różni się od poprawnego (od tego z Twojego starego pliku).
- **Brak `% klucz.length()`.** Gdy tekst jest dłuższy od klucza, wychodzi `StringIndexOutOfBoundsException`: klucz się skończył, a Ty prosisz o jego kolejną literę.
- **Brak `+ 26` w deszyfrowaniu.** Tak samo jak w Cezarze: `-3 % 26` to `-3` i wychodzi nie litera.
- **Klucz wielkimi literami bez `toLowerCase`.** `'K' - 'a'` to `-22`, więc przesunięcia są złe.
- **Pomylone `% 26` i `% klucz.length()`.** Pierwszy zawija literę po alfabecie, drugi numer po kluczu. Nie wolno ich zamienić.
- **`j++` poza blokiem litery.** Wtedy licznik zachowuje się jak `i` i spacje znów zjadają klucz.

**Czas:** ok. 3-5 godzin (ćwiczenia 1-2 szybko, 3-5 najwięcej czasu, 7 zależy od tempa czytania).

---
