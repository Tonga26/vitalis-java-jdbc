package main;

import model.HistoriaClinica;
import model.Paciente;
import service.HistoriaClinicaService;
import service.PacienteService;
import model.HistoriaClinica.GrupoSanguineo;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Clase responsable de gestionar la interacción con el usuario en la consola.
 * <p>
 * Actúa como un controlador que recibe las entradas del usuario, las valida
 * y delega la lógica de negocio a los servicios correspondientes ({@link PacienteService}
 * y {@link HistoriaClinicaService}).
 * </p>
 */
public class MenuHandler {
    private Scanner scanner;
    private PacienteService pacienteService;
    private HistoriaClinicaService hcService;

    /**
     * Constructor que inyecta las dependencias necesarias.
     *
     * @param scanner         El objeto Scanner para leer la entrada de la consola.
     * @param pacienteService Servicio para operaciones relacionadas con Pacientes.
     * @param hcService       Servicio para operaciones relacionadas con Historias Clínicas.
     */
    public MenuHandler(Scanner scanner, PacienteService pacienteService, HistoriaClinicaService hcService) {
        this.scanner = scanner;
        this.pacienteService = pacienteService;
        this.hcService = hcService;
    }

    /**
     * Guía al usuario a través del proceso de creación de un nuevo Paciente.
     * <p>
     * Solicita datos personales y datos obligatorios para la Historia Clínica inicial.
     * Maneja excepciones de formato de fecha y validaciones de negocio.
     * </p>
     */
    public void createPatient(){
        try {
            System.out.println("== ALTA DE NUEVO PACIENTE ==");
            String nombre = readInput("Nombre: ");
            String apellido = readInput("Apellido: ");
            String dni = readInput("DNI: ");
            String fechaTexto = readInput("Fecha Nacimiento (formato YYYY-MM-DD): ");
            LocalDate fechaNacimiento = LocalDate.parse(fechaTexto);

            System.out.println("== HISTORIA CLÍNICA (Obligatoria) ==");
            String nroHc = readInput("Número de historia clínica: ");
            String grupoTexto = readInput("Grupo sanguíneo (A+,A-,B+,B-,AB+,AB-,O+,O- o vacío): ");
            GrupoSanguineo grupo = (grupoTexto.isBlank()) ? null : GrupoSanguineo.fromDb(grupoTexto);
            String ant = readInput("Antecedentes (opcional): ");
            String med = readInput("Medicación actual (opcional): ");
            String obs = readInput("Observaciones (opcional): ");

            Paciente p = new Paciente();
            p.setNombre(nombre);
            p.setApellido(apellido);
            p.setDni(dni);
            p.setFechaNacimiento(fechaNacimiento);

            HistoriaClinica hc = new HistoriaClinica();
            hc.setNroHistoria(nroHc);
            hc.setGrupoSanguineo(grupo);
            hc.setAntecedentes(ant);
            hc.setMedicacionActual(med);
            hc.setObservaciones(obs);
            hc.setFechaApertura(LocalDate.now());

            p.setHistoriaClinica(hc);

            pacienteService.create(p);

            MenuDisplay.printSuccess("Paciente " + p.getNombre() + " " + p.getApellido() + " guardado con ID: " + p.getId());
        } catch (java.time.format.DateTimeParseException e) {
            MenuDisplay.printError("El formato de la fecha es incorrecto. Debe ser YYYY-MM-DD (ej: 1990-12-31)");
        } catch (IllegalArgumentException e) {
            MenuDisplay.printError("Dato inválido: " + e.getMessage());
        } catch (Exception e) {
            MenuDisplay.printError("Ocurrió un error inesperado: " + e.getMessage());
        }
    }

