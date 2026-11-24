"""Genera la vista de despliegue de EcoGestión (UML) en formato draw.io con posiciones fijas."""
import sys
from xml.sax.saxutils import escape

out = sys.argv[1]
cells = []
n = [100]


def nid():
    n[0] += 1
    return f"d{n[0]}"


def esc(t):
    return escape(t, {'"': "&quot;"})


NODE = ("shape=cube;size=12;direction=south;whiteSpace=wrap;html=1;boundedLbl=1;verticalAlign=top;"
        "align=center;fillColor=#FFFFFF;strokeColor=#333333;strokeWidth=1.6;fontFamily=Helvetica;"
        "fontSize=14;fontStyle=1;fontColor=#111111;spacingTop=2;")
ENV = NODE.replace("fontSize=14", "fontSize=13")
ART = ("rounded=1;arcSize=6;whiteSpace=wrap;html=1;fillColor=#E8F5E9;strokeColor=#1B5E20;"
       "fontFamily=Helvetica;fontSize=13;fontStyle=1;fontColor=#111111;verticalAlign=middle;")
ICON = "shape=note;size=5;html=1;fillColor=#E8F5E9;strokeColor=#1B5E20;"
LBL = ("text;html=1;whiteSpace=wrap;fontFamily=Helvetica;fontSize=13;fontColor=#111111;"
       "align={a};verticalAlign=middle;")


def box(label, x, y, w, h, style):
    cells.append(f'<mxCell id="{nid()}" value="{esc(label)}" style="{style}" vertex="1" parent="1">'
                 f'<mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>')


def node(stereo, name, x, y, w, h, env=False):
    box(f"&laquo;{stereo}&raquo;<br>{name}", x, y, w, h, ENV if env else NODE)


def artifact(name, x, y, w, h):
    box(f"&laquo;artifact&raquo;<br>{name}", x, y, w, h, ART)
    box("", x + w - 18, y + 6, 11, 14, ICON)


def line(x1, y1, x2, y2):
    cells.append(f'<mxCell id="{nid()}" value="" style="endArrow=none;html=1;strokeWidth=1.6;strokeColor=#222222;" '
                 f'edge="1" parent="1"><mxGeometry relative="1" as="geometry">'
                 f'<mxPoint x="{x1}" y="{y1}" as="sourcePoint"/><mxPoint x="{x2}" y="{y2}" as="targetPoint"/>'
                 f'</mxGeometry></mxCell>')


def label(t, x, y, w, h, a="center"):
    box(t, x, y, w, h, LBL.format(a=a))


# ---------------- fila superior: clientes ----------------
TOP, TH = 20, 250
# Smartphone del ciudadano
node("device", ":Smartphone del ciudadano", 20, TOP, 460, TH)
node("executionEnvironment", ":Navegador web móvil", 40, TOP + 70, 270, 160, env=True)
artifact("vistas del ciudadano<br>(HTML Thymeleaf + JS + Leaflet)", 60, TOP + 135, 220, 70)
node("device", ":Sensor GPS", 330, TOP + 120, 130, 60)
# PC
node("device", ":PC del ciudadano / administrador", 530, TOP, 380, TH)
node("executionEnvironment", ":Navegador web", 555, TOP + 70, 320, 160, env=True)
artifact("vistas web<br>(HTML Thymeleaf + JS + Leaflet)", 590, TOP + 135, 240, 70)
# Smartphone del operador
node("device", ":Smartphone del operador", 960, TOP, 380, TH)
node("executionEnvironment", ":Navegador web móvil", 985, TOP + 70, 320, 160, env=True)
artifact("vistas del operador<br>(HTML Thymeleaf + JS + Leaflet)", 1020, TOP + 135, 240, 70)

# ---------------- servidor de aplicación ----------------
SX, SY, SW, SH = 400, 360, 560, 240
node("device", ":Servidor web/API (Railway)", SX, SY, SW, SH)
node("executionEnvironment", ":JVM 21 · Spring Boot 4 (Tomcat embebido)", SX + 20, SY + 60, SW - 50, 160, env=True)
artifact("ecogestion.jar<br>(controller, service, repository)", SX + 40, SY + 120, 250, 70)
artifact("templates/<br>(vistas Thymeleaf)", SX + 310, SY + 120, 190, 70)

# ---------------- fila inferior ----------------
BY, BH, BW = 730, 200, 380
xs = [40, 490, 940]
node("device", ":Servidor de base de datos", xs[0], BY, BW, BH)
node("executionEnvironment", ":MongoDB Atlas (AWS N. Virginia)", xs[0] + 15, BY + 55, BW - 40, 135, env=True)
artifact("ecogestion: colecciones, índices 2dsphere<br>y fotos en GridFS", xs[0] + 30, BY + 112, BW - 75, 64)
node("device", ":Servicio de correo electrónico<br>(servidor SMTP)", xs[1], BY, BW, BH)
artifact("relay SMTP", xs[1] + 80, BY + 95, BW - 160, 70)
node("device", ":Servicio de mapas/GPS<br>(OpenStreetMap)", xs[2], BY, BW, BH)
artifact("Teselas del mapa<br>+ Nominatim (geocodificación)", xs[2] + 80, BY + 95, BW - 160, 70)

# ---------------- conexiones ----------------
top_y = TOP + TH
# clientes -> servidor
line(250, top_y, 520, SY + 12)
label("HTTPS:443<br>(HTML/JSON)", 130, 300, 150, 40, "right")
line(720, top_y, 680, SY + 12)
label("HTTPS:443<br>(HTML/JSON)", 728, 295, 150, 40, "left")
line(1150, top_y, 840, SY + 12)
label("HTTPS:443<br>(HTML/JSON)", 1100, 300, 150, 40, "left")

# servidor -> fila inferior
sb = SY + SH
line(520, sb, xs[0] + BW / 2, BY + 12)
label("MongoDB Wire Protocol<br>TLS · TCP:27017", 150, 640, 190, 44, "right")
line(680, sb, xs[1] + BW / 2, BY + 12)
label("SMTP/TLS:587", 700, 655, 130, 30, "left")
line(840, sb, xs[2] + BW / 2, BY + 12)
label("HTTPS:443<br>(Nominatim)", 1010, 622, 170, 44, "left")

xml = ('<mxfile host="app.diagrams.net"><diagram id="despliegue" name="Vista de despliegue">'
       '<mxGraphModel dx="1360" dy="950" grid="0" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" '
       'fold="1" page="1" pageScale="1" pageWidth="1380" pageHeight="950" background="#FFFFFF" math="0" shadow="0">'
       '<root><mxCell id="0"/><mxCell id="1" parent="0"/>' + "".join(cells) +
       '</root></mxGraphModel></diagram></mxfile>')
open(out, "w", encoding="utf-8").write(xml)
print("ok", out)
