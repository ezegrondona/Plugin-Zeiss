package com.microscopia.zeiss;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Procesador para canales de fluorescencia (AF594, AF488)
 */
public class ChannelProcessor {

    private String nombreCanal;
    private String palabraClave;
    private String colorLut;

    public ChannelProcessor(String nombreCanal, String palabraClave, String colorLut) {
        this.nombreCanal = nombreCanal;
        this.palabraClave = palabraClave;
        this.colorLut = colorLut;
    }

    /**
     * Procesa todos los archivos de este canal
     */
    public int procesar(String rootDir, ResultsTable masterTable, String tipoArea, int[] valorSustraccionRef) {
        int count = 0;
        int valorSustraccion = valorSustraccionRef[0];
        List<File> archivos = encontrarArchivosTif(new File(rootDir), palabraClave);

        for (File archivo : archivos) {
            try {
                ImagePlus imp = IJ.openImage(archivo.getAbsolutePath());
                if (imp == null) continue;

                imp.show();
                IJ.run(imp, "8-bit", "");
                IJ.run(imp, colorLut, "");

                if ("Area de Marca".equals(tipoArea)) {
                    procesarAreaMarca(imp);
                } else {
                    valorSustraccion = procesarAreaTotal(imp, valorSustraccion, valorSustraccionRef, nombreCanal);
                }

                // Extraer mediciones
                ResultsTable rt = ResultsTable.getResultsTable();
                if (rt != null && rt.size() > 0) {
                    masterTable.incrementCounter();
                    int row = masterTable.getCounter() - 1;

                    File parentFolder = archivo.getParentFile();
                    masterTable.setValue("Carpeta", row, parentFolder.getName());
                    masterTable.setValue("Archivo", row, archivo.getName());

                    for (String heading : rt.getHeadings()) {
                        if ("Label".equals(heading)) continue;
                        try {
                            double valor = rt.getValue(heading, rt.size() - 1);
                            masterTable.addValue(heading, valor);
                        } catch (Exception e) {
                            // Ignorar
                        }
                    }
                    count++;
                }

                IJ.run("Close All", "");

            } catch (Exception e) {
                IJ.log("Error procesando " + nombreCanal + " " + archivo.getName() + ": " + e.getMessage());
            }
        }

        return count;
    }

    /**
     * Procesa área de marca
     */
    private void procesarAreaMarca(ImagePlus imp) {
        String thresholdMethod = nombreCanal.equals("AF594") ? "Triangle dark" : "Moments dark";
        imp.setAutoThreshold(thresholdMethod);
        IJ.run(imp, "Create Selection", "");
        IJ.run(imp, "Measure", "");
    }

    /**
     * Procesa área total con sustracción de fondo
     */
    private int procesarAreaTotal(ImagePlus imp, int valorSustraccion, int[] valorSustraccionRef, String nombreCanal) {
        // Si es la primera vez, determinar el valor de sustracción
        if (valorSustraccion < 0) {
            WaitForUserDialog dialogo = new WaitForUserDialog(
                "Determinar de Fondo",
                "1. Dibuje un rectángulo sobre el FONDO de la imagen.\n" +
                "2. Presione la tecla 'm' para medir.\n" +
                "3. Presione OK para capturar el valor."
            );
            dialogo.show();

            ResultsTable rtTemp = ResultsTable.getResultsTable();
            if (rtTemp != null && rtTemp.size() > 0) {
                double valMedido = rtTemp.getValue("Mean", rtTemp.size() - 1);

                GenericDialog gdSust = new GenericDialog("Confirmar Sustraccion");
                gdSust.addNumericField("Valor a restar de todas las " + nombreCanal + ":", valMedido, 0);
                gdSust.showDialog();

                if (gdSust.wasCanceled()) {
                    valorSustraccion = 0;
                } else {
                    valorSustraccion = (int) gdSust.getNextNumber();
                }
            } else {
                valorSustraccion = 0;
            }

            valorSustraccionRef[0] = valorSustraccion;
        }

        // Aplicar sustracción si es necesario
        if (valorSustraccion > 0) {
            IJ.run(imp, "Select None", "");
            IJ.run(imp, "Subtract...", "value=" + valorSustraccion);
        }

        IJ.run(imp, "Measure", "");
        return valorSustraccion;
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
}