import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import javax.imageio.ImageIO;

public class ProcesadorImagenes extends JFrame {

    // ── Paleta ────────────────────────────────────────────────────────────────
    private static final Color C_BG       = new Color(0x1a1d23);
    private static final Color C_PANEL    = new Color(0x22262f);
    private static final Color C_SURFACE  = new Color(0x2a2f3a);
    private static final Color C_BORDER   = new Color(0x353b49);
    private static final Color C_ACCENT   = new Color(0x7c4dff);  // violeta
    private static final Color C_ACCENT2  = new Color(0x651fff);
    private static final Color C_TEXT     = new Color(0xe0e6f0);
    private static final Color C_TEXT_DIM = new Color(0x7a8499);
    private static final Color C_SUCCESS  = new Color(0x69f0ae);
    private static final Color C_RED      = new Color(0xef5350);
    private static final Color C_GREEN    = new Color(0x66bb6a);
    private static final Color C_BLUE     = new Color(0x42a5f5);
    private static final Color C_GRAY_ACC = new Color(0xb0bec5);

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font FONT_UI    = rf("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_LABEL = rf("Segoe UI", Font.BOLD,  11);
    private static final Font FONT_TITLE = rf("Segoe UI", Font.BOLD,  15);
    private static final Font FONT_MONO  = rf("Consolas", Font.PLAIN, 11);
    private static final Font FONT_BTN   = rf("Segoe UI", Font.BOLD,  12);

    private static Font rf(String n, int s, int sz) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Set<String> av = new HashSet<>(Arrays.asList(ge.getAvailableFontFamilyNames()));
        String f = av.contains(n) ? n : Font.SANS_SERIF;
        if (n.equals("Consolas") && !av.contains(n))
            f = av.contains("JetBrains Mono") ? "JetBrains Mono"
                    : av.contains("Courier New")    ? "Courier New" : Font.MONOSPACED;
        return new Font(f, s, sz);
    }

    // ── Estado ────────────────────────────────────────────────────────────────
    private BufferedImage imgOriginal   = null;
    private BufferedImage imgModificada = null;
    private String        filtroActual  = "Original";

