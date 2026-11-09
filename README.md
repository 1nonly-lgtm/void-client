# Void Client — by: 1nonly $

Launcher Minecraft (Windows) + mod z menu pod **prawym Shiftem**.

## Co jest w środku

**Launcher** (`launcher/`)
- logowanie kontem Microsoft (token szyfrowany przez Windows),
- Twój skin (i peleryna) w 3D z animacjami: spokój, chód, bieg, machanie – obracasz go myszką,
- zmiana skina (PNG 64×64, model klasyczny/smukły) prosto z launchera,
- profile z wyborem dowolnej wersji gry i loaderem Vanilla/Fabric,
- Java pobiera się sama (oficjalne paczki Mojang),
- wyszukiwarka Modrinth: mody, shadery, resource packi, z instalacją zależności,
- pakiet FPS dla profili Fabric: Sodium, Lithium, FerriteCore, Entity Culling, ImmediatelyFast, More Culling, ModernFix, BadOptimizations, Dynamic FPS,
- gra dostaje wyższy priorytet procesu w Windows,
- flagi JVM pod płynność, RAM, pełny ekran/okno,
- po starcie gry chowa się do zasobnika (0 wpływu na FPS),
- status na Discordzie,
- wszystkie ustawienia zapisują się same.

**Mod Void Client** (`mod/`, Fabric **1.20.1 – 26.1.x**)
- prawy Shift → animowane menu z zakładkami HUD / Gra / AutoText / Opcje,
- HUD: FPS, CPS, Ping, Koordynaty, Zegar, Keystrokes, Zbroja, napis „Void Client by: 1nonly $”,
- Gra: Freelook (trzymasz lewy Alt i rozglądasz się wokół postaci), Toggle Sprint, Zoom (klawisz C), Fullbright, AutoText,
- AutoText: 6 slotów na wiadomości/komendy, każdy pod własnym klawiszem (Sterowanie → Void Client → AutoText 1–6),
- Opcje: „Tryb FPS” (jedno kliknięcie ustawia grafikę pod klatki), kolor akcentu, tło HUD, cień tekstu, reset pozycji, zmiana skina z pliku PNG,
- „Edytuj HUD” – przeciąganie elementów myszką,
- ustawienia zapisują się w `config/voidclient.json`.

### Jak zbudowany jest mod

- `mod/core` – wspólne serce: menu, moduły, zapis ustawień (to samo na każdej wersji).
- `mod/yarn` – wersje 1.20.1 – 1.21.11. Każdy folder w `mod/yarn/versions` to jedna wersja gry.
- `mod/mojang` – wersje 26.1+ (oficjalne nazwy Mojang, Java 25).
- Wersje mappingów i Fabric API pobierają się automatycznie podczas budowania.

Jedno wydanie moda obsługuje grupę wersji:

| Plik | Wersje gry |
|---|---|
| voidclient-1.20.1.jar | 1.20 – 1.20.1 |
| voidclient-1.20.4.jar | 1.20.2 – 1.20.4 |
| voidclient-1.20.6.jar | 1.20.5 – 1.20.6 |
| voidclient-1.21.1.jar | 1.21 – 1.21.1 |
| voidclient-1.21.4.jar | 1.21.2 – 1.21.4 |
| voidclient-1.21.5.jar | 1.21.5 |
| voidclient-1.21.8.jar | 1.21.6 – 1.21.8 |
| voidclient-1.21.10.jar | 1.21.9 – 1.21.10 |
| voidclient-1.21.11.jar | 1.21.11 |
| voidclient-26.1.2.jar | 26.1 – 26.1.x |

Launcher sam wrzuca właściwy plik do profilu.

## Jak zbudować launcher (bez instalowania czegokolwiek)

Wszystko buduje się samo na serwerach GitHuba – za darmo.

