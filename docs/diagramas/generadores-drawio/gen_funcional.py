"""Genera la vista funcional (diagrama de componentes UML) de EcoGestión en formato draw.io.

Estructura: 9 componentes en cuadrícula 3x3 dentro del marco del sistema, conexiones ortogonales
con el conector de ensamble (bola = interfaz provista, socket = interfaz requerida) y una etiqueta
con el servicio que se consume. Dos interfaces requeridas salen hacia servicios externos.
"""
import math
import sys
from xml.sax.saxutils import escape

out = sys.argv[1]
cells = []
n = [10]


def nid():
    n[0] += 1
    return f"f{n[0]}"


def esc(t):
    return escape(t, {'"': "&quot;"})


def vertex(label, x, y, w, h, style):
    cells.append(f'<mxCell id="{nid()}" value="{esc(label)}" style="{style}" vertex="1" parent="1">'
                 f'<mxGeometry x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" as="geometry"/></mxCell>')


def polyline(points, dashed=False):
    (x1, y1), (x2, y2) = points[0], points[-1]
    mid = "".join(f'<mxPoint x="{x:.1f}" y="{y:.1f}"/>' for x, y in points[1:-1])
    arr = f'<Array as="points">{mid}</Array>' if mid else ""
    style = "endArrow=none;html=1;strokeWidth=1.6;strokeColor=#222222;rounded=0;" + ("dashed=1;" if dashed else "")
    cells.append(f'<mxCell id="{nid()}" value="" style="{style}" edge="1" parent="1">'
                 f'<mxGeometry relative="1" as="geometry"><mxPoint x="{x1:.1f}" y="{y1:.1f}" as="sourcePoint"/>'
                 f'<mxPoint x="{x2:.1f}" y="{y2:.1f}" as="targetPoint"/>{arr}</mxGeometry></mxCell>')


def text(t, x, y, w, h, align="center", size=13, bold=False):
    style = (f"text;html=1;whiteSpace=wrap;align={align};verticalAlign=middle;fontFamily=Helvetica;"
             f"fontSize={size};fontColor=#111111;{'fontStyle=1;' if bold else ''}")
    vertex(t, x, y, w, h, style)


BALL_R, SOCK_R = 8, 14
# dirección (hacia el proveedor) -> ángulos del arco del socket (0 = arriba, sentido horario)
ARCO = {"R": (0.5, 1.0), "L": (0.0, 0.5), "D": (0.75, 0.25), "U": (0.25, 0.75)}
VEC = {"R": (1, 0), "L": (-1, 0), "D": (0, 1), "U": (0, -1)}


def ensamble(px, py, hacia):
    """Bola (lado proveedor) dentro del socket (lado consumidor) en el punto (px, py)."""
    vertex("", px - BALL_R, py - BALL_R, 2 * BALL_R, 2 * BALL_R,
           "ellipse;html=1;fillColor=#FFFFFF;strokeColor=#222222;strokeWidth=1.6;")
    a0, a1 = ARCO[hacia]
    vertex("", px - SOCK_R, py - SOCK_R, 2 * SOCK_R, 2 * SOCK_R,
           f"shape=mxgraph.basic.arc;html=1;startAngle={a0};endAngle={a1};fillColor=none;"
           "strokeColor=#222222;strokeWidth=1.6;")


def conexion(consumidor_pts, proveedor_pts, punto, hacia, etiqueta, lx, ly, lw, lh, align="center"):
    """consumidor_pts termina en el socket; proveedor_pts empieza en la bola."""
    ux, uy = VEC[hacia]
    px, py = punto
    polyline(consumidor_pts + [(px - ux * SOCK_R, py - uy * SOCK_R)])
    polyline([(px + ux * BALL_R, py + uy * BALL_R)] + proveedor_pts)
    ensamble(px, py, hacia)
    text(etiqueta, lx, ly, lw, lh, align)


