package project.model;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Performs Connected Component Analysis.
 */
public class CCA {
    private int[][] components;
    ArrayList<Set<Integer>> labelEquivalence;
    Map<Integer, BoundingBox> labelToBoundingBox;

    /**
     * Label components.
     * @param image The image
     */
    public void collectComponents(BufferedImage image) {
        // initialize the component matrix
        components = new int[image.getWidth()][image.getHeight()];
        for(int i=0; i<components.length; i++) {
            for(int j=0; j<components[i].length; j++) {
                components[i][j] = 0;
            }
        }

        // label the components
        int label = 1;
        labelEquivalence = new ArrayList<>();
        for(int j=1; j<image.getHeight()-1; j++) {
            for(int i=1; i<image.getWidth()-1; i++) {
                if(image.getRGB(i, j) == ColorOperations.black()) {
                    int smallestLabel = label;
                    boolean newLabelNeeded = true;

                    // check if connected and find the smallest label in the mask
                    for(int k=-1; k<=1; k++) {
                        for(int l=-1; l<=0; l++) {
                            if(components[i+k][j+l] != 0) {
                                newLabelNeeded = false;
                                if(components[i+k][j+l] < smallestLabel) {
                                    smallestLabel = components[i + k][j + l];
                                }
                            }
                        }
                    }
                    components[i][j] = smallestLabel;

                    if(!newLabelNeeded) {
                        // update the label equivalence data for every label in the mask
                        for(int k=-1; k<=1; k++) {
                            for(int l=-1; l<=0; l++) {
                                if(components[i+k][j+l] != 0 && components[i+k][j+l] != smallestLabel) {
                                    Set<Integer> setWithThisLabel = null;
                                    for(int m=0; m<labelEquivalence.size(); m++) {
                                        Set<Integer> labelSet = labelEquivalence.get(m);
                                        if(labelSet.contains(components[i+k][j+l]) || labelSet.contains(smallestLabel)) {
                                            if(setWithThisLabel == null) {
                                                // first set with this label found, add the other label
                                                setWithThisLabel = labelSet;
                                                labelSet.add(components[i + k][j + l]);
                                                labelSet.add(smallestLabel);
                                            } else {
                                                // there's already a set with this label, move the labels and remove this set
                                                for(int otherLabel : labelSet) {
                                                    setWithThisLabel.add(otherLabel);
                                                }
                                                labelEquivalence.remove(labelSet);
                                                m--;
                                            }
                                        }
                                    }
                                    if(setWithThisLabel == null) {
                                        // no set with this label exists, create a new one
                                        Set<Integer> labelSet = new HashSet<>();
                                        labelSet.add(components[i+k][j+l]);
                                        labelSet.add(smallestLabel);
                                        labelEquivalence.add(labelSet);
                                    }
                                    components[i + k][j + l] = smallestLabel;
                                }
                            }
                        }
                    } else {
                        label++;
                    }
                }
            }
        }

        // colour the components
        for(int i=0; i<components.length; i++) {
            for(int j=0; j<components[i].length; j++) {
                image.setRGB(i, j, ColorOperations.labelToRgb(components[i][j]));
            }
        }
    }

    /**
     * Resolve label equivalences.
     * @param image The image
     */
    public void resolveEquivalences(BufferedImage image) {
        Map<Integer, Integer> representativeLabels = new HashMap<>();
        for(Set<Integer> labelSet : labelEquivalence) {
            // find the smallest label in this set
            int smallestLabel = Integer.MAX_VALUE;
            for(int otherLabel : labelSet) {
                if(otherLabel < smallestLabel) {
                    smallestLabel = otherLabel;
                }
            }

            // set that other labels are equivalent to the smallest one
            for(int otherLabel : labelSet) {
                if(otherLabel != smallestLabel) {
                    representativeLabels.put(otherLabel, smallestLabel);
                }
            }
        }

        // replace all labels with their representative labels
        for(int i=0; i<components.length; i++) {
            for (int j=0; j<components[i].length; j++) {
                if (components[i][j] != 0) {
                    Integer representativeLabel = representativeLabels.get(components[i][j]);
                    if(representativeLabel != null) {
                        components[i][j] = representativeLabel;
                    }
                }
            }
        }

        // update the image
        for(int i=0; i<components.length; i++) {
            for(int j=0; j<components[i].length; j++) {
                image.setRGB(i, j, ColorOperations.labelToRgb(components[i][j]));
            }
        }
    }

    /**
     * Get the bounding boxes.
     * @return The bounding boxes.
     */
    public Map<Integer, BoundingBox> getBoundingBoxes() {
        // collect all labels
        Set<Integer> labels = new HashSet<>();
        for(int i=0; i<components.length; i++) {
            for(int j=0; j<components[i].length; j++) {
                if(components[i][j]>0) {
                    labels.add(components[i][j]);
                }
            }
        }

        // initialize bounding boxes
        labelToBoundingBox = new HashMap<>();
        for(Integer label : labels) {
            labelToBoundingBox.put(label, new BoundingBox(-1, -1, -1, -1, label));
        }

        // find min x and max x
        for(int i=0; i<components.length; i++) {
            for(int j=0; j<components[i].length; j++) {
                if(components[i][j]>0) {
                    if(labelToBoundingBox.get(components[i][j]).xStart < 0) {
                        labelToBoundingBox.get(components[i][j]).xStart = i;
                        labelToBoundingBox.get(components[i][j]).xEnd = i;
                    } else {
                        labelToBoundingBox.get(components[i][j]).xEnd = i;
                    }
                }
            }
        }

        // find min y and max y
        for(int j=0; j<components[0].length; j++) {
            for(int i=0; i<components.length; i++) {
                if(components[i][j]>0) {
                    if(labelToBoundingBox.get(components[i][j]).yStart < 0) {
                        labelToBoundingBox.get(components[i][j]).yStart = j;
                        labelToBoundingBox.get(components[i][j]).yEnd = j;
                    } else {
                        labelToBoundingBox.get(components[i][j]).yEnd = j;
                    }
                }
            }
        }

        return labelToBoundingBox;
    }

    /**
     * Draw a bounding boxes.
     * @param image The image
     * @param boundingBox The bounding box to be drawn
     * @param color The colour
     */
    public static void drawBoundingBox(BufferedImage image, BoundingBox boundingBox, int color) {
        for(int j=boundingBox.xStart; j<=boundingBox.xEnd; j++) {
            image.setRGB(j, boundingBox.yStart, color);
            image.setRGB(j, boundingBox.yEnd, color);
        }
        for(int j=boundingBox.yStart; j<=boundingBox.yEnd; j++) {
            image.setRGB(boundingBox.xStart, j, color);
            image.setRGB(boundingBox.xEnd, j, color);
        }
    }

    /**
     * Draw all bounding boxes.
     * @param image The image
     */
    public void drawBoundingBoxes(BufferedImage image) {
        // draw bounding boxes
        for(BoundingBox boundingBox : labelToBoundingBox.values()) {
            drawBoundingBox(image, boundingBox, ColorOperations.white());
        }
    }

    /**
     * Get the component matrix.
     * @return The component matrix
     */
    public int[][] getComponents() {
        return components;
    }
}
