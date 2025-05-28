package sid.OntView2.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageMerger {
    private static final Logger logger = LogManager.getLogger(ImageMerger.class);

    public static void mergeImages(String directoryPath, String outputPath) {
        try {
            File directory = new File(directoryPath);
            File[] files = directory.listFiles();
            if (files == null) {
                System.err.println("The directory does not exist or cannot be read: " + directoryPath);
                return;
            }

            // Mapping images into a "grid": row -> (column -> image)
            Map<Integer, Map<Integer, Image>> imageGrid = new HashMap<>();
            int maxRow = -1;
            int maxCol = -1;

            // Archive: i_{row}_{col}.png
            Pattern pattern = Pattern.compile("i_(\\d+)_(\\d+)\\.png");

            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    Matcher matcher = pattern.matcher(fileName);
                    if (matcher.matches()) {
                        int row = Integer.parseInt(matcher.group(1));
                        int col = Integer.parseInt(matcher.group(2));

                        maxRow = Math.max(maxRow, row);
                        maxCol = Math.max(maxCol, col);

                        Image img = new Image(file.toURI().toString());
                        // The outer map is keyed by the row number and the inner map by the column number
                        imageGrid.computeIfAbsent(row, k -> new HashMap<>()).put(col, img);
                    }
                }
            }

            int numRows = maxRow + 1;
            int numCols = maxCol + 1;

            // Calculate max width column and max height row
            double[] colWidths = new double[numCols];
            double[] rowHeights = new double[numRows];
            for (int r = 0; r < numRows; r++) {
                for (int c = 0; c < numCols; c++) {
                    Image img = (imageGrid.containsKey(r)) ? imageGrid.get(r).get(c) : null;
                    if (img != null) {
                        colWidths[c] = Math.max(colWidths[c], img.getWidth());
                        rowHeights[r] = Math.max(rowHeights[r], img.getHeight());
                    }
                }
            }

            // Total dimension
            int totalWidth = 0;
            for (double w : colWidths) {
                totalWidth += (int) Math.ceil(w);
            }
            int totalHeight = 0;
            for (double h : rowHeights) {
                totalHeight += (int) Math.ceil(h);
            }

            WritableImage combinedImage = new WritableImage(totalWidth, totalHeight);

            // Draw each image at its calculated position
            int yOffset = 0;
            for (int r = 0; r < numRows; r++) {
                int xOffset = 0;
                for (int c = 0; c < numCols; c++) {
                    // Retrieve the image
                    Image img = (imageGrid.containsKey(r)) ? imageGrid.get(r).get(c) : null;
                    if (img != null) {
                        for (int y = 0; y < (int) img.getHeight(); y++) {
                            for (int x = 0; x < (int) img.getWidth(); x++) {
                                combinedImage.getPixelWriter().setColor(xOffset + x, yOffset + y,
                                    img.getPixelReader().getColor(x, y));
                            }
                        }
                    }
                    xOffset += (int) Math.ceil(colWidths[c]);
                }
                yOffset += (int) Math.ceil(rowHeights[r]);
            }

            ImageIO.write(SwingFXUtils.fromFXImage(combinedImage, null), "png", new File(outputPath));
            logger.debug("Merge done");

            deleteDirectory(directory);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!dir.delete()) {
            System.err.println("Could not delete : " + dir.getAbsolutePath());
        }
    }

}
