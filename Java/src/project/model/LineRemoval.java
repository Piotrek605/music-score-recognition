package project.model;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Performs line removal on the image.
 */
public class LineRemoval {
    // how many extra rows/columns to remove due to rotation
    private int lineDistortion = 2;

    private int staveSpaceWidth;

    private ArrayList<BoundingBox> staveLines;
    private ArrayList<ArrayList<BoundingBox>> staves;
    private ArrayList<BoundingBox> barLines;
    private ArrayList<BoundingBox> verticalLines;

    /**
     * Create a new instance.
     */
    public LineRemoval() {
        staveLines = new ArrayList<>();
        staves = new ArrayList<>();
        barLines = new ArrayList<>();
        verticalLines = new ArrayList<>();
    }

    /**
     * Remove the stave.
     * @param image The image
     * @param histogram The horizontal projection of the image
     * @param staveLineThreshold The threshold used to find stave lines in the projection
     */
    public void removeStave(BufferedImage image, int[] histogram, int staveLineThreshold) {
        for(int i=lineDistortion; i<histogram.length-lineDistortion; i++) {
            if(histogram[i] > staveLineThreshold) {
                // find the bottom
                int bottom = i+1;
                while(histogram[bottom] > staveLineThreshold) {
                    bottom++;
                }
                bottom--;

                int beginning = -1;
                int pixelsRemoved = 0;
                int end = -1;

                for(int j=0; j<image.getWidth(); j++) {
                    // check if the pixel is next to a symbol
                    boolean nextToSymbol = false;
                    if(image.getRGB(j, i-1) == ColorOperations.black() &&
                            image.getRGB(j, bottom+1) == ColorOperations.black()) {
                        nextToSymbol = true;
                    } else if(image.getRGB(j, i-lineDistortion) == ColorOperations.black() ||
                            image.getRGB(j, bottom+lineDistortion) == ColorOperations.black()) {
                        nextToSymbol = true;
                    }

                    if(!nextToSymbol) {
                        pixelsRemoved++;

                        // remove the line
                        for(int k=i-lineDistortion; k<=bottom+lineDistortion; k++) {
                            if(image.getRGB(j, k) == ColorOperations.black()) {
                                image.setRGB(j, k, ColorOperations.white());

                                if(beginning < 0 && pixelsRemoved > 5) {
                                    beginning = j;
                                }
                                end = j;
                            }
                        }
                    }
                }

                staveLines.add(new BoundingBox(beginning, i, end, bottom));
                i = bottom;
            }
        }
        System.out.println("Number of stave lines: " + staveLines.size());

        if(staveLines.size() % 5 != 0) {
            System.err.println("Number of stave lines must be divisible by 5!");
            return;
        }

        // find the average distance between two adjacent stave lines
        int sumOfDistances = 0;
        int timesAdded = 0;
        for(int i=4; i<staveLines.size(); i += 5) {
            sumOfDistances += staveLines.get(i).getYPosition() - staveLines.get(i-4).getYPosition();
            timesAdded++;
        }
        staveSpaceWidth = sumOfDistances/timesAdded/4;
        System.out.println("Average distance between two adjacent stave lines: " + staveSpaceWidth);

        // define staves
        for(int i=0; i<staveLines.size(); i+=5) {
            ArrayList<BoundingBox> stave = new ArrayList<>();
            int xStart = staveLines.get(i).xStart;

            for(int j=0; j<5; j++) {
                stave.add(staveLines.get(i+j));
                if(staveLines.get(i+j).xStart > xStart) {
                    xStart = staveLines.get(i+j).xStart;
                }
            }
            staves.add(stave);

            for(int j=0; j<5; j++) {
                staveLines.get(i+j).xStart = xStart;
            }
        }
        System.out.println("Number of staves: " + staves.size());
    }

    /**
     * Get the staves.
     * @return The staves
     */
    public ArrayList<ArrayList<BoundingBox>> getStaves() {
        return staves;
    }

    /**
     * Get the average stave space width.
     * @return The average stave space width
     */
    public int getStaveSpaceWidth() {
        return staveSpaceWidth;
    }