    /**
     * Muestra un listado tabular de todos los pacientes registrados.
     * <p>
     * Imprime ID, DNI, Nombre, Apellido, Nro de Historia y Grupo Sanguíneo.
     * Si no hay pacientes, muestra un mensaje de advertencia.
     * </p>
     */
    public void listPatients(){
        try {
            List<Paciente> pacientes = pacienteService.getAll();
            if (pacientes.isEmpty()) {
                System.out.println("⚠ No hay pacientes registrados.");
                return;
            }

            String format = "| %-4s | %-10s | %-15s | %-15s | %-12s | %-5s |%n";
            String line   = "+------+------------+-----------------+-----------------+--------------+-------+";

            System.out.println("\n=== LISTADO DE PACIENTES ===");
            System.out.println(line);
            System.out.printf(format, "ID", "DNI", "NOMBRE", "APELLIDO", "NRO HC", "GRUPO");
            System.out.println(line);

            for (Paciente p : pacientes){
                String nroHc = (p.getHistoriaClinica() != null ? p.getHistoriaClinica().getNroHistoria() : "S/D");
                String grupo = (p.getHistoriaClinica() != null && p.getHistoriaClinica().getGrupoSanguineo() != null
                        ? p.getHistoriaClinica().getGrupoSanguineo().db() : "-");

                System.out.printf(format,
                        p.getId(),
                        p.getDni(),
                        p.getNombre(),
                        p.getApellido(),
                        nroHc,
                        grupo
                );
            }

        } catch (Exception e) {
            System.err.println("Error al listar pacientes: " + e.getMessage());
        }
    }

    /**
     * Solicita un DNI al usuario y busca al paciente correspondiente.
     * Si lo encuentra, muestra su ficha detallada; si no, informa el error.
     */
    public void findPatientByDni() {
        try {
            String dni = readInput("Ingrese DNI a buscar: ");
            Optional<Paciente> opt = pacienteService.findByDni(dni);

            if (opt.isPresent()) {
                printPatientDetails(opt.get());
            } else {
                System.out.println("\n⚠ No se encontró ningún paciente con DNI: " + dni);
            }

        } catch (Exception e) {
            MenuDisplay.printError("Error al buscar paciente: " + e.getMessage());
        }
    }

    /**
     * Gestiona la actualización de datos personales de un paciente.
     * <p>
     * Permite al usuario dejar campos vacíos (ENTER) para mantener los valores actuales.
     * Valida la existencia del ID antes de proceder.
     * </p>
     */
    public void updatePatient() {
        try {
            String idStr = readInput("Ingrese el ID del paciente a actualizar: ");
            Long pacienteID = Long.parseLong(idStr);

            Optional<Paciente> opt = pacienteService.findById(pacienteID);

            if (opt.isEmpty()) {
                MenuDisplay.printError("No se encontró un Paciente con ID: " + pacienteID);
                return;
            }

            Paciente p = opt.get();
            System.out.println("\n════════ EDITANDO PACIENTE (ID: " + p.getId() + ") ════════");
            System.out.println("(Deje vacío y presione ENTER para mantener el valor actual)\n");

            String nombre = readInput("Nuevo Nombre [" + p.getNombre() + "]: ");
            if (!nombre.isBlank()) p.setNombre(nombre);

            String apellido = readInput("Nuevo Apellido [" + p.getApellido() + "]: ");
            if (!apellido.isBlank()) p.setApellido(apellido);

            String dni = readInput("Nuevo DNI [" + p.getDni() + "]: ");
            if (!dni.isBlank()) p.setDni(dni);

            String fechaActual = (p.getFechaNacimiento() != null) ? p.getFechaNacimiento().toString() : "N/A";
            String nuevaFechaStr = readInput("Nueva Fecha Nac (YYYY-MM-DD) [" + fechaActual + "]: ");

            if (!nuevaFechaStr.isBlank()) {
                try {
                    p.setFechaNacimiento(LocalDate.parse(nuevaFechaStr));
                } catch (DateTimeParseException e) {
                    System.out.println("❌ Fecha inválida. Se conservará la fecha anterior: " + fechaActual);
                }
            }

            pacienteService.update(p);
            System.out.println("\n✅ ¡Paciente actualizado con éxito!");

        } catch (NumberFormatException e) {
            MenuDisplay.printError("El ID debe ser un número válido.");
        } catch (Exception e) {
            MenuDisplay.printError("Error al actualizar paciente: " + e.getMessage());
        }
    }

