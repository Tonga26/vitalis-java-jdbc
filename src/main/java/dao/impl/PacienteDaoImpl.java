package dao.impl;

import dao.PacienteDao;
import model.HistoriaClinica;
import model.Paciente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación concreta del DAO para la entidad {@link Paciente} utilizando JDBC.
 * <p>
 * Características principales de esta implementación:
 * <ul>
 * <li><b>Inyección de Dependencias:</b> Recibe la conexión por constructor, delegando la gestión del ciclo de vida a la capa de Servicio.</li>
 * <li><b>Carga Ansiosa (Eager Loading):</b> Los métodos de lectura utilizan {@code LEFT JOIN} para recuperar el Paciente y su Historia Clínica en una sola consulta.</li>
 * <li><b>Baja Lógica (Soft Delete):</b> Los borrados actualizan el campo {@code eliminado} en lugar de borrar el registro físico.</li>
 * </ul>
 * </p>
 */
public class PacienteDaoImpl implements PacienteDao {

    private final Connection conn;

    /**
     * Constructor que inyecta la conexión a base de datos.
     *
     * @param conn La conexión JDBC activa y gestionada externamente.
     */
    public PacienteDaoImpl(Connection conn) {
        this.conn = conn;
    }

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Paciente}.
     * <p>
     * Este método es capaz de detectar si el ResultSet incluye columnas de {@link HistoriaClinica}
     * (gracias al JOIN) y poblar el objeto anidado si existe.
     * </p>
     *
     * @param rs El conjunto de resultados posicionado en la fila actual.
     * @return El objeto Paciente hidratado.
     * @throws SQLException Si ocurre un error al leer las columnas.
     */
    private Paciente map(ResultSet rs) throws SQLException {
        Paciente p = new Paciente();
        p.setId(rs.getLong("id"));
        p.setEliminado(rs.getBoolean("eliminado"));
        p.setDni(rs.getString("dni"));
        p.setNombre(rs.getString("nombre"));
        p.setApellido(rs.getString("apellido"));
        java.sql.Date f = rs.getDate("fecha_nacimiento");
        p.setFechaNacimiento(f != null ? f.toLocalDate() : null);

        long hcId = rs.getLong("hc_id");

        if (hcId > 0){
            HistoriaClinica h = new HistoriaClinica();
            h.setId(hcId);
            h.setEliminado(rs.getBoolean("hc_eliminado"));
            h.setNroHistoria(rs.getString("nro_historia"));
            String gs = rs.getString("grupo_sanguineo");
            h.setGrupoSanguineo(HistoriaClinica.GrupoSanguineo.fromDb(gs));
            h.setAntecedentes(rs.getString("antecedentes"));
            h.setMedicacionActual(rs.getString("medicacion_actual"));
            h.setObservaciones(rs.getString("observaciones"));
            java.sql.Date fa = rs.getDate("hc_fecha_apertura");
            h.setFechaApertura(fa != null ? fa.toLocalDate() : null);

            p.setHistoriaClinica(h);
        }

        return p;
    }

    /**
     * Inserta un nuevo paciente en la base de datos.
     * Recupera y asigna la clave primaria generada (ID) al objeto pasado por parámetro.
     *
     * @param p El paciente a persistir.
     * @return El mismo objeto paciente con su ID actualizado.
     * @throws SQLException Si ocurre un error durante la inserción.
     */
    @Override
    public Paciente create(Paciente p) throws SQLException {
        String sql = "INSERT INTO paciente (eliminado, dni, nombre, apellido, fecha_nacimiento) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setBoolean(1, p.isEliminado());
            ps.setString(2, p.getDni());
            ps.setString(3, p.getNombre());
            ps.setString(4, p.getApellido());
            if (p.getFechaNacimiento() != null){
                ps.setDate(5, java.sql.Date.valueOf(p.getFechaNacimiento()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()){
                if (rs.next()){
                    p.setId(rs.getLong(1));
                }
            }
        }
        return p;
    }

    /**
     * Busca un paciente por su ID primario.
     * Realiza un {@code LEFT JOIN} para traer también su historia clínica si existe y está activa.
     *
     * @param id El identificador único del paciente.
     * @return Un {@link Optional} con el paciente si se encuentra y no está eliminado lógico.
     * @throws SQLException Si ocurre un error en la consulta.
     */
    @Override
    public Optional<Paciente> findById(Long id) throws SQLException {
        String sql = "SELECT p.*, " +
                "hc.id AS hc_id, hc.eliminado AS hc_eliminado, hc.nro_historia, " +
                "hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, " +
                "hc.observaciones, " +
                "hc.fecha_apertura AS hc_fecha_apertura " +
                "FROM paciente p " +
                "LEFT JOIN historia_clinica hc ON p.id = hc.paciente_id AND hc.eliminado = 0 " +
                "WHERE p.id = ? AND p.eliminado = 0";

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
     * Recupera todos los pacientes activos del sistema.
     * Incluye la carga ansiosa de sus historias clínicas.
     *
     * @return Una lista de pacientes activos.
     * @throws SQLException Si ocurre un error en la consulta.
     */
    @Override
    public List<Paciente> getAll() throws SQLException {
        String sql = "SELECT p.*, " +
                "hc.id AS hc_id, hc.eliminado AS hc_eliminado, hc.nro_historia, " +
                "hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, " +
                "hc.observaciones, " +
                "hc.fecha_apertura AS hc_fecha_apertura " +
                "FROM paciente p " +
                "LEFT JOIN historia_clinica hc ON p.id = hc.paciente_id AND hc.eliminado = 0 " +
                "WHERE p.eliminado = 0";

        List<Paciente> pacientesEncontrados = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()){
            while (rs.next()){
                Paciente p = map(rs);
                pacientesEncontrados.add(p);
            }
        }
        return pacientesEncontrados;
    }

    /**
     * Actualiza los datos modificables de un paciente existente.
     * No afecta a la Historia Clínica asociada.
     *
     * @param p El paciente con los datos actualizados.
     * @throws SQLException Si ocurre un error durante la actualización.
     */
    @Override
    public void update(Paciente p) throws SQLException {
        String sql = "UPDATE paciente SET eliminado=?, dni=?, nombre=?, apellido=?, fecha_nacimiento=? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setBoolean(1, p.isEliminado());
            ps.setString(2, p.getDni());
            ps.setString(3, p.getNombre());
            ps.setString(4, p.getApellido());
            if (p.getFechaNacimiento() != null){
                ps.setDate(5, java.sql.Date.valueOf(p.getFechaNacimiento()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setLong(6, p.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Realiza una baja lógica del paciente.
     * Establece el campo {@code eliminado} a {@code true}.
     *
     * @param id El ID del paciente a dar de baja.
     * @throws SQLException Si ocurre un error en la actualización.
     */
    @Override
    public void delete(Long id) throws SQLException {
        String sql = "UPDATE paciente SET eliminado=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setBoolean(1, true);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Busca un paciente por su número de documento (DNI).
     * Incluye carga ansiosa de la Historia Clínica.
     *
     * @param dni El DNI a buscar.
     * @return Un {@link Optional} con el paciente si existe.
     * @throws SQLException Si ocurre un error en la consulta.
     */
    @Override
    public Optional<Paciente> findByDni(String dni) throws SQLException {
        String sql = "SELECT p.*, " +
                "hc.id AS hc_id, hc.eliminado AS hc_eliminado, hc.nro_historia, " +
                "hc.grupo_sanguineo, hc.antecedentes, hc.medicacion_actual, " +
                "hc.observaciones, " +
                "hc.fecha_apertura AS hc_fecha_apertura " +
                "FROM paciente p " +
                "LEFT JOIN historia_clinica hc ON p.id = hc.paciente_id AND hc.eliminado = 0 " +
                "WHERE p.dni = ? AND p.eliminado = 0";

        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }
}