    /**
     * Patch components broken by line removal.
     * @param originalImage The original image
     * @param image The image to be updated
     */
    public void patch(BufferedImage originalImage, BufferedImage image) {
        for(int i=0; i<staveLines.size()-1; i++) {
            int lastPathTop = -1;
            int lastPathBottom = -1;
            int lastLeftMoves = -1;
            for(int j=staveLines.get(i).xStart+5; j<=staveLines.get(i).xEnd-5; j++) {
                int x = j;
                int y = staveLines.get(i).yEnd+1;
                if(image.getRGB(x, y) == ColorOperations.black()) {
                    // find a path between this stave line and the next
                    boolean pathFound = false;
                    int leftMoves = 0;

                    for(int k=0; k<staveSpaceWidth*2; k++) {
                        boolean pixelFound = false;

                        int[] values = {0, -1, 1};
                        for(int l : values) {
                            if(originalImage.getRGB(x+l, y+1) == ColorOperations.black()) {
                                x += l;
                                y++;

                                pixelFound = true;
                                if(l == -1) {
                                    leftMoves++;
                                }
                                break;
                            }
                        }

                        if(!pixelFound) {
                            int[] values2 = {-1, 1};
                            for(int l : values2) {
                                if(originalImage.getRGB(x+l, y) == ColorOperations.black()) {
                                    x += l;
                                    pixelFound = true;
                                    if(l == -1) {
                                        leftMoves++;
                                    }
                                    break;
                                }
                            }
                        }

                        if(!pixelFound) {
                            break;
                        }

                        if(y == staveLines.get(i+1).yStart-1) {
                            pathFound = true;
                            break;
                        }
                    }

                    if(pathFound) {
                        // don't patch if there is a straight line moving out upwards
                        // from the end of the path (it could be a flat)
                        // but do remove if the line is a stem

                        boolean straightLine = true;
                        for(int l=y; l>staveLines.get(i).yEnd; l--) {
                            if(image.getRGB(x, l) == ColorOperations.white()) {
                                straightLine = false;
                                break;
                            }
                        }
                        if(straightLine) {
                            boolean stemLine = false;
                            for(BoundingBox verticalLine : verticalLines) {
                                if(Math.abs(verticalLine.getXPosition() - x) < staveSpaceWidth/5 &&
                                        y >= verticalLine.yStart && y <= verticalLine.yEnd &&
                                        verticalLine.getHeight() > 3*staveSpaceWidth) {
                                    stemLine = true;
                                }
                            }
                            if(!stemLine) {
                                continue;
                            }
                        }

                        // patch up
                        if(lastPathTop >= 0 && lastLeftMoves > staveSpaceWidth/4 &&
                                j-lastPathTop > 1 && j-lastPathTop < staveSpaceWidth*1.5 &&
                                x-lastPathBottom > 1 && x-lastPathBottom < staveSpaceWidth*1.2) {
                            for(int k=lastPathTop; k<=j; k++) {
                                for(int l=staveLines.get(i).yStart-1; l<=staveLines.get(i).yEnd+1; l++) {
                                    image.setRGB(k, l, ColorOperations.black());
                                }
                            }
                            for(int k=lastPathBottom; k<=x; k++) {
                                for(int l=staveLines.get(i+1).yStart-1; l<=staveLines.get(i+1).yEnd+1; l++) {
                                    image.setRGB(k, l, ColorOperations.black());
                                }
                            }
                        }

                        lastPathTop = j;
                        lastPathBottom = x;
                        lastLeftMoves = leftMoves;
                    }
                }
            }
        }
    }

