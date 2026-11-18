# Rozdział 6. Listy równoległe

**Po co ci to w projekcie:** w projekcie notatka to dwie listy stojące obok siebie: jedna z tytułami, druga z treściami. Ten rozdział uczy, jak je trzymać w zgodzie.

**Pliki:** wszystkie ćwiczenia w pakiecie `nauka` w IntelliJ, każde w osobnym pliku.

## Teoria

### Dwie listy jak dwie kolumny tabeli

Wyobraź sobie tabelę z dwiema kolumnami i numerami wierszy. Wiersz to numer. Element o numerze 2 z jednej listy i element o numerze 2 z drugiej listy to ta sama osoba.

| nr | imiona | oceny |
|---|---|---|
| 0 | Ola | 5 |
| 1 | Piotr | 4 |
| 2 | Kasia | 3 |

W kodzie: `imiona.get(2)` to `"Kasia"`, a `oceny.get(2)` to `"3"`. Razem to jeden wiersz.

### Zasada: każda operacja rusza OBIE listy

Dodanie osoby, zamiana dwóch osób, usunięcie - za każdym razem zmieniasz obie listy tak samo. Jeśli ruszysz tylko jedną, wiersze się rozjadą: imię zostaje na miejscu, a ocena jedzie gdzie indziej.

### Co się psuje, gdy zamienisz tylko jedną listę

Zamieniamy tylko imiona 0 i 2. Oceny zostają na miejscu:

```
PRZED zamianą:                    PO zamianie TYLKO imion:
 nr  imiona    oceny               nr  imiona    oceny
 [0] Ola       5                   [0] Kasia     5    <- ocena Oli trafila do Kasi
 [1] Piotr     4                   [1] Piotr     4
 [2] Kasia     3                   [2] Ola       3    <- ocena Kasi trafila do Oli
```

Dane są nadal poprawne jako listy, ale każda para imię-ocena jest już zła. Program nie zgłosi błędu. Zobaczysz to dopiero na wydruku.

## Ćwiczenia

### Ćwiczenie 1

Stwórz dwie listy `ArrayList<String>`: `imiona` i `oceny`. Dodaj trzy osoby (imię i ocenę jako tekst, np. `"5"`). Wypisz je w formacie `[i] imie: ocena`.

**Plik:** `R6_cw1.java`

**Oczekiwany wynik:**

```text
[0] Ola: 5
[1] Piotr: 4
[2] Kasia: 3
```

<details>
<summary>Podpowiedź</summary>

Dodawaj w tej samej kolejności do obu list: najpierw `imiona.add`, potem `oceny.add`, dla każdej osoby. Pętla idzie po `i` od 0 do `imiona.size()` i w każdym obrocie bierze `get(i)` z obu list.

</details>

### Ćwiczenie 2

Zamień miejscami osobę 0 i osobę 2 w OBU listach. Wypisz listę przed zamianą i po niej. Zamiana w każdej liście to trzy linijki z `tmp`.

**Plik:** `R6_cw2.java`

**Oczekiwany wynik:**

```text
PRZED:
[0] Ola: 5
[1] Piotr: 4
[2] Kasia: 3
PO:
[0] Kasia: 3
[1] Piotr: 4
[2] Ola: 5
```

<details>
<summary>Podpowiedź</summary>

Zrób zamianę dwa razy, raz w `imiona` i raz w `oceny`, tymi samymi numerami 0 i 2. Każda zamiana to `tmp = get(0)`, `set(0, get(2))`, `set(2, tmp)`.

</details>

### Ćwiczenie 3: celowy błąd

Tym razem inne dane: `imiona` to `Tomek`, `Ewa`, `Jan`, a `oceny` to `"2"`, `"5"`, `"4"` (w tej kolejności, ten sam numer = ta sama osoba). Zamień osoby 0 i 2, ale TYLKO w liście `imiona`. Zanim uruchomisz, zapisz na kartce, co Twoim zdaniem wypisze program. Potem uruchom i porównaj. Nie poprawiaj jeszcze kodu, najpierw przeczytaj wydruk.

**Plik:** `R6_cw3.java`

**Oczekiwany wynik:**

```text
PRZED:
[0] Tomek: 2
[1] Ewa: 5
[2] Jan: 4
PO (zamienione tylko imiona):
[0] Jan: 2
[1] Ewa: 5
[2] Tomek: 4
```

<details>
<summary>Podpowiedź</summary>

Lista `oceny` w ogóle się nie rusza. Sprawdź, czyja ocena stoi teraz pod numerem 0. Tak wygląda błąd, którego program nie zgłasza.

</details>

## Sprawdzian

Bez komputera. Narysuj na kartce tabelę z trzema wierszami: numer, imię, ocena. Dane: `Iza` 3, `Max` 5, `Lena` 2. Zamień w niej tylko imiona 0 i 2. Powiedz na głos, co się stanie z każdą osobą i jej oceną. Potem sprawdź swoją kartkę, zmieniając dane w programie z ćwiczenia 3 i uruchamiając go.

**Checklista, którą sprawdzi Claude:**

- [ ] Tabela na kartce ma numery 0-2 i obie kolumny (imię, ocena).
- [ ] Po zamianie na kartce zmieniła się tylko kolumna imion, kolumna ocen stoi bez zmian.
- [ ] Ustnie: wskazuje każdą osobę, która dostała cudzą ocenę, i mówi, czyja to ocena.
- [ ] Kartka zgadza się z wydrukiem `PO` z ćwiczenia 3.
- [ ] Ustnie: jak to naprawić (ta sama zamiana w obu listach, tymi samymi numerami).

## Typowe błędy

- **Zamiana tylko jednej listy.** Dokładnie to robi ćwiczenie 3. Dane wyglądają na poprawne, ale pary są złe.
- **Dodanie do jednej listy.** Druga lista jest krótsza, więc `get(i)` na niej wyrzuca `IndexOutOfBoundsException`.
- **Różne numery w obu listach.** `imiona.get(i)` razem z `oceny.get(i + 1)` przesuwa jedną kolumnę o wiersz.
- **Pętla `for-each` po jednej liście i indeks po drugiej.** Nie wiesz wtedy, który numer masz w ręku. Użyj pętli po `i`.
- **Usunięcie wiersza z jednej listy przez `remove(i)`.** Trzeba usunąć z obu list tym samym numerem.

**Czas:** ok. 1-2 godziny.

---
