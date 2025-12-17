package main;

import model.HistoriaClinica;
import model.Paciente;
import service.HistoriaClinicaService;
import service.PacienteService;
import model.HistoriaClinica.GrupoSanguineo;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class MenuHandler {
    private Scanner scanner;
    private PacienteService pacienteService;
    private HistoriaClinicaService hcService;

    public MenuHandler(Scanner scanner, PacienteService pacienteService, HistoriaClinicaService hcService) {
        this.scanner = scanner;
        this.pacienteService = pacienteService;
        this.hcService = hcService;
    }

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

    private String readInput(String mensaje) {
        System.out.print(mensaje);
        String texto = scanner.nextLine();
        return texto.trim();
    }

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