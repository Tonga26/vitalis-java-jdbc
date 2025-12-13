package dao;

import model.HistoriaClinica;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Interfaz de acceso a datos para Historia Clínica.
 * Extiende las operaciones CRUD genéricas y añade lógica específica de relación.
 */
public interface HistoriaClinicaDao extends GenericDao<HistoriaClinica> {

    /**
     * Busca la historia clínica perteneciente a un paciente específico.
     * Utiliza la clave foránea 'paciente_id'.
     *
     * @param pacienteId El ID del paciente dueño de la historia.
     * @return Un Optional con la historia si existe.
     * @throws SQLException Si falla la consulta.
     */
    Optional<HistoriaClinica> findByPacienteId(Long pacienteId) throws SQLException;

    /**
     * Elimina la historia clínica asociada a un paciente.
     * Útil para implementar borrado en cascada.
     *
     * @param pacienteId El ID del paciente.
     * @throws SQLException Si falla el borrado.
     */
    void deleteByPacienteId(Long pacienteId) throws SQLException;
}
