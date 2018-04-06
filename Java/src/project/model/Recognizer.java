package project.model;

import project.model.units.Measure;
import project.model.units.Note;
import project.utils.UnsupportedImageTypeException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Performs the symbol recognition.
 */
public class Recognizer {
    private ArrayList<ArrayList<BoundingBox>> staves;
    private int staveSpaceWidth;
    private int tolerance;
    private int stavesInSystem;

    private Map<ArrayList<BoundingBox>, String> whichClef;
    private Map<Integer, Integer> pitchAlterations;
    private Map<Character, Integer> globalPitchAlterations;

    private ArrayList<BoundingBox> barLines;
    private ArrayList<BoundingBox> verticalLines;
    private ArrayList<Measure> measures;

    private int[][] components;
    private Collection<BoundingBox> boundingBoxes;
    private TrainingSet trainingSet;

    // lists of bounding boxes of components
    private ArrayList<BoundingBox> quaverTails;
    private ArrayList<BoundingBox> dots;
    private ArrayList<BoundingBox> beams;
    private ArrayList<BoundingBox> hooks;
    private ArrayList<BoundingBox> sharpBeams;
    private ArrayList<BoundingBox> sharps;
    private ArrayList<BoundingBox> flats;
    private ArrayList<BoundingBox> naturalBeams;
    private ArrayList<BoundingBox> semibreveHalves;
    private ArrayList<BoundingBox> ties;

    private static double ratioOfPixelsThreshold = 0.8;

    /**
     * Create a new instance.
     * @param staves The staves
     * @param barLines The bar lines
     * @param verticalLines The vertical lines
     * @param staveSpaceWidth The average stave space width
     * @param boundingBoxes The bounding boxes of the components to be recognized
     * @param components The component matrix
     */
    public Recognizer(ArrayList<ArrayList<BoundingBox>> staves, ArrayList<BoundingBox> barLines,
                      ArrayList<BoundingBox> verticalLines, int staveSpaceWidth, Collection<BoundingBox> boundingBoxes,
                      int[][] components) {
        this.staves = staves;
        this.barLines = barLines;
        this.verticalLines = verticalLines;
        this.staveSpaceWidth = staveSpaceWidth;
        this.boundingBoxes = boundingBoxes;
        this.components = components;
        tolerance = staveSpaceWidth/5;

        try {
            trainingSet = new TrainingSet();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedImageTypeException e) {
            e.printStackTrace();
        }

        measures = new ArrayList<>();
        quaverTails = new ArrayList<>();
        dots = new ArrayList<>();
        beams = new ArrayList<>();
        hooks = new ArrayList<>();
        sharpBeams = new ArrayList<>();
        sharps = new ArrayList<>();
        flats = new ArrayList<>();
        naturalBeams = new ArrayList<>();
        semibreveHalves = new ArrayList<>();
        ties = new ArrayList<>();
        whichClef = new HashMap<>();
        pitchAlterations = new HashMap<>();
        globalPitchAlterations = new HashMap<>();

        for(int i=0; i<staves.size(); i++) {
            if(Math.abs(barLines.get(0).yEnd - staves.get(i).get(4).yEnd) < staveSpaceWidth/2) {
                stavesInSystem = i+1;
                break;
            }
        }
        System.out.println("Number of staves in a system: " + stavesInSystem);
    }

    /**
     * Get a Note object that corresponds to the given parameters.
     * @param yPosition The y position
     * @param stave The stave
     * @param tieType The tie type
     * @param voice The voice number
     * @param type The duration
     * @param dotted True if dotted
     * @param staff The staff number
     * @param beams The beams
     * @param accidental The accidental
     * @param chord True if chord
     * @return The note
     */
    private Note getNote(int yPosition, ArrayList<BoundingBox> stave, String tieType,
                         int voice, String type, boolean dotted, int staff, ArrayList<String> beams,
                         String accidental, boolean chord) {
        // determine the stave-relative position based on the y coordinate

        int position=1;
        boolean positionFound = false;

        // check above the stave
        for(int i=0; i<=10; i++) {
            int consideredYPosition = stave.get(0).getYPosition()-i*staveSpaceWidth/2;
            if(Math.abs(yPosition - consideredYPosition) < staveSpaceWidth/4) {
                positionFound = true;
                break;
            }
            position--;
        }
        if(!positionFound) {
            // check within the stave
            position = 2;
            for(int i=0; i<4; i++) {
                int consideredYPosition = (stave.get(i).getYPosition() + stave.get(i+1).getYPosition())/2;
                if(Math.abs(yPosition - consideredYPosition) < staveSpaceWidth/4) {
                    positionFound = true;
                    break;
                }
                position++;

                consideredYPosition = stave.get(i+1).getYPosition();
                if(Math.abs(yPosition - consideredYPosition) < staveSpaceWidth/4) {
                    positionFound = true;
                    break;
                }
                position++;
            }
        }
        if(!positionFound) {
            // check below the stave
            for(int i=1; i<=10; i++) {
                int consideredYPosition = stave.get(4).getYPosition()+i*staveSpaceWidth/2;
                if(Math.abs(yPosition - consideredYPosition) < staveSpaceWidth/4) {
                    positionFound = true;
                    break;
                }
                position++;
            }
        }

        // update the pitch based on the clef
        if(whichClef.containsKey(stave)) {
            if(whichClef.get(stave).equals("bass")) {
                position += 12;
            }
        }

        // determine the octave based on the position
        int octave;
        if(position <= -3) {
            octave = 6;
        } else if(position <= 4) {
            octave = 5;
        } else if(position <= 11) {
            octave = 4;
        } else if(position <= 18){
            octave = 3;
        } else if(position <= 25) {
            octave = 2;
        } else {
            octave = 1;
        }

        // determine the pitch based on the position
        char step = '0';
        switch((position+700) % 7) {
            case 4:
                step = 'C';
                break;
            case 3:
                step = 'D';
                break;
            case 2:
                step = 'E';
                break;
            case 1:
                step = 'F';
                break;
            case 0:
                step = 'G';
                break;
            case 6:
                step = 'A';
                break;
            case 5:
                step = 'B';
                break;
        }

        // update the pitch based on the accidentals and the key signature
        int alteration = 0;
        if(globalPitchAlterations.containsKey(step)) {
            alteration = globalPitchAlterations.get(step);
        }
        if(accidental != null) {
            switch(accidental) {
                case "sharp":
                    alteration = 1;
                    break;
                case "flat":
                    alteration = -1;
                    break;
                case "natural":
                    alteration = 0;
            }
            pitchAlterations.put(position, alteration);
        } else if(pitchAlterations.containsKey(position)) {
            alteration = pitchAlterations.get(position);
        }

        return new Note(step, alteration, octave, tieType, voice, type, dotted, staff, beams, accidental, chord);
    }

