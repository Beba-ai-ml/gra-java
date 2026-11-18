# Rozdział 12. Obrona przed prowadzącym

**Czas:** widełki 1-2 h (przeczytanie + samodzielne odpowiedzi + 10-minutowy quiz).

## Po co ci to w projekcie

Prowadzący nie ocenia tylko działania programu. Pyta, czy rozumiesz każdą linijkę.
Każdą odpowiedź mówisz WŁASNYMI słowami, krótko i z pokazaniem miejsca w kodzie.
Nie uczysz się tu na pamięć. Jeśli nie umiesz odpowiedzieć, wróć do rozdziału, którego dotyczy pytanie.

## Jak odpowiadać

1. Najpierw odpowiedź jednym zdaniem.
2. Potem „pokazuję”: wskazujesz metodę albo linijkę.
3. Jeśli nie wiesz, mów: „nie jestem pewien, sprawdzę” i sprawdź w kodzie, a nie zgaduj.

## Pytania z planu (1-8)

### 1. Czemu dwie listy, a nie jedna?

**Odpowiedź:** Projekt wymaga, żeby tytuł i treść były osobno, a ta sama pozycja w obu listach
była jedną notatką. Gdybym trzymał tytuł i treść w jednej liście, musiałbym łączyć je w jeden tekst
i rozbijać z powrotem. Przy sortowaniu zamieniam obie listy razem, więc notatka się nie rozjeżdża.
Gdybym zamienił tylko tytuły, treść zostałaby na starym miejscu i każda notatka dostałaby cudzą treść.

**Pokaż w kodzie:** deklaracje `tytuly` i `tresci` w `main`; w `sortujBabelkowo` dwie zamiany jedna pod drugą.

### 2. Pokaż, gdzie treść jest szyfrowana, a gdzie odszyfrowana. Czy w liście jest jawny tekst?

**Odpowiedź:** Szyfruję w momencie dodawania, tuż przed `add`, więc w liście `tresci` nie ma jawnego
tekstu. Odszyfrowuję w locie, dopiero przy wypisywaniu, i tylko do ekranu. Lista trzyma zawsze wersję zaszyfrowaną.

**Pokaż w kodzie:** w `case 1` wywołanie `szyfruj` przed `add` do `tresci`; w metodzie `wyswietl` (woła ją `case 2`) oraz w `case 3` wywołanie `deszyfruj`.

### 3. Czemu `+ 26` w deszyfrowaniu? Co by się stało bez tego?

**Odpowiedź:** W Javie `%` przy ujemnej liczbie daje wynik ujemny. Gdy odejmuję przesunięcie od litery
blisko `a` albo `A`, wynik wypada poniżej zera. Dodanie `26` przed `%` przesuwa wynik na dodatni zakres,
a `%` zawija go do alfabetu. Bez tego dla `b` z przesunięciem 3 wyszłoby coś poza literami.

**Pokaż w kodzie:** metoda `deszyfruj`, wyrażenie z `- szyfr + 26` i `% 26`.

### 4. Czemu `equals`, a nie `==`?

**Odpowiedź:** `==` na tekstach sprawdza, czy to TEN SAM obiekt w pamięci, a nie czy tekst jest taki sam.
Dwa identyczne napisy wczytane osobno mogą być różnymi obiektami, więc `==` da `false`.
`equals` porównuje zawartość, czyli znak po znaku, i to jest to, czego potrzebuję przy szukaniu tytułu.

**Pokaż w kodzie:** `szukajLiniowo`, linia z `equals`.

### 5. Jaka jest złożoność bąbelka i czemu właśnie taka?

**Odpowiedź:** O(N²), czyli około N razy N porównań. Mamy dwie pętle jedna w drugiej: zewnętrzna
przechodzi przez wszystkie elementy, wewnętrzna porównuje sąsiadów. Wewnętrzna pętla przy każdym
przebiegu jest krótsza o jeden element, bo największy już „wypłynął” na koniec, ale to nadal N²
w zapisie O (stała i skrócenie nie zmieniają rzędu).

**Pokaż w kodzie:** dwie zagnieżdżone pętle w `sortujBabelkowo`, zakres wewnętrznej (`n - i`).

### 6. Co zwraca wyszukiwanie, gdy nie znajdzie, i czemu akurat to?

**Odpowiedź:** Zwraca `-1`. Numery pozycji są zawsze od `0` w górę, więc `-1` nie może być prawdziwym
numerem. Dzięki temu `main` odróżnia „nie ma” od „jest na pozycji 0”.

**Pokaż w kodzie:** `return -1;` na końcu `szukajLiniowo` i `if (poz == -1)` w `main`.

### 7. Czemu lista zmieniona w metodzie jest zmieniona też w `main`?

**Odpowiedź:** Do metody trafia adres tej samej listy, a nie kopia listy. Wszystkie `add`, `set`,
zamiany w metodzie działają na tym samym obiekcie co w `main`. Dla zmiennych typu `int` byłoby inaczej:
dostają kopię wartości. Dokładniej: Java przekazuje kopię adresu. Dlatego `lista = new ArrayList<>();`
wewnątrz metody nie zmieni listy w `main`, bo podmienia tylko kopię adresu w metodzie.

**Pokaż w kodzie:** wywołanie `sortujBabelkowo(tytuly, tresci)` w `main` i nagłówek metody z parametrami `ArrayList`.

### 8. Co robi `skan.nextLine()` zaraz po `nextInt()`?

**Odpowiedź:** Zjada zostawiony w buforze Enter po liczbie. Bez tego następne `nextLine()` zwróciłoby
pusty napis i nie czekało na użytkownika. To jest lekarstwo z rozdziału 10.

