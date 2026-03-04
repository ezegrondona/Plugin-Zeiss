
import ij.*;
import ij.gui.*;
import ij.gui.Roi.*;
import ij.gui.HistogramWindow.*;
import ij.measure.*;
import ij.process.*;
import ij.util.*;
import ij.util.Tools;
import ij.text.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import ij.plugin.PlugIn;

import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.text.*;
import java.awt.event.MouseListener;


import javax.swing.*;
import javax.swing.JLabel;
import javax.swing.event.*;
import javax.swing.colorchooser.*;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 *     @version 1.0.0 date 28 Apr 2014
 *
 *     @author  Jie Shu, Guoping Qiu, Ilyas Mohammad
 *     This code is free.
 *     This tool is built in the hope that it will be useful for the research.
 *     The purpose of this tool is to detect the specified color pixels,
 *     segment and quantify the nuclei, and detect the candidate glands in
 *     IHC digital slides.
 *
 *
 */
public class Immunostaining_toolbox extends PlugInFrame implements ActionListener {

    Panel panel1, panel2, panel3, panel4, panel5, panel6, panel7;
    static Frame instance;
    static double[] prob = new double[16384];
    static double[] pronb = new double[16384];
    static double[] brownhist = new double[16384];
    static double[] nonbrownhist = new double[16384];
    static double[] problue = new double[16384];
    static double[] pronblue = new double[16384];
    static double totalb = 0;
    static double totalnb = 0;
    static Button model,  umodel,  data,  color,  nuclei,  gland,  prepro,  ok,  test;
    static JComboBox modellist,  type;
    static JCheckBox Qbox;
    static JTextField Pnuclei,  Nnuclei,  wz,  sz,  fz,  gb,  oc,  vf;
    static boolean datakey,  Ttick;
    static int l,  h,  size,  count,  Qindex,  ima;
    static JSpinner luman,  asize,  cnuclei;
    static JSpinner.NumberEditor numluman,  numasize,  numcnuclei;
    static JSlider luminance,  Asize,  Cnuclei;
    static ImagePlus imp,  Rimage;
    static JFormattedTextField bt,  it1,  it2;
    static Point point;
    static int[][] array = new int[3][1000];
    static int[] totalC = new int[1000];
    static int[] totalT = new int[1000];
    static int[][] quadCPA = new int[14][1000];
    static int[] nucleino = new int[100];

    /**
     *
     * tool menu with the functions included as:
     *          reading pre-defined model
     *          reading user defined model
     *          using semi-automated color selection tools to bulid model
     *          automaticly labeling positive areas in the slide
     *          manually setting parameters for nuclei segmetnation and quantification
     *          automaticly segment or quantify nuclei
     *          manually setting parameters for gland detection
     *          automaticly detect candidate gland structures
     *
     */
    public Immunostaining_toolbox() {
        super("IHC Tool Box");
        if (IJ.versionLessThan("1.43t")) {
            return;
        }
        instance = this;
        addKeyListener(IJ.getInstance());
        datakey = false;//state of training buttion, true = enable, flase = disable
        Ttick = true;//identification of nuclei segmentation or quantification mode
                    //true for segmentation, false for quantification
        setAlwaysOnTop(true);
        setLayout(new GridLayout(6, 1, 1, 1));

//-----training panel-----
        panel1 = new Panel();
        panel1.setLayout(new GridLayout(1, 3, 1, 1));

        data = new Button("Training");
        data.addActionListener(this);
        data.addKeyListener(IJ.getInstance());
        data.setVisible(true);
        data.setEnabled(true);
        panel1.add(data);
//*********Drop box for pre-defined model list***********
        String[] list = {"Select Model", "H-DAB", "H-DAB(more brown)", "PSR(pink)", "Elastin(brown)"};
        modellist = new JComboBox(list);
        ActionListener cbActionListener = new ActionListener() {//add actionlistner to listen for change

            @Override
            public void actionPerformed(ActionEvent e) {
                String s = (String) modellist.getSelectedItem();//get the selected item

                if (s.equals("Select Model")) {
                    IJ.showMessage("IHC_Toolbox", "Please select a model from the list");
                    Immunostaining_toolbox.data.setEnabled(true);
                    Immunostaining_toolbox.modellist.setEnabled(true);
                    Immunostaining_toolbox.umodel.setEnabled(true);
                    Immunostaining_toolbox.color.setEnabled(false);
                    Immunostaining_toolbox.nuclei.setEnabled(false);
                    Immunostaining_toolbox.gland.setEnabled(false);
                    Immunostaining_toolbox.wz.setEnabled(false);
                    Immunostaining_toolbox.sz.setEnabled(false);
                    Immunostaining_toolbox.fz.setEnabled(false);
                    Immunostaining_toolbox.gb.setEnabled(false);
                    Immunostaining_toolbox.oc.setEnabled(false);
                    Immunostaining_toolbox.vf.setEnabled(false);
                } else {
                    new ReadTxt(0);//0=read model from text file
                    Immunostaining_toolbox.data.setEnabled(false);
                    Immunostaining_toolbox.umodel.setEnabled(false);
                }

            }
        };
        modellist.addActionListener(cbActionListener);
        panel1.add(modellist);
//********************End of Drop box***************

        umodel = new Button("Read User Model");
        umodel.addActionListener(this);
        umodel.addKeyListener(IJ.getInstance());
        umodel.setVisible(true);
        umodel.setEnabled(true);
        panel1.add(umodel);

//----detecting panel-----
        panel2 = new Panel();
        panel2.setLayout(new GridLayout(1, 3, 1, 1));
        color = new Button("Color");
        color.addActionListener(this);
        color.addKeyListener(IJ.getInstance());
        color.setVisible(true);
        color.setEnabled(false);
        panel2.add(color);

        nuclei = new Button("Nuclei");
        nuclei.addActionListener(this);
        nuclei.addKeyListener(IJ.getInstance());
        nuclei.setVisible(true);
        nuclei.setEnabled(false);
        panel2.add(nuclei);

        gland = new Button("Gland");
        gland.addActionListener(this);
        gland.addKeyListener(IJ.getInstance());
        gland.setVisible(true);
        gland.setEnabled(false);
        panel2.add(gland);

        panel4 = new Panel();
        panel4.setLayout(new GridLayout(1, 3, 1, 1));
        Label text = new Label("Parameters For Nuclei Segmentation:");
        panel4.add(text);


//----nuclei parameters' panel---
        panel3 = new Panel();
        panel3.setLayout(new GridLayout(1, 3, 1, 1));

        Label text1 = new Label("window size");
        panel3.add(text1);
        wz = new JTextField("25");
        wz.setEnabled(false);
        panel3.add(wz);

        Label text2 = new Label("seed size");
        panel3.add(text2);
        sz = new JTextField("150");
        sz.setEnabled(false);
        panel3.add(sz);

        Label text3 = new Label("final size");
        panel3.add(text3);
        fz = new JTextField("150");
        fz.setEnabled(false);
        panel3.add(fz);

//*********Drop box for nuclei segmentation and quantification***********
        String[] listtype = {"Quantification", "Contour"};
        type = new JComboBox(listtype);
        ActionListener typeActionListener = new ActionListener() {//add actionlistner to listen for change

            @Override
            public void actionPerformed(ActionEvent e) {
                String s = (String) type.getSelectedItem();//get the selected item

                if (s.equals("Quantification")) {
                    Immunostaining_toolbox.Ttick = true;
                } else if (s.equals("Contour")) {
                    Immunostaining_toolbox.Ttick = false;
                }
            }
        };
        type.addActionListener(typeActionListener);
        panel4.add(type);
//********************End of Drop box***************

//-----gland parameters' panel-----------
        panel5 = new Panel();
        panel5.setLayout(new GridLayout(1, 2, 1, 1));
        Label text4 = new Label("Parameters For Gland Segmentation:");
        panel5.add(text4);

        panel6 = new Panel();
        panel6.setLayout(new GridLayout(1, 3, 1, 1));
        Label text5 = new Label("Gaussian blur");
        panel6.add(text5);
        gb = new JTextField("2");
        gb.setEnabled(false);
        panel6.add(gb);

        Label text6 = new Label("Open-by-recon.");
        panel6.add(text6);
        oc = new JTextField("40");
        oc.setEnabled(false);
        panel6.add(oc);

        Label text7 = new Label("Variance filter");
        panel6.add(text7);
        vf = new JTextField("5");
        vf.setEnabled(false);
        panel6.add(vf);

        add(panel1);
        add(panel2);
        add(panel4);
        add(panel3);
        add(panel5);
        add(panel6);
        pack();
        GUI.center(this);
        setVisible(true);
        point = panel2.getLocation();
    }

