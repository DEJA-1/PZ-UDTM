import numpy as np
import time
import argparse

def obciaz_cpu_i_ram(rozmiar_macierzy):
    a = np.random.rand(rozmiar_macierzy, rozmiar_macierzy)
    b = np.random.rand(rozmiar_macierzy, rozmiar_macierzy)
    wynik = np.dot(a, b)
    return wynik

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--praca', type=float, required=True, help='Czas pracy (s)')
    parser.add_argument('--odpoczynek', type=float, required=True, help='Czas odpoczynku (s)')
    parser.add_argument('--rozmiar', type=int, default=1000, help='Rozmiar macierzy (np. 1000, 5000)')
    args = parser.parse_args()

    print(f"Cykl: {args.praca}s pracy, {args.odpoczynek}s odpoczynku | Macierz: {args.rozmiar}x{args.rozmiar}")
    print("Naciśnij Ctrl+C, aby zatrzymać...")

    try:
        while True:
            print(f"[Praca] Obciążanie CPU i RAM przez {args.praca}s...")
            start_pracy = time.time()
            while time.time() - start_pracy < args.praca:
                obciaz_cpu_i_ram(args.rozmiar)

            print(f"[Odpoczynek] Czekanie {args.odpoczynek}s (RAM wolny)...")
            time.sleep(args.odpoczynek)

    except KeyboardInterrupt:
        print("\nZakończono program.")

if __name__ == "__main__":
    main()