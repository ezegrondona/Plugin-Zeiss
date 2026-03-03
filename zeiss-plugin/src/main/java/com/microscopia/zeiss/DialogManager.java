package com.microscopia.zeiss;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.io.DirectoryChooser;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.Desktop;
import java.net.URI;

/**
 * Gestiona todos los diálogos de interfaz del usuario
 */
public class DialogManager {

    // Colores para los diálogos
    private static final Color COLOR_MANUAL = new Color(157, 167, 159);      // Verde grisáceo
    private static final Color COLOR_CALIBRAR = new Color(25, 136, 255);     // Azul
    private static final Color COLOR_INFO = new Color(0, 151, 161);          // Teal/Turquesa
    private static final Color COLOR_BLANCO = Color.WHITE;

    // Fuentes
    private Font fontMsg = new Font("SansSerif", Font.PLAIN, 12);
    private Font fontBtn = new Font("SansSerif", Font.PLAIN, 9);
    private Font fontBtnBold = new Font("SansSerif", Font.BOLD, 9);
    private Font fontSection = new Font("SansSerif", Font.BOLD, 13);

    // Estructura de opciones de configuración
    public static class ConfigOptions {
        public boolean dapi;
        public boolean rojo;
        public boolean verde;
        public String tipoArea; // "Area de Marca" o "Area Total"

        public ConfigOptions(boolean dapi, boolean rojo, boolean verde, String tipoArea) {
            this.dapi = dapi;
            this.rojo = rojo;
            this.verde = verde;
            this.tipoArea = tipoArea;
        }
    }

    /**
     * Muestra la ventana de configuración inicial
     */
    public ConfigOptions mostrarVentanaConfiguracion(boolean[] irACalibrar) {
        GenericDialog gd = new GenericDialog("Analizar INTENSIDAD de Fluorescencia");

        // Panel superior con botones INFO y GUÍA
        Panel panelBotonesSup = new Panel();
        panelBotonesSup.setLayout(new GridLayout(1, 2, 5, 0));

        Button btnInfo = new Button("INFO DEL PLUGIN");
        Button btnGuia = new Button("GUIA DE USO");

        btnInfo.addActionListener(e -> mostrarInfoPlugin());
        btnGuia.addActionListener(e -> abrirGuia());

        panelBotonesSup.add(btnInfo);
        panelBotonesSup.add(btnGuia);
        gd.addPanel(panelBotonesSup);

        // Sección de determinaciones
        gd.addMessage("DETERMINACIONES A ANALIZAR");
        gd.addCheckbox("DAPI", true);
        gd.addCheckbox("Rojo (AF594)", false);
        gd.addCheckbox("Verde (AF488)", false);

        // Sección de medición de área
        gd.addMessage("MEDICION DE AREA");
        gd.addRadioButtonGroup("", new String[]{"Area de Marca", "Area Total de foto"}, 2, 1, "Area de Marca");

        // Sección de calibración
        gd.addMessage("CALIBRACION");

        Panel panelBotonesInf = new Panel();
        panelBotonesInf.setLayout(new GridLayout(1, 2, 5, 0));

        Button btnManual = new Button("GUIA DE CALIBRACION");
        Button btnCalibrar = new Button("CALIBRAR");

        btnManual.addActionListener(e -> abrirAyuda());
        btnCalibrar.addActionListener(e -> {
            irACalibrar[0] = true;
            cerrarDialogo(gd);
        });

        panelBotonesInf.add(btnManual);
        panelBotonesInf.add(btnCalibrar);
        gd.addPanel(panelBotonesInf);

        // Estilizar diálogo
        estilizarDialogo(gd, true, btnInfo, btnGuia, btnManual, btnCalibrar);

        gd.showDialog();

        if (gd.wasCanceled()) {
            return null;
        }

        boolean opcionDapi = gd.getNextBoolean();
        boolean opcionRojo = gd.getNextBoolean();
        boolean opcionVerde = gd.getNextBoolean();
        String tipoArea = gd.getNextRadioButton();

        // Validar que al menos un canal esté seleccionado
        if (!opcionDapi && !opcionRojo && !opcionVerde) {
            IJ.showMessage("Error", "Debe seleccionar al menos una determinación (DAPI, Rojo o Verde).");
            return null;
        }

        return new ConfigOptions(opcionDapi, opcionRojo, opcionVerde, tipoArea);
    }

