package main;

import service.HistoriaClinicaService;
import service.PacienteService;
import service.impl.HistoriaClinicaServiceImpl;
import service.impl.PacienteServiceImpl;

import java.util.Scanner;

/**
 * Punto de entrada para la lógica de navegación de la aplicación.
 * <p>
 * Esta clase encapsula el bucle principal del programa (Main Loop). Se encarga de:
 * <ul>
 * <li>Inicializar las dependencias (Servicios y Handlers).</li>
 * <li>Mostrar el menú de opciones repetitivamente.</li>
 * <li>Capturar la selección del usuario y delegar la ejecución a {@link MenuHandler}.</li>
 * <li>Manejar errores globales de entrada (como ingresar texto en lugar de números).</li>
 * </ul>
 * </p>
 */
public class AppMenu {
    private Scanner scanner;
    private MenuHandler menuHandler;

    /**
     * Inicializa la aplicación configurando las dependencias necesarias.
     * <p>
     * Crea las instancias de los servicios ({@link PacienteServiceImpl}, {@link HistoriaClinicaServiceImpl})
     * y configura el {@link MenuHandler} con un {@link Scanner} para la entrada de datos.
     * </p>
     */
    public AppMenu() {
        this.scanner = new Scanner(System.in);
        PacienteService pService = new PacienteServiceImpl();
        HistoriaClinicaService hService = new HistoriaClinicaServiceImpl();
        this.menuHandler = new MenuHandler(scanner, pService, hService);
    }

    /**
     * Inicia la ejecución del menú interactivo.
     * <p>
     * Mantiene la aplicación en ejecución dentro de un bucle {@code while} hasta que el usuario
     * decide salir (Opción 0).
     * </p>
     * <p>
     * Incluye manejo de excepciones para:
     * <ul>
     * <li>{@link NumberFormatException}: Cuando el usuario ingresa caracteres no numéricos en el menú.</li>
     * <li>{@link Exception}: Cualquier otro error inesperado durante la ejecución.</li>
     * </ul>
     * </p>
     */
    public void start() {
        boolean running = true;
        while (running) {
            try {
                MenuDisplay.mostrarMenuPrincipal();
                int opcion = Integer.parseInt(scanner.nextLine());

                switch (opcion) {
                    case 1:
                        menuHandler.createPatient();
                        break;
                    case 2:
                        menuHandler.listPatients();
                        break;
                    case 3:
                        menuHandler.findPatientByDni();
                        break;
                    case 4:
                        menuHandler.updatePatient();
                        break;
                    case 5:
                        menuHandler.updateClinicalHistory();
                        break;
                    case 6:
                        menuHandler.listClinicalHistories();
                        break;
                    case 7:
                        menuHandler.deletePatient();
                        break;
                    case 0:
                        System.out.println("Saliendo...");
                        running = false;
                        break;
                    default:
                        System.out.println("⚠ Opción inválida, intente de nuevo.");
                }
            } catch (NumberFormatException e) {
                MenuDisplay.printError("Debe ingresar un número válido.");
            } catch (Exception e) {
                MenuDisplay.printError("Error inesperado: " + e.getMessage());
            }
        }
        scanner.close();
    }
}