    public void windowClosed(WindowEvent e) {
        super.windowClosed(e);
        instance = null;
    }

    public void actionPerformed(ActionEvent e) {
        Immunostaining_toolbox.imp = WindowManager.getCurrentImage();
        //opened image
       
        if (imp == null) {
            IJ.beep();
            IJ.showStatus("No image");
            return;
        }
        ImageProcessor ip = imp.getProcessor();
        String label = e.getActionCommand();
        if (label == null) {
            return;
        }
        new buttonCommand(label, imp, ip);

    }
}

/**
 *
 * button action
 */
class buttonCommand extends Thread {

    private String command;
    public ImagePlus imp;//opened image
    private ImageProcessor ip;//processor for opened image

    buttonCommand(String command, ImagePlus imp, ImageProcessor ip) {
        super(command);
        this.command = command;
        this.imp = imp;
        this.ip = ip;
        setPriority(Math.max(getPriority() - 2, MIN_PRIORITY));
        start();
    }

    public void run() {
        try {
            runCommand(command, imp, ip);

        } catch (OutOfMemoryError e) {
            IJ.outOfMemory(command);
        } catch (Exception e) {
            CharArrayWriter caw = new CharArrayWriter();
            PrintWriter pw = new PrintWriter(caw);
            e.printStackTrace(pw);
            IJ.log(caw.toString());
            IJ.showStatus("");
        }
    }

    void runCommand(String command, ImagePlus imp, ImageProcessor ip) {

        IJ.showStatus(command + "...");
        if (command.equals("Training")) {
            new DataCollection(imp);
            Immunostaining_toolbox.modellist.setEnabled(false);
            Immunostaining_toolbox.umodel.setEnabled(false);
            Immunostaining_toolbox.wz.setEnabled(true);
            Immunostaining_toolbox.sz.setEnabled(true);
            Immunostaining_toolbox.fz.setEnabled(true);
            Immunostaining_toolbox.gb.setEnabled(true);
            Immunostaining_toolbox.oc.setEnabled(true);
            Immunostaining_toolbox.vf.setEnabled(true);

        }

        if (command.equals("Color")) {
            IJ.showStatus("Processing color detection...");
            new ModelFilter(imp, "color");

        }

        if (command.equals("Nuclei")) {
            IJ.showStatus("Processing nuclei segmentation...");
            Immunostaining_toolbox.Rimage = IJ.createImage("Result Image", "8-bit RGB", imp.getWidth(), imp.getHeight(), 1);
            Immunostaining_toolbox.Rimage.show();
            new ModelFilter(imp, "nuclei");

        }

        if (command.equals("Gland")) {
            IJ.showStatus("Processing gland segmentation...");
            new ModelFilter(imp, "gland");

        }

        if (command.equals("Read User Model")) {
            new ReadTxt(1);//1=read histogram from text file
            if (Immunostaining_toolbox.datakey == true) {
                Immunostaining_toolbox.data.setEnabled(false);
                Immunostaining_toolbox.modellist.setEnabled(false);
            }
        }
        imp.updateAndDraw();
    }

}

/**
 *
 * read statistical model from text file
 */
class ReadTxt {

    double[][] data = new double[16384][100];

    public ReadTxt(int mode) {
        if (mode == 0) {
            readModel();
        }
        if (mode == 1) {
            readUmodel();
        }
    }

    public void readModel() {
        String path = "/Model/";
        String select = (String) Immunostaining_toolbox.modellist.getSelectedItem();
        getText(path + select + ".txt");
        for (int i = 0; i < data.length; i++) {
            Immunostaining_toolbox.prob[i] = data[i][2];//probability for positive color
            Immunostaining_toolbox.pronb[i] = data[i][3];//probability for negative color
        }
        IJ.showStatus("Read Prebuilt-Model ["+select+"] Compeleted");

        Immunostaining_toolbox.color.setEnabled(true);
        Immunostaining_toolbox.nuclei.setEnabled(true);
        Immunostaining_toolbox.gland.setEnabled(true);
        Immunostaining_toolbox.wz.setEnabled(true);
        Immunostaining_toolbox.sz.setEnabled(true);
        Immunostaining_toolbox.fz.setEnabled(true);
        Immunostaining_toolbox.gb.setEnabled(true);
        Immunostaining_toolbox.oc.setEnabled(true);
        Immunostaining_toolbox.vf.setEnabled(true);
    }

    public void readUmodel() {
        data = readFile("User Brown Probability (Select StatisticalModel.txt)");
        if (Immunostaining_toolbox.datakey == true) {
            for (int i = 0; i < data.length; i++) {
                Immunostaining_toolbox.prob[i] = data[i][2];
                Immunostaining_toolbox.pronb[i] = data[i][3];
            }
        }

        IJ.showStatus("Read User-Model Compeleted");
    }

//*********TEST***********
    public void getText(String path) {
        String text = "";
        try {
            // get the text resource as a stream
            InputStream is = getClass().getResourceAsStream(path);
            InputStreamReader isr = new InputStreamReader(is);
            StringBuffer sb = new StringBuffer();
            char[] b = new char[8192];
            int n;
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            int i = 0;//line number
            while ((line = br.readLine()) != null) {
                // \\s+ means any number of whitespaces between tokens
                String[] tokens = line.split("\\s+");
                String var_1 = tokens[0];//cb bin
                String var_2 = tokens[1];//cr bin
                String var_3 = tokens[2];//probability for positive
                String var_4 = tokens[3];//probability for negative
                data[i][0] = Double.valueOf(var_1).doubleValue();
                data[i][1] = Double.valueOf(var_2).doubleValue();
                data[i][2] = Double.valueOf(var_3).doubleValue();
                data[i][3] = Double.valueOf(var_4).doubleValue();
                i++;
            }
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg == null || msg.equals("")) {
                msg = "" + e;
            }
            IJ.showMessage("JAR Demo", msg);
        }

    }

//************************
    //read user defined statistical color models from text file
    public double[][] readFile(String prompt) {

        File directory = new File("");
        String path = "";
        String filename = "";      
        Frame fram = new Frame();
        FileDialog fd = new FileDialog(fram, prompt, FileDialog.LOAD);

        if (prompt.equals("User Brown Probability (Select StatisticalModel.txt)")) {
            fd.setDirectory("/Model/");
            fd.setVisible(true);
            path = fd.getDirectory();
            filename = fd.getFile();
            Immunostaining_toolbox.datakey = true;
            Immunostaining_toolbox.color.setEnabled(true);
            Immunostaining_toolbox.nuclei.setEnabled(true);
            Immunostaining_toolbox.gland.setEnabled(true);
            Immunostaining_toolbox.wz.setEnabled(true);
            Immunostaining_toolbox.sz.setEnabled(true);
            Immunostaining_toolbox.fz.setEnabled(true);
            Immunostaining_toolbox.gb.setEnabled(true);
            Immunostaining_toolbox.oc.setEnabled(true);
            Immunostaining_toolbox.vf.setEnabled(true);
        }

        if ((path == null) || (filename == null)) {
            Immunostaining_toolbox.color.setEnabled(false);
            Immunostaining_toolbox.nuclei.setEnabled(false);
            Immunostaining_toolbox.gland.setEnabled(false);
            Immunostaining_toolbox.wz.setEnabled(false);
            Immunostaining_toolbox.sz.setEnabled(false);
            Immunostaining_toolbox.fz.setEnabled(false);
            Immunostaining_toolbox.gb.setEnabled(false);
            Immunostaining_toolbox.oc.setEnabled(false);
            Immunostaining_toolbox.vf.setEnabled(false);
            Immunostaining_toolbox.datakey = false;
            return null;

        }
        Vector<StringTokenizer> list = new Vector<StringTokenizer>(0, 16);
        try {
            FileReader fr = new FileReader(path + filename);
            BufferedReader br = new BufferedReader(fr);
            String line;
            list = new Vector<StringTokenizer>(0, 16);

            do {
                line = br.readLine();
                if (line != null) {
                    StringTokenizer st = new StringTokenizer(line);
                    if (st.hasMoreTokens()) {
                        list.addElement(st);
                    }
                }
            } while (line != null);
            fr.close();
        } catch (FileNotFoundException e) {
            IJ.error("File not found exception");
            Immunostaining_toolbox.color.setEnabled(false);
            Immunostaining_toolbox.nuclei.setEnabled(false);
            Immunostaining_toolbox.gland.setEnabled(false);
            Immunostaining_toolbox.wz.setEnabled(false);
            Immunostaining_toolbox.sz.setEnabled(false);
            Immunostaining_toolbox.fz.setEnabled(false);
            Immunostaining_toolbox.gb.setEnabled(false);
            Immunostaining_toolbox.oc.setEnabled(false);
            Immunostaining_toolbox.vf.setEnabled(false);
        } catch (IOException e) {
            IJ.error("IOException exception");
            Immunostaining_toolbox.color.setEnabled(false);
            Immunostaining_toolbox.nuclei.setEnabled(false);
            Immunostaining_toolbox.gland.setEnabled(false);
            Immunostaining_toolbox.wz.setEnabled(false);
            Immunostaining_toolbox.sz.setEnabled(false);
            Immunostaining_toolbox.fz.setEnabled(false);
            Immunostaining_toolbox.gb.setEnabled(false);
            Immunostaining_toolbox.oc.setEnabled(false);
            Immunostaining_toolbox.vf.setEnabled(false);
        }
        int n = list.size();
        double[][] data = new double[n][];
        for (int i = 0; i < n; i++) {
            StringTokenizer st = (StringTokenizer) list.elementAt(i);
            int nd = st.countTokens();
            data[i] = new double[nd];
            for (int j = 0; j < nd; j++) {
                String part = st.nextToken();
                try {
                    Double.valueOf(part).doubleValue();
                    data[i][j] = Double.valueOf(part).doubleValue();
                } catch (NumberFormatException e) {
                }
            }
        }
        return data;
    }
}