    /**
     * Remove vertical lines.
     * @param image The image where lines will be removed
     * @param imageWithBars The image where recognized bar lines will be marked
     */
    public void removeVerticalLines(BufferedImage image, BufferedImage imageWithBars) {
        int minHeight = (int)(2.2*staveSpaceWidth);
        int maxWidth = staveSpaceWidth/3;

        for(int j=0; j<image.getHeight(); j++) {
            for(int i=0; i<image.getWidth(); i++) {
                if(image.getRGB(i, j) == ColorOperations.black()) {
                    // find width
                    int width = lineDistortion;
                    while(image.getRGB(i+width, j) == ColorOperations.black()) {
                        width++;
                    }
                    if(width > maxWidth) {
                        continue;
                    }

                    // find height,
                    // allow a number of pixels to be off to the side
                    // and a gap of a number of pixels,
                    // that way distorted lines are also found
                    int height = 0;
                    int pixelsOff = 0;
                    int pixelGap = 0;
                    while(pixelsOff < staveSpaceWidth && pixelGap < staveSpaceWidth/6) {
                        height++;

                        boolean atLeastOnePixel = false;
                        for(int k=i; k<i+width; k++) {
                            if(image.getRGB(k, j+height) == ColorOperations.black()) {
                                atLeastOnePixel = true;
                                pixelsOff = 0;
                                pixelGap = 0;
                            }
                        }

                        if(!atLeastOnePixel) {
                            for(int k=i-lineDistortion; k<i; k++) {
                                if(image.getRGB(k, j+height) == ColorOperations.black()) {
                                    atLeastOnePixel = true;
                                }
                            }
                            for(int k=i+width; k<i+width-1+lineDistortion; k++) {
                                if(image.getRGB(k, j+height) == ColorOperations.black()) {
                                    atLeastOnePixel = true;
                                }
                            }
                        }

                        if(atLeastOnePixel) {
                            pixelsOff++;
                        } else {
                            pixelGap++;
                        }
                    }
                    height--;
                    if(height < minHeight) {
                        continue;
                    }

                    // find extra width to the left,
                    // there have to be at least some fraction of the width of pixels
                    // in the column for it to be considered part of the line
                    int widthLeft = 0;
                    int noPixels = height;
                    while(noPixels > 0.6*height) {
                        noPixels = 0;
                        widthLeft++;
                        for(int k=j; k<j+height; k++) {
                            if(image.getRGB(i-widthLeft, k) == ColorOperations.black()) {
                                noPixels++;
                            }
                        }
                    }
                    widthLeft--;

                    // find extra width to the right,
                    // there have to be at least some fraction of the width of pixels
                    // in the column for it to be considered part of the line
                    int widthRight = 0;
                    noPixels = height;
                    while(noPixels > 0.6*height) {
                        noPixels = 0;
                        widthRight++;
                        for(int k=j; k<j+height; k++) {
                            if(image.getRGB(i+widthRight, k) == ColorOperations.black()) {
                                noPixels++;
                            }
                        }
                    }
                    widthRight--;

                    // don't process the line if dimensions not within bounds
                    if(width + widthLeft + widthRight > maxWidth) {
                        continue;
                    }

                    verticalLines.add(new BoundingBox(i-widthLeft, j, i+width+widthRight-1, j+height-1));

                    // check if a bar line
                    int tolerance = 3;
                    boolean isBarLine = false;
                    // check if at the beginning of a stave
                    for(ArrayList<BoundingBox> stave : staves) {
                        if(Math.abs(j-stave.get(0).getYPosition()) <= tolerance) {
                            isBarLine = true;
                            break;
                        }
                    }
                    // check if at the end of a stave
                    if(isBarLine) {
                        isBarLine = false;
                        for(ArrayList<BoundingBox> stave : staves) {
                            if(Math.abs(j+height-1-stave.get(4).getYPosition()) <= tolerance) {
                                isBarLine = true;
                                break;
                            }
                        }
                    }
                    // check if connected to something
                    if(isBarLine) {
                        int threshold = staveSpaceWidth/2;
                        int noRows = 0;

                        for(int l=j-staveSpaceWidth; l<j+height+staveSpaceWidth; l++) {
                            int pixels = 0;
                            int gap = 0;

                            for(int m=i-widthLeft-1-threshold*2; m<i-widthLeft; m++) {
                                if(image.getRGB(m, l) == ColorOperations.black()) {
                                    pixels++;
                                } else {
                                    gap++;
                                }
                                if(gap > lineDistortion) {
                                    break;
                                }
                            }

                            gap = 0;
                            for(int m=i+width+widthRight; m<i+width+widthRight+threshold*2; m++) {
                                if(image.getRGB(m, l) == ColorOperations.black()) {
                                    pixels++;
                                } else {
                                    gap++;
                                }
                                if(gap > lineDistortion) {
                                    break;
                                }
                            }

                            // check if enough pixels found in the row
                            // for it to be considered part of another component
                            if(pixels > threshold) {
                                noRows++;
                            }
                        }

                        // check the height of the component,
                        // if it's big enough, the bar line is not a bar line,
                        // it could be a stem attached to a note head
                        if(noRows > staveSpaceWidth/2) {
                            isBarLine = false;
                        }
                    }

                    if(isBarLine) {
                        BoundingBox barLine = new BoundingBox(i, j, i+width-1, j+height-1);

                        // make sure it's added before next bar lines
                        int index=barLines.size()-1;
                        for(; index>=0; index--) {
                            if(barLine.yStart - barLines.get(index).yStart > staveSpaceWidth) {
                                break;
                            }
                            if(barLines.get(index).xEnd < barLine.xStart) {
                                break;
                            }
                        }
                        barLines.add(index+1, barLine);

                        // mark the recognized bar line
                        for (int l = j; l < j + height; l++) {
                            for (int k = i-lineDistortion; k < i+width-1+lineDistortion; k++) {
                                imageWithBars.setRGB(k, l, ColorOperations.colorToRgb(0, 0, 255));
                            }
                        }
                    } else {
                        // don't process the line if too far from staves
                        BoundingBox closestStaveLine = staveLines.get(0);
                        for(BoundingBox staveLine : staveLines) {
                            if(Math.abs(j-staveLine.getYPosition()) < Math.abs(j-closestStaveLine.getYPosition())) {
                                closestStaveLine = staveLine;
                            }
                        }
                        if(i < closestStaveLine.xStart + 4*staveSpaceWidth) {
                            continue;
                        }
                    }

                    // remove the line
                    int consecutiveRemoves = 0;
                    for(int l=j; l<j+height; l++) {
                        // check if the pixel is next to a component
                        boolean nextToSymbol = false;
                        if(image.getRGB(i-widthLeft-1, l) == ColorOperations.black() &&
                                image.getRGB(i-widthLeft-lineDistortion, l) == ColorOperations.black()) {
                            nextToSymbol = true;
                        }
                        if(image.getRGB(i+width+widthRight, l) == ColorOperations.black() &&
                                image.getRGB(i+width+widthRight-1+lineDistortion, l) == ColorOperations.black()) {
                            nextToSymbol = true;
                        }

                        if(!nextToSymbol) {
                            for(int k=i-widthLeft-lineDistortion; k<=i+width+widthRight-1+lineDistortion; k++) {
                                image.setRGB(k, l, ColorOperations.white());
                            }
                            consecutiveRemoves++;
                        } else {
                            // if a small number of consecutive pixels have been removed,
                            // add them back unless the line is a stem
                            if(consecutiveRemoves < staveSpaceWidth/4 ||
                                    (consecutiveRemoves < staveSpaceWidth/2 && height < 3*staveSpaceWidth)) {
                                for(int m=l-1; m >= l-consecutiveRemoves; m--) {
                                    for(int k=i-widthLeft; k<=i+width+widthRight-1; k++) {
                                        image.setRGB(k, m, ColorOperations.black());
                                    }
                                }
                            }
                            consecutiveRemoves = 0;
                        }
                    }

                    i += width + widthRight;
                }
            }
        }

        System.out.println("Number of bar lines: " + barLines.size());
    }

    /**
     * Get the bar lines.
     * @return The bar lines
     */
    public ArrayList<BoundingBox> getBarLines() {
        return barLines;
    }

    /**
     * Get all the vertical lines.
     * @return The vertical lines
     */
    public ArrayList<BoundingBox> getVerticalLines() {
        return verticalLines;
    }
}