    /**
     * Muestra el diálogo de calibración
     */
    public void mostrarDialogoCalibracion() {
        WaitForUserDialog dialogo = new WaitForUserDialog(
            "Calibrar imagen",
            "1. Abra una imagen y calibre desde SET SCALE.\n" +
            "2. Cierre la imagen sin guardar (DON'T SAVE).\n" +
            "3. Presione OK para continuar."
        );
        dialogo.show();
    }

    /**
     * Selecciona una carpeta
     */
    public File seleccionarCarpeta() {
        DirectoryChooser dc = new DirectoryChooser("Selecciona la carpeta con las fotos a analizar");
        String path = dc.getDirectory();
        return (path != null && !path.isEmpty()) ? new File(path) : null;
    }

    /**
     * Pregunta si continuar con otra carpeta
     */
    public boolean preguntarContinuar() {
        GenericDialog gd = new GenericDialog("Analizar otra carpeta");
        gd.addMessage("Presiona OK para seleccionar otra carpeta o Cancelar para salir.");
        estilizarDialogo(gd, false, null, null, null, null);
        gd.showDialog();
        return !gd.wasCanceled();
    }

    /**
     * Muestra información del plugin
     */
    private void mostrarInfoPlugin() {
        String infoTexto = "<html><center>" +
            "Plugin desarrollado por el Dr. Ezequiel Grondona.<br>" +
            "-Centro de Microscopia Electronica-<br>" +
            "-UNC - INICSA - CONICET-<br>" +
            "- 2026 -<br><br>" +
            "Lea atentamente la guia de uso antes de utilizar el plugin.<br>" +
            "Consultas y sugerencias: ezequiel.grondona@unc.edu.ar<br>" +
            "</center></html>";
        IJ.showMessage("Informacion del Plugin", infoTexto);
    }

    /**
     * Abre la guía de uso
     */
    private void abrirGuia() {
        try {
            String url = "https://drive.google.com/file/d/1SjVrP6OhyViGwUHqy1IcFMwNklp1JAp_/view?usp=sharing";
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            IJ.showMessage("Error", "No se pudo abrir el enlace:\n" + e.getMessage());
        }
    }

    /**
     * Abre la guía de calibración
     */
    private void abrirAyuda() {
        try {
            String url = "https://drive.google.com/file/d/16HZT60p1QiIpBeWDWTt_rA-cOUyVJrQf/view?usp=sharing";
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            IJ.showMessage("Error", "No se pudo abrir el enlace:\n" + e.getMessage());
        }
    }

    /**
     * Cierra un diálogo
     */
    private void cerrarDialogo(GenericDialog gd) {
        try {
            gd.dispose();
        } catch (Exception e) {
            // Ignorar
        }
    }

    /**
     * Estiliza un diálogo
     */
    private void estilizarDialogo(GenericDialog gd, boolean esPrincipal,
                                   Button btnInfo, Button btnGuia, Button btnManual, Button btnCalibrar) {
        // Estilizar botones
        if (btnInfo != null) {
            btnInfo.setFont(fontBtn);
            btnInfo.setForeground(COLOR_BLANCO);
            btnInfo.setBackground(COLOR_INFO);
        }
        if (btnGuia != null) {
            btnGuia.setFont(fontBtnBold);
            btnGuia.setForeground(COLOR_BLANCO);
            btnGuia.setBackground(COLOR_INFO);
        }
        if (btnManual != null) {
            btnManual.setFont(fontBtn);
            btnManual.setForeground(COLOR_BLANCO);
            btnManual.setBackground(COLOR_MANUAL);
        }
        if (btnCalibrar != null) {
            btnCalibrar.setFont(fontBtn);
            btnCalibrar.setForeground(COLOR_BLANCO);
            btnCalibrar.setBackground(COLOR_MANUAL);
        }

        // Estilizar componentes del diálogo
        for (Component comp : gd.getComponents()) {
            if (comp instanceof Label) {
                Label label = (Label) comp;
                String texto = label.getText();
                label.setFont(fontMsg);

                String textoUpper = texto.toUpperCase();
                if (textoUpper.contains("DETERMINACIONES") || textoUpper.contains("MEDICION") || textoUpper.contains("CALIBRACION")) {
                    label.setFont(fontSection);
                    label.setBackground(COLOR_CALIBRAR);
                    label.setForeground(COLOR_BLANCO);
                    label.setAlignment(Label.CENTER);
                }
            }
        }

        if (esPrincipal) {
            gd.setBackground(new Color(240, 245, 240));
        }
    }
}