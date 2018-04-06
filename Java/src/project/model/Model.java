package project.model;

import project.utils.ImageFile;
import project.utils.UnsupportedImageTypeException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * The <code>Model</code> class manages the data of the application
 * <p>
 * Other than possibly a draw method, which draws a representation of the object
 * on a graphics context, and possibly a toString method, which generates a
 * <code>String</code> representation of the object, it should not know about
 * the user interface
 * </p>
 */
public class Model
{
	private List<Rectangle>	rects	= new ArrayList<>();

	private BufferedImage	image	= null;
	private ArrayList<BufferedImage> images = new ArrayList<>();

	// data for displaying several stages overlapped for testing
	private ArrayList<BufferedImage> imagesLayers = new ArrayList<>();
	private boolean layersDisplayed = false;
	private int firstStageWithLayers = 2;
	private int lastStageWithLayers = 4;

	// deskewing
	private Deskewing deskewing;
	private int[] histogram;
	private ArrayList<int[]> histograms = new ArrayList<>();

	// line removal
	private LineRemoval lineRemoval;
	private int staveLineThreshold;
	private int staveSpaceWidth;
	private ArrayList<ArrayList<BoundingBox>> staves;
	private ArrayList<BoundingBox> barLines;
	private ArrayList<BoundingBox> verticalLines;

	// connected component analysis
	private CCA cca;
	private Map<Integer, BoundingBox> labelToBoundingBox;
	private int[][] components;

	private Recognizer recognizer;

	private int stage = 0;
	private int stageDisplayed = stage;

	private double scale = 1.0;

	/**
	 * Get the image to be displayed.
	 * @return The image
	 */
	public BufferedImage getImage()
	{
		if(!layersDisplayed || stageDisplayed < firstStageWithLayers || stageDisplayed > lastStageWithLayers) {
			if (stage == stageDisplayed) {
				return image;
			}
			return images.get(stageDisplayed);
		} else {
			return imagesLayers.get(stageDisplayed - firstStageWithLayers);
		}
	}

	/**
	 * Get the list of rectangles currently set in the model.
	 * 
	 * @return the <code>List</code> object containing the rectangles set
	 */
	public List<Rectangle> getRects()
	{
		return rects;
	}

	/**
	 * Sets or replaces the current image in the <code>Model</code> and clears
	 * the list of rectangles.
	 *
	 * @param bi
	 *            the image to set in the <code>Model</code>
	 */
	public void setImage(BufferedImage bi)
	{
		image = bi;
		rects.clear();
		histogram = project(image, 'x');
	}

	/**
	 * Get the dimensions of the image loaded
	 * 
	 * @return the <code>Dimension</code> object containing the dimensions of
	 *         the image loaded, or <code>(0, 0)</code> if there is no image
	 *         loaded.
	 */
	public Dimension getDimensions()
	{
		BufferedImage image = getImage();
		if (image != null)
			return new Dimension((int)(scale * image.getWidth()), (int)(scale * image.getHeight()));
		else
			return new Dimension(0, 0);
	}

	/**
	 * Adds a new <code>Rectangle</code> to the <code>Model</code>
	 *
	 * @param rect
	 *            the <code>Rectangle</code> to add to the <code>Model</code>
	 */
	public void addRect(Rectangle rect)
	{
		rects.add(rect);
	}

	/**
	 * Tests if the model is active, i.e. whether it currently has an image
	 *
	 * @return <code>true</code> if the model has an image, false otherwise
	 */
	public boolean isActive()
	{
		return image != null;
	}

	/**
	 * Loads an image from a file.
	 * 
	 * Any pre-existing rectangles will be cleared.
	 * 
	 * @param file
	 *            The <code>File</code> object identifying the file containing
	 *            the image to load
	 * @throws IOException
	 *             if there is a problem reading the file or if the file
	 *             contains no images
	 * @throws UnsupportedImageTypeException
	 *             if the file does not contain an image of a type supported
	 */
	public void loadImage(File file)
			throws IOException, UnsupportedImageTypeException
	{
		ImageFile newImageFile = new ImageFile(file);
		int numImages = newImageFile.getNumImages();
		if (numImages == 0)
			throw new IOException("Image file contains no images");
		BufferedImage bi = newImageFile.getBufferedImage(0);
		setImage(bi);

		// clear data
		stage = 0;
		stageDisplayed = stage;
		images.clear();
		imagesLayers.clear();
		histograms.clear();

		// initialize data
		staveLineThreshold = image.getWidth()/2;
		deskewing = new Deskewing(image, staveLineThreshold);
		lineRemoval = new LineRemoval();
		cca = new CCA();
	}