/**
 *
 * using semi-automated tool as color chooser
 * collect the existed color pixles by press collect button and automatically
 * generate statistical model and cumulate the histograms.
 * press Save to copy data to txt file.
 * @param imp1 --> original image
 * @param imp2 --> result image filted by color chooser
 * @param ixelR, pixelG, pixelB, histMean --> mean color RGB in selected region
 *
 */
class DataCollection extends PlugInFrame implements ChangeListener, ItemListener, FocusListener, WindowListener, ActionListener, ClipboardOwner {

    private ImagePlus imp1,  imp2;
    private int pixelR,  pixelG,  pixelB;
    private double[] histMean = new double[3];
    //color chooser
    private JColorChooser tcc = new JColorChooser(Toolbar.getForegroundColor());
    Locale local = new Locale("ENGLISH");
    ColorChooserSet mean;
    //static button action;
    static boolean press = true;
    private Button b1,  b2;

    public DataCollection(ImagePlus imp) {
        super("Color Chooser");
        tcc.setLocation(Immunostaining_toolbox.point);
        tcc.setLocale(local);
        tcc.setDefaultLocale(local);
        tcc.setPreviewPanel(new JPanel());
        imp1 = imp;
        filter();
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    public void filter() {
        if (imp1 == null) {
            IJ.noImage();
            return;
        } else {
            imp2 = IJ.createImage("Colour Filter", "8-bit RGB", imp1.getWidth(), imp1.getHeight(), 1);
        }
        if (!isSelection()) {
            IJ.error("Advance setting", "Rectangular selection on original image required");
            return;
        }

        //---draw the filtered out result image--
        imp2.show();
        getRGBhis();
        getProcess(pixelR, pixelG, pixelB);//get a process
        press = true;
        setLayout(new BorderLayout());

        Panel panelButton = new Panel(new FlowLayout());
        Panel panelColor = new Panel(new FlowLayout());

        AbstractColorChooserPanel panels[] = {new ColorScalePanel()};
        tcc.setChooserPanels(panels);
        tcc.getSelectionModel().addChangeListener(this);
        panelColor.add(tcc);
        b1 = new Button("Collection");
        b2 = new Button("Save_Model");
        b1.addActionListener(this);
        b2.addActionListener(this);
        panelButton.add(b1);
        panelButton.add(b2);
        add(panelButton, BorderLayout.NORTH);
        add(panelColor, BorderLayout.SOUTH);
        pack();
        WindowManager.addWindow(this);
        GUI.center(this);
        setVisible(true);
    }

    /**GrayScalePanel.java
     *A simple implementation of the AbstractColorChooserPanel class.
     *This class provides a slider and a textfield for picking out a shade
     * of gray. The colour space used here is RGB where R channel
     * value is selected as the value of sliding bar.
     */
    class ColorScalePanel extends AbstractColorChooserPanel implements
            ChangeListener {

        JSlider scalecb;
        JTextField cb;
        JSpinner jscb;
        JSpinner jscr;
        JSpinner.NumberEditor numcb;
        JSpinner.NumberEditor numcr;
        int G = pixelG;
        int B = pixelB;

        public ColorScalePanel() {
            setLayout(new GridLayout(0, 1));
            // create the slider and attached with a listener
            scalecb = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
            scalecb.addChangeListener(this);

            SpinnerNumberModel modelcb = new SpinnerNumberModel(scalecb.getValue(), 0, 255, 1);
            add(new JLabel("Choose Threshold:", JLabel.CENTER));
            JPanel jp = new JPanel();
            jp.add(new JLabel("0"));
            jp.add(scalecb);
            jp.add(new JLabel("255"));
            add(jp);

            JPanel jp2 = new JPanel();
            jscb = new JSpinner(modelcb);
            numcb = new JSpinner.NumberEditor(jscb);
            jscb.addChangeListener(this);
            jp2.add(jscb);
            add(jp2);
        }

        protected void buildChooser() {
        }

        // Make sure the slider is in sync with the colour selected.
        public void updateChooser() {
            Color c = getColorSelectionModel().getSelectedColor();
            scalecb.setValue(toYCbCr(c, 0));
        }

        protected int toYCbCr(Color c, int i) {
            int r = c.getRed();
            //int g = c.getGreen();
            //int b = c.getBlue();
            return r;
        }

        // Pick a name for our tab in the chooser
        public String getDisplayName() {
            return "YCbCr Scale";
        }

        // No need for an icon.
        public Icon getSmallDisplayIcon() {
            return null;
        }

        public Icon getLargeDisplayIcon() {
            return null;
        }

// And lastly, update the selection model as our slider changes.
        public void stateChanged(ChangeEvent ce) {
            int val;
            if (ce.getSource() == jscb) {
                scalecb.setValue(Integer.parseInt(jscb.getValue().toString()));
            }
            if (ce.getSource() == scalecb) {
                jscb.setValue(new Integer(scalecb.getValue()));
            }
            val = scalecb.getValue();
            Color color = new Color(val, pixelG, pixelB);
            getColorSelectionModel().setSelectedColor(color);
        }
    }

    public void itemStateChanged(ItemEvent e) {
        updateColor();
    }

    public void stateChanged(ChangeEvent e) {
        Color newColor = tcc.getColor();
        IJ.setForegroundColor(newColor.getRed(), newColor.getGreen(), newColor.getBlue());
        pixelR = newColor.getRed();
        pixelG = newColor.getGreen();
        pixelB = newColor.getBlue();
        getProcess(pixelR, pixelG, pixelB);
    }

    public void actionPerformed(ActionEvent e) {

        if (imp1 == null) {
            IJ.beep();
            IJ.showStatus("No image");
            return;
        }
        String label = e.getActionCommand();
        if (label == null) {
            return;
        }
        if (label.equals("Collection")) {
            Collection(imp1, imp2);
            b1.setEnabled(false);
            Immunostaining_toolbox.color.setEnabled(true);
            Immunostaining_toolbox.nuclei.setEnabled(true);
            Immunostaining_toolbox.gland.setEnabled(true);
        }

        if (label.equals("Save_Model")) {
            Save_Model(imp1, imp2);
        }

    }

    /** Collect pixels from result image
     * visable pixel collection
     * background pixel collection
     */
    void Collection(ImagePlus imp1, ImagePlus imp2) {
        BrownColorStats stats;
        if (imp2 == null) {
            IJ.noImage();
            return;
        }
        ColorProcessor cp2 = (ColorProcessor) imp2.getProcessor();
        ColorProcessor cp1 = (ColorProcessor) imp1.getProcessor();
        stats = new BrownColorStats(cp2, cp1);
        double[] bhist = stats.getHistogram();
        double[] nbhist = stats.getHistogram2();

        for (int i = 0; i < 16384; i++) {
            Immunostaining_toolbox.brownhist[i] = Immunostaining_toolbox.brownhist[i] + bhist[i];
            Immunostaining_toolbox.nonbrownhist[i] = Immunostaining_toolbox.nonbrownhist[i] + nbhist[i];
            Immunostaining_toolbox.totalb = Immunostaining_toolbox.totalb + bhist[i];
            Immunostaining_toolbox.totalnb = Immunostaining_toolbox.totalnb + nbhist[i];
        }

        for (int i = 0; i < 16384; i++) {
            Immunostaining_toolbox.prob[i] = Immunostaining_toolbox.brownhist[i] / Immunostaining_toolbox.totalb;
            Immunostaining_toolbox.pronb[i] = Immunostaining_toolbox.nonbrownhist[i] / Immunostaining_toolbox.totalnb;
        }

    }

    /**
     * Save statistical color model as ".txt" file based on collected pixels
     * @param e
     */
    void Save_Model(ImagePlus imp1, ImagePlus imp2) {
        Clipboard systemClipboard = null;
        double[] probability_brown = Immunostaining_toolbox.prob;
        double[] probability_nonbrown = Immunostaining_toolbox.pronb;
        String fname = "StatisticalModel.txt";
        try {
            systemClipboard = getToolkit().getSystemClipboard();
        } catch (Exception e) {
            systemClipboard = null;
        }
        if (systemClipboard == null) {
            IJ.error("Unable to save to File.");
            return;
        }
        IJ.showStatus("Saving histogram values...");
        File filehist = null;
        Frame frame = new Frame();
        FileDialog fdhist = new FileDialog(frame, "save histogram", FileDialog.SAVE);
        fdhist.setVisible(true);
        String path = fdhist.getDirectory();
        String filename = fdhist.getFile();


        File filemodel = new File(path + filename);

        CharArrayWriter aw = new CharArrayWriter(256 * 4);
        try {
            if (!filemodel.exists()) {
                filemodel.createNewFile();

            }

            FileWriter writer = new FileWriter(filemodel);

            PrintWriter pw = new PrintWriter(writer);
            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 128; j++) {
                    pw.print(i + "\t" + j + "\t" + probability_brown[i * 128 + j] + "\t" + probability_nonbrown[i * 128 + j] + "\n");
                }
            }
            String text = aw.toString();
            pw.close();
            StringSelection contents = new StringSelection(text);
            systemClipboard.setContents(contents, this);
            IJ.showStatus(text.length() + " characters saved to File");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void windowActivated(WindowEvent e) {
        super.windowActivated(e);
        updateColor();
    }

    void updateColor() {
        tcc.setColor(pixelR, pixelG, pixelB);
        tcc.setColor(Toolbar.getForegroundColor());
    }

    /**
     * detect browns by scrolling color chooser bar
     * filtered by CbCr color space
     * varible variance can be adjusted to fit the color range
     *
     */
// processing while color chooser bar chaning
    void getProcess(int r, int g, int b) {

        ImageProcessor ip = imp2.getProcessor();
        int red, green, blue;
        double cb = (0.5 * b - 0.169 * r - 0.331 * g + 128);
        double cr = (0.5 * r - 0.419 * g - 0.081 * b + 128);

        ColorProcessor color = (ColorProcessor) imp1.getProcessor();
        for (int x = 0; x < ip.getWidth(); x++) {
            for (int y = 0; y < ip.getHeight(); y++) {
                red = (int) (color.get(x, y) & 0xff0000) >> 16;
                green = (int) (color.get(x, y) & 0x00ff00) >> 8;
                blue = (int) (color.get(x, y) & 0x0000ff);
                double Cb = (0.5 * blue - 0.169 * red - 0.331 * green + 128);
                double Cr = (0.5 * red - 0.419 * green - 0.081 * blue + 128);
                double Cbv = (Cb - cb) * (Cb - cb);
                double Crv = (Cr - cr) * (Cr - cr);
                double variance = Math.sqrt(Cbv + Crv);
                if (variance >= 30) {//the radius of Eculidean distance
                    red = green = blue = 255;
                }
                int pix = (((int) red & 0xff) << 16) + (((int) green & 0xff) << 8) + ((int) blue & 0xff);
                ip.set(x, y, pix);
            }
        }
        imp2.updateAndDraw();
        IJ.wait(5);
    }

//get mean value of RGB from selected ROI
    void getRGBhis() {
        mean = new ColorChooserSet(imp1);
        histMean = mean.getMean();
        pixelR = (int) (histMean[0]);
        pixelG = (int) (histMean[1]);
        pixelB = (int) (histMean[2]);
    }

    //ROI rectangular selection
    boolean isSelection() {
        if (imp1 == null) {
            return false;
        }
        Roi roi = imp1.getRoi();
        if (roi == null) {
            return false;
        }
        return roi.getType() == Roi.RECTANGLE;
    }
}

//comput the mean value from selection and set the initial bar position
class ColorChooserSet implements Measurements {