# ------------------------------------------------------------------ marco y componentes
W, H = 300, 130
COLS = [130, 630, 1130]
ROWS = [110, 430, 750]
FRAME = (40, 30, 1600, 945)
vertex("", FRAME[0], FRAME[1], FRAME[2] - FRAME[0], FRAME[3] - FRAME[1],
       "rounded=1;arcSize=2;html=1;fillColor=#FFFFFF;strokeColor=#333333;strokeWidth=1.6;")
text("EcoGestión – Aplicación Spring Boot", FRAME[0] + 14, FRAME[1] + 6, 320, 24, "left", 13, True)

NOMBRES = [["Clasificación y\npriorización", "Gestión de\nreportes", "Horarios y\nrutas"],
           ["Estadísticas e\ninformes", "Gestión de\nusuarios", "Asignación y\natención"],
           ["Contenido\neducativo", "Notificaciones", "Puntos de\nrecolección"]]
for r, fila in enumerate(NOMBRES):
    for c, nombre in enumerate(fila):
        x, y = COLS[c], ROWS[r]
        vertex("", x, y, W, H, "rounded=0;html=1;fillColor=#FFF2CC;strokeColor=#222222;strokeWidth=1.6;")
        text("«componente»", x + 8, y + 6, 160, 22, "left", 13)
        text(nombre.replace("\n", "<br>"), x, y + 34, W, 80, "center", 17, True)
        # icono UML de componente
        vertex("", x + W - 34, y + 10, 22, 26, "shape=component;html=1;jettyWidth=8;jettyHeight=4;"
               "fillColor=#FFF2CC;strokeColor=#222222;")


def caja(r, c):
    x, y = COLS[c], ROWS[r]
    return dict(l=x, r=x + W, t=y, b=y + H, cx=x + W / 2, cy=y + H / 2)


CLA, REP, HOR = caja(0, 0), caja(0, 1), caja(0, 2)
EST, USU, ASI = caja(1, 0), caja(1, 1), caja(1, 2)
EDU, NOT, PUN = caja(2, 0), caja(2, 1), caja(2, 2)
G1 = (ROWS[0] + H + ROWS[1]) / 2       # canal horizontal entre fila 0 y 1
G2 = (ROWS[1] + H + ROWS[2]) / 2       # canal horizontal entre fila 1 y 2
GX1 = (COLS[0] + W + COLS[1]) / 2      # canal vertical entre columnas 0 y 1
GX2 = (COLS[1] + W + COLS[2]) / 2      # canal vertical entre columnas 1 y 2

# 1. Reportes -> Clasificación (horizontal)
y = REP["cy"] - 20
conexion([(REP["l"], y)], [(CLA["r"], y)], (GX1, y), "L",
         "Clasificar y<br>priorizar", GX1 - 80, y - 52, 160, 38)
# 2. Reportes -> Horarios y rutas (horizontal)
conexion([(REP["r"], y)], [(HOR["l"], y)], (GX2, y), "R",
         "Identificar<br>sector", GX2 - 80, y - 52, 160, 38)
# 3. Reportes -> Usuarios (vertical)
x = USU["cx"]
conexion([(x, REP["b"])], [(x, USU["t"])], (x, G1), "D",
         "Validar<br>ciudadano", x + 22, G1 - 19, 90, 38, "left")
# 4. Asignación -> Usuarios (horizontal)
y = USU["cy"] + 20
conexion([(ASI["l"], y)], [(USU["r"], y)], (GX2, y), "L",
         "Consultar<br>operadores", GX2 - 80, y + 18, 160, 38)
# 5. Asignación -> Reportes (consultar / actualizar estado)
xa, xr = ASI["l"] + 70, REP["r"] - 30
conexion([(xa, ASI["t"]), (xa, G1 + 20)], [(xr, G1 + 20), (xr, REP["b"])], ((xa + xr) / 2 + 40, G1 + 20), "L",
         "Consultar y actualizar<br>estado del reporte", (xa + xr) / 2 + 40 - 90, G1 + 28, 180, 38)
# 6. Notificaciones -> Usuarios (datos de contacto)
x = USU["cx"]
conexion([(x, NOT["t"])], [(x, USU["b"])], (x, G2), "U",
         "Obtener correo<br>del ciudadano", x + 22, G2 - 34, 140, 38, "left")
