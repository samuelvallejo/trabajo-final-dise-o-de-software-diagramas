"""Genera el diagrama de contexto (DFD nivel 0) de EcoGestión en formato draw.io."""
import math
import sys
from xml.sax.saxutils import escape

out = sys.argv[1]
cells = []
nid = [10]


def new_id():
    nid[0] += 1
    return f"c{nid[0]}"


def vertex(label, x, y, w, h, style):
    cells.append(
        f'<mxCell id="{new_id()}" value="{escape(label, {chr(34): "&quot;"})}" style="{style}" vertex="1" parent="1">'
        f'<mxGeometry x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" as="geometry"/></mxCell>'
    )


def arrow(x1, y1, x2, y2):
    cells.append(
        f'<mxCell id="{new_id()}" value="" style="endArrow=classic;endFill=1;html=1;strokeWidth=1.6;strokeColor=#222222;" edge="1" parent="1">'
        f'<mxGeometry relative="1" as="geometry"><mxPoint x="{x1:.1f}" y="{y1:.1f}" as="sourcePoint"/>'
        f'<mxPoint x="{x2:.1f}" y="{y2:.1f}" as="targetPoint"/></mxGeometry></mxCell>'
    )


def text(label, x, y, w, h, align="center", valign="middle"):
    style = (f"text;html=1;whiteSpace=wrap;align={align};verticalAlign={valign};"
             "fontFamily=Helvetica;fontSize=14;fontColor=#222222;spacing=2;")
    vertex(label, x, y, w, h, style)


BOX = ("rounded=0;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#222222;strokeWidth=1.8;"
       "fontFamily=Helvetica;fontSize=17;fontStyle=1;fontColor=#222222;")
CIRCLE = ("ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#E8F5E9;strokeColor=#1B5E20;strokeWidth=2.4;"
          "fontFamily=Helvetica;fontSize=15;fontColor=#1B5E20;")

CX, CY, R = 700, 390, 125
vertex("<b style='font-size:24px'>EcoGestión</b><br>Sistema de gestión<br>de residuos sólidos", CX - R, CY - R, 2 * R, 2 * R, CIRCLE)

BW, BH = 250, 72

# ---------- Ciudadano (arriba) ----------
bx, by = CX - BW / 2, 25
vertex("Ciudadano", bx, by, BW, BH, BOX)
top = by + BH
for dx, down in ((-35, False), (35, True)):
    yc = CY - math.sqrt(R * R - dx * dx)
    if down:
        arrow(CX + dx, top, CX + dx, yc)
    else:
        arrow(CX + dx, yc, CX + dx, top)
text("Registro e inicio de sesión<br>Datos personales<br>Reporte + fotos + ubicación",
     CX + 45, top + 12, 300, 80, align="left")
text("Estado e historial de reportes<br>Horarios, rutas y puntos de recolección<br>Contenido educativo",
     CX - 45 - 320, top + 12, 320, 80, align="right")

# ---------- Administrador (izquierda) ----------
bx, by = 35, CY - BH / 2
vertex("Administrador", bx, by, BW, BH, BOX)
right = bx + BW
for dy, toward in ((-24, True), (24, False)):
    xc = CX - math.sqrt(R * R - dy * dy)
    if toward:
        arrow(right, CY + dy, xc, CY + dy)
    else:
        arrow(xc, CY + dy, right, CY + dy)
text("Clasificación, prioridad y asignación<br>Horarios, rutas, puntos y contenido",
     right + 4, CY - 24 - 52, 300, 48, align="center", valign="bottom")
text("Reportes nuevos y posibles duplicados<br>Panel, estadísticas e informes",
     right + 4, CY + 24 + 4, 300, 48, align="center", valign="top")

# ---------- Operador de limpieza (derecha) ----------
bx, by = 2 * CX - 35 - BW, CY - BH / 2
vertex("Operador de limpieza", bx, by, BW, BH, BOX)
left = bx
for dy, toward in ((-24, False), (24, True)):
    xc = CX + math.sqrt(R * R - dy * dy)
    if toward:
        arrow(left, CY + dy, xc, CY + dy)
    else:
        arrow(xc, CY + dy, left, CY + dy)
text("Reportes asignados + ubicación",
     CX + R - 4, CY - 24 - 52, left - (CX + R) + 8, 48, align="center", valign="bottom")
text("Estado de atención<br>Trabajo realizado + fotos de evidencia",
     CX + R - 4, CY + 24 + 4, left - (CX + R) + 8, 48, align="center", valign="top")


# ---------- Servicios externos (abajo, en diagonal) ----------
def diagonal_pair(bcx, bcy, label_out, label_in, out_side_first):
    """Dos flechas paralelas entre el círculo y la caja centrada en (bcx, bcy)."""
    vertex_label = None
    dx, dy = bcx - CX, bcy - CY
    L = math.hypot(dx, dy)
    ux, uy = dx / L, dy / L
    nx, ny = -uy, ux  # normal
    btop = bcy - BH / 2
    pts = []
    for o in (-17, 17):
        t0 = math.sqrt(R * R - o * o)
        sx, sy = CX + nx * o + ux * t0, CY + ny * o + uy * t0
        t1 = (btop - sy) / uy
        ex, ey = sx + ux * t1, sy + uy * t1
        pts.append((sx, sy, ex, ey, o))
    return pts, (nx, ny)


def service(bcx, label, req, resp, outer_left):
    bcy = 690
    vertex(label, bcx - BW / 2, bcy - BH / 2, BW, BH, BOX)
    pts, _ = diagonal_pair(bcx, bcy, None, None, None)
    # la linea "interior" es la mas cercana al eje vertical del circulo
    pts.sort(key=lambda p: abs((p[0] + p[2]) / 2 - CX))
    inner, outer = pts[0], pts[1]
    sx, sy, ex, ey, _ = outer           # sistema -> servicio (solicitud)
    arrow(sx, sy, ex, ey)
    mx, my = (sx + ex) / 2, (sy + ey) / 2
    if outer_left:
        text(req, mx - 40 - 300, my - 60, 300, 48, align="right", valign="bottom")
    else:
        text(req, mx + 40, my - 60, 300, 48, align="left", valign="bottom")
    sx, sy, ex, ey, _ = inner           # servicio -> sistema (respuesta)
    arrow(ex, ey, sx, sy)
    mx, my = sx + 0.8 * (ex - sx), sy + 0.8 * (ey - sy)
    if outer_left:
        text(resp, mx + 30, my - 24, 230, 48, align="left", valign="middle")
    else:
        text(resp, mx - 30 - 230, my - 24, 230, 48, align="right", valign="middle")


service(330, "Servicio de correo<br>electrónico",
        "Notificación de cambio de estado<br>(destinatario, asunto, mensaje)",
        "Confirmación de envío", True)
service(2 * CX - 330, "Servicio de mapas/GPS",
        "Solicitud de mapa<br>y geocodificación",
        "Coordenadas GPS, mapa base<br>y dirección", False)

xml = (
    '<mxfile host="app.diagrams.net"><diagram id="contexto" name="Vista de contexto">'
    '<mxGraphModel dx="1400" dy="780" grid="0" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" '
    'fold="1" page="1" pageScale="1" pageWidth="1400" pageHeight="780" background="#FFFFFF" math="0" shadow="0">'
    '<root><mxCell id="0"/><mxCell id="1" parent="0"/>' + "".join(cells) + "</root></mxGraphModel></diagram></mxfile>"
)
with open(out, "w", encoding="utf-8") as f:
    f.write(xml)
print("ok", out)