    //protected ImageStatistics stats;
    protected int[][] histogram;
    protected Rectangle[] frame = new Rectangle[3];
    protected int plotScale = 1;
    protected int nBins = 256;
    private int width,  height = 1;
    protected getColor colormean;

    public ColorChooserSet(ImagePlus imp) {

        getMeanColor(imp, nBins, 0.0, 0.0);
    }

    public void getMeanColor(ImagePlus imp, int bins, double histMin, double histMax) {

        Rectangle rect;
        try {
            rect = imp.getRoi().getBounds();
        } catch (NullPointerException e) {
            rect = new Rectangle(0, 0, width, height);
        }
        ColorProcessor cp = (ColorProcessor) imp.getProcessor();
        colormean = new getColor(cp, rect);
    }

    public double[] getMean() {
        return colormean.getMean();
    }
}

//get mean value from color histogram
class getColor {

    int[][] histogram = new int[3][256];
    double[] hmean = new double[3];
    int histcount = 0;

    public getColor(ColorProcessor cp, Rectangle rect) {
        int[] pixels = (int[]) cp.getPixels();
        int width = cp.getWidth();
        histogram = getHistogram(width, pixels, rect);
        calculateStatistics(histogram);
    }

    public int[][] getHistogram(int width, int[] pixels, Rectangle roi) {

        int c, r, g, b;//, v;
        int roiY = roi.y;
        int roiX = roi.x;
        int roiWidth = roi.width;
        int roiHeight = roi.height;
        histcount = roi.width * roi.height;
        int[][] histogram = new int[3][256];
        for (int y = roiY; y < (roiY + roiHeight); y++) {
            int i = y * width + roiX;
            for (int x = 0; x < roiWidth; x++) {
                c = pixels[i];
                r = (c & 0xff0000) >> 16;
                g = (c & 0xff00) >> 8;
                b = c & 0xff;
                histogram[0][r]++;
                histogram[1][g]++;
                histogram[2][b]++;
                i++;
            }
        }
        return histogram;
    }
    /** Value of pixels included in masks. */
    public static final int WHITE = 0xFF;

    public double[] getMean() {
        return hmean;
    }

    protected void calculateStatistics(int[][] histogram) {
        for (int col = 0; col < 3; col++) {
            double sum = 0;
            for (int i = 0; i < 256; i++) {
                sum += i * histogram[col][i];
            }
            hmean[col] = sum / histcount;
        }

    }
}

/**
 *
 * compute color histograms with specified color space
 */
class BrownColorStats {

    public double[] histogram1 = new double[16384];
    public double[] histogram2 = new double[16384];
    public int[] darkY = new int[256];
    public int[] lightY = new int[256];
    int histcount = 0;
    int histcount2 = 0;
    int m, n, db, ndb, overlap;

    public BrownColorStats(ColorProcessor cp, ColorProcessor cp2) {
        int[] pixels = (int[]) cp.getPixels();
        int[] pixels2 = (int[]) cp2.getPixels();
        int width = cp.getWidth();
        int height = cp.getHeight();

        getColorhist(width, height, pixels);
        getNBColorhist(width, height, pixels, pixels2);
    }

    public void getColorhist(int width, int height, int[] pixels) {

        int c;
        double R, G, B;
        histcount = width * height;
        int v = 0;

        int pix = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (255 & 0xff);
        for (int k = 0; k < histcount; k++) {
            c = pixels[k];

            if (c != pix) {
                R = (c & 0xff0000) >> 16;
                G = (c & 0xff00) >> 8;
                B = c & 0xff;
                n = 0;
                double cb = (0.5 * B - 0.169 * R - 0.331 * G + 128);
                double cr = (0.5 * R - 0.419 * G - 0.081 * B + 128);


                int i = (int) (cb / 2);
                int j = (int) (cr / 2);
                n = i * 128 + j;
                histogram1[n] = histogram1[n] + 1;
                v++;
                int Y = (int) (0.299 * R + 0.587 * G + 0.114 * B);
                darkY[Y]++;

            }
        }
        histcount = v;

    }