1. Załóż konto na **github.com** i zaloguj się.
2. Kliknij **+** (prawy górny róg) → **New repository**. Nazwa np. `void-client`, zaznacz **Public** (wtedy strona i pobieranie działają dla wszystkich), kliknij **Create repository**.
3. Na stronie nowego repozytorium kliknij **uploading an existing file**. Rozpakuj zip i przeciągnij **całą zawartość** folderu `void-client` (foldery `launcher`, `mod`, `website`, `.github` i pliki). Na dole kliknij **Commit changes**.
   - Jeśli przeglądarka pominie ukryty folder `.github`: **Add file → Create new file**, wpisz nazwę `.github/workflows/build.yml`, wklej zawartość tego pliku z zipa, zapisz. To samo dla `.github/workflows/pages.yml`.
4. Wejdź w zakładkę **Actions**. Budowanie startuje samo i trwa ok. 20–30 minut (mod na 10 wersji gry + instalator).
5. Gdy pojawi się zielony ✓, instalator jest w zakładce **Releases** (prawa strona repozytorium) jako `VoidClient-Setup.exe`. Każda kolejna zmiana w repo robi nowe wydanie automatycznie.

Jeśli któraś wersja moda się nie zbuduje, reszta i tak trafi do instalatora. Na stronie uruchomienia w Actions jest podsumowanie, które wersje się udały – wklej mi błąd, a poprawię.

Windows może pokazać „Nieznany wydawca” – to normalne dla programów bez płatnego certyfikatu (**Więcej informacji → Uruchom mimo to**).

## Aktualizacje

Każda zmiana wrzucona do repozytorium robi nowe wydanie (1.0.1, 1.0.2…). Zainstalowany launcher:
1. kilka sekund po starcie sprawdza, czy jest nowa wersja (potem co godzinę),
2. pobiera ją w tle – na dole okna widać pasek z postępem,
3. pokazuje przycisk **Uruchom ponownie**, a jeśli go nie klikniesz, instaluje się przy zamknięciu launchera.

Nowe wersje moda przychodzą razem z launcherem. Działa to, gdy repozytorium jest **publiczne**.

## Strona do pobierania (GitHub Pages)

Strona jest w folderze `website` i sama podpina się pod najnowsze wydanie.

1. W repozytorium: **Settings → Pages → Build and deployment → Source: GitHub Actions**.
2. **Actions → Strona Void Client → Run workflow** (albo zmień cokolwiek w folderze `website`).
3. Po minucie strona działa pod adresem `https://TWOJA-NAZWA.github.io/void-client/`. Przycisk POBIERZ zawsze daje najnowszy instalator, a pod nim pokazuje się numer wersji i data.

Chcesz własną domenę (np. voidclient.pl)? Kup ją u dowolnego rejestratora i wpisz w **Settings → Pages → Custom domain**. Wtedy w `website/index.html` ustaw na górze skryptu `const REPO = 'TWOJA-NAZWA/void-client';`.

## Budowanie na swoim komputerze (opcjonalnie)

Potrzebne: Node.js 20, JDK 21 + Gradle 8.14 (wersje do 1.21.11), JDK 25 + Gradle 9.4 (26.1+).

```
cd mod\yarn
gradle build                 :: wszystkie wersje 1.20.1 – 1.21.11
gradle :mc-1_21_4:build      :: albo tylko jedna
cd ..\mojang
gradle build                 :: 26.1+
:: skopiuj pliki voidclient-*.jar z folderów build\libs do launcher\resources\mods
cd ..\..\launcher
npm install
npm start        :: podgląd
npm run dist     :: instalator w launcher\dist
```

## Status na Discordzie

1. discord.com/developers/applications → **New Application** → nazwa „Void Client”.
2. **Rich Presence → Art Assets** → wgraj logo i nazwij je `logo`.
3. Skopiuj **Application ID** i wklej w launcherze: **Opcje → Discord – Application ID**.

## Uwagi

- Menu pod prawym Shiftem działa na Fabricu od 1.20.1 do 26.1.x. Starsze wersje odpalisz normalnie, tylko bez menu.
- Moduły są „legit”, jak w Lunar/Badlion – nie ma tu cheatów typu fly czy killaura. Fullbright i Toggle Sprint są dozwolone na większości serwerów, ale sprawdź regulamin swojego.
- Gra i launcher trzymają pliki w `%APPDATA%\.voidclient`.
