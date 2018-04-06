package project.model;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

/**
 * Deskews the image.
 */
public class Deskewing {
    private BufferedImage image;
    private int staveLineThreshold;
    private int[] histogram;

    /**
     * Create a new instance.
     * @param image The image to be deskewed
     * @param staveLineThreshold The stave line threshold
     */
    public Deskewing(BufferedImage image, int staveLineThreshold) {
        updateImage(image);
        this.staveLineThreshold = staveLineThreshold;
    }

    /**
     * Get the image.
     * @return The image
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Update the image.
     * @param image The new image
     */
    private void updateImage(BufferedImage image) {
        this.image = image;
        histogram = Model.project(image, 'x');
    }

    /**
     * Binarize an image.
     * @param image The image to be binzarized
     * @return The binary image
     */
    private static BufferedImage binarizeImage(BufferedImage image) {
        BufferedImage image2 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for(int i=0; i<image.getWidth(); i++) {
            for(int j=0; j<image.getHeight(); j++) {
                int value = ColorOperations.rgbToValue(image.getRGB(i, j)) < 150 ?
                        ColorOperations.black() : ColorOperations.white();
                image2.setRGB(i, j, value);
            }
        }
        return image2;
    }

    /**
     * Rotate the image.
     * @param image The image to be rotated
     * @param angleDegrees The angle
     */
    private void rotateImage(BufferedImage image, double angleDegrees) {
        double rotationRequired = Math.toRadians(angleDegrees);
        double locationX = image.getWidth() / 2;
        double locationY = image.getHeight() / 2;
        AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);

        updateImage(binarizeImage(op.filter(image, null)));
    }

    /**
     * Get the sum of the values in the projection that are above the stave line threshold.
     * It is used in Stochastic Gradient Descent in the deskewing algorithm.
     * @return The sum of the values in the projection that are above the stave line threshold
     */
    private int sumAboveThreshold() {
        int sum = 0;
        for(int i=1; i<histogram.length; i++) {
            if(histogram[i] > staveLineThreshold && histogram[i-1] < staveLineThreshold) {
                sum += histogram[i];
            }
        }
        return sum;
    }

    /**
     * Deskew the image.
     */
    public void deskew() {
        double increment = 0.05;
        double angle = 0;
        double maxAngle = 2;
        BufferedImage originalImage = image;

        // maximize the sum of the values above the threshold
        int previousSum = sumAboveThreshold();
        int sum = previousSum;
        while(sum >= previousSum && angle < maxAngle) {
            angle += increment;
            System.out.println("Trying angle " + angle);
            rotateImage(originalImage, angle);
            previousSum = sum;
            sum = sumAboveThreshold();
        }
        // reverse the last rotation that broke the loop
        angle -= increment;
        rotateImage(originalImage, angle);

        int firstSum = sum;
        double firstAngle = angle;

        // switch the direction
        angle = 0;
        increment *= -1;

        // maximize the sum of the values above the threshold
        previousSum = sumAboveThreshold();
        sum = previousSum;
        while(sum >= previousSum && angle < maxAngle) {
            angle += increment;
            System.out.println("Trying angle " + angle);
            rotateImage(originalImage, angle);
            previousSum = sum;
            sum = sumAboveThreshold();
        }
        // reverse the last rotation that broke the loop
        angle -= increment;
        rotateImage(originalImage, angle);

        // check which direction was correct
        if(sum < firstSum) {
            angle = firstAngle;
            rotateImage(originalImage, angle);
        }

        System.out.println("Angle: " + angle);
    }
}