    public void getNBColorhist(int width, int height, int[] pixels, int[] pixels2) {

        int c, c1;
        double R, G, B;
        histcount2 = width * height;
        int v = 0;

        int pix = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (255 & 0xff);
        for (int k = 0; k < histcount2; k++) {
            c = pixels[k];
            c1 = pixels2[k];
            if (c == pix) {
                R = (c1 & 0xff0000) >> 16;
                G = (c1 & 0xff00) >> 8;
                B = c1 & 0xff;
                n = 0;
                double cb = (0.5 * B - 0.169 * R - 0.331 * G + 128);
                double cr = (0.5 * R - 0.419 * G - 0.081 * B + 128);


                int i = (int) (cb / 2);
                int j = (int) (cr / 2);
                n = i * 128 + j;

                histogram2[n] = histogram2[n] + 1;

                v++;
            }
        }
        histcount2 = v;

    }

    public int[] getDarky() {
        return darkY;
    }

    public int getHistcount() {
        return histcount;
    }

    public int getHistcount2() {
        return histcount2;
    }

    public double[] getHistogram() {
        return histogram1;
    }

    public double[] getHistogram2() {
        return histogram2;
    }

    public int getMinMax(int[] a) {
        int min = a[0];
        int max = a[0];
        int value;
        int Smax = a[0];
        for (int i = 0; i < a.length; i++) {
            value = a[i];
            if (value <= min) {
                min = value;
            }
            if (value >= max) {
                max = value;
            }
        }
        for (int i = 0; i < a.length; i++) {
            value = a[i];
            if (value != max) {
                if (value >= Smax) {
                    Smax = value;
                }
            }
        }
        return max;
    }
}

/**
 *
 * Analyse opened image with existed statistical model
 * detect the desired colors and label them with original color values on the
 * distribution of luminance Y in YCbCr then display them in four image windows.
 * Segmente or quantify nuclei in the image.
 * Detect candidate glands and locate them with bounding boxes.
 */
class ModelFilter extends PlugInFrame implements PlugIn {

    private ImagePlus imp1,  imp2,  imp3,  imp4,  imp5,  imp6,  imp7,  imp9,  imp10,  imp11,  imp12;
    public int pixelR,  pixelG,  pixelB,  ID,  counter;
    public double[] histMean = new double[3];
    public ImageStack stack;
    float arrayX[], arrayY[];
    BufferedImage bi;
    Panel panel;
    JColorChooser tcc = new JColorChooser(Toolbar.getForegroundColor());
    Checkbox cbForeground, cbBackground;
    //static Frame instance,  instance1;
    double[] darkbrownp = new double[16384];
    double[] nondbp = new double[16384];
    int[] size = new int[2000];//loop number
    int[] start = new int[2000];//start index
    int[][] pixelx = new int[2000][30000];
    int[][] pixely = new int[2000][30000];
    int[] psize = new int[2000];
    int[] avg = new int[2000];
    int[] centx = new int[2000];
    int[] centy = new int[2000];
    int separatestep = Integer.valueOf(Immunostaining_toolbox.wz.getText());//20 for 40x (magnitution), 10 for 20x, 5for 10x
    int seedsize = Integer.valueOf(Immunostaining_toolbox.sz.getText());//10for 10x, 50 for 20x, 150 for 40x
    int finalsize = Integer.valueOf(Immunostaining_toolbox.fz.getText());//100 for 40x
    int growcount = 10;//0 for 10x, 10 for 20x-40x

    public ModelFilter(ImagePlus imp, String command) {

        super("Filter");
        imp1 = imp;
        ImageProcessor ip = imp1.getProcessor();
        displayImage(imp1, command);
    }
    //dispaly automatically detected posiitve color pixels on the result image
    public void displayImage(ImagePlus imp1, String command) {
        darkbrownp = Immunostaining_toolbox.prob;
        nondbp = Immunostaining_toolbox.pronb;
        Imagedrawing(imp1, darkbrownp, nondbp, 0, 255, command);
    }

    //create result image
    public void Imagedrawing(ImagePlus imp1, double[] p, double[] np, int low, int sz, String command) {
        if (imp1 == null) {
            IJ.noImage();
            return;
        } else {
            imp5 = IJ.createImage("Dark_Brown Filter", "8-bit RGB", imp1.getWidth(), imp1.getHeight(), 1);

        }
        bi = imp5.getProcessor().getBufferedImage();
        imp2 = new ImagePlus("Stain Color Detection", bi);
        getnextProcess(imp1, p, np, 0, 255, command);
    }

