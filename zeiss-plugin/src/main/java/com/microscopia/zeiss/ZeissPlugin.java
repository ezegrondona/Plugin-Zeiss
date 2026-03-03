package com.microscopia.zeiss;

import ij.plugin.PlugIn;
import ij.IJ;
import java.io.File;

/**
 * Plugin principal para análisis de intensidad de fluorescencia (DAPI, AF594, AF488)
 * Desarrollado por el Dr. Ezequiel Grondona
 * Centro de Microscopia Electrónica - UNC - INICSA - CONICET
 */
public class ZeissPlugin_ implements PlugIn {

    private DialogManager dialogManager;
    private FluorescenceAnalyzer analyzer;

    @Override
    public void run(String arg) {
        try {
            // Inicializar componentes
            this.dialogManager = new DialogManager();
            this.analyzer = new FluorescenceAnalyzer();

            // Variable global para controlar flujo (calibración)
            boolean[] irACalibrar = {false};

            // Loop principal
            boolean continuar = true;
            while (continuar) {
                // 1. Mostrar ventana de configuración inicial
                DialogManager.ConfigOptions opciones = dialogManager.mostrarVentanaConfiguracion(irACalibrar);
                if (opciones == null) {
                    break;
                }

                // 2. Mostrar diálogo de calibración si el usuario lo solicita
                if (irACalibrar[0]) {
                    dialogManager.mostrarDialogoCalibracion();
                    irACalibrar[0] = false;
                }

                // 3. Seleccionar carpeta
                File carpetaSeleccionada = dialogManager.seleccionarCarpeta();
                if (carpetaSeleccionada == null || !carpetaSeleccionada.isDirectory()) {
                    break;
                }

                // 4. Renombrar archivos .tif
                analyzer.renombrarArchivos(carpetaSeleccionada.getAbsolutePath());

                // 5. Procesar carpeta
                boolean procesoExitoso = analyzer.procesarCarpeta(
                    carpetaSeleccionada.getAbsolutePath(),
                    opciones
                );

                if (!procesoExitoso) {
                    break;
                }

                // 6. Preguntar si continuar con otra carpeta
                continuar = dialogManager.preguntarContinuar();
            }

        } catch (Exception e) {
            IJ.error("Error en el Plugin", "Se produjo un error:\n" + e.getMessage());
            e.printStackTrace();
        }
    }
}