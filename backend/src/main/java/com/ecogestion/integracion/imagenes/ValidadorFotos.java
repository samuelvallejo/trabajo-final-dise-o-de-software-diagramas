package com.ecogestion.integracion.imagenes;

import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.model.Reporte;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * Reglas de las fotos (HU-03, criterio 3; HU-09): de 1 a 5 archivos JPG o PNG de máximo 5 MB.
 */
@Component
public class ValidadorFotos {

    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png");

    public void validar(List<MultipartFile> fotos) {
        if (fotos == null || fotos.isEmpty() || fotos.stream().allMatch(MultipartFile::isEmpty)) {
            throw new DatosInvalidosException("Debe adjuntar al menos 1 foto");
        }
        if (fotos.size() > Reporte.MAX_FOTOS) {
            throw new DatosInvalidosException("Puede adjuntar como máximo " + Reporte.MAX_FOTOS + " fotos");
        }
        for (MultipartFile foto : fotos) {
            if (foto.getSize() > TAMANO_MAXIMO) {
                throw new DatosInvalidosException("La foto " + foto.getOriginalFilename() + " supera 5 MB");
            }
            if (foto.getContentType() == null || !TIPOS.contains(foto.getContentType())) {
                throw new DatosInvalidosException("La foto " + foto.getOriginalFilename() + " debe ser JPG o PNG");
            }
        }
    }
}
