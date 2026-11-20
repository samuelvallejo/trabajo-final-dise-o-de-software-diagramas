package ecogestion.vp;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.DiagramManager;
import com.vp.plugin.ExportDiagramAsImageOption;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.VPPlugin;
import com.vp.plugin.VPPluginCommandLineSupport;
import com.vp.plugin.VPPluginInfo;
import com.vp.plugin.diagram.IClassDiagramUIModel;
import com.vp.plugin.diagram.IConnectorUIModel;
import com.vp.plugin.diagram.IDiagramTypeConstants;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.IShapeTypeConstants;
import com.vp.plugin.diagram.IShapeUIModel;
import com.vp.plugin.diagram.shape.IComponentInterfaceUIModel;
import com.vp.plugin.diagram.format.IFillColor;
import com.vp.plugin.model.*;
import com.vp.plugin.model.factory.IModelElementFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Genera en Visual Paradigm los 5 diagramas de arquitectura de EcoGestión.
 * Uso (línea de comandos de VP):
 *   Plugin -project semilla.vpp -pluginid ecogestion.vp -pluginargs "salida.vpp dirImagenes dirLayouts"
 */
public class EcoGestionVP implements VPPlugin, VPPluginCommandLineSupport {

    public void loaded(VPPluginInfo info) { }
    public void unloaded() { }

    private final IModelElementFactory f = IModelElementFactory.instance();
    private DiagramManager dm;
    private IDiagramUIModel d;
    private final Map<String, IModelElement> m = new HashMap<>();
    private final Map<String, IShapeUIModel> v = new HashMap<>();
    private Map<String, Rectangle> pos = new HashMap<>();
    private File layoutDir;

    /** Opción de VP "mostrar flechas de navegación" (se fija tras probar los valores en la exportación). */
    private static final int NAV_ARROWS = Integer.getInteger("ecogestion.nav", 3);
    private static final Color INK = Color.decode("#222222");
    private static final Color AMARILLO = Color.decode("#FFF2CC");
    private static final Color VERDE = Color.decode("#DCEDC8");
    private static final Color VERDE_CLARO = Color.decode("#E8F5E9");
    private static final Color GRIS = Color.decode("#E6E6E6");
    private static final Color ENUM = Color.decode("#FFF9C4");
    private static final Color AZUL = Color.decode("#E1F5FE");

