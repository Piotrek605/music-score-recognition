package project.utils;

import java.util.ArrayList;

/**
 * A utility class to provide a simple file filter for a file chooser dialog
 */
public class SimpleFileFilter extends javax.swing.filechooser.FileFilter
{
	private String				description	= "All Files";
	private ArrayList<String>	extensions	= new ArrayList<String>();

	/**
	 * Accept a file if it is a directory, or if it is a plain file whose
	 * extension matches one of the extension set. Filenames are converted to
	 * lower case before comparison.
	 *
	 * @param f
	 *            the file to test
	 * @return true if the given file matches the condition, otherwise false
	 */
	public boolean accept(java.io.File f)
	{
		// match directories so that the user can navigate through the file
		// system
		if (f.isDirectory())
			return (true);

		// Only matches plain files (not directories or other kinds of special
		// files
		if (!f.isFile())
			return false;

		// Only matches files whose names end with the given extensions
		String name = f.getName().toLowerCase();
		for (String extension : extensions)
			if (name.endsWith(extension))
				return true;

		// otherwise it doesn't match
		return false;
	}

	/**
	 * Get the description set for this file filter
	 *
	 * @return the description for this file filter
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * Add an extension to the list of file name extensions to be accepted by
	 * this filter
	 *
	 * @param ext
	 *            a <code>String</code> containing a file name extension (it
	 *            must begin with '.': e.g. ".tif")
	 */
	public void addExtension(String ext)
	{
		if (ext != null && ext.length() > 0)
			extensions.add(ext.toLowerCase());
	}

	/**
	 * Set a description for this filter
	 *
	 * @param desc
	 *            the description that will appear in the combo box of filters
	 *            for this filter in the file chooser dialog
	 */
	public void setDescription(String desc)
	{
		if (desc != null)
			description = desc;
	}
}
