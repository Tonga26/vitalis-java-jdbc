package dao.impl;

import dao.HistoriaClinicaDao;
import model.HistoriaClinica;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta del DAO para la entidad {@link HistoriaClinica} utilizando JDBC.
 * <p>
 * Gestiona la persistencia de los datos médicos, manteniendo la integridad referencial
 * con el Paciente mediante el campo {@code paciente_id}.
 * </p>
 */
public class HistoriaClinicaDaoImpl implements HistoriaClinicaDao {

    private final Connection conn;

    /**
     * Constructor con Inyección de Dependencia.
     * @param conn Conexión JDBC gestionada externamente.
     */
    public HistoriaClinicaDaoImpl(Connection conn) {
        this.conn = conn;
    }

    /**
     * Mapea el ResultSet a un objeto HistoriaClinica.
     * Incluye el mapeo de la FK {@code paciente_id} para mantener la coherencia en el modelo de dominio.
     *
     * @param rs ResultSet posicionado en la fila actual.
     * @return Objeto HistoriaClinica.
     * @throws SQLException Si ocurre error de lectura.
     */
    private HistoriaClinica map(ResultSet rs) throws SQLException{
        HistoriaClinica hc = new HistoriaClinica();
        hc.setId(rs.getLong("id"));
        hc.setEliminado(rs.getBoolean("eliminado"));
        hc.setNroHistoria(rs.getString("nro_historia"));
        String gs = rs.getString("grupo_sanguineo");
        hc.setGrupoSanguineo(HistoriaClinica.GrupoSanguineo.fromDb(gs));
        hc.setAntecedentes(rs.getString("antecedentes"));
        hc.setMedicacionActual(rs.getString("medicacion_actual"));
        hc.setObservaciones(rs.getString("observaciones"));
        java.sql.Date f = rs.getDate("fecha_apertura");
        hc.setFechaApertura(f != null ? f.toLocalDate() : null);
        hc.setPacienteId(rs.getLong("paciente_id"));
        return hc;
    }

    /**
     * Busca la historia clínica específica asociada a un paciente.
     *
     * @param pacienteId ID del paciente dueño de la historia.
     * @return Optional con la historia si existe y está activa.
     * @throws SQLException Error de base de datos.
     */
    @Override
    public Optional<HistoriaClinica> findByPacienteId(Long pacienteId) throws SQLException {
        String sql = "SELECT * FROM historia_clinica WHERE paciente_id = ? AND eliminado = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setLong(1, pacienteId);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Realiza la baja lógica de la historia clínica buscando por el ID del paciente.
     * Útil para operaciones de borrado en cascada manual.
     *
     * @param pacienteId ID del paciente.
     * @throws SQLException Error de base de datos.
     */
    @Override
    public void deleteByPacienteId(Long pacienteId) throws SQLException {
        String sql = "UPDATE historia_clinica SET eliminado = ? WHERE paciente_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setBoolean(1, true);
            ps.setLong(2, pacienteId);
            ps.executeUpdate();
        }
    }

    /**
     * Crea una nueva historia clínica.
     * Requiere que el objeto {@code hc} tenga seteaado el {@code pacienteId}.
     *
     * @param hc Objeto historia a persistir.
     * @return La historia con su ID generado.
     * @throws SQLException Error de inserción.
     */
    @Override
    public HistoriaClinica create(HistoriaClinica hc) throws SQLException {
        String sql = "INSERT INTO historia_clinica (eliminado, nro_historia, grupo_sanguineo, antecedentes, medicacion_actual, observaciones, fecha_apertura, paciente_id) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setBoolean(1, hc.isEliminado());
            ps.setString(2, hc.getNroHistoria());
            if (hc.getGrupoSanguineo() != null){
                ps.setString(3, hc.getGrupoSanguineo().db());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, hc.getAntecedentes());
            ps.setString(5, hc.getMedicacionActual());
            ps.setString(6, hc.getObservaciones());
            if (hc.getFechaApertura() != null){
                ps.setDate(7, java.sql.Date.valueOf(hc.getFechaApertura()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            ps.setLong(8, hc.getPacienteId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    hc.setId(rs.getLong(1));
                }
            }
            return hc;
        }
    }

    /**
     * Busca una historia clínica por su propio ID (PK).
     *
     * @param id Identificador de la historia.
     * @return Optional con la historia.
     * @throws SQLException Error de lectura.
     */
    @Override
    public Optional<HistoriaClinica> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM historia_clinica WHERE id = ? AND eliminado = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lista todas las historias clínicas activas del sistema.
     *
     * @return Lista de objetos HistoriaClinica.
     * @throws SQLException Error de lectura.
     */
    @Override
    public List<HistoriaClinica> getAll() throws SQLException {
        String sql = "SELECT * FROM historia_clinica WHERE eliminado = 0";

        List<HistoriaClinica> HcEncontradas = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                HistoriaClinica hc = map(rs);
                HcEncontradas.add(hc);
            }
        }
        return HcEncontradas;
    }

    /**
     * Actualiza los datos médicos de una historia existente.
     * No modifica la asociación con el paciente (paciente_id).
     *
     * @param hc Objeto con los datos nuevos.
     * @throws SQLException Error de actualización.
     */
    @Override
    public void update(HistoriaClinica hc) throws SQLException {
        String sql = "UPDATE historia_clinica SET eliminado=?, nro_historia=?, grupo_sanguineo=?, antecedentes=?, medicacion_actual=?, observaciones=?, fecha_apertura=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setBoolean(1, hc.isEliminado());
            ps.setString(2, hc.getNroHistoria());
            if (hc.getGrupoSanguineo() != null){
                ps.setString(3, hc.getGrupoSanguineo().db());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, hc.getAntecedentes());
            ps.setString(5, hc.getMedicacionActual());
            ps.setString(6, hc.getObservaciones());
            if (hc.getFechaApertura() != null){
                ps.setDate(7, java.sql.Date.valueOf(hc.getFechaApertura()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            ps.setLong(8, hc.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Baja lógica por ID de historia clínica.
     *
     * @param id Identificador de la historia.
     * @throws SQLException Error de actualización.
     */
    @Override
    public void delete(Long id) throws SQLException {
        String sql = "UPDATE historia_clinica SET eliminado = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setBoolean(1, true);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }
}