    /**
     * Count peaks in the image's projection.
     * @param symbol The image
     * @param dimension The dimension
     * @return
     */
    private int countPeaks(BufferedImage symbol, char dimension) {
        int firstSize;
        int secondSize;
        switch(dimension) {
            case 'x':
                firstSize = symbol.getHeight();
                secondSize = symbol.getWidth();
                break;
            case 'y':
                firstSize = symbol.getWidth();
                secondSize = symbol.getHeight();
                break;
            default:
                return 0;
        }

        int[] projection = Model.project(symbol, dimension);

        // count peaks
        int peaks = 0;
        int lastPeak = -tolerance;
        double threshold = 0.8;
        for(int x=0; x<firstSize; x++) {
            if(projection[x] > threshold*secondSize) {
                if(x == 0) {
                    peaks++;
                    lastPeak = x;
                } else if(projection[x-1] <= threshold*secondSize &&
                        projection[x] > threshold*secondSize &&
                        x - lastPeak > tolerance) {
                    peaks++;
                    lastPeak = x;
                }
            }
        }

        return peaks;
    }

    /**
     * Find the second beam for a given beam.
     * @param image The image
     * @param beamSubimage The image of the beam
     * @param beam The bounding box of the beam
     * @param stem The bounding box of the stem
     * @return The type of the second beam
     */
    private String findSecondBeam(BufferedImage image, BufferedImage beamSubimage, BoundingBox beam, BoundingBox stem) {
        int[] projection = Model.project(beamSubimage, 'y');
        int doubleBeamLowerThreshold = staveSpaceWidth - staveSpaceWidth/3;
        String secondBeamType = null;
        int threshold = 4*staveSpaceWidth/5;

        // check if double beam left hook
        int goodValues = 0;
        for(int n = stem.getXPosition() - beam.xStart;
            n > stem.getXPosition() - 1.5*staveSpaceWidth - beam.xStart && n >= 0;
            n--) {
            if(n >= projection.length) {
                n = projection.length-1;
            }
            if(projection[n] > doubleBeamLowerThreshold) {
                goodValues++;
                for(int i = beam.yStart; i<=beam.yEnd; i++) {
                    if(i % 5 == 0) {
                        image.setRGB(n + beam.xStart, i, ColorOperations.white());
                    }
                }
            }
        }
        if(goodValues > threshold) {
            secondBeamType = "backward hook";

            // check if double beam end here
            goodValues = 0;
            for(int n = stem.getXPosition() - staveSpaceWidth - beam.xStart;
                n > stem.getXPosition() - 2.5*staveSpaceWidth - beam.xStart && n >= 0;
                n--) {
                if(n >= projection.length) {
                    n = projection.length-1;
                }
                if(projection[n] > doubleBeamLowerThreshold) {
                    goodValues++;
                    for(int i = beam.yStart; i<=beam.yEnd; i++) {
                        if(i % 5 == 0) {
                            image.setRGB(n + beam.xStart, i, ColorOperations.white());
                        }
                    }
                }
            }
            if(goodValues > threshold) {
                secondBeamType = "end";
            }
        }

        // check if double beam right hook or continue
        goodValues = 0;
        for(int n = stem.getXPosition() - beam.xStart;
            n < stem.getXPosition() + 1.5*staveSpaceWidth - beam.xStart && n < projection.length;
            n++) {
            if(n < 0) {
                continue;
            }
            if(projection[n] > doubleBeamLowerThreshold) {
                goodValues++;
                for(int i = beam.yStart; i<=beam.yEnd; i++) {
                    if(i % 5 == 0) {
                        image.setRGB(n + beam.xStart, i, ColorOperations.white());
                    }
                }
            }
        }
        if(goodValues > threshold) {
            if(secondBeamType != null) {
                secondBeamType = "continue";
            } else {
                secondBeamType = "forward hook";

                // check if double beam start here
                goodValues = 0;
                for(int n = stem.getXPosition() + staveSpaceWidth - beam.xStart;
                    n < stem.getXPosition() + 2.5*staveSpaceWidth - beam.xStart && n < projection.length;
                    n++) {
                    if(n < 0) {
                        continue;
                    }
                    if(projection[n] > doubleBeamLowerThreshold) {
                        goodValues++;
                        for(int i = beam.yStart; i<=beam.yEnd; i++) {
                            if(i % 5 == 0) {
                                image.setRGB(n + beam.xStart, i, ColorOperations.white());
                            }
                        }
                    }
                }
                if(goodValues > threshold) {
                    secondBeamType = "begin";
                }
            }
        }

        return secondBeamType;
    }