    // ── Componentes principales ───────────────────────────────────────────────
    private ImagePanel panelOrig;
    private ImagePanel panelMod;
    private JLabel     lblInfoOrig;
    private JLabel     lblInfoMod;
    private JLabel     lblFiltroActual;
    private JButton    btnGuardar;

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ProcesadorImagenes().setVisible(true));
    }

    public ProcesadorImagenes() {
        setTitle("Procesador de Imágenes");
        setSize(1300, 780);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout(0, 0));

        add(buildPanelIzquierdo(), BorderLayout.WEST);
        add(buildPanelCentral(),  BorderLayout.CENTER);
    }

    // ── Panel izquierdo: controles ────────────────────────────────────────────
    private JPanel buildPanelIzquierdo() {
        JPanel panel = new DarkPanel();
        panel.setPreferredSize(new Dimension(230, 780));
        panel.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0,0,new Color(0x1e1a2e),getWidth(),0,new Color(0x1a1d2e)));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(C_ACCENT);
                g2.fillRect(0,getHeight()-2,getWidth(),2);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16,16,14,16));

        JLabel lbl = new JLabel("Procesador");
        lbl.setFont(FONT_TITLE); lbl.setForeground(C_TEXT);
        JLabel sub = new JLabel("de Imágenes");
        sub.setFont(new Font(FONT_UI.getFamily(), Font.PLAIN, 11));
        sub.setForeground(C_TEXT_DIM);
        JPanel ts = new JPanel(); ts.setLayout(new BoxLayout(ts, BoxLayout.Y_AXIS)); ts.setOpaque(false);
        ts.add(lbl); ts.add(Box.createVerticalStrut(2)); ts.add(sub);

        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_ACCENT); g2.fillOval(0,0,10,10); g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(10,10); }
        };
        dot.setOpaque(false); dot.setAlignmentY(CENTER_ALIGNMENT);
        header.add(ts, BorderLayout.CENTER); header.add(dot, BorderLayout.EAST);

        // Cuerpo de controles
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14,12,12,12));

        // ── Cargar imagen
        body.add(sectionLabel("IMAGEN"));
        body.add(Box.createVerticalStrut(8));
        body.add(accentBtn("📂  Cargar imagen", C_ACCENT, C_ACCENT2, e -> cargarImagen()));
        body.add(Box.createVerticalStrut(4));
        JLabel dropHint = new JLabel("  o arrastra una imagen aquí");
        dropHint.setFont(new Font(FONT_UI.getFamily(), Font.PLAIN, 10));
        dropHint.setForeground(C_TEXT_DIM);
        body.add(dropHint);

        body.add(Box.createVerticalStrut(16));

        // ── Filtros
        body.add(sectionLabel("FILTROS"));
        body.add(Box.createVerticalStrut(8));

        String[][] filtros = {
                {"⬜  Original",      "Original"},
                {"🔲  Escala de grises","Grises"},
                {"⬛  Negativo",       "Negativo"},
                {"🔳  Negativo grises","NegativoGrises"},
        };
        for (String[] f : filtros) {
            body.add(filtroBtn(f[0], f[1]));
            body.add(Box.createVerticalStrut(4));
        }

        // Indicador filtro actual
        lblFiltroActual = new JLabel("  Filtro: Original");
        lblFiltroActual.setFont(new Font(FONT_MONO.getFamily(), Font.PLAIN, 10));
        lblFiltroActual.setForeground(C_SUCCESS);
        body.add(lblFiltroActual);

        body.add(Box.createVerticalStrut(16));

        // ── Análisis
        body.add(sectionLabel("ANÁLISIS"));
        body.add(Box.createVerticalStrut(8));
        body.add(smallBtn("📊  Histograma Rojo",     e -> abrirHistograma("R")));
        body.add(Box.createVerticalStrut(4));
        body.add(smallBtn("📊  Histograma Verde",    e -> abrirHistograma("G")));
        body.add(Box.createVerticalStrut(4));
        body.add(smallBtn("📊  Histograma Azul",     e -> abrirHistograma("B")));
        body.add(Box.createVerticalStrut(4));
        body.add(smallBtn("📊  Histograma RGB",      e -> abrirHistograma("RGB")));
        body.add(Box.createVerticalStrut(4));
        body.add(smallBtn("📋  Tabla frecuencias",   e -> abrirTablaFrecuencias()));

        body.add(Box.createVerticalStrut(16));

        // ── Guardar
        body.add(sectionLabel("EXPORTAR"));
        body.add(Box.createVerticalStrut(8));
        btnGuardar = accentBtn("💾  Guardar imagen", new Color(0x00897b), new Color(0x00695c),
                e -> guardarImagen());
        btnGuardar.setEnabled(false);
        body.add(btnGuardar);

        body.add(Box.createVerticalGlue());

        panel.add(header, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null); scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        styleScrollBar(scroll.getVerticalScrollBar());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ── Panel central: imagen original + modificada ───────────────────────────
    private JPanel buildPanelCentral() {
        JPanel centro = new JPanel(new GridLayout(1, 2, 6, 0));
        centro.setOpaque(false);
        centro.setBorder(new EmptyBorder(10, 6, 10, 10));

        // Panel imagen original
        JPanel cardOrig = buildImageCard("IMAGEN ORIGINAL", false);
        panelOrig  = (ImagePanel) cardOrig.getClientProperty("imagePanel");
        lblInfoOrig = (JLabel)    cardOrig.getClientProperty("infoLabel");

        // Panel imagen modificada
        JPanel cardMod = buildImageCard("IMAGEN MODIFICADA", true);
        panelMod  = (ImagePanel) cardMod.getClientProperty("imagePanel");
        lblInfoMod = (JLabel)    cardMod.getClientProperty("infoLabel");

        // Drag & drop en ambos panels
        configurarDragDrop(panelOrig);
        configurarDragDrop(panelMod);

        centro.add(cardOrig);
        centro.add(cardMod);
        return centro;
    }

    private JPanel buildImageCard(String titulo, boolean esMod) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PANEL);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),12,12));
                g2.setColor(C_BORDER);
                g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,12,12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10,10,10,10));

        // Header de la card
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(FONT_LABEL);
        lblTit.setForeground(esMod ? C_ACCENT : C_SUCCESS);

        JLabel lblInfo = new JLabel("Sin imagen");
        lblInfo.setFont(new Font(FONT_MONO.getFamily(), Font.PLAIN, 10));
        lblInfo.setForeground(C_TEXT_DIM);
        cardHeader.add(lblTit,  BorderLayout.WEST);
        cardHeader.add(lblInfo, BorderLayout.EAST);

        ImagePanel imgPanel = new ImagePanel();
        imgPanel.setBackground(C_SURFACE);
        imgPanel.setBorder(new RoundBorder(C_BORDER, 8));

        card.add(cardHeader, BorderLayout.NORTH);
        card.add(imgPanel,   BorderLayout.CENTER);

        card.putClientProperty("imagePanel", imgPanel);
        card.putClientProperty("infoLabel",  lblInfo);
        return card;
    }

    // ── Drag & Drop ───────────────────────────────────────────────────────────
    private void configurarDragDrop(JComponent comp) {
        new DropTarget(comp, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    java.util.List<File> files = (java.util.List<File>)
                            e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) cargarArchivo(files.get(0));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
    }

    // ── Carga de imagen ───────────────────────────────────────────────────────
    private void cargarImagen() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (PNG, JPG, BMP, GIF, TIFF)", "png","jpg","jpeg","bmp","gif","tiff","tif","webp"));
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            cargarArchivo(fc.getSelectedFile());
    }

    private void cargarArchivo(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                JOptionPane.showMessageDialog(this, "No se pudo leer el archivo:\n" + file.getName(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Convertir siempre a ARGB para procesamiento uniforme
            imgOriginal = toARGB(img);
            aplicarFiltro("Original");

            String info = img.getWidth() + " × " + img.getHeight() + " px  |  " + file.getName();
            lblInfoOrig.setText(info);
            btnGuardar.setEnabled(true);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private BufferedImage toARGB(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics(); g2.drawImage(src,0,0,null); g2.dispose();
        return dst;
    }

    // ── Aplicar filtros ───────────────────────────────────────────────────────
    private void aplicarFiltro(String filtro) {
        if (imgOriginal == null) {
            JOptionPane.showMessageDialog(this, "Primero carga una imagen.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        filtroActual = filtro;
        int w = imgOriginal.getWidth(), h = imgOriginal.getHeight();
        imgModificada = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb  = imgOriginal.getRGB(x, y);
                int alpha = (argb >> 24) & 0xff;
                int r     = (argb >> 16) & 0xff;
                int g     = (argb >>  8) & 0xff;
                int b     =  argb        & 0xff;

                int nr = r, ng = g, nb = b;

                switch (filtro) {
                    case "Grises": {
                        int gray = (int)(0.299*r + 0.587*g + 0.114*b);
                        nr = ng = nb = gray;
                        break;
                    }
                    case "Negativo": {
                        nr = 255-r; ng = 255-g; nb = 255-b;
                        break;
                    }
                    case "NegativoGrises": {
                        int gray = (int)(0.299*r + 0.587*g + 0.114*b);
                        int neg  = 255 - gray;
                        nr = ng = nb = neg;
                        break;
                    }
                    // "Original" → sin cambios
                }
                imgModificada.setRGB(x, y, (alpha<<24)|(nr<<16)|(ng<<8)|nb);
            }
        }

        panelOrig.setImage(imgOriginal);
        panelMod.setImage(imgModificada);
        lblFiltroActual.setText("  Filtro: " + filtro);
        lblInfoMod.setText(w + " × " + h + " px  |  " + filtro);
    }

    // ── Guardar imagen ────────────────────────────────────────────────────────
    private void guardarImagen() {
        if (imgModificada == null) return;
        JFileChooser fc = new JFileChooser();
        fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("JPEG (*.jpg)", "jpg"));
        fc.setFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
        fc.setSelectedFile(new File("imagen_modificada.png"));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        String fmt = fc.getFileFilter().getDescription().contains("JPEG") ? "jpg" : "png";
        if (!file.getName().toLowerCase().endsWith("." + fmt))
            file = new File(file.getAbsolutePath() + "." + fmt);

        try {
            // JPEG no soporta alpha
            BufferedImage toSave = imgModificada;
            if (fmt.equals("jpg")) {
                toSave = new BufferedImage(imgModificada.getWidth(), imgModificada.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = toSave.createGraphics();
                g2.setColor(Color.WHITE); g2.fillRect(0,0,toSave.getWidth(),toSave.getHeight());
                g2.drawImage(imgModificada,0,0,null); g2.dispose();
            }
            ImageIO.write(toSave, fmt, file);
            JOptionPane.showMessageDialog(this, "Imagen guardada:\n" + file.getAbsolutePath(),
                    "Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Histogramas ───────────────────────────────────────────────────────────
    private void abrirHistograma(String canal) {
        if (imgOriginal == null) {
            JOptionPane.showMessageDialog(this,"Primero carga una imagen.","Aviso",JOptionPane.WARNING_MESSAGE);
            return;
        }
        JDialog dialog = new JDialog(this, "Histograma — " + canal, false);
        dialog.setSize(680, 440);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        dialog.setLayout(new BorderLayout(0,8));

        // Selector original / modificada
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        top.setBackground(C_PANEL);
        JLabel lblSel = new JLabel("Imagen:");
        lblSel.setFont(FONT_LABEL); lblSel.setForeground(C_TEXT_DIM);
        JComboBox<String> combo = new JComboBox<>(new String[]{"Original","Modificada"});
        combo.setFont(FONT_UI); combo.setBackground(C_SURFACE); combo.setForeground(C_TEXT);
        top.add(lblSel); top.add(combo);
        dialog.add(top, BorderLayout.NORTH);

        HistogramaPanel hPanel = new HistogramaPanel(canal);
        hPanel.setImageData(calcularHistograma(imgOriginal));
        dialog.add(hPanel, BorderLayout.CENTER);

        combo.addActionListener(e -> {
            BufferedImage img = combo.getSelectedIndex()==0 ? imgOriginal : imgModificada;
            hPanel.setImageData(calcularHistograma(img));
        });

        dialog.setVisible(true);
    }

    // ── Tabla de frecuencias ──────────────────────────────────────────────────
    private void abrirTablaFrecuencias() {
        if (imgOriginal == null) {
            JOptionPane.showMessageDialog(this,"Primero carga una imagen.","Aviso",JOptionPane.WARNING_MESSAGE);
            return;
        }
        JDialog dialog = new JDialog(this, "Tabla de Distribución de Frecuencias", false);
        dialog.setSize(700, 560);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_BG);
        dialog.setLayout(new BorderLayout(0,8));

        // Selector
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        top.setBackground(C_PANEL);
        JLabel lblSel = new JLabel("Imagen:");
        lblSel.setFont(FONT_LABEL); lblSel.setForeground(C_TEXT_DIM);
        JComboBox<String> comboImg = new JComboBox<>(new String[]{"Original","Modificada"});
        comboImg.setFont(FONT_UI); comboImg.setBackground(C_SURFACE); comboImg.setForeground(C_TEXT);
        JLabel lblCan = new JLabel("  Canal:");
        lblCan.setFont(FONT_LABEL); lblCan.setForeground(C_TEXT_DIM);
        JComboBox<String> comboCan = new JComboBox<>(new String[]{"Rojo","Verde","Azul","Gris"});
        comboCan.setFont(FONT_UI); comboCan.setBackground(C_SURFACE); comboCan.setForeground(C_TEXT);
        top.add(lblSel); top.add(comboImg); top.add(lblCan); top.add(comboCan);
        dialog.add(top, BorderLayout.NORTH);

        String[] cols = {"Valor (0-255)", "Frec. Absoluta", "Frec. Relativa (%)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabla = new JTable(model);
        tabla.setFont(FONT_MONO); tabla.setForeground(C_TEXT); tabla.setBackground(C_SURFACE);
        tabla.setGridColor(C_BORDER); tabla.setRowHeight(20);
        tabla.getTableHeader().setFont(FONT_LABEL);
        tabla.getTableHeader().setBackground(C_PANEL);
        tabla.getTableHeader().setForeground(C_ACCENT);
        tabla.setSelectionBackground(new Color(C_ACCENT.getRed(),C_ACCENT.getGreen(),C_ACCENT.getBlue(),60));

        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(new EmptyBorder(4,8,8,8));
        sp.setBackground(C_BG); sp.getViewport().setBackground(C_SURFACE);
        styleScrollBar(sp.getVerticalScrollBar());
        dialog.add(sp, BorderLayout.CENTER);

        Runnable actualizar = () -> {
            BufferedImage img = comboImg.getSelectedIndex()==0 ? imgOriginal : imgModificada;
            String canal = new String[]{"R","G","B","Gray"}[comboCan.getSelectedIndex()];
            int[] hist = calcularHistograma(img)[canalIdx(canal)];
            long total = 0; for (int v : hist) total += v;
            model.setRowCount(0);
            DecimalFormat df = new DecimalFormat("0.0000");
            for (int i = 0; i < 256; i++) {
                if (hist[i] > 0) {
                    double rel = (total > 0) ? (hist[i] * 100.0 / total) : 0;
                    model.addRow(new Object[]{i, hist[i], df.format(rel)});
                }
            }
        };

        comboImg.addActionListener(e -> actualizar.run());
        comboCan.addActionListener(e -> actualizar.run());
        actualizar.run();
        dialog.setVisible(true);
    }

    // ── Calculo de histogramas ────────────────────────────────────────────────
    // Retorna int[4][256]: [0]=R, [1]=G, [2]=B, [3]=Gray
    private int[][] calcularHistograma(BufferedImage img) {
        int[][] h = new int[4][256];
        int w = img.getWidth(), ht = img.getHeight();
        for (int y = 0; y < ht; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x,y);
                int r = (argb>>16)&0xff, g=(argb>>8)&0xff, b=argb&0xff;
                h[0][r]++; h[1][g]++; h[2][b]++;
                int gray = (int)(0.299*r+0.587*g+0.114*b);
                h[3][gray]++;
            }
        }
        return h;
    }

    private int canalIdx(String canal) {
        switch(canal) { case "R": return 0; case "G": return 1; case "B": return 2; default: return 3; }
    }

    // ── Clases de UI ─────────────────────────────────────────────────────────

    /** Panel que muestra una imagen centrada y ajustada */
    static class ImagePanel extends JPanel {
        private BufferedImage img;
        ImagePanel() {
            setPreferredSize(new Dimension(300, 300));
            setBackground(C_SURFACE);
        }
        void setImage(BufferedImage i) { img = i; repaint(); }
        BufferedImage getImage() { return img; }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img == null) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_TEXT_DIM);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                String msg = "Arrastra una imagen aquí";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth()-fm.stringWidth(msg))/2, getHeight()/2);
                // Icono de imagen
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        0, new float[]{6,4}, 0));
                int pw=60,ph=50,px=(getWidth()-pw)/2,py=getHeight()/2-ph-8;
                g2.drawRoundRect(px,py,pw,ph,8,8);
                g2.setStroke(new BasicStroke(2));
                g2.fillOval(px+8,py+8,10,10);
                int[] xp={px+6,px+22,px+38,px+54,px+pw},yp={py+ph,py+ph-16,py+ph-28,py+ph-20,py+ph};
                g2.drawPolyline(xp,yp,5);
                g2.dispose();
                return;
            }
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double scaleX=(double)getWidth()/img.getWidth();
            double scaleY=(double)getHeight()/img.getHeight();
            double scale=Math.min(scaleX,scaleY);
            int sw=(int)(img.getWidth()*scale), sh=(int)(img.getHeight()*scale);
            int ox=(getWidth()-sw)/2, oy=(getHeight()-sh)/2;
            g2.drawImage(img, ox, oy, sw, sh, null);
            g2.dispose();
        }
    }

    /** Panel que dibuja histogramas */
    static class HistogramaPanel extends JPanel {
        private int[][] data;
        private final String canal;

        HistogramaPanel(String canal) {
            this.canal = canal;
            setBackground(C_BG);
            setBorder(new EmptyBorder(16,20,16,20));
        }

        void setImageData(int[][] d) { data = d; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null) return;
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pad=40, w=getWidth()-pad*2, h=getHeight()-pad*2;

            // Fondo
            g2.setColor(C_SURFACE);
            g2.fillRoundRect(pad/2, pad/2, getWidth()-pad, getHeight()-pad, 10, 10);

            // Determinar canales a dibujar
            int[][] canales; Color[] colores;
            if (canal.equals("RGB")) {
                canales = new int[][]{data[0],data[1],data[2]};
                colores = new Color[]{C_RED, C_GREEN, C_BLUE};
            } else {
                int idx = canal.equals("R")?0:canal.equals("G")?1:2;
                Color col = canal.equals("R")?C_RED:canal.equals("G")?C_GREEN:C_BLUE;
                canales = new int[][]{data[idx]};
                colores = new Color[]{col};
            }

            // Max para escala
            int maxVal = 1;
            for (int[] ch : canales) for (int v : ch) if (v > maxVal) maxVal = v;

            // Ejes
            g2.setColor(C_BORDER);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(pad, pad, pad, pad+h);
            g2.drawLine(pad, pad+h, pad+w, pad+h);

            // Etiquetas eje X
            g2.setColor(C_TEXT_DIM);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            for (int i = 0; i <= 255; i += 51) {
                int x = pad + (int)(i * w / 255.0);
                g2.drawString(String.valueOf(i), x-4, pad+h+14);
            }

            // Barras
            float barW = w / 256.0f;
            for (int ci = 0; ci < canales.length; ci++) {
                Color col = colores[ci];
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(),
                        canal.equals("RGB") ? 120 : 180));
                for (int i = 0; i < 256; i++) {
                    int barH = (int)((long)canales[ci][i] * h / maxVal);
                    int bx = pad + (int)(i * barW);
                    g2.fillRect(bx, pad+h-barH, Math.max(1,(int)barW), barH);
                }
            }

            // Titulo
            g2.setColor(C_TEXT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            String title = "Histograma " + (canal.equals("RGB")?"RGB":"Canal "+canal);
            g2.drawString(title, pad, pad-6);

            // Leyenda RGB
            if (canal.equals("RGB")) {
                String[] names={"Rojo","Verde","Azul"};
                Color[] cols={C_RED,C_GREEN,C_BLUE};
                int lx = pad+w-120;
                for (int i=0;i<3;i++) {
                    g2.setColor(cols[i]); g2.fillRect(lx,pad+8+i*16,12,10);
                    g2.setColor(C_TEXT); g2.setFont(new Font("Segoe UI",Font.PLAIN,10));
                    g2.drawString(names[i], lx+16, pad+17+i*16);
                }
            }

            g2.dispose();
        }
    }

    // ── Helpers de construcción de UI ─────────────────────────────────────────

    private JPanel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL); lbl.setForeground(C_ACCENT);
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        p.add(lbl, BorderLayout.WEST);
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_BORDER); g.fillRect(0,getHeight()/2,getWidth(),1);
            }
        };
        line.setOpaque(false); line.setBorder(new EmptyBorder(0,6,0,0));
        p.add(line, BorderLayout.CENTER);
        return p;
    }

    private JButton accentBtn(String text, Color c1, Color c2, ActionListener al) {
        JButton btn = new JButton(text) {
            private float hov=0f; private boolean pr=false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { anim(1f); }
                @Override public void mouseExited(MouseEvent e)  { pr=false; anim(0f); }
                @Override public void mousePressed(MouseEvent e)  { pr=true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { pr=false; repaint(); }
                private void anim(float t) {
                    javax.swing.Timer tm=new javax.swing.Timer(12,null);
                    tm.addActionListener(ev->{hov+=(t-hov)*0.25f;if(Math.abs(hov-t)<0.01f){hov=t;tm.stop();}repaint();});
                    tm.start();
                }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w=getWidth(),h=getHeight(),arc=8;
                if (!isEnabled()) {
                    g2.setColor(C_SURFACE); g2.fill(new RoundRectangle2D.Float(0,0,w,h,arc,arc));
                    g2.setColor(C_TEXT_DIM); g2.setFont(getFont());
                    FontMetrics fm=g2.getFontMetrics();
                    g2.drawString(getText(),(w-fm.stringWidth(getText()))/2,(h+fm.getAscent()-fm.getDescent())/2);
                    g2.dispose(); return;
                }
                Color top=blend(c1,c1.darker(),hov*0.3f), bot=blend(c2,c2.darker(),hov*0.3f);
                if(pr){top=c2.darker();bot=c2.darker().darker();}
                g2.setPaint(new GradientPaint(0,0,top,0,h,bot));
                g2.fill(new RoundRectangle2D.Float(0,0,w,h,arc,arc));
                g2.setPaint(new GradientPaint(0,0,new Color(255,255,255,pr?0:(int)(30+hov*15)),0,h/2,new Color(255,255,255,0)));
                g2.fill(new RoundRectangle2D.Float(0,0,w,h/2,arc,arc));
                g2.setColor(new Color(0,0,0,30));
                g2.draw(new RoundRectangle2D.Float(0,0,w-1,h-1,arc,arc));
                g2.setColor(Color.WHITE); g2.setFont(getFont());
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(w-fm.stringWidth(getText()))/2,(h+fm.getAscent()-fm.getDescent())/2+(pr?1:0));
                g2.dispose();
            }
        };
        btn.setFont(FONT_BTN); btn.setForeground(Color.WHITE); btn.setOpaque(false);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(al);
        return btn;
    }

    private JButton smallBtn(String text, ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? C_SURFACE : new Color(0x1e2230));
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),7,7));
                g2.setColor(getModel().isRollover() ? C_ACCENT : C_BORDER);
                g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,7,7));
                g2.setColor(getModel().isRollover() ? C_TEXT : C_TEXT_DIM);
                g2.setFont(getFont());
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),8,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btn.setFont(new Font(FONT_UI.getFamily(), Font.PLAIN, 12));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(al);
        return btn;
    }

    private JButton filtroBtn(String text, String filtro) {
        JButton btn = smallBtn(text, e -> aplicarFiltro(filtro));
        return btn;
    }

    private void styleScrollBar(JScrollBar sb) {
        sb.setBackground(C_SURFACE);
        sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor=C_BORDER; thumbHighlightColor=C_ACCENT; trackColor=C_SURFACE;
            }
            @Override protected JButton createDecreaseButton(int o) { return zb(); }
            @Override protected JButton createIncreaseButton(int o) { return zb(); }
            private JButton zb() { JButton b=new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        });
    }

    private Color blend(Color a, Color b, float t) {
        t=Math.max(0,Math.min(1,t));
        return new Color((int)(a.getRed()+(b.getRed()-a.getRed())*t),
                (int)(a.getGreen()+(b.getGreen()-a.getGreen())*t),
                (int)(a.getBlue()+(b.getBlue()-a.getBlue())*t));
    }

    // ── Clases base ───────────────────────────────────────────────────────────

    static class DarkPanel extends JPanel {
        DarkPanel() { setOpaque(true); setBackground(C_PANEL); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setColor(C_PANEL); g2.fillRect(0,0,getWidth(),getHeight());
            g2.setColor(C_BORDER); g2.fillRect(getWidth()-1,0,1,getHeight());
            g2.dispose();
        }
    }

    static class RoundBorder extends AbstractBorder {
        private final Color c; private final int r;
        RoundBorder(Color c, int r) { this.c=c; this.r=r; }
        @Override public void paintBorder(Component comp, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c); g2.draw(new RoundRectangle2D.Float(x,y,w-1,h-1,r,r)); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(r/2,r/2,r/2,r/2); }
    }
}