**Pokaż w kodzie:** linia `skan.nextLine(); // zjada enter po liczbie` zaraz po `wybor = skan.nextInt();`.

## Pytania dodatkowe (9-14), z wymagań PDF 3.0

### 9. Dlaczego cały kod jest w jednej klasie `Main` i nie tworzysz obiektów?

**Odpowiedź:** Wymaganie projektu: programowanie proceduralne, bez własnych klas. Dane trzymam w
dwóch listach równoległych zamiast w obiekcie, a logikę w statycznych metodach. Statyczna metoda
wywołuje się bez tworzenia obiektu, więc `main` może wołać `szyfruj(...)` bezpośrednio.

**Pokaż w kodzie:** nagłówek klasy `public class Main` jako jedyna klasa; słowo `static` w nagłówkach metod.

### 10. Dlaczego nie użyłeś `Collections.sort` ani `Arrays.sort`?

**Odpowiedź:** Projekt zakazuje gotowych sortowań i wymaga własnego algorytmu. Sortowanie bąbelkowe
napisałem pętlami. Do porównania dwóch tytułów w sortowaniu używam gotowego `compareTo`, bo PDF na nie
pozwala. Poza tym korzystam tylko z podstawowych metod: `equals` przy szukaniu, `toCharArray` i `new String`
w szyfrze, `add`, `get`, `set`, `size` na listach. Żadna z nich nie sortuje ani nie szuka za mnie.

**Pokaż w kodzie:** `sortujBabelkowo` (pętle + `if` + zamiana). Pokaż też wynik grepa na zakazane metody.

### 11. Co znaczy `znak - 'A'` i czemu `(char)` wraca do litery?

**Odpowiedź:** Każdy znak ma swój numer w ASCII (`'A'` to 65). Odjęcie `'A'` daje pozycję w alfabecie
(0 dla `A`, 25 dla `Z`). Po dodaniu przesunięcia i `% 26` dodaję `'A'` z powrotem, a `(char)` mówi
Javie, że ta liczba ma być znakiem, a nie zwykłą liczbą.

**Pokaż w kodzie:** w `szyfruj` wyrażenie z `- 'A'` i `+ 'A'` oraz rzutowanie `(char)`.

### 12. Czemu wielkie i małe litery szyfrujesz osobno?

**Odpowiedź:** Bo mają różne numery: wielkie zaczynają się od 65, małe od 97. Każdą grupę zawijam
w jej własnym alfabecie (od `A` do `Z` albo od `a` do `z`). Inne znaki (cyfry, spacje) zostawiam bez zmian,
bo nie należą do żadnego alfabetu.

**Pokaż w kodzie:** w `szyfruj` dwa warunki `if`/`else if`: zakres `'A'..'Z'` i zakres `'a'..'z'`.

### 13. Po co `toCharArray()` i `new String(...)`?

**Odpowiedź:** `String` jest niezmienny, nie zmienię w nim pojedynczej litery. `toCharArray()` robi
z tekstu tablicę znaków, którą mogę edytować w pętli po `i`. `new String(...)` składa ją z powrotem
w tekst, który zwracam.

**Pokaż w kodzie:** początek `szyfruj` (`toCharArray`) i `return new String(...)` na końcu.

### 14. Czemu sortujesz po tytułach, a nie po treściach? Czemu „Zupa” jest przed „bank”?

**Odpowiedź:** Wymaganie projektu: sortowanie alfabetyczne po tytułach. `compareTo` porównuje numery
znaków po kolei, a wielkie litery mają mniejsze numery niż małe. Dlatego „Zupa” (`Z`) wypada przed „bank” (`b`).
Gdybym chciał ignorować wielkość liter, musiałbym to zrobić sam przed porównaniem.

**Pokaż w kodzie:** wywołanie `compareTo` w `sortujBabelkowo`. Pokaż też wynik testu z rozdziału 7.

## Quiz na 10 minut

Zamknij notatki, odmierz 10 minut, odpowiadaj na głos. Sprawdzaj odpowiedzi dopiero po czasie.

1. Co zwraca `szukajLiniowo`, gdy nie ma notatki? (odp.: `-1`)
2. Czemu `while (wybor != 0);` ma średnik? (odp.: bo to koniec instrukcji `do-while`)
3. Ile wynosi `'C' - 'A'`? (odp.: 2)
4. Co robi `% 26` dla `27`? (odp.: daje `1`)
5. Dlaczego `"Zupa"` jest przed `"bank"`? (odp.: wielkie litery mają mniejsze numery w ASCII)
6. Czemu `==` nie nadaje się do szukania tytułu? (odp.: sprawdza obiekt, nie zawartość)
7. Co się stanie, jeśli zamienisz tylko `tytuly` w sortowaniu? (odp.: treści zostaną na starych miejscach)
8. Czemu `ArrayList` zmieniony w metodzie jest zmieniony w `main`? (odp.: metoda dostaje kopię adresu tej samej listy, a nie kopię listy)
9. Co zjada `skan.nextLine()` po `nextInt()`? (odp.: zostawiony Enter)
10. Gdzie w kodzie treść jest jawna? (odp.: nigdzie w liście; jawna jest tylko chwilowo: w zmiennej `tresc` w `case 1`, zanim ją zaszyfruję, i przy wypisywaniu)

**Ocena siebie:** 10 z 10 bez zajrzenia - gotowy. 7-9 - przeczytaj ponownie pytania, których nie znasz. Mniej niż 7 - wróć do rozdziału, którego dotyczy błąd, i zrób jeszcze raz jego ćwiczenia.
