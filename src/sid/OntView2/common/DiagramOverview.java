package sid.OntView2.common;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DiagramOverview extends Canvas{
    PaintFrame paintframe;
    public Stage overviewStage;

    public DiagramOverview(PaintFrame pPaintFrame) {
        this.paintframe = pPaintFrame;
        setOnMousePressed(this::handleOverviewDrag);
        setOnMouseDragged(this::handleOverviewDrag);

        initDiagramOverview();
    }

    public void closeDiagramOverview() {
        overviewStage.close();
        overviewStage = null;
        paintframe.showDiagramOverview = false;
        paintframe.diagramOverview = null;
    }

    public void initDiagramOverview() {
        overviewStage = new Stage();
        BorderPane root = new BorderPane(this);

        double viewportWorldW = paintframe.scroll.getViewportBounds().getWidth() / paintframe.factor;
        double viewportWorldH = paintframe.scroll.getViewportBounds().getHeight() / paintframe.factor;
        double contentWidth = paintframe.scroll.getHmax() + viewportWorldW;
        double contentHeight = paintframe.scroll.getVmax() + viewportWorldH;

        double maxSize = 400;
        double aspect = contentWidth / contentHeight;
        double ow = aspect >= 1 ? maxSize : maxSize * aspect;
        double oh = aspect >= 1 ? maxSize / aspect : maxSize;

        setWidth(ow);
        setHeight(oh);

        if (isFocusWithin()) {
            requestFocus();
        }

        Scene scene = new Scene(root, ow, oh);
        overviewStage.setScene(scene);
        overviewStage.setTitle("Diagram Overview");
        overviewStage.setAlwaysOnTop(true);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double margin = 80;
        overviewStage.setX(screen.getMaxX() - ow - margin);
        overviewStage.setY(screen.getMinY() + margin);
        overviewStage.setResizable(false);

        overviewStage.show();

        drawOverview();
        if (paintframe.scroll != null) {
            paintframe.scroll.hvalueProperty().addListener((o, a, b) -> drawOverview());
            paintframe.scroll.vvalueProperty().addListener((o, a, b) -> drawOverview());
        }

        overviewStage.setOnCloseRequest(e -> closeDiagramOverview());
    }

    public void drawOverview() {
        if (overviewStage == null || !overviewStage.isShowing() || paintframe.scroll == null) {
            return;
        }

        GraphicsContext gc = getGraphicsContext2D();
        double ow = getWidth();
        double oh = getHeight();
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
}