    /**
     * Check if a component has a beam shape.
     * @param originalImage The original image
     * @param boundingBox The component
     * @return True if the component has a beam shape
     */
    private boolean isBeam(BufferedImage originalImage, BoundingBox boundingBox) {
        // count number of lines going from left to right for every slope
        Map<Double, Integer> noLines = new HashMap<>();
        for(int i=0; i<boundingBox.getHeight(); i++) {
            for(int j=0; j<boundingBox.getHeight(); j++) {
                int x1 = 0;
                int y1 = i;

                int x2 = boundingBox.getWidth();
                int y2 = j;

                // line parameters
                double a = ((double)(y1-y2))/(x1-x2);
                double b = y1 - a * (double)x1;

                // count the number of pixels on that line
                int noPixels = 0;
                for(int k=0; k<boundingBox.getWidth(); k++) {
                    if(boundingBox.getImage(originalImage, components).getRGB(k, (int)(a*(double)k+b)) == ColorOperations.black()) {
                        noPixels++;
                    }
                }
                if(noPixels > boundingBox.getWidth() - staveSpaceWidth/4) {
                    if(noLines.containsKey(a)) {
                        noLines.put(a, noLines.get(a)+1);
                    } else {
                        noLines.put(a, 1);
                    }
                }
            }
        }

        for(Integer x : noLines.values()) {
            if(x > boundingBox.getHeight()/3 || x > staveSpaceWidth/4) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if an accidental is part of the key signature.
     * @param symbol The accidental's bounding box
     * @param stave The stave
     * @param accidental The accidental's type
     * @param lastKeySignatureAccidentalX The x position of the last recognized key signature accidental
     * @param fifths The number of accidentals in the key signature currently recognized
     * @return True if the accidental is part of the key signature
     */
    private boolean isPartOfKeySignature(BoundingBox symbol, ArrayList<BoundingBox> stave, String accidental,
                                        int lastKeySignatureAccidentalX, int fifths) {
        switch(fifths) {
            case 0:
                // distance from the beginning
                if(symbol.getXPosition() - stave.get(0).xStart > 3.5*staveSpaceWidth &&
                        symbol.getXPosition() - stave.get(0).xStart < 5.5*staveSpaceWidth) {
                    // vertical position
                    switch(accidental) {
                        case "sharp":
                            if(Math.abs(symbol.getYPosition() - stave.get(0).getYPosition()) < tolerance) {
                                globalPitchAlterations.put('F', 1);
                                return true;
                            }
                            break;
                        case "flat":
                            if(Math.abs(symbol.getYPosition() - stave.get(2).getYPosition()) < tolerance) {
                                globalPitchAlterations.put('B', -1);
                                return true;
                            }
                            break;
                    }
                }
                break;
            case 1:
                // distance from the previous key signature sharp
                if(symbol.getXPosition() - lastKeySignatureAccidentalX < 1.5*staveSpaceWidth) {
                    switch(accidental) {
                        case "sharp":
                            // vertical position
                            if(Math.abs(symbol.getYPosition() -
                                    (stave.get(1).getYPosition()+stave.get(2).getYPosition())/2) < tolerance) {
                                globalPitchAlterations.put('C', 1);
                                return true;
                            }
                            break;
                    }
                }
                break;
        }

        return false;
    }

    /**
     * Check if a vertical line is a stem connected to the given component.
     * @param symbol The component
     * @param verticalLine The vertical line
     * @return True if the line is the component's stem
     */
    private boolean isStemFor(BoundingBox symbol, BoundingBox verticalLine) {
        if(Math.abs(verticalLine.getXPosition() - symbol.getXPosition()) < staveSpaceWidth &&
                ((symbol.getYPosition() < verticalLine.yEnd + staveSpaceWidth &&
                        symbol.getYPosition() > verticalLine.yStart) ||
                        (symbol.getYPosition() > verticalLine.yStart - staveSpaceWidth &&
                                symbol.getYPosition() < verticalLine.yEnd))) {
            return true;
        }
        return false;
    }

    /**
     * Perform symbol recognition
     * @param image The image to be updated
     * @param originalImage The binary image without CCA markings
     * @param veryOriginalImage The original image
     */
    public void recognize(BufferedImage image, BufferedImage originalImage, BufferedImage veryOriginalImage) {
        ArrayList<BoundingBox> boundingBoxesLeft = new ArrayList<>();
        for(BoundingBox boundingBox : boundingBoxes) {
            // dimensions
            if(boundingBox.getWidth() <= 1 || boundingBox.getHeight() <= 1) {
                // discard
                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 0, 0));
                continue;
            }

            // above or below the staves
            if(boundingBox.yEnd < staves.get(0).get(0).getYPosition() - 3*staveSpaceWidth ||
                    boundingBox.yStart > staves.get(staves.size()-1).get(4).getYPosition() + 3*staveSpaceWidth) {
                // discard
                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 0, 0));
                continue;
            }

            // dimensions
            if(boundingBox.getWidth() > 0.2*staveSpaceWidth && boundingBox.getWidth() < 0.7*staveSpaceWidth &&
                    boundingBox.getHeight() > 0.2*staveSpaceWidth && boundingBox.getHeight() < 0.7*staveSpaceWidth &&
                    Math.abs(boundingBox.getWidth() - boundingBox.getHeight()) < tolerance) {
                boolean typeDetermined = false;

                // distance from bar line
                for(BoundingBox barLine : barLines) {
                    if(boundingBox.xStart > barLine.xEnd &&
                            boundingBox.xStart - barLine.xEnd < staveSpaceWidth/2 &&
                            boundingBox.getXPosition() > barLine.xStart &&
                            boundingBox.getXPosition() < barLine.xEnd) {
                        // repetition dot
                        CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(150, 0, 0));
                        typeDetermined = true;
                        break;
                    }
                }
                if(typeDetermined) {
                    continue;
                }

