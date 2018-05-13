package gk;
// https://javastart.pl/static/grafika_awt_swing/obsluga-zdarzen-mysz/
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Preview extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener {

    public static final int MAX_LEVEL = 12;

    public static final double ZOOM_RATE = 1.1;

    private static final Path2D CROSS = new Path2D.Double();

    private static final Shape DISC = new Arc2D.Double(-HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, -HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, HyperEdge.BOUNDING_BOX_EDGE_LENGTH, HyperEdge.BOUNDING_BOX_EDGE_LENGTH, 0, 360, Arc2D.OPEN);

    static {
        CROSS.moveTo(-HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, -HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2);
        CROSS.lineTo(HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, -HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2);
        CROSS.lineTo(HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2);
        CROSS.lineTo(-HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2);
        CROSS.lineTo(-HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, -HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2);
        CROSS.lineTo(HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2);
        CROSS.moveTo(-HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2);
        CROSS.lineTo(HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2, -HyperEdge.BOUNDING_BOX_EDGE_LENGTH / 2);
    }

    private final HyperEdge startSymbol = new HyperEdge(0, 0, null);

    private final List<HyperEdge> edges = new LinkedList();

    private final JFileChooser imageChooser = new JFileChooser();

    private final JPopupMenu contextMenu = new JPopupMenu();

    private final JMenuItem symmetricalScalingItem = new JCheckBoxMenuItem("Symetryczne skalowanie", true);

    private final JMenuItem drawingBoundingBoxesItem = new JCheckBoxMenuItem("Rysowanie ramek", true);

    private final JMenuItem drawingLastLevelOnlyItem = new JCheckBoxMenuItem("Rysowanie tylko ostatniego poziomu", true);

    private final JMenuItem levelsNumberItem = new JMenu("Liczba poziomów");

    private final JMenuItem symbolItem = new JMenu("Symbol");

    private final JMenuItem crossModeItem = new JRadioButtonMenuItem("Krzyż", true);

    private final JMenuItem discModeItem = new JRadioButtonMenuItem("Koło");

    private final JMenuItem imageModeItem = new JRadioButtonMenuItem("Obraz");

    private final Point2D.Double mouseHandle = new Point2D.Double();

    private final Point2D.Double centerOfView = new Point2D.Double(0.5, 0.5);

    private double zoom = 1.0;

    private Stroke pathStroke;

    private HyperEdge currentEdge;

    private HyperEdge lastEdge;

    private Object symbol;

    private BufferedImage lastLoadedImage;

    private int levelsNumber = 2;

    public Preview() {
        String[] supportedImageExtensions = ImageIO.getReaderFileSuffixes();
        String imageTypesDescription = Arrays.stream(supportedImageExtensions).sorted(String::compareTo).map( s->"*." + s).collect(Collectors.joining(", ", "Pliki obrazów (", ")"));
        imageChooser.setFileFilter(new FileNameExtensionFilter(imageTypesDescription, supportedImageExtensions));
        ActionListener contextMenuHandler =  action->{
    if (action.getSource() == symmetricalScalingItem) {
        edges.stream().forEach( edge->{
});
    } else if (action.getSource() == crossModeItem) {
        setSymbol(CROSS);
    } else if (action.getSource() == discModeItem) {
        setSymbol(DISC);
    } else if (action.getSource() == imageModeItem) {
        setSymbol(chooseImage());
    }
    repaint();
};
        symmetricalScalingItem.addActionListener(contextMenuHandler);
        drawingBoundingBoxesItem.addActionListener(contextMenuHandler);
        drawingLastLevelOnlyItem.addActionListener(contextMenuHandler);
        crossModeItem.addActionListener(contextMenuHandler);
        discModeItem.addActionListener(contextMenuHandler);
        imageModeItem.addActionListener(contextMenuHandler);
        JSlider slider = new JSlider(2, MAX_LEVEL, levelsNumber);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.addChangeListener( event->{
    levelsNumber = slider.getValue();
    repaint();
});
        levelsNumberItem.add(slider);
        ButtonGroup symbolItems = new ButtonGroup();
        symbolItems.add(crossModeItem);
        symbolItems.add(discModeItem);
        symbolItems.add(imageModeItem);
        symbolItem.add(crossModeItem);
        symbolItem.add(discModeItem);
        symbolItem.add(imageModeItem);
        contextMenu.add(symmetricalScalingItem);
        contextMenu.add(drawingBoundingBoxesItem);
        contextMenu.add(drawingLastLevelOnlyItem);
        contextMenu.add(levelsNumberItem);
        contextMenu.add(symbolItem);
        updatePathStroke();
        setSymbol(CROSS);
    }

    private void setSymbol(Object symbol) {
        if (symbol != null) {
            this.symbol = symbol;
        } else if (this.symbol == CROSS) {
            crossModeItem.setSelected(true);
        } else if (this.symbol == DISC) {
            discModeItem.setSelected(true);
        }
    }

    private BufferedImage chooseImage() {
        BufferedImage loadedImage = lastLoadedImage;
        if (imageChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                loadedImage = ImageIO.read(imageChooser.getSelectedFile());
            } catch (IOException ioe) {
                loadedImage = null;
            }
            if (loadedImage != null) {
                lastLoadedImage = loadedImage;
            } else {
                String windowTitle = ((JFrame) SwingUtilities.getWindowAncestor(Preview.this)).getTitle();
                JOptionPane.showMessageDialog(this, "Wystąpił błąd przy odczycie pliku.", windowTitle, JOptionPane.ERROR_MESSAGE);
            }
        }
        return loadedImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(pathStroke);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        g2d.translate(getWidth() * centerOfView.x, getHeight() * centerOfView.y);
        g2d.scale(zoom, -zoom);
        g.setColor(Color.BLACK);
        Object drawnSymbol = drawingLastLevelOnlyItem.isSelected() ? null : symbol;
        HyperEdge.paint(g2d, drawnSymbol, drawingBoundingBoxesItem.isSelected(), startSymbol);
        paintEdges(g2d);
    }

    private void paintEdges(Graphics2D g2d, HyperEdge... path) {
        if (path.length + 2 <= levelsNumber) {
            edges.stream().forEach( edge->      {
            HyperEdge[] extendedPath = new HyperEdge[path.length + 1];
            System.arraycopy(path, 0, extendedPath, 0, path.length);
            extendedPath[path.length] = edge;
            Object drawnSymbol = drawingLastLevelOnlyItem.isSelected() && path.length + 2 != levelsNumber ? null : symbol;
            HyperEdge.paint(g2d, drawnSymbol, extendedPath.length == 1 && drawingBoundingBoxesItem.isSelected(), extendedPath);
            paintEdges(g2d, extendedPath);
            });
        }
    }

    private Point2D.Double getGlobalCoords(MouseEvent mouseEvent) {
        return new Point2D.Double((mouseEvent.getX() - getWidth() * centerOfView.x) / zoom, (getHeight() * centerOfView.y - mouseEvent.getY()) / zoom);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            Point2D.Double globalCoords = getGlobalCoords(e);
            currentEdge = edges.stream().filter( edge->edge.contains(globalCoords)).findAny().orElse(null);
            if (currentEdge != null) {
                lastEdge = currentEdge;
            }
            switch(e.getClickCount()) {
                case 1:
                    if (currentEdge != null) {
                        currentEdge.grip(globalCoords);
                    }
                    break;
                case 2:
                    if (currentEdge != null) {
                        edges.remove(currentEdge);
                        currentEdge = null;
                    } else {
                        edges.add(new HyperEdge(globalCoords.getX(), globalCoords.getY(), lastEdge));
                    }
                    repaint();
            }
        } else if (SwingUtilities.isMiddleMouseButton(e)) {
            mouseHandle.setLocation(e.getX(), e.getY());
        } else if (SwingUtilities.isRightMouseButton(e)) {
            contextMenu.show(this, e.getX(), e.getY());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (currentEdge != null) {
                currentEdge.drag(getGlobalCoords(e), symmetricalScalingItem.isSelected(), !e.isShiftDown());
                repaint();
            }
        } else if (SwingUtilities.isMiddleMouseButton(e)) {
            centerOfView.x += (e.getX() - mouseHandle.x) / getWidth();
            centerOfView.y += (e.getY() - mouseHandle.y) / getHeight();
            mouseHandle.setLocation(e.getX(), e.getY());
            repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double zoomMultiplier = Math.pow(ZOOM_RATE, -e.getWheelRotation());
        zoom *= zoomMultiplier;
        Point2D.Double globalCoords = getGlobalCoords(e);
        centerOfView.x -= (globalCoords.x * (zoomMultiplier - 1.0) / getWidth()) * zoom;
        centerOfView.y += (globalCoords.y * (zoomMultiplier - 1.0) / getHeight()) * zoom;
        updatePathStroke();
        repaint();
    }

    private void updatePathStroke() {
        pathStroke = new BasicStroke((float) (1 / zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    public static void main(String[] args) {
        UIManager.getDefaults().addResourceBundle(PolishResourceBundle.class.getName());
        EventQueue.invokeLater(()->{
    Preview preview = new Preview();
    preview.addMouseListener(preview);
    preview.addMouseMotionListener(preview);
    preview.addMouseWheelListener(preview);
    JFrame frame = new JFrame();
    frame.add(preview);
    frame.setTitle("Gramatyki kolażowe");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(HyperEdge.BOUNDING_BOX_EDGE_LENGTH * 2, HyperEdge.BOUNDING_BOX_EDGE_LENGTH * 2);
    frame.setLocationByPlatform(true);
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    frame.setVisible(true);
});
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }
}
