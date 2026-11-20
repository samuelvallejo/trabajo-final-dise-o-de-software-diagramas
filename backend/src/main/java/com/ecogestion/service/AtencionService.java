package com.ecogestion.service;

import com.ecogestion.dto.ReporteDto.ReporteResponse;
import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.integracion.imagenes.AlmacenamientoImagenes;
import com.ecogestion.model.Reporte;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Componente "Asignación y atención" del lado del operador de limpieza. Requiere "Consultar y actualizar
 * estado del reporte" de Gestión de reportes, que a su vez dispara "Notificar cambio de estado".
 * Historias HU-08 y HU-09.
 */
@Service
public class AtencionService {

    private final ReporteService reporteService;
    private final AlmacenamientoImagenes almacenamientoImagenes;

    public AtencionService(ReporteService reporteService, AlmacenamientoImagenes almacenamientoImagenes) {
        this.reporteService = reporteService;
        this.almacenamientoImagenes = almacenamientoImagenes;
    }

    /** Reportes asignados al operador que no están resueltos (HU-08). */
    public List<ReporteResponse> misAsignaciones(String operadorId, String orden) {
        return reporteService.reportesDeOperador(operadorId, orden).stream().map(ReporteResponse::de).toList();
    }

    public ReporteResponse detalle(String reporteId, String operadorId) {
        return ReporteResponse.de(reporteService.buscarParaOperador(reporteId, operadorId));
    }

    /** Asignado → En proceso (HU-09, criterio 1). */
    public ReporteResponse iniciarAtencion(String reporteId, String operadorId) {
        Reporte reporte = reporteService.buscarParaOperador(reporteId, operadorId);
        reporte.iniciarAtencion();
        return ReporteResponse.de(reporteService.actualizarEstado(reporte));
    }

    /** En proceso → Resuelto con descripción y fotos de evidencia (HU-09, criterios 2 y 3). */
    public ReporteResponse registrarCierre(String reporteId, String operadorId, String descripcionTrabajo,
                                           List<MultipartFile> evidencias) {
        if (descripcionTrabajo == null || descripcionTrabajo.isBlank()) {
            throw new DatosInvalidosException("La descripción del trabajo realizado es obligatoria");
        }
        Reporte reporte = reporteService.buscarParaOperador(reporteId, operadorId);
        reporte.registrarCierre(descripcionTrabajo, almacenamientoImagenes.guardar(evidencias));
        return ReporteResponse.de(reporteService.actualizarEstado(reporte));
    }
}