    /**
     * Gestiona la actualización de la Historia Clínica de un paciente.
     * <p>
     * Muestra los valores permitidos para el Grupo Sanguíneo antes de solicitar la entrada.
     * Permite edición parcial (dejando campos vacíos).
     * Actualiza la persistencia a través de {@link HistoriaClinicaService}.
     * </p>
     */
    public void updateClinicalHistory(){
        try {
            String idStr = readInput("Ingrese el ID del Paciente cuya Historia Clínica desea actualizar: ");
            Long pacienteID = Long.parseLong(idStr);

            Optional<Paciente> opt = pacienteService.findById(pacienteID);

            if (opt.isEmpty()) {
                System.out.println("No se encontró un Paciente con ese ID.");
                return;
            }
            if (opt.get().getHistoriaClinica() == null) {
                System.out.println("Este paciente no tiene Historia Clínica asociada.");
                return;
            }

            Paciente p = opt.get();
            System.out.println("\n════════ EDITANDO HISTORIA CLÍNICA ════════\n");
            System.out.println("(Deje vacío y presione ENTER para mantener el valor actual)\n");

            String nroHistoria = readInput("Nuevo número de historia [" + p.getHistoriaClinica().getNroHistoria() + "]: ");
            if (!nroHistoria.isBlank()) p.getHistoriaClinica().setNroHistoria(nroHistoria);

            System.out.print("Valores permitidos: ");
            for (GrupoSanguineo g : GrupoSanguineo.values()) {
                System.out.print(g.db() + " ");
            }
            System.out.println();
            GrupoSanguineo currentGrupo = p.getHistoriaClinica().getGrupoSanguineo();
            String displayGrupo = (currentGrupo != null) ? currentGrupo.db() : "Sin Asignar";
            String grupoTexto = readInput("Nuevo grupo sanguíneo [" + displayGrupo + "]: ");
            if (!grupoTexto.isBlank()) {
                try {
                    GrupoSanguineo gs = GrupoSanguineo.fromDb(grupoTexto);
                    p.getHistoriaClinica().setGrupoSanguineo(gs);
                } catch (IllegalArgumentException e){
                    System.out.println("❌ Grupo no válido. Se conserva el valor anterior.");
                }
            }

            String ant = readInput("Nuevos antecedentes (opcional): ");
            if (!ant.isBlank()) p.getHistoriaClinica().setAntecedentes(ant);

            String med = readInput("Nueva medicación actual (opcional): ");
            if (!med.isBlank()) p.getHistoriaClinica().setMedicacionActual(med);

            String obs = readInput("Nuevas observaciones (opcional): ");
            if (!obs.isBlank()) p.getHistoriaClinica().setObservaciones(obs);

            if (p.getHistoriaClinica() != null) {
                hcService.update(p.getHistoriaClinica());
            }
            System.out.println("\n✅ ¡Historia clinica actualizada con éxito!");
        } catch (NumberFormatException e) {
            MenuDisplay.printError("El ID debe ser un número válido.");
        } catch (Exception e) {
            MenuDisplay.printError("Error al actualizar la historia clínica: " + e.getMessage());
        }
    }

    /**
     * Muestra un listado detallado (tipo ficha) de todas las historias clínicas.
     * <p>
     * Recupera el nombre del paciente asociado a cada historia mediante su ID para
     * dar contexto al reporte. Muestra textos largos (antecedentes, medicación)
     * en bloques completos.
     * </p>
     */
    public void listClinicalHistories() {
        try {
            System.out.println("\n=== LISTADO COMPLETO DE HISTORIAS CLÍNICAS ===");

            List<HistoriaClinica> historias = hcService.getAll();

            if (historias.isEmpty()) {
                System.out.println("⚠ No hay historias clínicas registradas.");
                return;
            }

            for (HistoriaClinica hc : historias) {
                String nombrePaciente = "Desconocido";
                String dniPaciente = "S/D";

                if (hc.getPacienteId() != null) {
                    Optional<Paciente> optP = pacienteService.findById(hc.getPacienteId());
                    if (optP.isPresent()) {
                        Paciente p = optP.get();
                        nombrePaciente = p.getApellido().toUpperCase() + ", " + p.getNombre();
                        dniPaciente = p.getDni();
                    }
                }

                String grupo = (hc.getGrupoSanguineo() != null) ? hc.getGrupoSanguineo().db() : "S/D";
                String ant   = (hc.getAntecedentes() != null && !hc.getAntecedentes().isBlank()) ? hc.getAntecedentes() : "Sin antecedentes";
                String med   = (hc.getMedicacionActual() != null && !hc.getMedicacionActual().isBlank()) ? hc.getMedicacionActual() : "Ninguna";
                String obs   = (hc.getObservaciones() != null && !hc.getObservaciones().isBlank()) ? hc.getObservaciones() : "-";

                System.out.println("──────────────────────────────────────────────────────────");
                System.out.printf("👤 PACIENTE: %-30s | 🆔 DNI: %s%n", nombrePaciente, dniPaciente);
                System.out.println("   - - - - - - - - - - - - - - - - - - - - - - - - - - -");

                System.out.printf("📄 Nro HC: %-10s | 🩸 Grupo: %s | 🆔 ID HC: %d%n",
                        hc.getNroHistoria(), grupo, hc.getId());

                System.out.println("📋 Antecedentes: " + ant);
                System.out.println("💊 Medicación:   " + med);

                if (!obs.equals("-")) {
                    System.out.println("📝 Observaciones: " + obs);
                }
            }
            System.out.println("──────────────────────────────────────────────────────────");

        } catch (Exception e) {
            MenuDisplay.printError("Error al listar historias: " + e.getMessage());
        }
    }

