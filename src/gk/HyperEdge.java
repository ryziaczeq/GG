package gk;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class HyperEdge {

    private enum TransformationMode {

        NONE, TRANSLATION, HORIZONTAL_SCALING, VERTICAL_SCALING, ROTATION
    }

    ;

    public static final int BOUNDING_BOX_EDGE_LENGTH = 200;

    public static final int BOUNDING_BOX_MOUSE_LEEWAY = 10;

    public static final double SMALLEST_VALID_SCALE = 1e-6;

    public static final int TRANSLATION_STEP = 10;

    public static final double SCALING_STEP = 0.1;

    public static final int ROTATION_STEPS = 32;

    private static final Shape BOUNDING_BOX = new Rectangle2D.Double(-BOUNDING_BOX_EDGE_LENGTH / 2, -BOUNDING_BOX_EDGE_LENGTH / 2, BOUNDING_BOX_EDGE_LENGTH, BOUNDING_BOX_EDGE_LENGTH);

    private static final Stroke BOUNDING_BOX_STROKE = new BasicStroke(BOUNDING_BOX_MOUSE_LEEWAY * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private final Point2D.Double center = new Point2D.Double();

    private final Point2D.Double globalCoordsGrip = new Point2D.Double();

    private final Point2D.Double localCoordsGrip = new Point2D.Double();

    private double horizontalScale = 1.0;

    private double verticalScale = 1.0;

    private double rotation;

    private AffineTransform boundingBoxTransform;

    private TransformationMode currentMode = TransformationMode.NONE;

    public HyperEdge(double x, double y, HyperEdge prototype) {
        center.setLocation(x, y);
        if (prototype != null) {
            horizontalScale = prototype.horizontalScale;
            verticalScale = prototype.verticalScale;
            rotation = prototype.rotation;
        }
        else{
            horizontalScale = 1.0;
            verticalScale = 1.0;
            rotation = 0;                   
        }
        updateBoundingBoxTransform();
    }

    private static AffineTransform getTransform(Point2D.Double translation, double horizontalScale, double verticalScale, double rotation) {
        AffineTransform transform = new AffineTransform();
        transform.translate(translation.x, translation.y);
        transform.rotate(rotation);
        transform.scale(horizontalScale, verticalScale);
        return transform;
    }

    private void updateBoundingBoxTransform() {
        boundingBoxTransform = getTransform(center, horizontalScale, verticalScale, rotation);
    }

    private static AffineTransform getTransform(HyperEdge... edges) {
        double horizontalScale = 1.0, verticalScale = 1.0, rotation = 0, x = 0, y = 0;
        for (HyperEdge edge : edges) {
            horizontalScale *= edge.horizontalScale;
            verticalScale *= edge.verticalScale;
            rotation += edge.rotation;
            double old_x = x;
            x = x * edge.horizontalScale * Math.cos(edge.rotation) - y * edge.verticalScale * Math.sin(edge.rotation) + edge.center.x;
            y = old_x * edge.horizontalScale * Math.sin(edge.rotation) + y * edge.verticalScale * Math.cos(edge.rotation) + edge.center.y;
        }
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.rotate(rotation);
        transform.scale(horizontalScale, verticalScale);
        return transform;
    }
        //return getTransform(new Point2D.Double(x, y), horizontalScale, verticalScale, rotation);
    

    public static void paint(Graphics2D g2d, Object symbol, boolean drawBoundingBox, HyperEdge... path) {
        if (symbol != null || drawBoundingBox) {
            AffineTransform transform = getTransform(path);
            if (symbol != null) {
                if (symbol instanceof Path2D) {
                    g2d.draw(new Path2D.Double((Shape) symbol, transform));
                } else if (symbol instanceof Shape) {
                    g2d.fill(new Path2D.Double((Shape) symbol, transform));
                } else if (symbol instanceof BufferedImage) {
                    BufferedImage image = (BufferedImage) symbol;
                    AffineTransform imageTransform = new AffineTransform(transform);
                    imageTransform.scale((double) BOUNDING_BOX_EDGE_LENGTH / image.getWidth(), -(double) BOUNDING_BOX_EDGE_LENGTH / image.getHeight());
                    imageTransform.translate(-image.getWidth() * 0.5, -image.getHeight() * 0.5);
                    g2d.drawImage(image, imageTransform, null);
                }
            }
            if (drawBoundingBox) {
                g2d.draw(new Path2D.Double(BOUNDING_BOX, transform));
            }
        }
    }

    public boolean contains(Point2D.Double globalCoords) {
        Shape insides = new Path2D.Double(BOUNDING_BOX, boundingBoxTransform);
        return insides.contains(globalCoords) || BOUNDING_BOX_STROKE.createStrokedShape(insides).contains(globalCoords);
    }

    private Point2D.Double convertToLocalCoords(Point2D.Double globalCoords) {
        Point2D.Double localCoords = new Point2D.Double();
        try {
            boundingBoxTransform.inverseTransform(globalCoords, localCoords);
        } catch (NoninvertibleTransformException nte) {
        }
        return localCoords;
    }

    public void grip(Point2D.Double globalCoords) {
        Point2D.Double localCoords = convertToLocalCoords(globalCoords);
        if (Math.abs(localCoords.x) < BOUNDING_BOX_EDGE_LENGTH / 4 && Math.abs(localCoords.y) < BOUNDING_BOX_EDGE_LENGTH / 4) {
            currentMode = TransformationMode.TRANSLATION;
        } else if (Math.abs(localCoords.y) < BOUNDING_BOX_EDGE_LENGTH / 4) {
            currentMode = TransformationMode.HORIZONTAL_SCALING;
        } else if (Math.abs(localCoords.x) < BOUNDING_BOX_EDGE_LENGTH / 4) {
            currentMode = TransformationMode.VERTICAL_SCALING;
        } else {
            currentMode = TransformationMode.ROTATION;
        }
        globalCoordsGrip.setLocation(globalCoords);
        localCoordsGrip.setLocation(localCoords);
    }

    private static double normalizeScale(double scale, boolean fixedRate) {
        if (fixedRate) {
            scale = Math.round(scale / SCALING_STEP) * SCALING_STEP;
        }
        return Math.max(Math.abs(scale), SMALLEST_VALID_SCALE);
    }

    public void drag(Point2D.Double globalCoords, boolean symmetricalScaling, boolean fixedRate) {
        Point2D.Double localCoords = convertToLocalCoords(globalCoords);
        switch(currentMode) {
            case TRANSLATION:
                if (fixedRate) {
                    globalCoords.x = Math.round((center.x + globalCoords.x - globalCoordsGrip.x) / TRANSLATION_STEP) * TRANSLATION_STEP - center.x + globalCoordsGrip.x;
                    globalCoords.y = Math.round((center.y + globalCoords.y - globalCoordsGrip.y) / TRANSLATION_STEP) * TRANSLATION_STEP - center.y + globalCoordsGrip.y;
                }
                center.x += globalCoords.x - globalCoordsGrip.x;
                center.y += globalCoords.y - globalCoordsGrip.y;
                break;
            case HORIZONTAL_SCALING:
                horizontalScale = normalizeScale(horizontalScale * localCoords.x / localCoordsGrip.x, fixedRate);
                if (symmetricalScaling) {
                    verticalScale = horizontalScale;
                }
                break;
            case VERTICAL_SCALING:
                verticalScale = normalizeScale(verticalScale * localCoords.y / localCoordsGrip.y, fixedRate);
                if (symmetricalScaling) {
                    horizontalScale = verticalScale;
                }
                break;
            case ROTATION:
                rotation += Math.atan2(localCoords.y / verticalScale, localCoords.x / horizontalScale) - Math.atan2(localCoordsGrip.y / verticalScale, localCoordsGrip.x / horizontalScale);
                if (fixedRate) {
                    rotation = Math.round(rotation / (2 * Math.PI / ROTATION_STEPS)) * (2 * Math.PI / ROTATION_STEPS);
                }
        }
        globalCoordsGrip.setLocation(globalCoords);
        updateBoundingBoxTransform();
    }

    public void averageScales() {
        horizontalScale = verticalScale = (horizontalScale + verticalScale) / 2;
        updateBoundingBoxTransform();
    }
}
