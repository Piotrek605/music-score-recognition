package project.model;

import project.utils.ImageFile;
import project.utils.UnsupportedImageTypeException;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Compares symbols' images to the ones in the training set.
 */
public class TrainingSet {
    private static int gridSize = 2;

    private BufferedImage filledNoteHead;
    private BufferedImage minim;
    private BufferedImage semibreve;

    private BufferedImage tail;

    private BufferedImage crotchetRest;
    private BufferedImage quaverRest;
    private BufferedImage semiquaverRest;

    /**
     * Create a new instance and load files.
     * @throws IOException
     * @throws UnsupportedImageTypeException
     */
    public TrainingSet()
            throws IOException, UnsupportedImageTypeException
    {
        ImageFile newImageFile = new ImageFile(new File("symbols/filledNoteHead.png"));
        filledNoteHead = newImageFile.getBufferedImage(0);

        newImageFile = new ImageFile(new File("symbols/semibreve.jpg"));
        semibreve = newImageFile.getBufferedImage(0);

        newImageFile = new ImageFile(new File("symbols/minim.png"));
        minim = newImageFile.getBufferedImage(0);

        newImageFile = new ImageFile(new File("symbols/tail.png"));
        tail = newImageFile.getBufferedImage(0);

        newImageFile = new ImageFile(new File("symbols/crotchetRest.png"));
        crotchetRest = newImageFile.getBufferedImage(0);

        newImageFile = new ImageFile(new File("symbols/quaverRest.png"));
        quaverRest = newImageFile.getBufferedImage(0);

        newImageFile = new ImageFile(new File("symbols/semiquaverRest.png"));
        semiquaverRest = newImageFile.getBufferedImage(0);
    }

    /**
     * Check if the given component is a filled note head.
     * @param symbol The component
     * @return True if the symbol is a filled note head
     */
    public boolean isFilledNoteHead(BufferedImage symbol) {
        return compare(filledNoteHead, symbol);
    }

    /**
     * Check if the given component is a minim.
     * @param symbol The component
     * @return True if the symbol is a minim
     */
    public boolean isMinim(BufferedImage symbol) {
        return compare(minim, symbol);
    }

    /**
     * Check if the given component is a semibreve.
     * @param symbol The component
     * @return True if the symbol is a semibreve
     */
    public boolean isSemibreve(BufferedImage symbol) {
        return compare(semibreve, symbol);
    }

    /**
     * Check if the given component is a tail.
     * @param symbol The component
     * @return True if the symbol is a tail
     */
    public boolean isTail(BufferedImage symbol) {
        return compare(tail, symbol);
    }

    /**
     * Check if the given component is a crotchet rest.
     * @param symbol The component
     * @return True if the symbol is a crotchet rest
     */
    public boolean isCrotchetRest(BufferedImage symbol) {
        return compare(crotchetRest, symbol);
    }

    /**
     * Check if the given component is a quaver rest.
     * @param symbol The component
     * @return True if the symbol is a quaver rest
     */
    public boolean isQuaverRest(BufferedImage symbol) {
        return compare(quaverRest, symbol);
    }

    /**
     * Check if the given component is a semiquaver rest.
     * @param symbol The component
     * @return True if the symbol is a semiquaver rest
     */
    public boolean isSemiquaverRest(BufferedImage symbol) {
        return compare(semiquaverRest, symbol);
    }

    /**
     * Get the ratio of black to white pixels for an image.
     * @param image The image
     * @return The ratio of black to white pixels
     */
    public static double ratioOfPixels(BufferedImage image) {
        int blackPixels = 0;
        int whitePixels = 0;

        for(int i=0; i<image.getWidth(); i++) {
            for(int j=0; j<image.getHeight(); j++) {
                if(image.getRGB(i, j) == ColorOperations.black()) {
                    blackPixels++;
                } else {
                    whitePixels++;
                }
            }
        }

        return ((double)blackPixels)/(blackPixels+whitePixels);
    }

    /**
     * Compare two symbols by splitting them into a grid and comparing
     * the number of pixels in each cell.
     * @param symbol1 The first symbol
     * @param symbol2NotCropped The second symbol
     * @return
     */
    private boolean compare(BufferedImage symbol1, BufferedImage symbol2NotCropped) {
        // crop the second symbol so that the symbols are in proportion
        BufferedImage symbol2 = symbol2NotCropped;
        if((double)(symbol2NotCropped.getWidth())/symbol2NotCropped.getHeight() > (double)(symbol1.getWidth())/symbol1.getHeight()) {
            int newWidth = symbol1.getWidth()*symbol2NotCropped.getHeight()/symbol1.getHeight();
            symbol2 = symbol2NotCropped.getSubimage((symbol2NotCropped.getWidth()-newWidth)/2, 0, newWidth, symbol2NotCropped.getHeight());
        }

        // loop through each cell
        for(int i=0; i<gridSize; i++) {
            for(int j=0; j<gridSize; j++) {
                double ratio1 = ratioOfPixels(symbol1);
                double ratio2 = ratioOfPixels(symbol2);

                if(Math.abs(ratio1 - ratio2) > 0.2) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Quickly display an image in a new window.
     * @param image The image
     * @param text The text to be added to the window
     */
    public static void display(BufferedImage image, String text) {
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.getContentPane().add(new JLabel(text));
        frame.pack();
        frame.setVisible(true);
    }
}
