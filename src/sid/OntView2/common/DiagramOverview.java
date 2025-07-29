package sid.OntView2.common;

import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DiagramOverview extends Canvas{
    PaintFrame paintframe;
    public Stage overviewStage;
    private final Stage ownerStage;
    private static final double MAX_OVERVIEW_SIZE = 400;
    private static final double ABSOLUTE_MAX_OVERVIEW = 400;
    private static final double DYNAMIC_MAX_RATIO = 0.3;
    private static final double RESIZE_DELAY = 200;
    private boolean adjustingDimensions = false;
    private double ratio = 1.0;

    public DiagramOverview(PaintFrame pPaintFrame, Stage ownerStage) {
        this.paintframe = pPaintFrame;
        this.ownerStage = ownerStage;
        setOnMousePressed(this::handleOverviewDrag);
        setOnMouseDragged(this::handleOverviewDrag);

        initDiagramOverview();
    }

    private double calculateMaxOverviewSize() {
        double ownerWidth = ownerStage.getWidth();
        double dynamic = ownerWidth * DYNAMIC_MAX_RATIO;
        return Math.min(dynamic, ABSOLUTE_MAX_OVERVIEW);
    }

    public void closeDiagramOverview() {
        overviewStage.close();
        overviewStage = null;
        paintframe.showDiagramOverview = false;
        paintframe.diagramOverview = null;
    }

    public void initDiagramOverview() {
        overviewStage = new Stage();
        overviewStage.initOwner(ownerStage);
        overviewStage.initModality(Modality.NONE);

        BorderPane root = new BorderPane(this);

        Dimension2D dims = computeOverviewDimensions(calculateMaxOverviewSize());
        double ow = dims.getWidth();
        double oh = dims.getHeight();
        ratio = ow / (oh + VisConstants.WINDOW_TITLE_BAR);

        setWidth(ow);
        setHeight(oh);

        if (isFocusWithin()) {
            requestFocus();
        }

        Scene scene = new Scene(root, ow, oh);
        overviewStage.setAlwaysOnTop(true);
        overviewStage.setScene(scene);
        overviewStage.setWidth(ow);
        overviewStage.setHeight(oh + VisConstants.WINDOW_TITLE_BAR);
        overviewStage.setTitle("Diagram Overview");
        overviewStage.show();

        relocateStage();
        drawOverview();
        prepareListeners();


        overviewStage.setOnCloseRequest(e -> closeDiagramOverview());
    }

    private void prepareListeners(){
        PauseTransition pauseH = new PauseTransition(Duration.millis(RESIZE_DELAY));
        pauseH.setOnFinished(e -> {
            adjustingDimensions = true;
            int newWidth = (int) Math.floor(overviewStage.getHeight() * ratio);
            overviewStage.setWidth(newWidth);
            updateCanvasSize();
            drawOverview();
            adjustingDimensions = false;
        });

        overviewStage.heightProperty().addListener((obs, oldH, newH) -> {
            if (!adjustingDimensions) pauseH.playFromStart();
        });

        PauseTransition pauseW = new PauseTransition(Duration.millis(RESIZE_DELAY));
        pauseW.setOnFinished(e -> {
            adjustingDimensions = true;
            int newHeight = (int) Math.floor(overviewStage.getWidth() / ratio);
            overviewStage.setHeight(newHeight);
            updateCanvasSize();
            drawOverview();
            adjustingDimensions = false;
        });

        overviewStage.widthProperty().addListener((obs, oldW, newW) -> {
            if (!adjustingDimensions) pauseW.playFromStart();
        });
    }

    public void updateOverviewSize() {
        if (overviewStage == null || !overviewStage.isShowing()) return;

        Dimension2D dims = computeOverviewDimensions(calculateMaxOverviewSize());
        double ow = dims.getWidth();
        double oh = dims.getHeight();
        ratio = ow / (oh + VisConstants.WINDOW_TITLE_BAR);

        this.setWidth(ow);
        this.setHeight(oh);

        overviewStage.setWidth(ow);
        overviewStage.setHeight(oh + VisConstants.WINDOW_TITLE_BAR);

        drawOverview();
        //relocateStage();
    }

    public void drawOverview() {
        if (overviewStage == null || !overviewStage.isShowing() || paintframe.scroll == null) return;

        GraphicsContext gc = getGraphicsContext2D();
        double ow = getWidth(), oh = getHeight();
        gc.clearRect(0,0,ow,oh);
        gc.setFill(Color.WHITE);
        gc.fillRect(0,0,ow,oh);

        double viewportWorldW = paintframe.scroll.getViewportBounds().getWidth() / paintframe.factor;
        double viewportWorldH = paintframe.scroll.getViewportBounds().getHeight() / paintframe.factor;
        double contentWidth  = paintframe.scroll.getHmax() + viewportWorldW;
        double contentHeight = paintframe.scroll.getVmax() + viewportWorldH;

        double scale = Math.min(ow/contentWidth, oh/contentHeight);

        drawShapes(gc, scale, paintframe.getVisGraph());

        // Calculate rectangle size in overview canvas
        double viewportRectW = viewportWorldW * scale;
        double viewportRectH = viewportWorldH * scale;

        double normH = paintframe.scroll.getHmax() > 0
            ? paintframe.scroll.getHvalue() / paintframe.scroll.getHmax() : 0;
        double normV = paintframe.scroll.getVmax() > 0
            ? paintframe.scroll.getVvalue() / paintframe.scroll.getVmax() : 0;

        // Rectangle position in overview canvas
        double rectX = normH * (ow - viewportRectW);
        double rectY = normV * (oh - viewportRectH);
        gc.setStroke(Color.DARKRED);
        gc.setLineWidth(1.5);
        gc.strokeRect(rectX, rectY, viewportRectW, viewportRectH);
    }

    private void handleOverviewDrag(MouseEvent e) {
        if (paintframe.scroll == null) return;

        double ow = getWidth();
        double oh = getHeight();

        double viewportWorldW = paintframe.scroll.getViewportBounds().getWidth() / paintframe.factor;
        double viewportWorldH = paintframe.scroll.getViewportBounds().getHeight() / paintframe.factor;

        double maxH = paintframe.scroll.getHmax();
        double maxV = paintframe.scroll.getVmax();

        double fracX = e.getX() / ow;
        double fracY = e.getY() / oh;

        double targetCenterX = fracX * (maxH + viewportWorldW);
        double targetCenterY = fracY * (maxV + viewportWorldH);

        double newOffsetX = targetCenterX - viewportWorldW / 2;
        double newOffsetY = targetCenterY - viewportWorldH / 2;

        newOffsetX = Math.max(0, Math.min(newOffsetX, maxH));
        newOffsetY = Math.max(0, Math.min(newOffsetY, maxV));

        paintframe.scroll.setHvalue(newOffsetX);
        paintframe.scroll.setVvalue(newOffsetY);

        drawOverview();
    }

    private void drawShapes(GraphicsContext g, double scale, VisGraph visGraph) {
        if (visGraph != null || g != null) {
            g.save();
            g.scale(scale, scale);
            paintframe.draw(g);
            g.restore();
        }
    }

    private Dimension2D computeOverviewDimensions(double maxOverviewSize) {
        System.out.println("Computing overview dimensions with max size: " + maxOverviewSize);
        Bounds vp = paintframe.scroll.getViewportBounds();
        double viewportWorldW = vp.getWidth() / paintframe.factor;
        double viewportWorldH = vp.getHeight() / paintframe.factor;

        double contentWidth = paintframe.scroll.getHmax() + viewportWorldW;
        double contentHeight = paintframe.scroll.getVmax() + viewportWorldH;

        double aspect = contentWidth / contentHeight;
        double overviewWidth = aspect >= 1 ? maxOverviewSize : maxOverviewSize * aspect;
        double overviewHeight = aspect >= 1 ? maxOverviewSize / aspect : maxOverviewSize;
        
        double finalH = overviewHeight;
        if (paintframe.scroll.getVmax() > 0) finalH += VisConstants.NEEDED_HEIGHT * overviewHeight / contentHeight;

        return new Dimension2D(overviewWidth, finalH);
    }

    private void updateCanvasSize() {
        double newW = overviewStage.getWidth();
        double newH = overviewStage.getHeight() - VisConstants.WINDOW_TITLE_BAR;
        setWidth(newW);
        setHeight(newH);
    }

    public void relocateStage(){
        Dimension2D dims = computeOverviewDimensions(calculateMaxOverviewSize());
        double ow = dims.getWidth();
        double oh = dims.getHeight();

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double margin = 80;
        overviewStage.setX(screen.getMaxX() - ow - margin);
        overviewStage.setY(screen.getMaxY() - oh - margin);
    }

}