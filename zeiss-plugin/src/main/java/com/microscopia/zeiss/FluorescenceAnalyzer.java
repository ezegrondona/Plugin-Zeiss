package com.microscopia.zeiss;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica principal para análisis de fluorescencia
 */
public class FluorescenceAnalyzer {

    private DialogManager dialogManager = new DialogManager();

    /**
     * Renombra archivos .tif con el prefijo del nombre de la subcarpeta
     */
    public void renombrarArchivos(String rootDir) {
        File rootFile = new File(rootDir);
        renombrarArchivosRecursivo(rootFile);
    }

    private void renombrarArchivosRecursivo(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        String folderName = folder.getName();
        if (folderName.isEmpty()) return;

        for (File file : files) {
            if (file.isDirectory()) {
                renombrarArchivosRecursivo(file);
            } else if (file.getName().toLowerCase().endsWith(".tif")) {
                // Evitar renombrar dos veces
                if (file.getName().startsWith(folderName + "_")) {
                    continue;
                }
                String newName = folderName + "_" + file.getName();
                File newFile = new File(file.getParent(), newName);
                try {
                    file.renameTo(newFile);
                } catch (Exception e) {
                    // Ignorar errores
                }
            }
        }
    }

    /**
     * Procesa la carpeta seleccionada
     */
    public boolean procesarCarpeta(String rootDir, DialogManager.ConfigOptions opciones) {
        try {
            // Configurar mediciones
            String cfg = "area mean display redirect=None decimal=3";
            IJ.run("Set Measurements...", cfg);

            // Tablas de resultados
            ResultsTable masterDapi = new ResultsTable();
            ResultsTable masterAf594 = new ResultsTable();
            ResultsTable masterAf488 = new ResultsTable();

            // Variable compartida para sustracción
            int[] valorSustraccionRef = {-1}; // -1 = no determinado

            // Procesar canales
            int countDapi = 0;
            int countRojo = 0;
            int countVerde = 0;

            if (opciones.dapi) {
                countDapi = procesarDapi(rootDir, masterDapi);
            }

            if (opciones.rojo) {
                ChannelProcessor procesadorRojo = new ChannelProcessor("AF594", "af594", "Red");
                countRojo = procesadorRojo.procesar(rootDir, masterAf594, opciones.tipoArea, valorSustraccionRef);
            }

            if (opciones.verde) {
                ChannelProcessor procesadorVerde = new ChannelProcessor("AF488", "af488", "Green");
                countVerde = procesadorVerde.procesar(rootDir, masterAf488, opciones.tipoArea, valorSustraccionRef);
            }

            // Guardar resultados
            if (masterDapi.size() > 0) {
                masterDapi.save(new File(rootDir, "Resultados_DAPI.csv").getAbsolutePath());
            }
            if (masterAf594.size() > 0) {
                masterAf594.save(new File(rootDir, "Resultados_AF594.csv").getAbsolutePath());
            }
            if (masterAf488.size() > 0) {
                masterAf488.save(new File(rootDir, "Resultados_AF488.csv").getAbsolutePath());
            }

            // Limpiar ventanas temporales
            limpiarVentanas();

            // Mostrar mensaje de éxito
            mostrarMensajeExito(rootDir, opciones, countDapi, countRojo, countVerde);

            return true;

        } catch (Exception e) {
            IJ.error("Error en procesamiento", "Se produjo un error:\n" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Procesa imágenes DAPI
     */
    private int procesarDapi(String rootDir, ResultsTable masterDapi) {
        int count = 0;
        List<File> archivos = encontrarArchivosTif(new File(rootDir), "dapi");

        for (File archivo : archivos) {
            try {
                ImagePlus imp = IJ.openImage(archivo.getAbsolutePath());
                if (imp == null) continue;

                imp.show();
                IJ.run(imp, "8-bit", "");
                imp.setAutoThreshold("Triangle dark");
                IJ.run(imp, "Convert to Mask", "");
                IJ.run(imp, "Analyze Particles...", "size=10-Infinity show=Outlines exclude summarize");

                // Extraer datos de Summary
                TextWindow sumWin = (TextWindow) WindowManager.getWindow("Summary");
                if (sumWin != null) {
                    String[] headers = sumWin.getTextPanel().getColumnHeadings().split("\t");
                    String[] lines = sumWin.getTextPanel().getText().split("\n");

                    if (lines.length >= 2) {
                        String[] vals = lines[lines.length - 1].split("\t");
                        masterDapi.incrementCounter();
                        int row = masterDapi.getCounter() - 1;

                        File parentFolder = archivo.getParentFile();
                        masterDapi.setValue("Carpeta", row, parentFolder.getName());
                        masterDapi.setValue("Archivo", row, archivo.getName());

                        for (int i = 0; i < headers.length && i < vals.length; i++) {
                            try {
                                masterDapi.addValue(headers[i], Double.parseDouble(vals[i]));
                            } catch (NumberFormatException e) {
                                masterDapi.setValue(headers[i], row, vals[i]);
                            }
                        }
                        count++;
                    }
                }
                IJ.run("Close All", "");

            } catch (Exception e) {
                IJ.log("Error procesando DAPI " + archivo.getName() + ": " + e.getMessage());
            }
        }

        return count;
    }

    /**
     * Encuentra archivos .tif que contengan una palabra clave
     */
    private List<File> encontrarArchivosTif(File folder, String keyword) {
        List<File> resultados = new ArrayList<>();
        encontrarArchivosTifRecursivo(folder, keyword.toLowerCase(), resultados);
        return resultados;
    }

    private void encontrarArchivosTifRecursivo(File folder, String keyword, List<File> resultados) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                encontrarArchivosTifRecursivo(file, keyword, resultados);
            } else if (file.getName().toLowerCase().endsWith(".tif") &&
                       file.getName().toLowerCase().contains(keyword)) {
                resultados.add(file);
            }
        }
    }

    /**
     * Limpia ventanas temporales
     */
    private void limpiarVentanas() {
        for (String winName : new String[]{"Summary", "Results"}) {
            java.awt.Window win = WindowManager.getWindow(winName);
            if (win != null) {
                win.dispose();
            }
        }
    }

    /**
     * Muestra mensaje de éxito
     */
    private void mostrarMensajeExito(String rootDir, DialogManager.ConfigOptions opciones,
                                     int countDapi, int countRojo, int countVerde) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Proceso Finalizado en:\n").append(rootDir).append("\n\n");

        if (opciones.dapi) {
            mensaje.append("DAPI:  ").append(countDapi).append(" procesadas\n");
        }
        if (opciones.rojo) {
            mensaje.append("AF594: ").append(countRojo).append(" procesadas\n");
        }
        if (opciones.verde) {
            mensaje.append("AF488: ").append(countVerde).append(" procesadas\n");
        }

        WaitForUserDialog dialogo = new WaitForUserDialog("Exito", mensaje.toString());
        dialogo.show();
    }
}