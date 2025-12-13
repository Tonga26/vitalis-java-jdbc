package dao;

import model.Paciente;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Interfaz de acceso a datos específica para la entidad Paciente.
 * <p>
 * Hereda las operaciones CRUD básicas de {@link GenericDao} y define
 * consultas especializadas como la búsqueda por DNI.
 * </p>
 */
public interface PacienteDao extends GenericDao<Paciente> {

    /**
     * Busca un paciente por su Documento Nacional de Identidad.
     *
     * @param dni El número de documento a buscar.
     * @return Un {@link Optional} con el paciente si existe, o vacío si no.
     * @throws SQLException Si ocurre un error de base de datos.
     */
    Optional<Paciente> findByDni(String dni) throws SQLException;

}