    /**
     * Gestiona el proceso de eliminación (baja lógica) de un paciente.
     * <p>
     * Solicita confirmación explícita del usuario antes de proceder.
     * </p>
     */
    public void deletePatient() {
        try {
            System.out.println("\n=== ELIMINAR PACIENTE ===");
            String idStr = readInput("Ingrese el ID del paciente a eliminar: ");
            Long id = Long.parseLong(idStr);

            Optional<Paciente> opt = pacienteService.findById(id);

            if (opt.isEmpty()) {
                MenuDisplay.printError("No existe ningún paciente con el ID: " + id);
                return;
            }

            Paciente p = opt.get();

            System.out.println("⚠ VA A ELIMINAR A: " + p.getNombre() + " " + p.getApellido() + " (DNI: " + p.getDni() + ")");
            String confirmacion = readInput("¿Está seguro? Esta acción no se puede deshacer (s/n): ");

            if (confirmacion.equalsIgnoreCase("s")) {
                pacienteService.delete(id);
                System.out.println("✅ Paciente eliminado correctamente.");
            } else {
                System.out.println("🛑 Operación cancelada.");
            }

        } catch (NumberFormatException e) {
            MenuDisplay.printError("El ID debe ser un número válido.");
        } catch (Exception e) {
            MenuDisplay.printError("Error al intentar eliminar: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para leer una línea de texto desde la consola.
     *
     * @param mensaje El mensaje (prompt) a mostrar al usuario antes de leer.
     * @return El texto ingresado por el usuario, sin espacios al inicio ni al final.
     */
    private String readInput(String mensaje) {
        System.out.print(mensaje);
        String texto = scanner.nextLine();
        return texto.trim();
    }

    /**
     * Método auxiliar para imprimir los detalles de un paciente y su historia clínica
     * en formato de ficha.
     *
     * @param p El objeto Paciente a mostrar.
     */
    private void printPatientDetails(Paciente p) {
        HistoriaClinica h = p.getHistoriaClinica();

        System.out.println("\n" + "═".repeat(12) + " FICHA DEL PACIENTE " + "═".repeat(12));

        System.out.printf(" %-20s: %s %s%n", "Nombre Completo", p.getNombre(), p.getApellido());
        System.out.printf(" %-20s: %s%n", "DNI", p.getDni());

        String fecha = (p.getFechaNacimiento() != null) ? p.getFechaNacimiento().toString() : "No registrada";
        System.out.printf(" %-20s: %s%n", "Fecha Nacimiento", fecha);

        System.out.println("─".repeat(14) + " DATOS CLÍNICOS " + "─".repeat(14));

        if (h != null) {
            String nroHc = (h.getNroHistoria() != null) ? h.getNroHistoria() : "S/D";
            String grupo = (h.getGrupoSanguineo() != null) ? h.getGrupoSanguineo().db() : "-";
            String obs   = (h.getObservaciones() != null) ? h.getObservaciones() : "-";

            System.out.printf(" %-20s: %s%n", "Nro. Historia", nroHc);
            System.out.printf(" %-20s: %s%n", "Grupo Sanguíneo", grupo);
            System.out.printf(" %-20s: %s%n", "Observaciones", obs);
        } else {
            System.out.println(" ⚠ El paciente no tiene Historia Clínica asociada.");
        }

        System.out.println("═".repeat(44));
    }

}