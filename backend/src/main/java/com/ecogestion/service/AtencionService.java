package com.ecogestion.service;

import com.ecogestion.dto.ReporteDto.ReporteResponse;
import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.integracion.imagenes.AlmacenamientoImagenes;
import com.ecogestion.model.Reporte;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Lógica del componente "Asignación y atención" del lado del operador de limpieza: ver sus reportes
 * asignados (HU-08), empezar la atención y registrar el cierre con fotos de evidencia (HU-09).
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Alta cohesión:</b> solo contiene lo que hace el operador; lo que hace el administrador está
 *       en {@link ReporteService}.</li>
 *   <li><b>Bajo acoplamiento:</b> no usa repositorios; obtiene y guarda los reportes a través del servicio
 *       "Consultar y actualizar estado del reporte" de {@link ReporteService}, que además dispara la
 *       notificación al ciudadano.</li>
 *   <li><b>Experto en información:</b> los cambios de estado los hace el propio {@link Reporte}
 *       ({@code iniciarAtencion}, {@code registrarCierre}); este servicio solo los pide.</li>
 * </ul>
 */
@Service
public class AtencionService {

    private final ReporteService reporteService;
    private final AlmacenamientoImagenes almacenamientoImagenes;

    public AtencionService(ReporteService reporteService, AlmacenamientoImagenes almacenamientoImagenes) {
        this.reporteService = reporteService;
        this.almacenamientoImagenes = almacenamientoImagenes;
    }

    /** Reportes asignados al operador que aún no están resueltos (HU-08). */
    public List<ReporteResponse> misAsignaciones(String operadorId, String orden) {
        return reporteService.reportesDeOperador(operadorId, orden).stream().map(ReporteResponse::de).toList();
    }

    public ReporteResponse detalle(String reporteId, String operadorId) {
        return ReporteResponse.de(reporteService.buscarParaOperador(reporteId, operadorId));
    }

    /** ASIGNADO → EN_PROCESO (HU-09, criterio 1). */
    public ReporteResponse iniciarAtencion(String reporteId, String operadorId) {
        Reporte reporte = reporteService.buscarParaOperador(reporteId, operadorId);
        reporte.iniciarAtencion();
        return ReporteResponse.de(reporteService.actualizarEstado(reporte));
    }

    /** EN_PROCESO → RESUELTO con la descripción del trabajo y de 1 a 5 fotos de evidencia (HU-09, criterios 2 y 3). */
    public ReporteResponse registrarCierre(String reporteId, String operadorId, String descripcionTrabajo,
                                           List<MultipartFile> evidencias) {
        // Se valida antes de guardar las fotos para no dejar archivos sin reporte si falta la descripción
        if (descripcionTrabajo == null || descripcionTrabajo.isBlank()) {
            throw new DatosInvalidosException("La descripción del trabajo realizado es obligatoria");
        }
        Reporte reporte = reporteService.buscarParaOperador(reporteId, operadorId);
        reporte.registrarCierre(descripcionTrabajo, almacenamientoImagenes.guardar(evidencias));
        return ReporteResponse.de(reporteService.actualizarEstado(reporte));
    }
}
