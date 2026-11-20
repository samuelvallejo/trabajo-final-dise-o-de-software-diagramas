package com.ecogestion.integracion.imagenes;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Guarda las fotos en MongoDB con GridFS (bucket {@code fs}), dentro de la misma base de datos
 * del sistema (vista de despliegue).
 */
@Component
public class GridFsAlmacenamientoImagenes implements AlmacenamientoImagenes {

    private final GridFsTemplate gridFs;
    private final ValidadorFotos validador;

    public GridFsAlmacenamientoImagenes(GridFsTemplate gridFs, ValidadorFotos validador) {
        this.gridFs = gridFs;
        this.validador = validador;
    }

    @Override
    public List<String> guardar(List<MultipartFile> fotos) {
        validador.validar(fotos);
        List<String> urls = new ArrayList<>();
        for (MultipartFile foto : fotos) {
            try (InputStream contenido = foto.getInputStream()) {
                ObjectId id = gridFs.store(contenido, foto.getOriginalFilename(), foto.getContentType());
                urls.add(RUTA_PUBLICA + id.toHexString());
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo guardar la foto " + foto.getOriginalFilename(), e);
            }
        }
        return urls;
    }

    @Override
    public Optional<Imagen> obtener(String id) {
        if (!ObjectId.isValid(id)) {
            return Optional.empty();
        }
        GridFSFile archivo = gridFs.findOne(Query.query(Criteria.where("_id").is(new ObjectId(id))));
        if (archivo == null) {
            return Optional.empty();
        }
        GridFsResource recurso = gridFs.getResource(archivo);
        try (InputStream contenido = recurso.getInputStream()) {
            return Optional.of(new Imagen(recurso.getContentType(), contenido.readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen " + id, e);
        }
    }
}
