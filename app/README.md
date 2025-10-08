# PZ System Monitor

Aplikacja mobilna umożliwiająca zdalne monitorowanie zasobów systemowych urządzenia **Raspberry Pi** z poziomu Androida.
Użytkownik ma podgląd temperatury CPU, zużycia RAM/CPU oraz listy aktywnych procesów. Za pomocą WebSocket komunikuje się z serwerem, co umożliwia zastosowanie Remote Terminal.

---

## Opis

Aplikacja łączy się z backendem (System Status API), który udostępnia dane systemowe w formacie **JSON**.
Dane są cyklicznie odświeżane co kilka sekund i prezentowane w czytelnym, dynamicznym UI zbudowanym w **Jetpack Compose**.

---

## Funkcjonalności

* Automatyczne odświeżanie danych co kilka sekund.
* Wizualizacja temperatury CPU.
* Dynamiczne paski zużycia CPU i RAM (animacje, kolory zależne od obciążenia).
* Lista procesów z informacjami o nazwie, stanie (*sleeping*, *running*) i zużyciu RAM.
* Obsługa stanów: **Loading**, **Error**, **Success** (brak danych z serwera, błędy połączenia).
* Zdalne zakończenie (ubicie) wybranego procesu.
* Przełączanie widoku temperatury: **Internal / External**.
* **Tryb testowy** z danymi lokalnymi (`FakeSystemRepository`) — bez działającego backendu.
* Remote terminal - komunikacja z terminalem Raspberry PI

---

## Technologie

* **Kotlin**, **Jetpack Compose**, **Material 3**
* **Kotlin Coroutines**
* **Hilt** (Dependency Injection)
* **Retrofit** + **OkHttp** (REST/JSON)
* **WebSocket**

---

## Architektura aplikacji

W aplikacji mobilnej zastosowano architekturę **MVI (Model–View–Intent)**, która ułatwia zarządzanie stanem oraz zwiększa czytelność kodu.
**Model** w tej architekturze to stan aplikacji (np. zużycie CPU, pamięci RAM, procesy), **View** to interfejs użytkownika (UI), a **Intent** to zdarzenia inicjowane przez użytkownika.

Aplikacja została podzielona na trzy główne warstwy:

* **Data layer** – odpowiedzialna za komunikację z backendem (REST API) oraz dostarczanie danych do aplikacji. W tej warstwie znajdują się:

  * `Retrofit` **client** – obsługuje zapytania HTTP.
  * **Repository** – implementuje interfejs z warstwy `domain` i zarządza źródłami danych (np. API, komunikacja WebSocket, symulacja danych w trybie testowym).

* **Domain layer** – zawiera logikę biznesową oraz definicje interfejsów (np. `ISystemRepository`, `IWebSocketRepository`). Warstwa ta jest niezależna od frameworków i platform, co umożliwia łatwe testowanie logiki aplikacji.

* **Presentation layer** – odpowiada za interfejs użytkownika oraz zarządzanie stanem aplikacji. W tej warstwie znajdują się:

  * **ViewModel** – zarządza stanem widoku (`HomeState`), pobiera dane z warstwy `domain`, cyklicznie odświeża dane oraz przetwarza je dla UI.
  * **HomeState** – model stanu widoku, zawiera dane dotyczące CPU, RAM, procesów oraz statusy ładowania i błędów.
  * **Resource wrapper** – generyczna klasa obsługująca stany danych: `Success`, `Error`, `Loading`.
  * **Composable functions** – budują interfejs użytkownika w oparciu o Jetpack Compose, reagując na zmiany stanu.
