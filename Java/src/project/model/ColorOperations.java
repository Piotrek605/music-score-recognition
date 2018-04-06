package project.model;

/**
 * Contains method used for colouring.
 */
public class ColorOperations {
    /**
     * Convert a greyscale colour to the rgb value.
     * @param value The greyscale colour
     * @return The rgb value
     */
    public static int valueToRgb(int value) {
        return colorToRgb(value, value, value);
    }

    /**
     * Convert a rgb value to a greyscale colour.
     * @param rgb The rgb value
     * @return The greyscale colour
     */
    public static int rgbToValue(int rgb) {
        int r = (rgb)&0xFF;
        int g = (rgb>>8)&0xFF;
        int b = (rgb>>16)&0xFF;
        int a = (rgb>>24)&0xFF;

        if(a != 255) {
            return 255;
        }
        return (r+g+b)/3;
    }

    /**
     * Convert a colour to the rgb value.
     * @param r The red value
     * @param g The green value
     * @param b The blue value
     * @return The rgb value
     */
    public static int colorToRgb(int r, int g, int b) {
        return (255<<24) | (r<<16) | (g<<8) | b;
    }

    /**
     * Get the colour for the label
     * @param label The label
     * @return The colour
     */
    public static int labelToRgb(int label) {
        if(label <= 10) {
            return colorToRgb(25*label, 0, 0);
        }
        if(label <= 20) {
            return colorToRgb(25*(20-label), 25*(label-10), 0);
        }
        if(label <= 30) {
            return colorToRgb(0, 25*(30-label), 25*(label-20));
        }
        if(label <= 40) {
            return colorToRgb(25*(label-30), 0, 255);
        }
        if(label <= 50) {
            return colorToRgb(25*(50-label), 25*(label-40), 255);
        }
        if(label <= 60) {
            return colorToRgb(25*(label-50), 255, 25*(60-label));
        }
        if(label <= 70) {
            return colorToRgb(255, 255, 25*(label-60));
        }
        if(label <= 80) {
            return colorToRgb(25*(80-label), 25*(80-label), 25*(80-label));
        }
        return labelToRgb(label-80);
    }

    /**
     * Get the white rgb value
     * @return The white rgb value
     */
    public static int white() {
        return valueToRgb(255);
    }

    /**
     * Get the black rgb value
     * @return The black rgb value
     */
    public static int black() {
        return valueToRgb(0);
    }
}
