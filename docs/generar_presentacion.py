"""Inserta los 5 diagramas en la plantilla de PowerPoint del curso.

Uso: python generar_presentacion.py plantilla.pptx diagramas EcoGestion-Diseno-de-arquitectura.pptx
Reemplaza la imagen de ejemplo de las diapositivas 2 a 6 (sin cambiar diseño ni orden) y el
texto "Título de la presentación".
"""
import sys

from PIL import Image
from pptx import Presentation
from pptx.util import Inches

src, diag_dir, out = sys.argv[1], sys.argv[2], sys.argv[3]
TITULO = "EcoGestión – Diseño de arquitectura"
DIAGRAMAS = {2: "01-vista-contexto.png", 3: "02-vista-funcional.png", 4: "03-vista-logica.png",
             5: "04-vista-desarrollo.png", 6: "05-vista-despliegue.png"}
ALT = {2: "Vista de contexto (DFD nivel 0) de EcoGestión",
       3: "Vista funcional: diagrama de componentes UML de EcoGestión",
       4: "Vista lógica: modelo de clases inicial de EcoGestión",
       5: "Vista de desarrollo: diagrama de clases por capas de EcoGestión",
       6: "Vista de despliegue: diagrama de despliegue UML de EcoGestión"}
BOX_L, BOX_T, BOX_W, BOX_H = 0.7, 1.45, 11.93, 5.8   # área bajo el título (pulgadas)
A = "{http://schemas.openxmlformats.org/drawingml/2006/main}"
R_EMBED = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}embed"

prs = Presentation(src)
for idx, slide in enumerate(prs.slides, start=1):
    for shape in slide.shapes:
        if shape.has_text_frame and shape.text_frame.text.strip() == "Título de la presentación":
            p = shape.text_frame.paragraphs[0]
            p.runs[0].text = TITULO
            for extra in p.runs[1:]:
                extra.text = ""
    if idx not in DIAGRAMAS:
        continue
    pic = [s for s in slide.shapes if s.shape_type == 13][0]
    path = f"{diag_dir}/{DIAGRAMAS[idx]}"
    _, new_rid = slide.part.get_or_add_image_part(path)
    blip = pic._element.blipFill.find(A + "blip")
    old_rid = blip.get(R_EMBED)
    blip.set(R_EMBED, new_rid)
    src_rect = pic._element.blipFill.find(A + "srcRect")
    if src_rect is not None:
        pic._element.blipFill.remove(src_rect)
    if old_rid != new_rid:
        slide.part.drop_rel(old_rid)
    w, h = Image.open(path).size
    k = min(BOX_W / w, BOX_H / h)
    pic.left, pic.top = Inches(BOX_L + (BOX_W - w * k) / 2), Inches(BOX_T + (BOX_H - h * k) / 2)
    pic.width, pic.height = Inches(w * k), Inches(h * k)
    pic._element.nvPicPr.cNvPr.set("descr", ALT[idx])
    pic._element.nvPicPr.cNvPr.set("name", f"Diagrama {idx - 1}")
prs.save(out)
print("ok", out)
