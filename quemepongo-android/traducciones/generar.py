#!/usr/bin/env python3
"""Genera app/src/main/res/values*/strings.xml desde textos.py y revisa que
todos los idiomas tengan los mismos huecos (%1$s...). Uso: python3 traducciones/generar.py"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from textos import IDIOMAS, P, T  # noqa: E402

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
HUECO = re.compile(r"%(\d+\$)?[sd]")


def escapar(texto: str) -> str:
    texto = texto.replace("\\", "\\\\").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return texto.replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")


def revisar() -> None:
    errores = []
    for clave, textos in T.items():
        if len(textos) != len(IDIOMAS):
            errores.append(f"{clave}: tiene {len(textos)} textos, faltan idiomas")
            continue
        base = sorted(HUECO.findall(textos[0]))
        for idioma, t in zip(IDIOMAS, textos):
            if sorted(HUECO.findall(t)) != base:
                errores.append(f"{clave} [{idioma}]: los huecos no coinciden con el inglés")
    for clave, formas in P.items():
        if len(formas) != len(IDIOMAS):
            errores.append(f"{clave}: faltan idiomas en el plural")
    if errores:
        sys.exit("\n".join(errores))


def escribir() -> None:
    for i, idioma in enumerate(IDIOMAS):
        carpeta = os.path.join(RAIZ, "values" if idioma == "en" else f"values-{idioma}")
        os.makedirs(carpeta, exist_ok=True)
        lineas = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- Generado por traducciones/generar.py: no editar a mano. -->", "<resources>"]
        if idioma == "en":
            lineas.append('    <string name="app_name" translatable="false">Qué Me Pongo</string>')
            lineas.append('    <string name="prenda_tipo_color" translatable="false">%1$s · %2$s</string>')
        for clave, textos in T.items():
            lineas.append(f'    <string name="{clave}">{escapar(textos[i])}</string>')
        for clave, formas in P.items():
            uno, varios = formas[i]
            lineas.append(f'    <plurals name="{clave}">')
            lineas.append(f'        <item quantity="one">{escapar(uno)}</item>')
            lineas.append(f'        <item quantity="other">{escapar(varios)}</item>')
            # Francés, italiano y portugués usan "many" para números muy grandes.
            if idioma in ("fr", "it", "pt"):
                lineas.append(f'        <item quantity="many">{escapar(varios)}</item>')
            lineas.append("    </plurals>")
        lineas.append("</resources>")
        with open(os.path.join(carpeta, "strings.xml"), "w", encoding="utf-8") as f:
            f.write("\n".join(lineas) + "\n")
    print(f"{len(T)} textos y {len(P)} plurales en {len(IDIOMAS)} idiomas")


if __name__ == "__main__":
    revisar()
    escribir()
