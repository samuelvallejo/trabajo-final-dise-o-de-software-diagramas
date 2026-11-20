"""Crea las etiquetas y los 15 issues de historias de usuario en GitHub usando gh CLI.

Uso:  python crear_issues.py <owner/repo> [--dry-run]
Requiere gh autenticado con permiso de escritura. Es idempotente: no duplica
etiquetas ni issues cuyo título ya exista.
"""
import json
import os
import subprocess
import sys
import tempfile

from historias import ETIQUETAS, HISTORIAS, cuerpo_de, etiquetas_de, titulo_de

GH = os.environ.get("GH_BIN", "gh")
repo = sys.argv[1]
dry = "--dry-run" in sys.argv
AQUI = os.path.dirname(os.path.abspath(__file__))


def gh(*args, capture=True):
    r = subprocess.run([GH, *args], capture_output=capture, text=True, encoding="utf-8")
    if r.returncode != 0:
        raise SystemExit(f"gh {' '.join(args)}\n{r.stderr}")
    return r.stdout


# 1) Permiso de escritura
perm = json.loads(gh("api", f"repos/{repo}", "--jq", ".permissions"))
if not perm.get("push"):
    raise SystemExit(f"Sin permiso de escritura en {repo}: {perm}")
print("Permiso de escritura: OK")

# 2) Etiquetas faltantes
existentes = {l["name"] for l in json.loads(gh("label", "list", "-R", repo, "--limit", "200", "--json", "name"))}
for nombre, (color, desc) in ETIQUETAS.items():
    if nombre in existentes:
        print(f"Etiqueta ya existe: {nombre}")
        continue
    print(f"Creando etiqueta: {nombre}")
    if not dry:
        gh("label", "create", nombre, "-R", repo, "--color", color, "--description", desc)

# 3) Issues (sin duplicar por título)
issues = json.loads(gh("issue", "list", "-R", repo, "--state", "all", "--limit", "500",
                       "--json", "number,title,url"))
por_titulo = {i["title"]: i for i in issues}
resultado = []
for h in HISTORIAS:
    titulo = titulo_de(h)
    if titulo in por_titulo:
        print(f"Ya existe: {titulo}")
        url = por_titulo[titulo]["url"]
    elif dry:
        print(f"[dry-run] Crearía: {titulo}  {etiquetas_de(h)}")
        url = ""
    else:
        with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False, encoding="utf-8") as f:
            f.write(cuerpo_de(h))
            body = f.name
        args = ["issue", "create", "-R", repo, "--title", titulo, "--body-file", body]
        for e in etiquetas_de(h):
            args += ["--label", e]
        url = gh(*args).strip().splitlines()[-1]
        os.unlink(body)
        print(f"Creado: {titulo} -> {url}")
    resultado.append((h, url))

# 4) Documento resumen con enlaces
roles = lambda h: ", ".join([h["rol"]] + h.get("extra_roles", [])).replace("operador", "operador de limpieza")
filas = "\n".join(
    f"| HU-{h['id']} | {h['titulo']} | {roles(h).capitalize()} | {h['prioridad']} | {h['puntos']} | "
    f"{'[#' + url.rsplit('/', 1)[-1] + '](' + url + ')' if url else '—'} |"
    for h, url in resultado)
total = sum(h["puntos"] for h, _ in resultado)
detalle = "\n\n---\n\n".join(f"# {titulo_de(h)}\n\n{cuerpo_de(h)}" for h, _ in resultado)
with open(os.path.join(AQUI, "historias-de-usuario.md"), "w", encoding="utf-8") as f:
    f.write("# EcoGestión – Historias de usuario\n\n"
            "| N.º | Historia | Rol | Prioridad | Puntos | Issue |\n|---|---|---|---|---|---|\n"
            f"{filas}\n\n**Total:** {total} puntos de historia.\n\n---\n\n{detalle}\n")
print("Resumen escrito en historias-de-usuario.md")