    //doing process to filter positive brown color pixels
    public void getnextProcess(ImagePlus imp1, double[] p, double[] np, int low, int sz, String command) {
        ImageProcessor ip2 = imp2.getProcessor();
        imp4 = IJ.createImage("Negative Brown", "8-bit RGB", imp1.getWidth(), imp1.getHeight(), 1);
        imp7 = IJ.createImage("imp7", "8-bit RGB", imp1.getWidth(), imp1.getHeight(), 1);
        imp10 = IJ.createImage("imp10", "8-bit RGB", imp1.getWidth(), imp1.getHeight(), 1);
        ImageProcessor ip7 = imp7.getProcessor();
        ImageProcessor ip10 = imp10.getProcessor();

        int n, red, green, blue, pix, pixl, pixn;
        double R, G, B, Prob, Problue;
        counter = 0;
        ColorProcessor cp = (ColorProcessor) imp1.getProcessor();
        for (int x = 0; x < imp1.getWidth(); x++) {
            for (int y = 0; y < imp1.getHeight(); y++) {
                R = (cp.get(x, y) & 0xff0000) >> 16;
                G = (cp.get(x, y) & 0x00ff00) >> 8;
                B = (cp.get(x, y) & 0x0000ff);

                double cb = (0.5 * B - 0.169 * R - 0.331 * G + 128);
                double cr = (0.5 * R - 0.419 * G - 0.081 * B + 128);


                int i = (int) (cb / 2);
                int j = (int) (cr / 2);
                n = i * 128 + j;
                int Y = Math.round((int) (0.299 * R + 0.587 * G + 0.114 * B));
                Prob = p[n] / np[n];
                Problue = Immunostaining_toolbox.problue[n] / Immunostaining_toolbox.pronblue[n];
                //positive color
                red = green = blue = 255;
                pix = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (255 & 0xff);
                pixn = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (255 & 0xff);
                pixl = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (255 & 0xff);
    //------------positive color detection------------
                pix = (((int) R & 0xff) << 16) + (((int) G & 0xff) << 8) + ((int) B & 0xff);
                if (p[n] != 0 && np[n] == 0) {

                    ip7.set(x, y, pix);
                    if (Y >= 0 && Y <= 255) {
                        counter++;
                        ip2.set(x, y, pix);
                        ip7.set(x, y, pix);
                    }
                } else if (Prob >= 0.7) {
                    ip7.set(x, y, pix);
                    if (Y >= 0 && Y <= 255) {
                        counter++;
                        ip2.set(x, y, pix);
                        ip7.set(x, y, pix);
                    }
                } else {
                    pix = (((int) R & 0xff) << 16) + (((int) G & 0xff) << 8) + ((int) B & 0xff);
                }
            }
        }

        if (command.equals("color")) {
            imp2.show();
        }
        if (command.equals("gland")) {
            gland(imp1);
        }
        if (command.equals("nuclei")) {
            ImageProcessor ip1 = imp1.getProcessor();
            ImageProcessor ip11 = Immunostaining_toolbox.Rimage.getProcessor();
            for (int x = 0; x < imp1.getWidth(); x++) {
                for (int y = 0; y < imp1.getHeight(); y++) {
                    ip11.set(x, y, ip1.get(x, y));

                }
            }
            Immunostaining_toolbox.Rimage.updateAndDraw();
            imp4.updateAndDraw();
            IJ.wait(10);
            //imp4.show();
            imp2.updateAndDraw();
            //imp2.show();
            imp7.updateAndDraw();
            //imp7.show();
//-------------------------------------------
            imp5 = imp1.duplicate();
            Separate(imp5);
            //imp5.show();
//--------------------------------------------
            IJ.wait(10);
            NucleiSegmentation();
            imp4.changes = false;
            imp7.changes = false;
            imp9.changes = false;
            imp10.changes = false;
        }
    }
   

//---------separate clustered nuclei----------
    void Separate(ImagePlus imp) {
        int s = separatestep;
        ImagePlus imp8 = IJ.createImage("imp8", "8-bit", s * 2, s * 2, 1);
        ImageProcessor ip8 = imp8.getProcessor();
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();

        AutoThresholder at = new AutoThresholder();
        //global threshold
        int level = at.getThreshold(AutoThresholder.Method.valueOf("Default"), histogram(imp, 0, 0, imp.getWidth(), imp.getHeight()));
        ByteProcessor bp = (ByteProcessor) imp.getProcessor();
        bp.smooth();

        int avg = 0;
        int ts = 2;
        ImagePlus dup = IJ.createImage("Enlarged Image", "8-bit", imp.getWidth() + s * ts, imp.getHeight() + s * ts, 1);
        ImageProcessor dupip = dup.getProcessor();
        //local threshold: using a samll window to filter out noise pixels
        for (int x = ts / 2 * s; x < dup.getWidth() - ts / 2 * s; x++) {
            for (int y = ts / 2 * s; y < dup.getHeight() - ts / 2 * s; y++) {
                dupip.set(x, y, bp.get(x - ts / 2 * s, y - ts / 2 * s));
            }
        }
        ImagePlus dupdup = dup.duplicate();
        ImageProcessor dupdupip = dupdup.getProcessor();
        //move the small window pixel by pixel
        //to improve the processing speed, you can move the small window
        //with different paces
        int dis = s;
        for (int x = 0; x < dup.getWidth(); x++) {
            for (int y = 0; y < dup.getHeight(); y++) {
                //for (int x = 1; x < dup.getWidth()-1; x ++) {
                //for (int y = 1; y < dup.getHeight()-1 ; y ++) {
                int[] hist = new int[256];
                if (dupip.get(x, y) < level) {//locate the center of small box at
                                              // the pixels with intensity
                                              //lower than the local threshold
                    int sum = 0;
                    //calculate intensity histogram for the pixels at small window
                    for (int i = x - s; i < x + s; i++) {
                        for (int j = y - s; j < y + s; j++) {
                            if (dupip.get(i, j) < 255) {
                                hist[dupip.get(i, j)]++;
                            }
                        }
                    }
                    int l = at.getThreshold(AutoThresholder.Method.valueOf("Default"), hist);
                    for (int i = x - s; i < x + s; i++) {
                        for (int j = y - s; j < y + s; j++) {
                            if (dupdupip.get(i, j) >= l && dupdupip.get(i, j) < level) {
                                //set pixels with intensity larger than local threshold
                                // and smaller than global threshold to be background (255)
                                dupdupip.set(i, j, 255);
                            }
                        }
                    }
                }
            }
        }
        for (int x = 0; x < imp.getWidth(); x++) {
            for (int y = 0; y < imp.getHeight(); y++) {
                bp.set(x, y, dupdupip.get(x + ts / 2 * s, y + ts / 2 * s));
            }
        }
        dup.close();
        dupdup.close();


    }

//-----draw nuclei on original image
    void NucleiSegmentation() {
        float[] X;
        float[] Y;
        //imp5 is the original image after applying local threshold
        //--median filter to reduce noise---

        ImageConverter ic5 = new ImageConverter(imp5);
        ic5.convertToGray8();
        ImageProcessor ip5 = imp5.getProcessor();
		//remove regions outside of TMA core
		for (int x = 0; x < imp5.getWidth(); x++) {
            for (int y = 0; y < imp5.getHeight(); y++) {
					if(ip5.get(x,y)>227){
						ip5.set(x,y,255);
					}
			}
		}
		ip5=imp5.getProcessor();
        //Make Binary
        IJ.showStatus("Make Binary...");
        int threshold = ip5.getAutoThreshold();
        ip5.setColor(255);
        for (int x = 0; x < imp1.getWidth(); x++) {
            for (int y = 0; y < imp1.getHeight(); y++) {
                if (ip5.get(x, y) < threshold) 
                {
                    ip5.set(x, y, 0);
                } else {
                    ip5.set(x, y, 255);
                }
            }
        }
        //--obtain initial seed regions--
        IJ.showStatus("Extract Seed Regions...");
        IJ.run(imp5, "Watershed", null);
        int particlecount = MeasureSize(imp5, seedsize, 0);
        ImageProcessor ip2 = imp2.getProcessor();//positive color image
        //segmentation result image
        ImageProcessor ip11 = Immunostaining_toolbox.Rimage.getProcessor();
        ip5 = imp5.getProcessor();
        for (int x = 0; x < imp5.getWidth(); x++) {
            for (int y = 0; y < imp5.getHeight(); y++) {
                if (x == 1 || x == imp5.getWidth() - 1 || y == 1 || y == imp5.getHeight() - 1) {
                    ip5.set(x, y, 0);
                }
            }
        }
        //partical analyzer to filter out noise particals
        Analyzer anal = new Analyzer();
        ResultsTable rtable = anal.getResultsTable();
        //find the center of each partical by finding maximum values' pixel
        MaximumFinder mf = new MaximumFinder();
//---locate seed region with its maximum point--
        mf.findMaxima(ip5, 10, 0, MaximumFinder.LIST, true, false);
        //record the cooridiantes of center points
        X = rtable.getColumn(0);//x=0,y=1
        Y = rtable.getColumn(1);
        particlecount = Analyzer.getCounter();
        if (IJ.isResultsWindow()) {
            IJ.selectWindow("Results");
            IJ.run("Close");
        }
        //set the center points with color value 0, black point
        for (int i = 0; i < particlecount; i++) {
            ip5.set((int) X[i], (int) Y[i], 0);
        }

        ImagePlus impdup = imp1.duplicate();
        ImageConverter ic = new ImageConverter(impdup);
        ic.convertToGray8();
        IJ.run(impdup, "Smooth", null);
//--record seed region with its pixles in an array for each partical---
        for (int i = 0; i < particlecount; i++) {
            SeedRegion(imp5, impdup, imp1, (int) X[i], (int) Y[i], i);
        }
//--region growing seed region with edge map and watershed
        int stop = 0;
        int lc = 1;
        //initial seed region array for region growing
        int[] sig = new int[2000];
        for (int i = 0; i < 2000; i++) {
            sig[i] = 1;
        }

//---Make Binary-----
        ImageProcessor ipdp = impdup.getProcessor();
		//remove regions outside of TMA core
		for (int x = 0; x < imp5.getWidth(); x++) {
            for (int y = 0; y < imp5.getHeight(); y++) {
					if(ipdp.get(x,y)>227){
						ipdp.set(x,y,255);
					}
			}
		}
        threshold = ipdp.getAutoThreshold();
        for (int x = 0; x < imp1.getWidth(); x++) {
            for (int y = 0; y < imp1.getHeight(); y++) {
                ip5.set(x, y, 255);
                if (ipdp.get(x, y) <= threshold) {
                    ipdp.set(x, y, 0);
                } else {
                    ipdp.set(x, y, 255);
                }
            }
        }
        IJ.showStatus("Region Growing...");

        do {

            stop = 0;
            //region grow each partical from recorded seed region
            for (int i = 0; i < particlecount; i++) {
                if (sig[i] != 0) {
                    sig[i] = Grow(impdup, imp1, imp5, i, particlecount);
                }
            }
            for (int i = 0; i < particlecount; i++) {
                stop += sig[i];
            }
            lc++;
            IJ.showStatus("Iteration  " + lc);
        } while (stop != 0);

        impdup.changes = false;
        impdup.close();
        ip5 = imp5.getProcessor();
        for (int x = 0; x < imp1.getWidth(); x++) {
            for (int y = 0; y < imp1.getHeight(); y++) {
                if (ip5.get(x, y) == 100) {
                    ip5.set(x, y, 255);
                }
            }
        }
        for (int x = 0; x < imp1.getWidth(); x++) {
            for (int y = 0; y < 2; y++) {
                ip5.set(x, y, 255);
            }
            for (int y = imp1.getHeight() - 2; y < imp1.getHeight(); y++) {
                ip5.set(x, y, 255);
            }
        }
        for (int y = 0; y < imp1.getHeight(); y++) {
            for (int x = 0; x < 2; x++) {
                ip5.set(x, y, 255);
            }
            for (int x = imp1.getWidth() - 2; x < imp1.getWidth(); x++) {
                ip5.set(x, y, 255);
            }
        }

        ip5.invert();
        IJ.run(imp5, "Watershed", null);
        IJ.run(imp5, "Open", null);
        ip5 = imp5.getProcessor();
//----check pixel contained in each particle-----
        IJ.showStatus("Noise Pixels Elimination...");
        imp9 = IJ.createImage("imp9", "8-bit", imp1.getWidth(), imp1.getHeight(), 1);
        imp9.changes = false;
        ImageProcessor ip9 = imp9.getProcessor();
        mf.findMaxima(ip5, 10, 0, MaximumFinder.LIST, true, false);
        X = rtable.getColumn(0);//x=0,y=1
        Y = rtable.getColumn(1);
        particlecount = Analyzer.getCounter();
        if (IJ.isResultsWindow()) {
            IJ.selectWindow("Results");
            IJ.run("Close");
        }
        counter = 0;
        pixelx = new int[2000][10000];
        pixely = new int[2000][10000];

        if (Immunostaining_toolbox.Ttick == false) {
            //check the segmented nuclei are postive or negative and
            //count the number of positive segmetned nuclei
            for (int i = 0; i < particlecount; i++) {
                PositiveSelection(imp5, (int) X[i], (int) Y[i], i, false);
            }
            /*IJ.run(imp5, "Find Edges", null);
           ip5 = imp5.getProcessor();
            int pix;
            for (int x = 0; x < imp1.getWidth(); x++) {
                for (int y = 0; y < imp1.getHeight(); y++) {
                    if (ip5.get(x, y) == 255) {
                        pix = ((255 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);
                        ip11.set(x, y, pix);
                    }
                }
            }*/
			/*IJ.run(imp9, "Erode", null);
			ic = new ImageConverter(imp4);
        	ic.convertToGray8();
			IJ.run(imp4, "Erode", null);*/
			ip9 = imp9.getProcessor();
            int pix;
            for (int x = 0; x < imp1.getWidth(); x++) {
                for (int y = 0; y < imp1.getHeight(); y++) {
                    if (ip9.get(x, y) == 0) {
                        pix = ((255 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);
                        ip11.set(x, y, pix);
                    }
                }
            }
			
			ImageProcessor ip4 = imp4.getProcessor();
            for (int x = 0; x < imp1.getWidth(); x++) {
                for (int y = 0; y < imp1.getHeight(); y++) {
                    if (ip4.get(x, y) == 0) {
                        pix = ((0 & 0xff) << 16) + ((255 & 0xff) << 8) + (0 & 0xff);
                        ip11.set(x, y, pix);
                    }
                }
            }

        }
        if (Immunostaining_toolbox.Ttick == true) {
            //ImagePlus impdp=imp5.duplicate();
            for (int i = 0; i < particlecount; i++) {
                PositiveSelection(imp5, (int) X[i], (int) Y[i], i, true);
            }

//----draw positive nuclei-----
           IJ.showStatus("Ellipse Approximation...");
            ImageProcessor ip4 = imp4.getProcessor();
            ip9 = imp9.getProcessor();
            int pix;
            for (int x = 0; x < imp1.getWidth(); x++) {
                for (int y = 0; y < imp1.getHeight(); y++) {
                    if (ip9.get(x, y) == 0) {
                        pix = ((255 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);
                        ip11.set(x, y, pix);
                    }
                }
            }
            IJ.showMessage("", "Positive Nuclei= " + counter);
        }
        imp5.changes = false;
        Immunostaining_toolbox.Rimage.updateAndDraw();
        Immunostaining_toolbox.nucleino[Immunostaining_toolbox.ima] = counter;
		
    }

//Obtain seed region of each particle. Grow the center points with 8 neighbour
//pixels. Record the average intensity values of each seed region.
    void SeedRegion(ImagePlus imp, ImagePlus imp1, ImagePlus imp2, int x, int y, int index) {
        int[] eightx = {1, 0, -1, 0, 1, 1, -1, -1};
        int[] eighty = {0, 1, 0, -1, 1, -1, 1, -1};
        int[] whitex = new int[30000];
        int[] whitey = new int[30000];
        whitex[0] = x;
        whitey[0] = y;
        int m = 0;
        int s = 1;

        ImageProcessor ip = imp.getProcessor();
        ImageProcessor ip1 = imp1.getProcessor();
        avg[index] += ip1.get(x, y);
        int pix = ((0 & 0xff) << 16) + ((255 & 0xff) << 8) + (0 & 0xff);

        do {
            for (int j = 0; j < 8; j++) {
                if (ip.get(whitex[m] + eightx[j], whitey[m] + eighty[j]) == 255) {
                    boolean exist = false;
                    for (int k = 0; k < s; k++) {
                        if (whitex[k] == whitex[m] + eightx[j] && whitey[k] == whitey[m] + eighty[j]) {
                            exist = true;
                        }
                    }
                    if (!exist && s <= 100) {
                        whitex[s] = whitex[m] + eightx[j];
                        whitey[s] = whitey[m] + eighty[j];
                        avg[index] += ip1.get(whitex[s], whitey[s]);
                        ip.set(x, y, 0);
                        s++;
                    }

                }
            }
            m++;

        } while (s <= 100 && m != s);
        pixelx[index] = whitex;
        pixely[index] = whitey;
        start[index] = 0;
        size[index] = m;
        psize[index] = m;
        avg[index] = (int) (avg[index] / m);
    }


//--Grow seed region and draw split line---------
    int Grow(ImagePlus imp, ImagePlus imp1, ImagePlus imp2, int label, int time) {
        int count = 0;
        int s = start[label] + size[label];
        int[] eightx = {1, 0, -1, 0, 1, 1, -1, -1};
        int[] eighty = {0, 1, 0, -1, 1, -1, 1, -1};

        ImageProcessor ip = imp.getProcessor();//binary image for growing
        ImageProcessor ip1 = imp1.getProcessor();//original input image
        ImageProcessor ip2 = imp2.getProcessor();//imp2 is the positive color image

        int sum = 0;
        for (int i = start[label]; i < (start[label] + size[label]); i++) {//3.1
            for (int j = 0; j < 8; j++) {//3.2

                if ((pixelx[label][i] + eightx[j]) > 0 && (pixelx[label][i] + eightx[j]) < imp.getWidth() && (pixely[label][i] + eighty[j]) > 0 && (pixely[label][i] + eighty[j]) < imp.getHeight()) {//3.3
                   
                    if (ip.get(pixelx[label][i] + eightx[j], pixely[label][i] + eighty[j]) == 0) {//3.4
                        if (ip2.get(pixelx[label][i] + eightx[j], pixely[label][i] + eighty[j]) == 0) {//3.5
                            boolean exist = false;
                            for (int m = 0; m < s; m++) {
                                if (pixelx[label][m] == (pixelx[label][i] + eightx[j]) && pixely[label][m] == (pixely[label][i] + eighty[j])) //label1=n;
                                {
                                    exist = true;
                                }
                            }
                            //find the pixels in the growing process which have
                            //intensity values larger than the average values of grown regions
                            if (!exist && ip1.get(pixelx[label][i] + eightx[j], pixely[label][i] + eighty[j]) > avg[label])
                            {
                                ip2.set(pixelx[label][i] + eightx[j], pixely[label][i] + eighty[j], 100);
                            }
                        }//3.5
                        else if (ip2.get(pixelx[label][i] + eightx[j], pixely[label][i] + eighty[j]) == 255) {
                            ip2.set(pixelx[label][i] + eightx[j], pixely[label][i] + eighty[j], 0);
                            count++;//record the number of valid grown loops

                            pixelx[label][s] = pixelx[label][i] + eightx[j];
                            pixely[label][s] = pixely[label][i] + eighty[j];
                            sum += ip1.get(pixelx[label][s], pixely[label][s]);
                            s++;
                        }

                    }//3.4

                }//3.3
            }//3.2


        }//3.1
        if (count > growcount) {
            start[label] = start[label] + size[label];
            size[label] = count;
            psize[label] += count;
            avg[label] = (int) ((avg[label] + sum / count) / 2);
            return 1;
        } else {
            return 0;
        }
    }

//--compute the proportion of darkbrown pixels in each nuclus-----
    void PositiveSelection(ImagePlus imp, int x, int y, int index, boolean contour) {
        ImageConverter ic = new ImageConverter(imp2);
        ic.convertToGray8();
        imp2.updateAndDraw();
        int[] whitex = new int[100000];
        int[] whitey = new int[100000];
        int[] wx = new int[100000];
        int[] wy = new int[100000];
        int[] hist = new int[256];
        int size = 1;
        int m = 0;
        int cntx = 0;
        int cnty = 0;
        double darkpixel = 0;
        int[] eightx = {1, 0, -1, 0, 1, 1, -1, -1};
        int[] eighty = {0, 1, 0, -1, 1, -1, 1, -1};

        ImageProcessor ip = imp.getProcessor();
        ImageProcessor ip1 = imp1.getProcessor();
        ImageProcessor ip2 = imp2.getProcessor();
        ImageProcessor ip4 = imp4.getProcessor();
        ImageProcessor ip9 = imp9.getProcessor();
        whitex[0] = x;
        whitey[0] = y;
        do {
            if (ip2.get(whitex[m], whitey[m]) != 255) {
                hist[ip2.get(whitex[m], whitey[m])]++;
                darkpixel++;
            }
            for (int j = 0; j < 8; j++) {
                if (ip.get(whitex[m] + eightx[j], whitey[m] + eighty[j]) == 255) {
                    boolean exist = false;
                    for (int k = 0; k < size; k++) {
                        if (whitex[k] == whitex[m] + eightx[j] && whitey[k] == whitey[m] + eighty[j]) {
                            exist = true;
                        }
                    }
                    if (!exist) {
                        whitex[size] = whitex[m] + eightx[j];
                        whitey[size] = whitey[m] + eighty[j];
                        cntx += whitex[size];
                        cnty += whitey[size];
                        size++;
                    }

                }
            }
            m++;
        } while (m != size);
        centx[index] = (int) (cntx / m);
        centy[index] = (int) (cnty / m);
        int pix = ((0 & 0xff) << 16) + ((255 & 0xff) << 8) + (0 & 0xff);

        double proportion = darkpixel / (double) size;
        psize[index] = size;
        wx = whitex;
        wy = whitey;

        AutoThresholder at = new AutoThresholder();
        int l = at.getThreshold(AutoThresholder.Method.valueOf("Default"), hist);
        int objectsize = size;

        /*for (int k = 0; k < size; k++) {
            if (ip2.get(whitex[k], whitey[k]) > l) {
                whitex[k] = 0;
                whitey[k] = 0;
                objectsize--;
            }
        }*/

        pixelx[index] = whitex;
        pixely[index] = whitey;
        if (contour) {
            if (objectsize >= finalsize && proportion > 0.1) {
                drawOval(imp9, index);
                counter++;
            } else {
                drawOval(imp4, index);
            }
        }

        if (!contour) {
            if (objectsize >= finalsize && darkpixel>=5) {
                for (int j = 0; j < psize[index]; j++) {
                    if (whitex[j] != 0 && whitey[j] != 0) {
                        ip9.set(whitex[j], whitey[j], 0);
                    }
                }
            } else if (objectsize >= finalsize && darkpixel<5) {
                for (int j = 0; j < psize[index]; j++) {
                    if (whitex[j] != 0 && whitey[j] != 0) {
                        ip4.set(whitex[j], whitey[j], 0);
                    }
                }
            }
        }

        imp9.updateAndDraw();
    }

    void drawOval(ImagePlus imp, int i) {
        int max = 0;
        int leftx = 5000;
        int lefty = 5000;
        int rightx = 0;
        int righty = 0;
        int w = 0;
        int h = 0;
        ImageProcessor ip = imp.getProcessor();
        ip.setColor(0);
        for (int j = 0; j < psize[i]; j++) {
            if (pixelx[i][j] != 0 && pixely[i][j] != 0) {
                //ip.set(pixelx[i][j],pixely[i][j],0);
                if (pixelx[i][j] < leftx) {
                    leftx = pixelx[i][j];
                }
                if (pixely[i][j] < lefty) {
                    lefty = pixely[i][j];
                }
                if (pixelx[i][j] > rightx) {
                    rightx = pixelx[i][j];
                }
                if (pixely[i][j] > righty) {
                    righty = pixely[i][j];
                }
            }
        }
        w = rightx - leftx;
        h = righty - lefty;
        int r = Math.min(w, h);
        ip.drawOval(leftx, lefty, w, h);

    }

    int MeasureSize(ImagePlus imp, int s, int type) {
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();

        Analyzer ana = new Analyzer();
        ResultsTable rt = ana.getResultsTable();
        rt.reset();

        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.RECORD_STARTS + ParticleAnalyzer.IN_SITU_SHOW + ParticleAnalyzer.SHOW_MASKS, ParticleAnalyzer.AREA, rt, 0, 100000, 0.00, 1.00);
        pa.analyze(imp);
        double area[] = rt.getColumnAsDoubles(0);
        int leth = 0;
        double sum = 0;
        for (int i = 0; i < area.length; i++) {
            if (area[i] >= 30) {
                sum += area[i];
                leth++;
            }
        }
        //calculate the averge size of all particals
        int avge = (int) (sum / leth);
        //Compare the avearge size value with pre-set parameters from main panel.
        //For some images have large smaller size particals, we should set the seedsize value
        //as the smaller value from the comparison.
        if (avge <= seedsize) {
            s = avge;
        }
        //remove particals which have size smaller than the size value to remove noise particles
        pa = new ParticleAnalyzer(16384 + 4096, ParticleAnalyzer.AREA, rt, s, 100000, 0.00, 1.00);
        pa.analyze(imp);

        return Analyzer.getCounter();

// ParticleAnalyzer.SHOW_RESULTS + ParticleAnalyzer.SHOW_OUTLINES+  + ParticleAnalyzer.DISPLAY_SUMMARY,ParticleAnalyzer.SHOW_MASKS
    }


//-----Histogram---
    int[] histogram(ImagePlus imp, int sx, int sy, int ex, int ey) {
        int[] histogram = new int[256];
        ByteProcessor bp = (ByteProcessor) imp.getProcessor();
        for (int x = sx; x < ex; x++) {
            for (int y = sx; y < ey; y++) {
                int Y = bp.get(x, y);
                histogram[Y]++;
            }
        }
        return histogram;

    }

//-------GLAND DETECTION----------------
//imp is binary image after open-by-reconsctruction
//imp1 is edge image of imp
//imp2 is nuclei particles
    void gland(ImagePlus imp) {
        int gaussianR = Integer.valueOf(Immunostaining_toolbox.gb.getText());
        int openR = Integer.valueOf(Immunostaining_toolbox.oc.getText());
        int varianceR = Integer.valueOf(Immunostaining_toolbox.vf.getText());
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();
        ImagePlus imp2 = IJ.createImage("nuclei", "8-bit", imp.getWidth(), imp.getHeight(), 1);
        ImageProcessor ip = imp.getProcessor();
        ImageProcessor ip2 = imp2.getProcessor();
        for (int x = 0; x < imp.getWidth(); x++) {
            for (int y = 0; y < imp.getHeight(); y++) {
                ip2.set(x, y, ip.get(x, y));
            }
        }
        //ImagePlus imp2=imp.duplicate();//nuclei particles
//****potential luminal regions extraction*******
        IJ.run(imp, "Gaussian Blur...", "radius=" + gaussianR);
        ImagePlus imp1 = imp.duplicate();//seed image for openbyrecon
        IJ.run(imp1, "Minimum...", "radius=" + openR);
        GreyscaleRecon gs = new GreyscaleRecon();
        gs.exec(imp, imp1, "GreyscaleReconstruct ", true, true);
        imp1.close();
        IJ.run(imp, "Variance...", "radius=" + varianceR);
        ip = imp.getProcessor();
        for (int x = 0; x < imp.getWidth(); x++) {
            for (int y = 0; y < imp.getHeight(); y++) {
                if (ip.get(x, y) > 1) {
                    ip.set(x, y, 255);
                }
            }
        }
//****core edge discription*********************
        IJ.run(imp, "Make Binary", null);
        IJ.run(imp, "Fill Holes", null);
//****nuclei partical extraction****************
        ContrastEnhancer ce = new ContrastEnhancer();
        ce.equalize(imp2);
        Lapla la = new Lapla();
        ip2 = imp2.getProcessor();
        la.filtering(ip2, imp2);
        ImagePlus temp = WindowManager.getImage("FFT of " + imp.getTitle());
        temp.close();
        //Make Binary of nuclei image
        ip2 = imp2.getProcessor();
        int threshold = ip2.getAutoThreshold();
        for (int x = 0; x < imp2.getWidth(); x++) {
            for (int y = 0; y < imp2.getHeight(); y++) {
                if (ip2.get(x, y) <= threshold) {
                    ip2.set(x, y, 255);
                } else {
                    ip2.set(x, y, 0);
                }
            }
        }
        IJ.run(imp2, "Make Binary", null);
        IJ.run(imp2, "Watershed", null);
        imp.changes = false;
        imp1.changes = false;
        imp2.changes = false;
        nucleiGroup ng = new nucleiGroup();
        ng.exec(imp, imp2);

    }
}