    // ------------------------------------------------------------------ entrada
    public void invoke(String[] a) {
        try {
            if (a.length < 3) throw new IllegalArgumentException("args: salida.vpp dirImagenes dirLayouts");
            dm = ApplicationManager.instance().getDiagramManager();
            ProjectManager pm = ApplicationManager.instance().getProjectManager();
            IProject p = pm.getProject();
            for (IDiagramUIModel z : p.toDiagramArray()) z.delete();
            for (IModelElement e : p.toModelElementArray()) {
                try { e.delete(); } catch (UnsupportedOperationException ignorado) { /* elementos internos del proyecto */ }
            }
            layoutDir = new File(a[2]);

            List<IDiagramUIModel> ds = new ArrayList<>();
            ds.add(contexto());
            ds.add(funcional());
            ds.add(logica());
            ds.add(desarrollo());
            ds.add(despliegue());

            if (!pm.saveProjectAs(new File(a[0]))) throw new IllegalStateException("No se pudo guardar el proyecto");
            String[] png = {"01-vista-contexto.png", "02-vista-funcional.png", "03-vista-logica.png",
                    "04-vista-desarrollo.png", "05-vista-despliegue.png"};
            for (int i = 0; i < ds.size(); i++) {
                ExportDiagramAsImageOption o = new ExportDiagramAsImageOption(ExportDiagramAsImageOption.IMAGE_TYPE_PNG);
                o.setScale(2f);
                o.setTextAntiAliasing(true);
                o.setGraphicAntiAliasing(true);
                ApplicationManager.instance().getModelConvertionManager()
                        .exportDiagramAsImage(ds.get(i), new File(a[1], png[i]), o);
            }
            System.out.println("ECOGESTION_OK diagramas=" + p.toDiagramArray().length);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("ECOGESTION_ERROR " + t);
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ utilidades
    private IDiagramUIModel nuevo(String tipo, String nombre) {
        d = dm.createDiagram(tipo);
        d.setName(nombre);
        d.setDiagramBackground(Color.WHITE);
        m.clear();
        v.clear();
        return d;
    }

    private void estilo(IShapeUIModel s, Color fill, int size, boolean bold) {
        s.getFillColor().setType(IFillColor.TYPE_SOLID, false);
        s.getFillColor().setColor1(fill, false);
        s.getLineModel().setColor(INK, false);
        s.getLineModel().setWeight(1.3f, false);
        s.getElementFont().setValues("Arial", bold, false, size, INK);
    }

    private IShapeUIModel forma(String key, IModelElement e, int x, int y, int w, int h) {
        IShapeUIModel s = (IShapeUIModel) dm.createDiagramElement(d, e);
        s.setBounds(x, y, w, h);
        s.setRequestResetCaption(true);
        m.put(key, e);
        v.put(key, s);
        return s;
    }

    private Point[] pts(int... xy) {
        Point[] p = new Point[xy.length / 2];
        for (int i = 0; i < p.length; i++) p[i] = new Point(xy[2 * i], xy[2 * i + 1]);
        return p;
    }

    private Point centro(String key) {
        IShapeUIModel s = v.get(key);
        return new Point(s.getX() + s.getWidth() / 2, s.getY() + s.getHeight() / 2);
    }

    /** Punto donde el segmento desde el centro de la forma hacia "hacia" cruza su borde. */
    private Point borde(IShapeUIModel s, Point hacia) {
        double cx = s.getX() + s.getWidth() / 2.0, cy = s.getY() + s.getHeight() / 2.0;
        double dx = hacia.x - cx, dy = hacia.y - cy;
        if (dx == 0 && dy == 0) return new Point((int) cx, (int) cy);
        double tx = dx == 0 ? Double.MAX_VALUE : (s.getWidth() / 2.0) / Math.abs(dx);
        double ty = dy == 0 ? Double.MAX_VALUE : (s.getHeight() / 2.0) / Math.abs(dy);
        double t = Math.min(tx, ty);
        return new Point((int) Math.round(cx + dx * t), (int) Math.round(cy + dy * t));
    }

    private IConnectorUIModel conector(IModelElement rel, String from, String to, Point[] puntos) {
        if (puntos == null) {
            Point a = centro(from), b = centro(to);
            puntos = new Point[]{borde(v.get(from), b), borde(v.get(to), a)};
        }
        IConnectorUIModel c = (IConnectorUIModel) dm.createConnector(d, rel, v.get(from), v.get(to), puntos);
        c.getLineModel().setColor(INK, false);
        c.getElementFont().setValues("Arial", false, false, 12, INK);
        if (rel.getName() != null && !rel.getName().isEmpty()) {
            // sin layout automático VP no calcula la posición del nombre: se coloca junto al tramo central
            int k = 0;   // tramo más largo
            for (int i = 1; i < puntos.length - 1; i++)
                if (puntos[i].distance(puntos[i + 1]) > puntos[k].distance(puntos[k + 1])) k = i;
            Point a = puntos[k], b = puntos[k + 1];
            int mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
            int w = rel.getName().length() * 7 + 12;
            c.setShowConnectorName(IDiagramUIModel.SHOW_CONNECTOR_NAME_YES);
            c.getCaptionUIModel().setVisible(true);
            c.getCaptionUIModel().setBounds(mx + 6, my - 9, w, 18);
        }
        return c;
    }

    private void caption(IConnectorUIModel c, int x, int y, int w, int h) {
        c.setShowConnectorName(IDiagramUIModel.SHOW_CONNECTOR_NAME_YES);
        c.getCaptionUIModel().setVisible(true);
        c.getCaptionUIModel().setBounds(x, y, w, h);
    }

    private void cargarLayout(String archivo, double escala) throws Exception {
        pos = new HashMap<>();
        for (String l : Files.readAllLines(new File(layoutDir, archivo).toPath(), StandardCharsets.UTF_8)) {
            String[] t = l.trim().split(";");
            if (t.length < 5) continue;
            pos.put(t[0], new Rectangle((int) (Integer.parseInt(t[1]) * escala), (int) (Integer.parseInt(t[2]) * escala),
                    Integer.parseInt(t[3]), Integer.parseInt(t[4])));
        }
    }

    /** Coloca la forma centrada en el centro escalado del layout de referencia, con su tamaño natural. */
    private void ubicar(String key, double escala) {
        Rectangle r = pos.get(key);
        IShapeUIModel s = v.get(key);
        if (r == null || s == null) return;
        int cx = r.x + (int) (r.width * escala / 2), cy = r.y + (int) (r.height * escala / 2);
        s.setBounds(cx - s.getWidth() / 2, cy - s.getHeight() / 2, s.getWidth(), s.getHeight());
    }

    // ================================================================== 1. CONTEXTO (DFD nivel 0)
    private IDiagramUIModel contexto() {
        nuevo(IDiagramTypeConstants.DIAGRAM_TYPE_YOURDON_DE_MARCO_DFDUI_MODEL, "1. Vista de contexto");
        final int CX = 700, CY = 390, R = 125, BW = 250, BH = 72;
        IYdDfdProcess proc = f.createYdDfdProcess();
        proc.setName("EcoGestión");
        IShapeUIModel ps = forma("sys", proc, CX - R, CY - R, 2 * R, 2 * R);
        estilo(ps, VERDE_CLARO, 20, true);
        entidad("ciu", "Ciudadano", CX - BW / 2, 25, BW, BH);
        entidad("adm", "Administrador", 35, CY - BH / 2, BW, BH);
        entidad("ope", "Operador de limpieza", 2 * CX - 35 - BW, CY - BH / 2, BW, BH);
        entidad("cor", "Servicio de correo electrónico", 330 - BW / 2, 690 - BH / 2, BW, BH);
        entidad("map", "Servicio de mapas/GPS", 2 * CX - 330 - BW / 2, 690 - BH / 2, BW, BH);

        // Ciudadano (arriba)
        int top = 25 + BH;
        int yc = (int) (CY - Math.sqrt(R * R - 35 * 35));
        flujo("ciu", "sys", "Registro e inicio de sesión\nDatos personales\nReporte + fotos + ubicación",
                pts(CX + 35, top, CX + 35, yc), CX + 45, top + 12, 230, 64);
        flujo("sys", "ciu", "Estado e historial de reportes\nHorarios, rutas y puntos\nde recolección\nContenido educativo",
                pts(CX - 35, yc, CX - 35, top), CX - 45 - 230, top + 12, 230, 64);
        // Administrador (izquierda)
        int right = 35 + BW;
        int xl = (int) (CX - Math.sqrt(R * R - 24 * 24));
        flujo("adm", "sys", "Clasificación, prioridad y asignación\nHorarios, rutas, puntos y contenido",
                pts(right, CY - 24, xl, CY - 24), right + 10, CY - 24 - 48, 280, 44);
        flujo("sys", "adm", "Reportes nuevos y posibles duplicados\nPanel, estadísticas e informes",
                pts(xl, CY + 24, right, CY + 24), right + 10, CY + 24 + 6, 280, 44);
        // Operador (derecha)
        int left = 2 * CX - 35 - BW;
        int xr = (int) (CX + Math.sqrt(R * R - 24 * 24));
        flujo("sys", "ope", "Reportes asignados + ubicación",
                pts(xr, CY - 24, left, CY - 24), xr + 10, CY - 24 - 30, 270, 26);
        flujo("ope", "sys", "Estado de atención\nTrabajo realizado + fotos de evidencia",
                pts(left, CY + 24, xr, CY + 24), xr + 10, CY + 24 + 6, 270, 44);
        // Servicios (diagonales)
        servicio("cor", 330, "Notificación de cambio de estado\n(destinatario, asunto, mensaje)", "Confirmación de envío", true);
        servicio("map", 2 * CX - 330, "Solicitud de mapa\ny geocodificación", "Coordenadas GPS,\nmapa base y dirección", false);
        return d;
    }

    private void entidad(String key, String nombre, int x, int y, int w, int h) {
        IYdDfdExternalEntity e = f.createYdDfdExternalEntity();
        e.setName(nombre);
        IShapeUIModel s = forma(key, e, x, y, w, h);
        estilo(s, Color.WHITE, 16, true);
    }

    private void flujo(String from, String to, String nombre, Point[] p, int tx, int ty, int tw, int th) {
        IYdDfdDataFlow r = f.createYdDfdDataFlow();
        r.setName(nombre);
        r.setFrom(m.get(from));
        r.setTo(m.get(to));
        IConnectorUIModel c = conector(r, from, to, p);
        c.setConnectorStyle(IConnectorUIModel.CS_OBLIQUE);
        c.getLineModel().setWeight(1.5f, false);
        c.getElementFont().setValues("Arial", false, false, 13, INK);
        caption(c, tx, ty, tw, th);
    }

    private void servicio(String key, int bcx, String solicitud, String respuesta, boolean outerLeft) {
        final int CX = 700, CY = 390, R = 125, BH = 72, bcy = 690;
        double dx = bcx - CX, dy = bcy - CY, L = Math.hypot(dx, dy);
        double ux = dx / L, uy = dy / L, nx = -uy, ny = ux;
        double btop = bcy - BH / 2.0;
        double[][] lines = new double[2][];
        int k = 0;
        for (int o : new int[]{-17, 17}) {
            double t0 = Math.sqrt(R * R - o * o);
            double sx = CX + nx * o + ux * t0, sy = CY + ny * o + uy * t0;
            double t1 = (btop - sy) / uy;
            lines[k++] = new double[]{sx, sy, sx + ux * t1, sy + uy * t1};
        }
        // interior = más cercana al eje vertical del proceso
        Arrays.sort(lines, Comparator.comparingDouble(l -> Math.abs((l[0] + l[2]) / 2 - CX)));
        double[] in = lines[0], out = lines[1];
        // sistema -> servicio (exterior)
        double mx = (out[0] + out[2]) / 2, my = (out[1] + out[3]) / 2;
        flujo("sys", key, solicitud, pts((int) out[0], (int) out[1], (int) out[2], (int) out[3]),
                outerLeft ? (int) mx - 40 - 260 : (int) mx + 40, (int) my - 60, 260, 44);
        // servicio -> sistema (interior)
        double px = in[0] + 0.8 * (in[2] - in[0]), py = in[1] + 0.8 * (in[3] - in[1]);
        flujo(key, "sys", respuesta, pts((int) in[2], (int) in[3], (int) in[0], (int) in[1]),
                outerLeft ? (int) px + 30 : (int) px - 30 - 190, (int) py - 22, 190, 44);
    }

    // ================================================================== 2. FUNCIONAL (componentes)
    // Misma estructura que la imagen: 9 componentes en cuadrícula 3x3 dentro del sistema, conexiones
    // ortogonales nombradas con el servicio requerido (consumidor -> proveedor) y dos interfaces externas.
    private static final int CW = 300, CH = 130;
    private static final int[] COLS = {130, 630, 1130}, ROWS = {110, 430, 750};

    private IDiagramUIModel funcional() {
        nuevo(IDiagramTypeConstants.DIAGRAM_TYPE_COMPONENT_DIAGRAM, "2. Vista funcional");
        IPackage sistema = f.createPackage();
        sistema.setName("EcoGestión – Aplicación Spring Boot");
        IShapeUIModel marco = forma("sistema", sistema, 40, 30, 1560, 915);
        estilo(marco, Color.WHITE, 13, true);
        marco.getFillColor().setTransparency(100, false);

        String[][][] grid = {
                {{"CLA", "Clasificación y priorización"}, {"REP", "Gestión de reportes"}, {"HOR", "Horarios y rutas"}},
                {{"EST", "Estadísticas e informes"}, {"USU", "Gestión de usuarios"}, {"ASI", "Asignación y atención"}},
                {{"EDU", "Contenido educativo"}, {"NOT", "Notificaciones"}, {"PUN", "Puntos de recolección"}}};
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) {
                IComponent comp = f.createComponent();
                sistema.addChild(comp);
                comp.setName(grid[r][c][1]);
                IShapeUIModel s = forma(grid[r][c][0], comp, COLS[c], ROWS[r], CW, CH);
                estilo(s, AMARILLO, 15, true);
            }
        // interfaces externas requeridas, en el borde del sistema
        interfazExterna("MAPAS", "Servicio de mapas/GPS", sistema, 975, 44, 200, 40);
        interfazExterna("CANAL", "Canal de notificación (correo · SMS · push)", sistema, 735, 896, 310, 40);

        int G1 = 335, G2 = 655;
        requiere("REP", "CLA", "Clasificar y priorizar", pts(630, 155, 430, 155), 450, 100, 170, 42);
        requiere("REP", "HOR", "Identificar sector", pts(930, 155, 1130, 155), 950, 100, 170, 42);
        requiere("REP", "USU", "Validar ciudadano", pts(780, 240, 780, 430), 790, 250, 125, 42);
        requiere("ASI", "USU", "Consultar operadores", pts(1130, 515, 930, 515), 950, 525, 170, 42);
        requiere("ASI", "REP", "Consultar y actualizar estado del reporte",
                pts(1200, 430, 1200, G1 + 20, 920, G1 + 20, 920, 240), 930, G1 + 27, 265, 42);
        requiere("NOT", "USU", "Obtener correo del ciudadano", pts(780, 750, 780, 560), 790, 600, 200, 42);
        requiere("ASI", "NOT", "Notificar cambio de estado",
                pts(1200, 560, 1200, G2 + 20, 870, G2 + 20, 870, 750), 960, G2 + 27, 200, 42);
        requiere("EST", "REP", "Consultar reportes",
                pts(360, 430, 360, G1 + 20, 690, G1 + 20, 690, 240), 420, G1 + 27, 160, 42);
        requiere("EDU", "USU", "Validar rol de administrador",
                pts(360, 750, 360, G2 + 20, 690, G2 + 20, 690, 560), 410, G2 - 30, 200, 42);
        requiere("EST", "CLA", "Consultar tipos y prioridades", pts(220, 430, 220, 240), 30, 280, 185, 42);
        requiere("PUN", "HOR", "Consultar sector",
                pts(1430, 815, 1470, 815, 1470, 200, 1430, 200), 1480, 480, 115, 42);
        requiere("REP", "MAPAS", null, pts(905, 110, 905, 64, 975, 64), 0, 0, 0, 0);
        requiere("NOT", "CANAL", null, pts(860, 880, 860, 896), 0, 0, 0, 0);

        marco.sendToBack();
        for (Map.Entry<String, IShapeUIModel> e : v.entrySet())
            if (!e.getKey().equals("sistema")) e.getValue().bringToFront();
        return d;
    }

    private void interfazExterna(String key, String nombre, IPackage paquete, int x, int y, int w, int h) {
        IClass it = f.createClass();
        paquete.addChild(it);
        it.setName(nombre);
        it.addStereotype("Interface");
        IShapeUIModel s = forma(key, it, x, y, w, h);
        estilo(s, AZUL, 12, false);
    }

    /** Interfaz requerida por el consumidor y provista por el proveedor (dependencia de uso con nombre). */
    private void requiere(String consumidor, String proveedor, String servicio, Point[] ruta, int lx, int ly, int lw, int lh) {
        IUsage u = f.createUsage();
        if (servicio != null) u.setName(servicio);
        u.setFrom(m.get(consumidor));
        u.setTo(m.get(proveedor));
        IConnectorUIModel c = conector(u, consumidor, proveedor, ruta);
        c.setConnectorStyle(IConnectorUIModel.CS_OBLIQUE);
        c.getLineModel().setWeight(1.5f, false);
        if (servicio != null) caption(c, lx, ly, lw, lh);
    }

    // ================================================================== 3. LÓGICA (clases)
    private void operacion(IClass c, String firma) {
        // nombre(p1: T1, p2: T2): R
        int a = firma.indexOf('('), b = firma.lastIndexOf(')');
        IOperation op = c.createOperation();
        op.setName(firma.substring(0, a).trim());
        String ret = firma.substring(b + 1).replaceFirst("^\\s*:\\s*", "").trim();
        if (!ret.isEmpty()) op.setReturnType(ret);   // constructores sin tipo de retorno
        op.setVisibility(IOperation.VISIBILITY_PUBLIC);
        String params = firma.substring(a + 1, b).trim();
        if (!params.isEmpty()) for (String p : params.split(",")) {
            String[] t = p.split(":", 2);
            IParameter pr = op.createParameter();
            pr.setName(t[0].trim());
            if (t.length > 1) pr.setType(t[1].trim());
        }
    }

    private void asociacion(String a, String b, String nombre, String agregacion, String multA, String multB) {
        asociacionRuta(a, b, nombre, agregacion, multA, multB, null);
    }

    private void asociacionRuta(String a, String b, String nombre, String agregacion, String multA, String multB, Point[] ruta) {
        IAssociation r = f.createAssociation();
        r.setName(nombre);
        r.setFrom(m.get(a));
        r.setTo(m.get(b));
        IAssociationEnd e1 = (IAssociationEnd) r.getFromEnd(), e2 = (IAssociationEnd) r.getToEnd();
        e1.setMultiplicity(multA);
        e2.setMultiplicity(multB);
        if (agregacion != null) e1.setAggregationKind(agregacion);
        Point[] p = ruta;
        if (a.equals(b)) {   // autoasociación
            Point c = centro(a);
            IShapeUIModel s = v.get(a);
            int rx = s.getX() + s.getWidth();
            p = pts(rx, c.y - 20, rx + 60, c.y - 20, rx + 60, c.y + 20, rx, c.y + 20);
        }
        IConnectorUIModel c = conector(r, a, b, p);
        c.setShowConnectorName(IDiagramUIModel.SHOW_CONNECTOR_NAME_YES);
    }

    private void dependencia(String a, String b, String nombre) {
        IDependency r = f.createDependency();
        if (nombre != null) r.setName(nombre);
        r.setFrom(m.get(a));
        r.setTo(m.get(b));
        conector(r, a, b, null);
    }

    /** Crea una clase dentro de un paquete (el nombre se asigna después de entrar al paquete). */
    private IClass claseEn(IPackage pkg, String nombre, String estereotipo, String[] attrs, String[] ops) {
        IClass c = f.createClass();
        if (pkg != null) pkg.addChild(c);
        c.setName(nombre);
        if (estereotipo != null) c.addStereotype(estereotipo);
        if (attrs != null) for (String a : attrs) {
            IAttribute at = c.createAttribute();
            if (a.startsWith("extends ")) {          // línea informativa "extends MongoRepository<...>"
                at.setName(a);
                at.setVisibility(IAttribute.VISIBILITY_UNSPECIFIED);
                continue;
            }
            boolean protegido = a.startsWith("#");
            String[] t = (protegido ? a.substring(1) : a).split(":", 2);
            at.setName(t[0].trim());
            if (t.length > 1) at.setType(t[1].trim());
            at.setVisibility(protegido ? IAttribute.VISIBILITY_PROTECTED : IAttribute.VISIBILITY_PRIVATE);
        }
        if (ops != null) for (String o : ops) {
            boolean privada = o.startsWith("-");
            operacion(c, privada ? o.substring(1) : o);
            if (privada) {
                IOperation[] arr = c.toOperationArray();
                arr[arr.length - 1].setVisibility(IOperation.VISIBILITY_PRIVATE);
            }
        }
        m.put(nombre, c);
        return c;
    }

    /** Dibuja todas las clases del mapa en la posición (centro) del layout de referencia. */
    private void dibujarClases(double escala, Color fill) {
        for (Map.Entry<String, IModelElement> e : new ArrayList<>(m.entrySet())) {
            if (v.containsKey(e.getKey()) || !(e.getValue() instanceof IClass)) continue;
            // tamaño y posición explícitos (VP ajusta el tamaño de forma diferida y las conexiones
            // se anclarían a un tamaño provisional)
            Rectangle r = pos.get(e.getKey());
            IShapeUIModel s = forma(e.getKey(), e.getValue(), (int) (r.x * escala), (int) (r.y * escala),
                    (int) (r.width * escala), (int) (r.height * escala));
            estilo(s, fill, 12, false);
        }
    }

    private IConnectorUIModel asociacionDirigida(String a, String b, String nombre, String multA, String multB) {
        IAssociation r = f.createAssociation();
        if (nombre != null) r.setName(nombre);
        r.setFrom(m.get(a));
        r.setTo(m.get(b));
        IAssociationEnd e1 = (IAssociationEnd) r.getFromEnd(), e2 = (IAssociationEnd) r.getToEnd();
        if (multA != null) e1.setMultiplicity(multA);
        if (multB != null) e2.setMultiplicity(multB);
        e1.setNavigable(IAssociationEnd.NAVIGABLE_UNSPECIFIED);
        e2.setNavigable(IAssociationEnd.NAVIGABLE_NAVIGABLE);
        IConnectorUIModel c = conector(r, a, b, null);
        c.setShowConnectorName(IDiagramUIModel.SHOW_CONNECTOR_NAME_YES);
        return c;
    }

    /** Generalización UML: la subclase hereda de la superclase. */
    private void generalizacion(String superclase, String subclase) {
        IGeneralization g = f.createGeneralization();
        g.setFrom(m.get(superclase));
        g.setTo(m.get(subclase));
        conector(g, superclase, subclase, null);
    }

    private void dependenciaEstereotipo(String a, String b, String estereotipo) {
        dependenciaEstereotipo(a, b, estereotipo, null);
    }

    private void dependenciaEstereotipo(String a, String b, String estereotipo, Point[] ruta) {
        IDependency r = f.createDependency();
        r.setFrom(m.get(a));
        r.setTo(m.get(b));
        r.setName("«" + estereotipo + "»");
        IConnectorUIModel c = conector(r, a, b, ruta);
        c.setShowConnectorName(IDiagramUIModel.SHOW_CONNECTOR_NAME_YES);
    }

    // ================================================================== 3. LÓGICA: modelo de clases inicial
    private IDiagramUIModel logica() throws Exception {
        nuevo(IDiagramTypeConstants.DIAGRAM_TYPE_CLASS_DIAGRAM, "3. Vista lógica – Modelo de clases inicial");
        claseEn(null, "GestorReportes", null, new String[]{
                "ciudadanos : List<Ciudadano>", "operadores : List<Operador>", "reportes : List<Reporte>"}, new String[]{
                "GestorReportes()", "registrarReporte(c : Ciudadano, descripcion : String) : Reporte",
                "getCiudadanos() : List<Ciudadano>", "setCiudadanos(ciudadanos : List<Ciudadano>) : void",
                "getOperadores() : List<Operador>", "setOperadores(operadores : List<Operador>) : void",
                "getReportes() : List<Reporte>", "setReportes(reportes : List<Reporte>) : void"});
        claseEn(null, "Ciudadano", null, new String[]{"id : String", "nombre : String", "documento : String", "correo : String", "contrasenaHash : String", "sector : String"},
                new String[]{"Ciudadano(id : String, nombre : String, documento : String, correo : String)",
                        "getId() : String", "setId(id : String) : void", "getNombre() : String", "setNombre(nombre : String) : void",
                        "getDocumento() : String", "setDocumento(documento : String) : void",
                        "getCorreo() : String", "setCorreo(correo : String) : void",
                        "getSector() : String", "setSector(sector : String) : void"});
        claseEn(null, "Operador", null, new String[]{"id : String", "nombre : String", "disponible : boolean"},
                new String[]{"Operador(id : String, nombre : String, disponible : boolean)",
                        "getId() : String", "setId(id : String) : void", "getNombre() : String", "setNombre(nombre : String) : void",
                        "isDisponible() : boolean", "setDisponible(valor : boolean) : void", "marcarOcupado() : void"});
        claseEn(null, "Reporte", null, new String[]{"id : String", "descripcion : String", "tipoResiduo : TipoResiduo", "prioridad : Prioridad", "latitud : double", "longitud : double", "direccion : String", "sector : String", "fotos : List<String>", "fechaReporte : LocalDateTime",
                        "fechaResolucion : LocalDateTime [0..1]", "estado : EstadoReporte", "descripcionTrabajo : String [0..1]",
                        "fotosEvidencia : List<String>", "ciudadano : Ciudadano", "operador : Operador [0..1]",
                        "reporteOriginal : Reporte [0..1]"},
                new String[]{"Reporte(id : String, fecha : LocalDateTime, ciudadano : Ciudadano, descripcion : String)",
                        "cambiarEstado(nuevo : EstadoReporte) : void", "marcarComoDuplicado(original : Reporte) : void", "registrarCierre(descripcion : String, fotos : List<String>) : void", "getId() : String", "setId(id : String) : void", "getDescripcion() : String",
                        "setDescripcion(descripcion : String) : void", "getFechaReporte() : LocalDateTime",
                        "setFechaReporte(fecha : LocalDateTime) : void", "getFechaResolucion() : LocalDateTime",
                        "setFechaResolucion(fecha : LocalDateTime) : void", "getTipoResiduo() : TipoResiduo", "setTipoResiduo(tipo : TipoResiduo) : void",
                        "getPrioridad() : Prioridad", "setPrioridad(prioridad : Prioridad) : void",
                        "getEstado() : EstadoReporte", "setEstado(estado : EstadoReporte) : void",
                        "getCiudadano() : Ciudadano", "setCiudadano(ciudadano : Ciudadano) : void",
                        "getOperador() : Operador", "setOperador(operador : Operador) : void"});
        cargarLayout("03-vista-logica.csv", 1.0);
        dibujarClases(0.85, Color.WHITE);
        asociacion("GestorReportes", "Ciudadano", "registra", null, "1", "0..*");
        asociacion("GestorReportes", "Operador", "administra", null, "1", "0..*");
        IShapeUIModel g = v.get("GestorReportes"), op = v.get("Operador"), rp = v.get("Reporte");
        int xr = Math.max(op.getX() + op.getWidth(), g.getX() + g.getWidth()) + 40;
        int gy = g.getY() + g.getHeight() / 2;
        asociacionRuta("GestorReportes", "Reporte", "gestiona", null, "1", "0..*",
                pts(g.getX() + g.getWidth(), gy, xr, gy, xr, rp.getY() - 30,
                        Math.min(xr, rp.getX() + rp.getWidth() - 30), rp.getY() - 30,
                        Math.min(xr, rp.getX() + rp.getWidth() - 30), rp.getY()));
        asociacion("Ciudadano", "Reporte", "historial", null, "1", "0..*");
        asociacion("Operador", "Reporte", "atiende", null, "0..1", "0..*");
        asociacion("Reporte", "Reporte", "duplicadoDe", null, "0..*", "0..1");
        return d;
    }

    // ================================================================== 4. DESARROLLO: clases por capas
    private IDiagramUIModel desarrollo() throws Exception {
        nuevo(IDiagramTypeConstants.DIAGRAM_TYPE_CLASS_DIAGRAM, "4. Vista de desarrollo – Clases por capas");
        ((IClassDiagramUIModel) d).setShowAssociationNavigationArrows(NAV_ARROWS);
        String[][] capas = {{"controller", "#E8F0FE"}, {"service", "#EAF5EA"}, {"repository", "#FDF3E6"}, {"model", "#FFFFFF"}};
        Map<String, IPackage> pk = new LinkedHashMap<>();
        for (String[] c : capas) {
            IPackage p = f.createPackage();
            p.setName(c[0]);
            pk.put(c[0], p);
        }
        claseEn(pk.get("controller"), "ReporteController", "RestController", new String[]{"reporteService : ReporteService"},
                new String[]{"ReporteController(reporteService : ReporteService)",
                        "registrarReporte(cuenta : CuentaUsuario, request : ReporteRequest, fotos : List<MultipartFile>) : ResponseEntity<ReporteResponse>",
                        "asignarReporte(id : String, request : AsignacionRequest) : ResponseEntity<ReporteResponse>",
                        "listarReportes(cuenta : CuentaUsuario, estado : EstadoReporte, tipoResiduo : TipoResiduo, prioridad : Prioridad, sector : String) : ResponseEntity<List<ReporteResponse>>",
                        "obtenerReporte(cuenta : CuentaUsuario, id : String) : ResponseEntity<ReporteResponse>"});
        claseEn(pk.get("service"), "ReporteService", "Service", new String[]{"ciudadanoRepository : CiudadanoRepository",
                        "operadorRepository : OperadorRepository", "reporteRepository : ReporteRepository",
                        "clasificacionService : ClasificacionService", "notificacionService : NotificacionService",
                        "almacenamientoImagenes : AlmacenamientoImagenes"},
                new String[]{"ReporteService(ciudadanoRepository : CiudadanoRepository, operadorRepository : OperadorRepository, reporteRepository : ReporteRepository, ...)",
                        "registrarReporte(request : ReporteRequest, fotos : List<MultipartFile>, ciudadanoId : String) : ReporteResponse",
                        "asignarReporte(id : String, operadorId : String, reasignar : boolean) : ReporteResponse",
                        "listarReportes(usuarioId : String, rol : Rol, filtro : FiltroReportes) : List<ReporteResponse>",
                        "obtenerReporte(id : String, usuarioId : String, rol : Rol) : ReporteResponse",
                        "-validarDisponibilidad(operador : Operador) : void"});
        claseEn(pk.get("repository"), "CiudadanoRepository", "interface", new String[]{"extends MongoRepository<Ciudadano, String>"},
                new String[]{"findById(id : String) : Optional<Ciudadano>", "findByCorreo(correo : String) : Optional<Ciudadano>"});
        claseEn(pk.get("repository"), "OperadorRepository", "interface", new String[]{"extends MongoRepository<Operador, String>"},
                new String[]{"findById(id : String) : Optional<Operador>", "save(operador : Operador) : Operador",
                        "findByDisponibleTrueOrderByNombreAsc() : List<Operador>"});
        claseEn(pk.get("repository"), "ReporteRepository", "interface", new String[]{"extends MongoRepository<Reporte, String>"},
                new String[]{"findById(id : String) : Optional<Reporte>", "save(reporte : Reporte) : Reporte",
                        "findByEstado(estado : EstadoReporte) : List<Reporte>",
                        "findByCiudadanoOrderByFechaReporteDesc(ciudadano : Ciudadano) : List<Reporte>",
                        "countByOperadorAndEstadoNot(operador : Operador, estado : EstadoReporte) : long",
                        "buscarAbiertosCercanos(punto : GeoJsonPoint, metros : double, tipo : TipoResiduo) : List<Reporte>"});
        claseEn(pk.get("model"), "Reporte", "Document", new String[]{"id : String", "descripcion : String", "tipoResiduo : TipoResiduo", "prioridad : Prioridad", "latitud : double", "longitud : double", "direccion : String", "sector : String", "ubicacion : GeoJsonPoint", "fotos : List<String>",
                        "fechaReporte : LocalDateTime", "fechaResolucion : LocalDateTime [0..1]", "estado : EstadoReporte",
                        "descripcionTrabajo : String [0..1]", "fotosEvidencia : List<String>",
                        "ciudadano : Ciudadano", "operador : Operador [0..1]", "reporteOriginal : Reporte [0..1]", "version : Long"},
                new String[]{"Reporte(fechaReporte : LocalDateTime, ciudadano : Ciudadano, descripcion : String)",
                        "clasificar(tipo : TipoResiduo, prioridad : Prioridad) : void",
                        "estaPendiente() : boolean", "asignar(operador : Operador) : void",
                        "cambiarEstado(nuevo : EstadoReporte) : void", "marcarComoDuplicado(original : Reporte) : void",
                        "registrarCierre(descripcion : String, evidencias : List<String>) : void",
                        "getId() : String", "setId(id : String) : void", "getFechaReporte() : LocalDateTime",
                        "getFechaResolucion() : LocalDateTime", "getEstado() : EstadoReporte", "getCiudadano() : Ciudadano",
                        "getOperador() : Operador", "getVersion() : Long"});
        IClass usuario = claseEn(pk.get("model"), "Usuario", null, new String[]{"#id : String", "#nombre : String", "#documento : String",
                        "#correo : String", "#contrasenaHash : String", "#intentosFallidos : int"},
                new String[]{"getRol() : Rol", "estaBloqueado(ahora : LocalDateTime) : boolean", "getId() : String",
                        "getNombre() : String", "getCorreo() : String"});
        usuario.setAbstract(true);
        usuario.toOperationArray()[0].setAbstract(true);
        claseEn(pk.get("model"), "Ciudadano", "Document", new String[]{"sector : String"},
                new String[]{"Ciudadano(nombre : String, documento : String, correo : String, contrasenaHash : String)",
                        "getRol() : Rol", "getSector() : String", "setSector(sector : String) : void"});
        claseEn(pk.get("model"), "Operador", "Document", new String[]{"disponible : boolean"},
                new String[]{"Operador(nombre : String, documento : String, correo : String, contrasenaHash : String)",
                        "getRol() : Rol", "marcarOcupado() : void", "marcarDisponible() : void", "isDisponible() : boolean"});

        cargarLayout("04-vista-desarrollo.csv", 1.0);
        dibujarClases(0.85, Color.WHITE);
        // marcos de cada capa (detrás de las clases)
        String[][] miembros = {{"controller", "ReporteController"}, {"service", "ReporteService"},
                {"repository", "CiudadanoRepository", "OperadorRepository", "ReporteRepository"},
                {"model", "Reporte", "Usuario", "Ciudadano", "Operador"}};
        for (int i = 0; i < miembros.length; i++) {
            String nombre = miembros[i][0];
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = 0, maxY = 0;
            for (int j = 1; j < miembros[i].length; j++) {
                IShapeUIModel s = v.get(miembros[i][j]);
                minX = Math.min(minX, s.getX()); minY = Math.min(minY, s.getY());
                maxX = Math.max(maxX, s.getX() + s.getWidth()); maxY = Math.max(maxY, s.getY() + s.getHeight());
            }
            IShapeUIModel ps = forma("pkg:" + nombre, pk.get(nombre), minX - 20, minY - 40, maxX - minX + 40, maxY - minY + 60);
            estilo(ps, Color.decode(capas[i][1]), 13, true);
            ps.sendToBack();
        }
        for (Map.Entry<String, IShapeUIModel> e : v.entrySet())
            if (!e.getKey().startsWith("pkg:")) e.getValue().bringToFront();

        asociacionDirigida("ReporteController", "ReporteService", null, null, "1");
        asociacionDirigida("ReporteService", "CiudadanoRepository", null, null, "1");
        asociacionDirigida("ReporteService", "OperadorRepository", null, null, "1");
        asociacionDirigida("ReporteService", "ReporteRepository", null, null, "1");
        IShapeUIModel svc = v.get("ReporteService"), rpt = v.get("Reporte");
        int cx = Math.min(svc.getX() + svc.getWidth(), rpt.getX() + rpt.getWidth()) - 40;
        dependenciaEstereotipo("ReporteService", "Reporte", "create",
                pts(cx, svc.getY() + svc.getHeight(), cx, rpt.getY()));
        dependenciaEstereotipo("ReporteRepository", "Reporte", "use");
        dependenciaEstereotipo("CiudadanoRepository", "Ciudadano", "use");
        IShapeUIModel orp = v.get("OperadorRepository"), rep = v.get("Reporte"), ope = v.get("Operador");
        int ox = orp.getX() + orp.getWidth() / 2;
        int oy = rep.getY() + rep.getHeight() + 40;
        dependenciaEstereotipo("OperadorRepository", "Operador", "use",
                pts(ox, orp.getY() + orp.getHeight(), ox, oy, ope.getX() + 40, oy, ope.getX() + 40, ope.getY()));
        generalizacion("Usuario", "Ciudadano");
        generalizacion("Usuario", "Operador");
        asociacionDirigida("Reporte", "Ciudadano", "historial", "0..*", "1");
        asociacionDirigida("Reporte", "Operador", "atiende", "0..*", "0..1");
        asociacion("Reporte", "Reporte", "duplicadoDe", null, "0..*", "0..1");
        return d;
    }

    // ================================================================== 5. DESPLIEGUE
    private IShapeUIModel nodo(String key, String estereo, String nombre, String padre, int x, int y, int w, int h) {
        INode n = f.createNode();
        n.setName(nombre);
        n.addStereotype(estereo);
        if (padre != null) m.get(padre).addChild(n);
        IShapeUIModel s = forma(key, n, x, y, w, h);
        estilo(s, Color.WHITE, 13, true);
        if (padre != null) { v.get(padre).addChild(s); s.setBounds(x, y, w, h); }
        return s;
    }

    private void artefacto(String key, String nombre, String padre, int x, int y, int w, int h) {
        IArtifact a = f.createArtifact();
        a.setName(nombre);
        m.get(padre).addChild(a);
        IShapeUIModel s = forma(key, a, x, y, w, h);
        estilo(s, VERDE_CLARO, 12, true);
        v.get(padre).addChild(s);
        s.setBounds(x, y, w, h);
    }

    private void enlace(String a, String b, String protocolo, int[] p, int tx, int ty, int tw, int th) {
        IAssociation r = f.createAssociation();
        r.setName(protocolo);
        r.setFrom(m.get(a));
        r.setTo(m.get(b));
        IConnectorUIModel c = conector(r, a, b, pts(p));
        c.setConnectorStyle(IConnectorUIModel.CS_OBLIQUE);
        c.getLineModel().setWeight(1.5f, false);
        caption(c, tx, ty, tw, th);
    }

    private IDiagramUIModel despliegue() {
        nuevo(IDiagramTypeConstants.DIAGRAM_TYPE_DEPLOYMENT_DIAGRAM, "5. Vista de despliegue");
        int TOP = 20, TH = 250;
        nodo("ciu", "device", "Smartphone del ciudadano", null, 20, TOP, 460, TH);
        nodo("ciuNav", "executionEnvironment", "Navegador web móvil", "ciu", 40, TOP + 70, 270, 160);
        artefacto("ciuArt", "vistas del ciudadano (HTML Thymeleaf + JS)", "ciuNav", 60, TOP + 125, 230, 80);
        nodo("gps", "device", "Sensor GPS", "ciu", 330, TOP + 115, 130, 70);
        nodo("pc", "device", "PC del ciudadano / administrador", null, 530, TOP, 380, TH);
        nodo("pcNav", "executionEnvironment", "Navegador web", "pc", 555, TOP + 70, 320, 160);
        artefacto("pcArt", "vistas web (HTML Thymeleaf + JS)", "pcNav", 590, TOP + 125, 250, 80);
        nodo("ope", "device", "Smartphone del operador", null, 960, TOP, 380, TH);
        nodo("opeNav", "executionEnvironment", "Navegador web móvil", "ope", 985, TOP + 70, 320, 160);
        artefacto("opeArt", "vistas del operador (HTML Thymeleaf + JS)", "opeNav", 1015, TOP + 125, 260, 80);

        int SX = 400, SY = 360, SW = 560, SH = 250;
        nodo("srv", "device", "Servidor web/API (Railway)", null, SX, SY, SW, SH);
        nodo("jvm", "executionEnvironment", "JVM 21 · Spring Boot 4 (Tomcat embebido)", "srv", SX + 20, SY + 65, SW - 45, 165);
        artefacto("jar", "ecogestion.jar (controller, service, repository)", "jvm", SX + 40, SY + 125, 260, 80);
        artefacto("tpl", "templates/ (vistas Thymeleaf)", "jvm", SX + 315, SY + 125, 190, 80);

        int BY = 740, BH = 200, BW = 380;
        int[] xs = {40, 490, 940};
        nodo("bd", "device", "Servidor de base de datos (Railway)", null, xs[0], BY, BW, BH);
        nodo("mongo", "executionEnvironment", "MongoDB", "bd", xs[0] + 15, BY + 60, BW - 35, 125);
        artefacto("bdArt", "ecogestion: colecciones, índices 2dsphere y fotos en GridFS", "mongo", xs[0] + 30, BY + 110, BW - 70, 60);
        nodo("smtp", "device", "Servicio de correo electrónico (servidor SMTP)", null, xs[1], BY, BW, BH);
        artefacto("smtpArt", "relay SMTP", "smtp", xs[1] + 80, BY + 95, BW - 160, 70);
        nodo("maps", "device", "Servicio de mapas/GPS (Google Maps Platform)", null, xs[2], BY, BW, BH);
        artefacto("mapsArt", "Maps JavaScript API + Geocoding API", "maps", xs[2] + 80, BY + 95, BW - 160, 70);

        int top = TOP + TH;
        enlace("ciu", "srv", "HTTPS:443 (HTML/JSON)", new int[]{250, top, 520, SY}, 110, 300, 170, 40);
        enlace("pc", "srv", "HTTPS:443 (HTML/JSON)", new int[]{720, top, 680, SY}, 728, 300, 170, 40);
        enlace("ope", "srv", "HTTPS:443 (HTML/JSON)", new int[]{1150, top, 840, SY}, 1100, 300, 170, 40);
        int sb = SY + SH;
        enlace("srv", "bd", "MongoDB Wire Protocol / TCP:27017", new int[]{520, sb, xs[0] + BW / 2, BY}, 60, 655, 250, 22);
        enlace("srv", "smtp", "SMTP/TLS:587", new int[]{680, sb, xs[1] + BW / 2, BY}, 700, 670, 130, 22);
        enlace("srv", "maps", "HTTPS:443 (Geocoding)", new int[]{840, sb, xs[2] + BW / 2, BY}, 1010, 640, 180, 22);
        return d;
    }
}