	/**
	 * Get the horizontal projection.
	 * @return The horizontal projection
	 */
	public int[] getHistogram() {
		if(stage == stageDisplayed) {
			return histogram;
		}
		return histograms.get(stageDisplayed);
	}

	/**
	 * Project the image.
	 * @param symbol The image to be projected
	 * @param dimension The dimension
	 * @return The projection
	 */
	public static int[] project(BufferedImage symbol, char dimension) {
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
				return null;
		}

		// project
		int[] projection = new int[firstSize];
		for(int x=0; x<firstSize; x++) {
			projection[x] = 0;
			for(int y=0; y<secondSize; y++) {
				int pixel = 0;
				switch(dimension) {
					case 'x':
						pixel = symbol.getRGB(y, x);
						break;
					case 'y':
						pixel = symbol.getRGB(x, y);
						break;
				}

				if(pixel == ColorOperations.black()) {
					projection[x]++;
				}
			}
		}

		return projection;
	}

	/**
	 * Go to the next stage of image processing.
	 */
	public void processImage() {
		images.add(image);
		histograms.add(histogram);

		// create a new image
		int imageType = stage < lastStageWithLayers ? BufferedImage.TYPE_BYTE_BINARY : BufferedImage.TYPE_INT_RGB;
		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), imageType);
		for(int i=0; i<image.getWidth(); i++) {
			for(int j=0; j<image.getHeight(); j++) {
				newImage.setRGB(i, j, image.getRGB(i, j));
			}
		}

		switch(stage) {
			case 0:
				deskewing.deskew();
				newImage = deskewing.getImage();
				break;
			case 1:
				lineRemoval.removeStave(newImage, histogram, staveLineThreshold);
				staves = lineRemoval.getStaves();
				staveSpaceWidth = lineRemoval.getStaveSpaceWidth();
				break;
			case 2:
				lineRemoval.removeVerticalLines(newImage, imagesLayers.get(0));
				barLines = lineRemoval.getBarLines();
				verticalLines = lineRemoval.getVerticalLines();
				break;
			case 3:
				lineRemoval.patch(images.get(1), newImage);
				break;
			case 4:
				cca.collectComponents(newImage);
				break;
			case 5:
				cca.resolveEquivalences(newImage);
				components = cca.getComponents();
				break;
			case 6:
				labelToBoundingBox = cca.getBoundingBoxes();
				cca.drawBoundingBoxes(newImage);
				break;
			case 7:
				recognizer = new Recognizer(staves, barLines, verticalLines,
						staveSpaceWidth, labelToBoundingBox.values(), components);
				recognizer.recognize(newImage, images.get(lastStageWithLayers), images.get(1));
				recognizer.generateXML();
				break;
			default:
				return;
		}

		setImage(newImage);
		stage++;
		stageDisplayed = stage;

		// overlap the stages for testing
		if(stage >= firstStageWithLayers && stage <= lastStageWithLayers) {
			BufferedImage previousImage = images.get(stage-1);

			// set the changed pixels to a different color
			BufferedImage imageLayers = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
			for (int i = 0; i < image.getWidth(); i++) {
				for (int j = 0; j < image.getHeight(); j++) {
					imageLayers.setRGB(i, j, previousImage.getRGB(i, j));
					if (image.getRGB(i, j) != previousImage.getRGB(i, j)) {
						imageLayers.setRGB(i, j, ColorOperations.colorToRgb(255, 0, 0));
					}
				}
			}

			imagesLayers.add(imageLayers);
		}
	}

	/**
	 * Get the scale of the image.
	 * @return The scale
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * Set the scale of the image.
	 * @param scale The new scale
	 */
	public void setScale(double scale) {
		this.scale = scale;
	}

	/**
	 * Display the previous stage.
	 */
	public void displayPrevious() {
		if(stageDisplayed > 0) {
			stageDisplayed--;
		}
	}

	/**
	 * Display the next stage.
	 */
	public void displayNext() {
		if(stageDisplayed < stage) {
			stageDisplayed++;
		}
	}

	/**
	 * Toggle overlapping stages.
	 */
	public void toggleLayers() {
		layersDisplayed = !layersDisplayed;
	}

	/**
	 * Get the current stage description.
	 * @return The description of the current stage
	 */
	public String getStageDescription() {
		switch(stageDisplayed) {
			case 1:
				return "Image straightened";
			case 2:
				return "Stave lines removed";
			case 3:
				return "Vertical lines removed";
			case 4:
				return "Patched up";
			case 5:
				return "Components collected";
			case 6:
				return "Label equivalences resolved";
			case 7:
				return "Bounding boxes drawn";
			case 8:
				return "Symbols recognized";
		}
		return "";
	}
}
