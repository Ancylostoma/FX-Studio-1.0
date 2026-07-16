#!/usr/bin/env python3
"""
Calculadora del código de activación mensual de FotoEstudio.
Usa la MISMA fórmula que app/src/main/java/com/example/data/LicenseManager.kt

Uso:
    python3 calcular_codigo_licencia.py            -> código del mes actual
    python3 calcular_codigo_licencia.py 2026 12    -> código de diciembre 2026
"""
import sys
import datetime

# Deben coincidir EXACTAMENTE con las constantes en LicenseManager.kt
MULTIPLIER = 9973
OFFSET = 734911


def codigo_para(anio: int, mes: int) -> str:
    seed = anio * 100 + mes
    raw = (seed * MULTIPLIER + OFFSET) % 1_000_000
    return str(raw).zfill(6)


if __name__ == "__main__":
    if len(sys.argv) == 3:
        anio, mes = int(sys.argv[1]), int(sys.argv[2])
    else:
        hoy = datetime.date.today()
        anio, mes = hoy.year, hoy.month

    print(f"Periodo: {anio}-{mes:02d}")
    print(f"Código de activación: {codigo_para(anio, mes)}")
