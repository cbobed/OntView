package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class VisConnectorDashed extends VisConnectorIsA {

	double[] dash = { 4.0f };
	public static Color color = Color.BLACK;
	static double frac = 0.01;

	public VisConnectorDashed(Shape par_from, Shape par_to) {
		super(par_from, par_to);
	}

	public void draw(GraphicsContext g){
        if (visible){
			// Save the previous stroke and fill settings
			Color prevColor = (Color) g.getFill();
			Color prevStroke = (Color) g.getStroke();
			double prevLineWidth = g.getLineWidth();
			StrokeLineCap prevLineCap = g.getLineCap();
			double prevDashOffset = g.getLineDashOffset();
			double prevMiterLimit = g.getMiterLimit();
			double[] prevDashes = g.getLineDashes();


			fromPoint = from.getConnectionPoint(new Point2D(from.getPosX(),from.getPosY()),false);
			toPoint   = to.getConnectionPoint(new Point2D(from.getPosX(),from.getPosY()),true);

			// Set the new stroke properties
			g.setStroke(color);
			g.setFill(color);
			g.setLineWidth(width);
			g.setLineCap(StrokeLineCap.BUTT);
			g.setLineJoin(StrokeLineJoin.MITER);
			g.setMiterLimit(5.0f);
			g.setLineDashes(dash);
			g.setLineDashOffset(0.0);

			drawCurve(g, VisConstants.NURB);

			// Restore previous settings
			g.setFill(prevColor);
			g.setStroke(prevStroke);
			g.setLineWidth(prevLineWidth);
			g.setLineCap(prevLineCap);
			g.setLineJoin(StrokeLineJoin.MITER);
			g.setMiterLimit(prevMiterLimit);
			g.setLineDashOffset(prevDashOffset);
			g.setLineDashes(prevDashes);
		}
	}

}
