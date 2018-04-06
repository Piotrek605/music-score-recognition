package project.utils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Encapsulates an image file containing possibly multiple images
 */
public class ImageFile
{
	private ImageReader	ir;
	private int			numImages;
	private File		file;

	/**
	 * Opens a File object and initialises a project.utils.ImageFile object from
	 * it
	 *
	 * @param f
	 *            the File object to create the project.utils.ImageFile from
	 * @throws IOException
	 *             if f is not readable or does not exist
	 * @throws UnsupportedImageTypeException
	 *             if no suitable image reader can be found for this file or if
	 *             the file does not contain a correctly formatted image
	 */
	public ImageFile(File f) throws IOException, UnsupportedImageTypeException
	{
		ImageInputStream iis;

		if (!f.exists() || !f.canRead())
			throw new IOException("File: \"" + f.getName()
					+ "\" does not exist or is unreadable");
		iis = ImageIO.createImageInputStream(f);
		Iterator<ImageReader> irit = ImageIO.getImageReaders(iis);
		if (!irit.hasNext())
		{
			iis.close();
			throw new UnsupportedImageTypeException("File: \"" + f.getName()
					+ "\" does not contain an image format that this program can handle");
		}
		file = f;
		ir = irit.next();
		ir.setInput(iis, false);
		numImages = ir.getNumImages(true);
	}

	/**
	 * get the base name on the file currently open (i.e. with no path prefix or
	 * file name extension suffix)
	 *
	 * @return the name of the file, without any preceding path or filename
	 *         extension
	 */
	public String getBaseName()
	{
		String name = file.getName();
		int i = name.lastIndexOf('.');
		if (i < 1) // if no '.' or the last '.' is the first character
			return name;
		return name.substring(0, i);
	}

	/**
	 * get the name on the file currently open (i.e. with no path prefix)
	 *
	 * @return the name of the file, without any preceding path
	 */
	public String getName()
	{
		return file.getName();
	}

	/**
	 * get the directory that this file is in
	 *
	 * @return parent directory of this file
	 */
	public File getDir()
	{
		return file.getParentFile();
	}

	/**
	 * releases resources held by this object (e.g. open file descriptors etc.)
	 */
	public void dispose()
	{
		if (ir != null)
		{
			ir.dispose();
			ir = null;
		}
	}

	/**
	 * Make sure all resources are recovered when this object is discarded
	 */
	protected void finalize()
	{
		try
		{
			dispose();
		}
		finally
		{
			try
			{
				super.finalize();
			}
			catch (Throwable ignored)
			{
			}
		}
	}

	/**
	 * Some image file formats (e.g. tiff) can hold multiple images in the same
	 * file. This method returns the number of images in this file
	 *
	 * @return Number of pages of images in this image file
	 */
	public int getNumImages()
	{
		return numImages;
	}

	/**
	 * Reads an image from an project.utils
	 *
	 * @param imageNo
	 *            the index of the image to read: in the range
	 *            <code>[0..getNumImages()-1]</code>
	 * @return the requested image
	 * @throws IOException
	 *             if the <code>project.utils.ImageFile</code> has already been
	 *             disposed of or if there is an error in reading the image
	 */
	public BufferedImage getBufferedImage(int imageNo) throws IOException
	{
		if (ir == null)
			throw new IOException(
					"Attempt to read image when image reader has already been disposed of");
		if (imageNo < 0 || imageNo >= numImages)
		{
			throw new IndexOutOfBoundsException("Tried to read image number "
					+ imageNo + " from image file " + file + " which contains "
					+ (numImages == 0 ? "no images"
							: numImages == 1 ? "only image 0"
									: "images 0.." + (numImages - 1)));
		}

		BufferedImage bi = ir.read(imageNo);

		// In case the image is not in the standard Java colour space or the
		// standard Java
		// encoding, translate it. This is typically only necessary if you want
		// to optimise
		// speed of some kinds of access to or manipulation of the image.
		bi = convertToIntRGB(bi);

		return bi;

	}

	/**
	 * Utility method to return a TYPE_INT_RGB RGB version of a buffered image
	 *
	 * @param bi
	 *            the input image
	 * @return the corresponding TYPE_INT_RGB RGB version of the image
	 */
	public static BufferedImage convertToIntRGB(BufferedImage bi)
	{
		if (bi.getType() == BufferedImage.TYPE_INT_RGB && bi.getColorModel()
				.getColorSpace().getType() == ColorSpace.CS_sRGB)
			return bi;

		BufferedImage argbImage = new BufferedImage(bi.getWidth(),
				bi.getHeight(), BufferedImage.TYPE_INT_RGB);
		ColorConvertOp cco = new ColorConvertOp(
				bi.getColorModel().getColorSpace(),
				ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
		cco.filter(bi, argbImage);
		return argbImage;
	}
}
