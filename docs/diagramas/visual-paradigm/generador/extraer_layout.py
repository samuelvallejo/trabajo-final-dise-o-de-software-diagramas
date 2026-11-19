"""Extrae las cajas (x, y, w, h) de las clases de un SVG de PlantUML -> CSV id;x;y;w;h.

El plugin de Visual Paradigm usa estos CSV para colocar cada clase en la misma posición que el
diagrama PlantUML de referencia.
Uso: python extraer_layout.py diagrama.svg > layouts/diagrama.csv
"""
import re
import sys

svg = open(sys.argv[1], encoding="utf-8").read()
for parte in re.split(r'(?=<g class="(?:entity|cluster)")', svg):
    m = re.match(r'<g class="(entity|cluster)" data-entity="([^"]+)"', parte)
    if not m:
        continue
    cuerpo = parte.split('<g class="link"')[0]
    r = re.search(r'<rect[^>]*?height="([\d.]+)"[^>]*?width="([\d.]+)"[^>]*?x="([\d.]+)"[^>]*?y="([\d.]+)"', cuerpo)
    if r:
        h, w, x, y = map(float, r.groups())
        print("%s;%.0f;%.0f;%.0f;%.0f" % (m.group(2), x, y, w, h))