                // dot
                dots.add(boundingBox);
                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 0, 255));
                continue;
            } else if(boundingBox.getWidth() > 2.2*staveSpaceWidth && boundingBox.getHeight() > staveSpaceWidth/3 &&
                    boundingBox.getWidth() > 1.3*boundingBox.getHeight()) {
                if(isBeam(originalImage, boundingBox)) {
                    // beam
                    beams.add(boundingBox);
                    CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 0, 0));
                    continue;
                }
            } else if(boundingBox.getWidth() > staveSpaceWidth && boundingBox.getWidth() < 1.5*staveSpaceWidth &&
                    boundingBox.getHeight() > staveSpaceWidth/3 && boundingBox.getHeight() < staveSpaceWidth) {
                // ratio of black to white pixels
                if(TrainingSet.ratioOfPixels(boundingBox.getImage(originalImage, components)) > ratioOfPixelsThreshold) {
                    // is connected to a stem
                    boolean stemFound = false;
                    for(BoundingBox verticalLine : verticalLines) {
                        if(isStemFor(boundingBox, verticalLine)) {
                            stemFound = true;
                            break;
                        }
                    }
                    if(stemFound) {
                        // beam (hook)
                        beams.add(boundingBox);
                        hooks.add(boundingBox);
                        CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 0, 0));
                        continue;
                    }
                }
            } else if(boundingBox.getWidth() > 0.5*staveSpaceWidth && boundingBox.getWidth() < 1.5*staveSpaceWidth &&
                    boundingBox.getHeight() > 2*staveSpaceWidth && boundingBox.getHeight() < 3.2*staveSpaceWidth) {
                // shape
                if(trainingSet.isTail(boundingBox.getImage(originalImage, components))) {
                    // quaver tail
                    quaverTails.add(boundingBox);
                    CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 150, 150));
                    continue;
                }
            }
            if(boundingBox.getWidth() > 2.5*boundingBox.getHeight() &&
                    boundingBox.getHeight() > staveSpaceWidth/4) {
                // tie or slur
                ties.add(boundingBox);
                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 180, 255));
                continue;
            }

            boundingBoxesLeft.add(boundingBox);
        }

        // loop through each system
        for(int i=0; i<staves.size(); i+=stavesInSystem) {
            ArrayList<BoundingBox> barLinesInThisSystem = new ArrayList<>();
            for(BoundingBox barLine : barLines) {
                if(Math.abs(barLine.yStart - staves.get(i).get(0).yStart) < tolerance) {
                    barLinesInThisSystem.add(barLine);
                }
            }

            int firstBarInSystemIndex = measures.size();
            boolean newSystem = measures.size() == 0 ? false : true;

            int fifths = 0;
            int lastKeySignatureAccidentalX = -1;
            globalPitchAlterations.clear();

            // loop through each bar in this system
            for(int j=0; j<barLinesInThisSystem.size(); j++) {
                if(Math.abs(barLinesInThisSystem.get(j).getXPosition() - staves.get(i).get(0).xStart) < 3*staveSpaceWidth) {
                    continue;
                }

                int leftBound = staves.get(i).get(0).xStart;
                if(j > 0) {
                    leftBound = barLinesInThisSystem.get(j-1).getXPosition();
                }

                Measure measure = new Measure(measures.size()+1);
                if(newSystem) {
                    measure.setNewSystem();
                    newSystem = false;
                }

                boolean[] timeSignatureRecognized = new boolean[stavesInSystem];
                for(int z = 0; z<stavesInSystem; z++) {
                    timeSignatureRecognized[z] = false;
                }

                // loop through each stave in this system
                for(int k=0; k<stavesInSystem; k++) {
                    ArrayList<BoundingBox> stave = staves.get(i+k);
                    int upperBound = i+k == 0 ?
                            stave.get(0).yStart - 5*staveSpaceWidth :
                            (stave.get(0).yStart + staves.get(i+k-1).get(4).yEnd)/2;
                    int lowerBound = i+k == staves.size()-1 ?
                            stave.get(4).yEnd + 5*staveSpaceWidth :
                            (stave.get(4).yEnd + staves.get(i+k+1).get(0).yStart)/2;

                    ArrayList<BoundingBox> relevantComponents = new ArrayList<>();
                    for(BoundingBox boundingBox : boundingBoxesLeft) {
                        if(boundingBox.getYPosition() > upperBound && boundingBox.getYPosition() < lowerBound) {
                            if(boundingBox.xStart > leftBound &&
                                    boundingBox.xEnd < barLinesInThisSystem.get(j).getXPosition()) {
                                relevantComponents.add(boundingBox);
                            }
                        }
                    }

                    Comparator<BoundingBox> componentComparator = (BoundingBox component1, BoundingBox component2) -> {
                        if(component1.xStart < component2.xStart) {
                            return -1;
                        }
                        if(component1.xStart == component2.xStart) {
                            return 0;
                        }
                        return 1;
                    };
                    Collections.sort(relevantComponents, componentComparator);

                    int lastNoteXPosition = -1;
                    String lastNoteType = "";
                    pitchAlterations.clear();

                    for(int m=0; m<relevantComponents.size(); m++) {
                        BoundingBox boundingBox = relevantComponents.get(m);

                        // distance from the beginning of the stave
                        if(boundingBox.xStart - stave.get(0).xStart <= 1.5*staveSpaceWidth) {
                            // height
                            if(boundingBox.getHeight() > 6*staveSpaceWidth) {
                                // treble clef
                                whichClef.put(stave, "treble");
                                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 0, 0));
                                continue;
                            }
                            if(boundingBox.getHeight() > 3*staveSpaceWidth) {
                                // bass clef
                                whichClef.put(stave, "bass");
                                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 255, 0));
                                continue;
                            }
                        }

                        // dimensions
                        if(boundingBox.getWidth() > 0.8*staveSpaceWidth && boundingBox.getWidth() < 1.5*staveSpaceWidth &&
                                boundingBox.getHeight() < staveSpaceWidth) {
                            if(isBeam(originalImage, boundingBox)) {
                                // ratio of black to white pixels
                                if(TrainingSet.ratioOfPixels(boundingBox.getImage(originalImage, components)) < ratioOfPixelsThreshold) {
                                    // sharp beam
                                    sharpBeams.add(boundingBox);
                                    CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 0, 255));

                                    // find the other beam
                                    for(int n=0; n<sharpBeams.size(); n++) {
                                        BoundingBox sharpBeam = sharpBeams.get(n);
                                        if(sharpBeam == boundingBox) {
                                            continue;
                                        }

                                        if(Math.abs(boundingBox.getXPosition() - sharpBeam.getXPosition()) < tolerance &&
                                                Math.abs(boundingBox.getYPosition() - sharpBeam.getYPosition()) < 1.5*staveSpaceWidth) {
                                            BoundingBox newBoundingBox = new BoundingBox(
                                                    Math.min(boundingBox.xStart, sharpBeam.xStart),
                                                    Math.min(boundingBox.yStart, sharpBeam.yStart),
                                                    Math.max(boundingBox.xEnd, sharpBeam.xEnd),
                                                    Math.max(boundingBox.yEnd, sharpBeam.yEnd),
                                                    boundingBox.label);

                                            sharpBeams.remove(sharpBeam);
                                            sharpBeams.remove(boundingBox);

                                            if(isPartOfKeySignature(newBoundingBox, stave, "sharp",
                                                    lastKeySignatureAccidentalX, fifths)) {
                                                fifths++;
                                                lastKeySignatureAccidentalX = newBoundingBox.getXPosition();
                                            } else {
                                                if(newBoundingBox.getXPosition() > lastKeySignatureAccidentalX + staveSpaceWidth) {
                                                    sharps.add(newBoundingBox);
                                                }
                                            }

                                            CCA.drawBoundingBox(image, newBoundingBox, ColorOperations.colorToRgb(0, 0, 255));
                                            break;
                                        }
                                    }

                                    continue;
                                }
                            }
                        } else if(boundingBox.getHeight() > 0.9*staveSpaceWidth && boundingBox.getHeight() < 1.5*staveSpaceWidth &&
                                boundingBox.getWidth() > 0.5*staveSpaceWidth && boundingBox.getWidth() < 1.2*staveSpaceWidth) {
                            // check for the flat hole
                            if(TrainingSet.ratioOfPixels(boundingBox.getImage(originalImage, components).getSubimage(
                                    0, boundingBox.getHeight()/3,
                                    boundingBox.getWidth()/2, boundingBox.getHeight()/3)) < (1-ratioOfPixelsThreshold)) {
                                // flat
                                if(isPartOfKeySignature(boundingBox, stave, "flat",
                                        lastKeySignatureAccidentalX, fifths)) {
                                    fifths--;
                                    lastKeySignatureAccidentalX = boundingBox.getXPosition();
                                } else {
                                    if(boundingBox.getXPosition() > lastKeySignatureAccidentalX + staveSpaceWidth) {
                                        flats.add(boundingBox);
                                    }
                                }

                                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 150, 255));
                                continue;
                            }
                        } if(boundingBox.getWidth() > 0.5*staveSpaceWidth && boundingBox.getWidth() < 0.9*staveSpaceWidth &&
                                boundingBox.getHeight() < 0.8*staveSpaceWidth) {
                            if(isBeam(originalImage, boundingBox)) {
                                // natural beam
                                naturalBeams.add(boundingBox);
                                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 0, 255));
                                continue;
                            }
                        }

                        // is time signature
                        if(!timeSignatureRecognized[k] && boundingBox.getWidth() > staveSpaceWidth
                                && staves.indexOf(stave) < stavesInSystem && measures.size() == 0) {
                            // vertical position
                            if(Math.abs(boundingBox.yStart - stave.get(0).getYPosition()) < staveSpaceWidth/4 &&
                                    Math.abs(stave.get(4).getYPosition() - boundingBox.yEnd) < staveSpaceWidth/4) {
                                // whole time signature
                                timeSignatureRecognized[k] = true;
                                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 0, 255));
                                continue;
                            } else if(Math.abs(boundingBox.yStart - stave.get(1).getYPosition()) < staveSpaceWidth/4 &&
                                    Math.abs(stave.get(3).getYPosition() - boundingBox.yEnd) < staveSpaceWidth/4) {
                                // C time signature
                                timeSignatureRecognized[k] = true;
                                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 0, 255));
                                continue;
                            }
                        }

                        // is dotted
                        boolean dotted = false;
                        for(BoundingBox dot : dots) {
                            if(dot.xStart > boundingBox.xEnd &&
                                    dot.xStart - boundingBox.xEnd < 3*staveSpaceWidth/4 &&
                                    Math.abs(boundingBox.getYPosition() - dot.getYPosition()) < staveSpaceWidth) {
                                // dotted value
                                dotted = true;
                            }
                        }

                        // is tied
                        String tieType = null;
                        for(BoundingBox tie : ties) {
                            if(Math.abs(tie.getYPosition() - boundingBox.getYPosition()) < 2*staveSpaceWidth &&
                                    Math.abs(tie.getXPosition() - boundingBox.getXPosition()) < 2*staveSpaceWidth) {
                                if(tie.xStart > boundingBox.xStart) {
                                    tieType = "start";
                                    break;
                                }
                                if(boundingBox.xStart > tie.xStart) {
                                    tieType = "stop";
                                    break;
                                }
                            }
                        }

                        // is sharp
                        String accidental = null;
                        for(BoundingBox sharp : sharps) {
                            if(sharp.xEnd < boundingBox.xStart &&
                                    boundingBox.xStart - sharp.xEnd < 2*staveSpaceWidth &&
                                    Math.abs(boundingBox.getYPosition() - sharp.getYPosition()) < tolerance) {
                                accidental = "sharp";
                                break;
                            }
                        }

                        // is flat
                        if(accidental == null) {
                            for(BoundingBox flat : flats) {
                                if(flat.xEnd < boundingBox.xStart &&
                                        boundingBox.xStart - flat.xEnd < 2*staveSpaceWidth &&
                                        Math.abs(flat.getYPosition() - boundingBox.getYPosition()) < tolerance) {
                                    accidental = "flat";
                                }
                            }
                        }

                        // is natural
                        boolean topBeamFound = false;
                        boolean bottomBeamFound = false;
                        for(BoundingBox naturalBeam : naturalBeams) {
                            if(boundingBox.xStart > naturalBeam.xEnd &&
                                    boundingBox.xStart - naturalBeam.xEnd < 2*staveSpaceWidth) {
                                if(boundingBox.getYPosition() > naturalBeam.getYPosition() &&
                                        boundingBox.getYPosition() - naturalBeam.getYPosition() < 0.8*staveSpaceWidth) {
                                    topBeamFound = true;
                                }
                                if(boundingBox.getYPosition() < naturalBeam.getYPosition() &&
                                        naturalBeam.getYPosition() - boundingBox.getYPosition() < 0.8*staveSpaceWidth) {
                                    bottomBeamFound = true;
                                }
                            }
                        }
                        if(topBeamFound && bottomBeamFound) {
                            accidental = "natural";
                        }

                        // dimensions
                        if(boundingBox.getWidth() > staveSpaceWidth && boundingBox.getWidth() < 2*staveSpaceWidth &&
                                boundingBox.getHeight() > staveSpaceWidth/2 && boundingBox.getHeight() < staveSpaceWidth) {
                            // vertical position
                            if(Math.abs(boundingBox.yStart - stave.get(1).getYPosition()) < tolerance) {
                                // ratio of black to white pixels
                                if(TrainingSet.ratioOfPixels(boundingBox.getImage(originalImage, components)) > ratioOfPixelsThreshold) {
                                    // semibreve rest
                                    measure.addNote(new Note(k+1, "whole", dotted, k+1));
                                    CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 150, 150));
                                    continue;
                                }
                            } else if(Math.abs(boundingBox.yEnd - stave.get(2).getYPosition()) < tolerance) {
                                // ratio of black to white pixels
                                if(TrainingSet.ratioOfPixels(boundingBox.getImage(originalImage, components)) > ratioOfPixelsThreshold) {
                                    // minim rest
                                    measure.addNote(new Note(k+1, "half", dotted, k+1));
                                    CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 150, 150));
                                    continue;
                                }
                            }
                        } else if(boundingBox.getWidth() > staveSpaceWidth && boundingBox.getWidth() < 1.3*staveSpaceWidth &&
                                boundingBox.getHeight() > 2.5*staveSpaceWidth && boundingBox.getHeight() < 3.5*staveSpaceWidth) {
                            // vertical position
                            if(Math.abs(boundingBox.getYPosition() - stave.get(2).getYPosition()) < staveSpaceWidth/2) {
                                // shape
                                if(trainingSet.isCrotchetRest(boundingBox.getImage(originalImage, components))) {
                                    // crotchet rest
                                    measure.addNote(new Note(k+1, "quarter", dotted, k+1));
                                    CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 150, 255));
                                    continue;
                                }
                            }
                        } else if(boundingBox.getWidth() > 2*staveSpaceWidth/3 && boundingBox.getWidth() < 1.5*staveSpaceWidth &&
                                boundingBox.getHeight() > 1.3*staveSpaceWidth && boundingBox.getHeight() < 2*staveSpaceWidth) {
                            // vertical position
                            if(Math.abs(boundingBox.yStart - stave.get(1).getYPosition()) < staveSpaceWidth/2 &&
                                    Math.abs(boundingBox.yEnd - stave.get(3).getYPosition()) < staveSpaceWidth/2) {
                                // shape
                                if(trainingSet.isQuaverRest(boundingBox.getImage(originalImage, components))) {
                                    // quaver rest
                                    measure.addNote(new Note(k+1, "eighth", dotted, k+1));
                                    CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 0, 150));
                                    continue;
                                }
                            }
                        } else if(boundingBox.getWidth() > staveSpaceWidth && boundingBox.getWidth() < 2*staveSpaceWidth &&
                                boundingBox.getHeight() > 2.3*staveSpaceWidth && boundingBox.getHeight() < 3*staveSpaceWidth) {
                            // vertical position
                            if(Math.abs(boundingBox.yStart - stave.get(1).getYPosition()) < staveSpaceWidth/2 &&
                                    Math.abs(boundingBox.yEnd - stave.get(4).getYPosition()) < staveSpaceWidth/2) {
                                // shape
                                if(trainingSet.isSemiquaverRest(boundingBox.getImage(originalImage, components))) {
                                    // semiquaver rest
                                    measure.addNote(new Note(k+1, "16th", dotted, k+1));
                                    CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 0, 255));
                                    continue;
                                }
                            }
                        }

                        // note or chord
                        boolean chord = false;

                        // count notes in the chord
                        int notes = 0;
                        for(int l=0; l < 15; l++) {
                            if(boundingBox.getHeight() > l*staveSpaceWidth + 4*staveSpaceWidth/5 &&
                                    boundingBox.getHeight() < l*staveSpaceWidth + 1.4*staveSpaceWidth) {
                                notes = l+1;
                                break;
                            }
                        }
                        if(notes == 0) {
                            continue;
                        }
                        if(notes > 1) {
                            // split the chord
                            for(int l=0; l<notes; l++) {
                                BoundingBox newComponent = new BoundingBox(
                                        boundingBox.xStart,
                                        boundingBox.yStart + l*boundingBox.getHeight()/notes,
                                        boundingBox.xEnd,
                                        boundingBox.yStart + (l+1)*boundingBox.getHeight()/notes,
                                        boundingBox.label);
                                relevantComponents.add(m+1, newComponent);
                                CCA.drawBoundingBox(image, newComponent, ColorOperations.white());
                            }
                            continue;
                        }

                        // is above or below the stave
                        if(boundingBox.getYPosition() < stave.get(0).getYPosition() - 3*staveSpaceWidth/4 ||
                                boundingBox.getYPosition() > stave.get(4).getYPosition() + 3*staveSpaceWidth/4) {
                            // is there a ledger line
                            int ledgerLines = countPeaks(boundingBox.getImage(veryOriginalImage, components), 'x');
                            if(ledgerLines != 1 && ledgerLines != 2) {
                                // discard
                                CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 0, 0));
                                continue;
                            }
                        }

                        // dimensions
                        if(boundingBox.getHeight() > 0.9*staveSpaceWidth && boundingBox.getHeight() < 1.3*staveSpaceWidth) {
                            if(boundingBox.getWidth() > staveSpaceWidth && boundingBox.getWidth() < 3*staveSpaceWidth) {
                                // is middle filled
                                if(TrainingSet.ratioOfPixels(boundingBox.getImage(originalImage, components).getSubimage(
                                        boundingBox.getWidth()/3, boundingBox.getHeight()/3,
                                        boundingBox.getWidth()/3, boundingBox.getHeight()/3)) > ratioOfPixelsThreshold) {
                                    // filled note head
                                    if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < staveSpaceWidth/2) {
                                        chord = true;
                                    }

                                    // is attached to a tail
                                    boolean isTailed = false;
                                    for(BoundingBox quaverTail : quaverTails) {
                                        if((boundingBox.yStart > quaverTail.yEnd - staveSpaceWidth &&
                                                boundingBox.yStart - quaverTail.yEnd < 4*staveSpaceWidth &&
                                                Math.abs(boundingBox.xEnd - quaverTail.xStart) < 3*staveSpaceWidth/4) ||
                                                (quaverTail.yStart > boundingBox.yEnd - staveSpaceWidth &&
                                                        quaverTail.yStart - boundingBox.yEnd < 4*staveSpaceWidth &&
                                                        Math.abs(boundingBox.xStart - quaverTail.xStart) < 3*staveSpaceWidth/4)){
                                            isTailed = true;
                                            break;
                                        }
                                    }
                                    if(isTailed) {
                                        // tailed quaver
                                        if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < 2*staveSpaceWidth &&
                                                lastNoteType != "" && lastNoteType != "eighth") {
                                            continue;
                                        }
                                        lastNoteType = "eighth";

                                        lastNoteXPosition = boundingBox.getXPosition();
                                        measure.addNote(getNote(boundingBox.getYPosition(), stave,
                                                tieType, k+1, "eighth", dotted, k+1, null,
                                                accidental, chord));
                                        CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 255, 0));
                                        continue;
                                    }

                                    // is beamed
                                    ArrayList<String> relevantBeams = new ArrayList<>();
                                    BoundingBox relevantStem = null;
                                    BoundingBox relevantBeam = null;
                                    for(BoundingBox beam : beams) {
                                        // find the stem connecting the note head to the beam
                                        for(BoundingBox verticalLine : verticalLines) {
                                            if(isStemFor(boundingBox, verticalLine) &&
                                                    beam.yEnd > verticalLine.yStart - staveSpaceWidth/2 &&
                                                    beam.yStart < verticalLine.yEnd + staveSpaceWidth/2) {
                                                int noRelevantBeams = relevantBeams.size();
                                                if(Math.abs(verticalLine.getXPosition() - beam.xStart) < staveSpaceWidth) {
                                                    if(hooks.contains(beam)) {
                                                        relevantBeams.add("forward hook");
                                                    } else {
                                                        relevantBeams.add("begin");
                                                    }
                                                } else if(Math.abs(verticalLine.getXPosition() - beam.xEnd) < staveSpaceWidth) {
                                                    if(hooks.contains(beam)) {
                                                        relevantBeams.add("backward hook");
                                                    } else {
                                                        relevantBeams.add("end");
                                                    }
                                                } else if(beam.xStart < verticalLine.getXPosition() && beam.xEnd > verticalLine.getXPosition()) {
                                                    relevantBeams.add("continue");
                                                }
                                                if(relevantBeams.size() > noRelevantBeams) {
                                                    relevantStem = verticalLine;
                                                    relevantBeam = beam;
                                                    CCA.drawBoundingBox(image, verticalLine, ColorOperations.white());
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if(relevantBeams.size() == 1) {
                                        String secondBeamType = findSecondBeam(image, relevantBeam.getImage(originalImage, components), relevantBeam, relevantStem);
                                        if(secondBeamType != null) {
                                            relevantBeams.add(secondBeamType);
                                        }
                                    }
                                    switch(relevantBeams.size()) {
                                        case 0:
                                            break;
                                        case 1:
                                            // beamed quaver
                                            if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < 2*staveSpaceWidth &&
                                                    lastNoteType != "" && lastNoteType != "eighth") {
                                                continue;
                                            }
                                            lastNoteType = "eighth";

                                            lastNoteXPosition = boundingBox.getXPosition();
                                            measure.addNote(getNote(boundingBox.getYPosition(), stave,
                                                    tieType, k+1, "eighth", dotted, k+1, relevantBeams,
                                                    accidental, chord));
                                            CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 255, 0));
                                            continue;
                                        default:
                                            // beamed semiquaver
                                            if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < 2*staveSpaceWidth &&
                                                    lastNoteType != "" && lastNoteType != "16th") {
                                                continue;
                                            }
                                            lastNoteType = "16th";

                                            lastNoteXPosition = boundingBox.getXPosition();
                                            measure.addNote(getNote(boundingBox.getYPosition(), stave,
                                                    tieType, k+1, "16th", dotted, k+1, relevantBeams,
                                                    accidental, chord));
                                            CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(255, 255, 0));
                                            continue;
                                    }

                                    // is connected to a stem
                                    boolean stemFound = false;
                                    for(BoundingBox verticalLine : verticalLines) {
                                        if(isStemFor(boundingBox, verticalLine)) {
                                            stemFound = true;
                                            CCA.drawBoundingBox(image, verticalLine, ColorOperations.white());
                                            break;
                                        }
                                    }
                                    if(stemFound) {
                                        // crotchet
                                        if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < 2*staveSpaceWidth &&
                                                lastNoteType != "" && lastNoteType != "quarter") {
                                            continue;
                                        }
                                        lastNoteType = "quarter";

                                        lastNoteXPosition = boundingBox.getXPosition();
                                        measure.addNote(getNote(boundingBox.getYPosition(), stave,
                                                tieType, k+1, "quarter", dotted, k+1, null,
                                                accidental, chord));
                                        CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(150, 150, 0));
                                        continue;
                                    }
                                }

                                // number of vertical lines
                                switch(countPeaks(boundingBox.getImage(originalImage, components), 'y')) {
                                    case 0:
                                    case 1:
                                        // is connected to a stem
                                        boolean stemFound = false;
                                        for(BoundingBox verticalLine : verticalLines) {
                                            if(isStemFor(boundingBox, verticalLine)) {
                                                stemFound = true;
                                                CCA.drawBoundingBox(image, verticalLine, ColorOperations.white());
                                                break;
                                            }
                                        }
                                        if(stemFound) {
                                            // minim
                                            if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < staveSpaceWidth/2) {
                                                chord = true;
                                            }

                                            if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < 2*staveSpaceWidth &&
                                                    lastNoteType != "" && lastNoteType != "half") {
                                                continue;
                                            }
                                            lastNoteType = "half";

                                            lastNoteXPosition = boundingBox.getXPosition();
                                            measure.addNote(getNote(boundingBox.getYPosition(), stave,
                                                    tieType, k+1, "half", dotted, k+1, null,
                                                    accidental, chord));
                                            CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(150, 0, 150));
                                            continue;
                                        }
                                        break;
                                    case 2:
                                        // semibreve
                                        if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < staveSpaceWidth/2) {
                                            chord = true;
                                        }

                                        if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < 2*staveSpaceWidth &&
                                                lastNoteType != "" && lastNoteType != "whole") {
                                            continue;
                                        }
                                        lastNoteType = "whole";

                                        lastNoteXPosition = boundingBox.getXPosition();
                                        measure.addNote(getNote(boundingBox.getYPosition(), stave,
                                                tieType, k+1, "whole", dotted, k+1, null,
                                                accidental, chord));
                                        CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 50, 150));
                                        continue;
                                }
                            }
                            if(boundingBox.getWidth() > 2*staveSpaceWidth/3 && boundingBox.getWidth() < 1.3*staveSpaceWidth) {
                                // number of vertical lines
                                switch(countPeaks(boundingBox.getImage(originalImage, components), 'y')) {
                                    case 1:
                                        // semibreve half

                                        // discard if the other half already recognised
                                        boolean otherHalfRecognised = false;
                                        for(BoundingBox semibreveHalf : semibreveHalves) {
                                            if((boundingBox.xStart - semibreveHalf.xEnd < staveSpaceWidth/5 ||
                                                    semibreveHalf.xStart - boundingBox.xEnd < staveSpaceWidth/5) &&
                                                    Math.abs(semibreveHalf.getYPosition() - boundingBox.getYPosition()) < tolerance) {
                                                otherHalfRecognised = true;
                                                break;
                                            }
                                        }
                                        if(otherHalfRecognised) {
                                            CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 0, 0));
                                            continue;
                                        }

                                        int actualXPosition = boundingBox.getXPosition() + staveSpaceWidth/2;
                                        if(Math.abs(lastNoteXPosition - actualXPosition) < tolerance) {
                                            chord = true;
                                        }

                                        if(Math.abs(lastNoteXPosition - boundingBox.getXPosition()) < 2*staveSpaceWidth &&
                                                lastNoteType != "" && lastNoteType != "whole") {
                                            continue;
                                        }
                                        lastNoteType = "whole";

                                        lastNoteXPosition = actualXPosition;
                                        semibreveHalves.add(boundingBox);
                                        measure.addNote(getNote(boundingBox.getYPosition(), stave,
                                                tieType, k+1, "whole", dotted, k+1, null,
                                                accidental, chord));
                                        CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 150, 150));
                                        continue;
                                }
                            }
                        }

                        CCA.drawBoundingBox(image, boundingBox, ColorOperations.colorToRgb(0, 0, 0));
                    }

                    measure.addBackup();
                }

                if(!measure.isEmpty()) {
                    measures.add(measure);
                }
            }

            ArrayList<String> clefsInThisSystem = new ArrayList<>();
            for(int j=0; j<stavesInSystem; j++) {
                if(whichClef.containsKey(staves.get(i+j))) {
                    clefsInThisSystem.add(whichClef.get(staves.get(i+j)));
                } else {
                    System.out.println("No clef recognized on stave  " + (i+j));
                }
            }
            measures.get(firstBarInSystemIndex).addAttributes(fifths, 4, 4, clefsInThisSystem, stavesInSystem);
        }
        measures.get(measures.size()-1).setLast();
    }

    /**
     * Generate a MusicXML file.
     */
    public void generateXML() {
        try {
            File template = new File("xmlHeader.xml");
            File output = new File("output.xml");
            Files.copy(template.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);

            for(Measure measure : measures) {
                Files.write(output.toPath(), measure.getXML().getBytes(), StandardOpenOption.APPEND);
            }

            String ending = "\r\n\r\n\t" + "</part>";
            ending += "\r\n" + "</score-partwise>";
            Files.write(output.toPath(), ending.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
