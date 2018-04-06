package project.model;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a bounding box surrounding a component.
 */
public class BoundingBox {
    public int xStart;
    public int yStart;
    public int xEnd;
    public int yEnd;

    // the label assigned by CCA
    public int label;

    // subimage cache for speed-up
    private Map<BufferedImage, BufferedImage> imageCache;

    /**
     * Create a new bounding box.
     * @param xStart The left x coordinate
     * @param yStart The top y coordinate
     * @param xEnd The right x coordinate
     * @param yEnd The bottom y coordinate
     */
    public BoundingBox(int xStart, int yStart, int xEnd, int yEnd) {
        this.xStart = xStart;
        this.yStart = yStart;
        this.xEnd = xEnd;
        this.yEnd = yEnd;
        imageCache = new HashMap<>();
    }

    /**
     * Create a new bounding box
     * @param xStart The left x coordinate
     * @param yStart The top y coordinate
     * @param xEnd The right x coordinate
     * @param yEnd The bottom y coordinate
     * @param label The label
     */
    public BoundingBox(int xStart, int yStart, int xEnd, int yEnd, int label) {
        this(xStart, yStart, xEnd, yEnd);
        this.label = label;
    }

    /**
     * Get the width.
     * @return The width
     */
    public int getWidth() {
        return xEnd - xStart + 1;
    }

    /**
     * Get the height.
     * @return The height
     */
    public int getHeight() {
        return yEnd - yStart + 1;
    }

    /**
     * Get the x position.
     * @return The x position
     */
    public int getXPosition() {
        return (xStart+xEnd)/2;
    }

    /**
     * Get the y position.
     * @return The y position
     */
    public int getYPosition() {
        return (yStart+yEnd)/2;
    }

    /**
     * Get the subimage
     * @param image The image to extract the subimage from
     * @param components The label matrix
     * @return The subimage
     */
    public BufferedImage getImage(BufferedImage image, int[][] components) {
        if(imageCache.containsKey(image)) {
            return imageCache.get(image);
        }

        BufferedImage subimage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for(int i=0; i<subimage.getWidth(); i++) {
            for(int j=0; j<subimage.getHeight(); j++) {
                if(components[i+xStart][j+yStart] == label) {
                    subimage.setRGB(i, j, ColorOperations.black());
                } else {
                    subimage.setRGB(i, j, ColorOperations.white());
                }
            }
        }
        imageCache.put(image, subimage);
        return subimage;
    }

    /**
     * Get the string representation.
     * @return The string representation
     */
    public String toString() {
        return "BoundingBox x:" + getXPosition() + " y:" + getYPosition() +
                " xStart:" + xStart + " yStart:" + yStart +
                " xEnd:" + xEnd + " yEnd:" + yEnd +
                " width:" + getWidth() + " height:" + getHeight();
    }
}
