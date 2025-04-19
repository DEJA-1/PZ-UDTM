# PZ System Monitor

Aplikacja mobilna umożliwiająca zdalne monitorowanie zasobów systemowych urządzenia Raspberry Pi.
Użytkownik ma możliwość podglądu temperatury CPU, zużycia pamięci RAM, wykorzystania CPU oraz listy
aktywnych procesów.

## Opis

Aplikacja łączy się z backendem udostępniającym dane systemowe za pomocą REST API. Dane są
cyklicznie odświeżane, a UI prezentuje je w czytelnej formie z wykorzystaniem komponentów Jetpack
Compose.

## Funkcjonalności

- Automatyczne odświeżanie danych co n sekund
- Wizualizacja temperatury CPU
- Dynamiczne paski zużycia CPU i RAM z animacją oraz zmianą koloru
- Lista procesów z informacjami o nazwie, stanie, zużyciu pamięci i czasie CPU
- Obsługa stanów ładowania oraz błędów połączenia
- Tryb testowy z danymi generowanymi lokalnie (`FakeSystemRepository`)

## Technologie

- Kotlin
- Jetpack Compose
- Material3
- Kotlin Coroutines
- Hilt (Dependency Injection)
- REST API (JSON)

## Architektura

- `ViewModel` zarządza stanem ekranu i logiką pobierania danych
- `HomeState` przechowuje bieżący stan UI
- `Resource` jest generycznym wrapperem obsługującym stany: `Success`, `Error`, `Loading`
- `FakeSystemRepository` symuluje backend na potrzeby developmentu

## Backend API

Backend (System Status API) powinien działać na urządzeniu z systemem Linux (np. Raspberry Pi).
Komunikuje się poprzez protokół HTTP i udostępnia następujące endpointy:

- `GET /cpu` – dane o temperaturze i wykorzystaniu CPU
- `GET /memory` – dane o pamięci RAM
- `GET /processes` – lista aktywnych procesów

Przykładowe zapytanie:

    ```bash
    http://<adres-ip-raspberry>:3000/cpu

## Uruchomienie

   ```bash
   git clone https://github.com/DEJA-1/PZ-UDTM.git