# 7. Asignación -> Notificaciones (notificar cambio de estado)
xr2 = NOT["r"] - 60
conexion([(xa, ASI["b"]), (xa, G2 + 20)], [(xr2, G2 + 20), (xr2, NOT["t"])], ((xa + xr2) / 2 + 40, G2 + 20), "L",
         "Notificar cambio<br>de estado", (xa + xr2) / 2 + 40 - 90, G2 + 28, 180, 38)
# 8. Estadísticas -> Reportes (consultar reportes)
xe, xr3 = EST["r"] - 70, REP["l"] + 60
conexion([(xe, EST["t"]), (xe, G1 + 20)], [(xr3, G1 + 20), (xr3, REP["b"])], ((xe + xr3) / 2 - 40, G1 + 20), "R",
         "Consultar reportes", (xe + xr3) / 2 - 40 - 110, G1 + 30, 220, 24)
# 9. Contenido educativo -> Usuarios (validar administrador)
xc, xu = EDU["r"] - 70, USU["l"] + 60
conexion([(xc, EDU["t"]), (xc, G2 + 20)], [(xu, G2 + 20), (xu, USU["b"])], ((xc + xu) / 2 - 40, G2 + 20), "R",
         "Validar rol de administrador", (xc + xu) / 2 - 40 - 130, G2 - 14, 260, 24)
# 10. Estadísticas -> Clasificación (tipos y prioridades) (vertical)
x = CLA["cx"] - 60
conexion([(x, EST["t"])], [(x, CLA["b"])], (x, G1 - 10), "U",
         "Consultar tipos<br>y prioridades", x - 160, G1 - 30, 140, 38, "right")
# 11. Puntos de recolección -> Horarios y rutas (sectores), rodeando por la derecha
xr4 = FRAME[2] - 130
yp, yh = PUN["cy"], HOR["cy"] + 25
conexion([(PUN["r"], yp), (xr4, yp)], [(xr4, yh), (HOR["r"], yh)], (xr4, (yp + yh) / 2), "U",
         "Consultar<br>sector", xr4 + 22, (yp + yh) / 2 - 20, 100, 38, "left")

# 12-13. Interfaces requeridas hacia servicios externos (socket abierto en el borde del marco)
def requerida_externa(x1, y1, x2, y2, hacia, etiqueta, lx, ly, lw, lh, align):
    ux, uy = VEC[hacia]
    polyline([(x1, y1), (x2 - ux * SOCK_R, y2 - uy * SOCK_R)])
    a0, a1 = ARCO[hacia]
    vertex("", x2 - SOCK_R, y2 - SOCK_R, 2 * SOCK_R, 2 * SOCK_R,
           f"shape=mxgraph.basic.arc;html=1;startAngle={a0};endAngle={a1};fillColor=none;"
           "strokeColor=#222222;strokeWidth=1.6;")
    text(etiqueta, lx, ly, lw, lh, align)


x = REP["cx"] + 80
requerida_externa(x, REP["t"], x, FRAME[1] + 10, "U", "Servicio de mapas/GPS", x + 22, FRAME[1] + 2, 200, 24, "left")
x = NOT["cx"] + 80
requerida_externa(x, NOT["b"], x, FRAME[3] - 10, "D", "Canal de notificación (correo · SMS · push)",
                  x + 22, FRAME[3] - 32, 320, 24, "left")

xml = ('<mxfile host="app.diagrams.net"><diagram id="funcional" name="Vista funcional">'
       '<mxGraphModel dx="1640" dy="980" grid="0" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" '
       'fold="1" page="1" pageScale="1" pageWidth="1640" pageHeight="980" background="#FFFFFF" math="0" shadow="0">'
       '<root><mxCell id="0"/><mxCell id="1" parent="0"/>' + "".join(cells) + '</root></mxGraphModel></diagram></mxfile>')
open(out, "w", encoding="utf-8").write(xml)
print("ok", out)
