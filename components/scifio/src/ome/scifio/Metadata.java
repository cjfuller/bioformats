package ome.scifio;

import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;

import loci.formats.FormatTools;

/**
 * Interface for all SciFIO Metadata objects.
 * Based on the format, a Metadata object can
 * be a single N-dimensional collection of bytes
 * (an image) or a list of multiple images.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="">Trac</a>,
 * <a href="">Gitweb</a></dd></dl>
 */
public interface Metadata extends Serializable {

  /**
   * Sets the 8-bit color lookup table associated with
   * the image at the given index's planes.
   * If no image planes have been opened, or if {@link #isIndexed()} returns
   * false, then this may return null. Also, if {@link #getPixelType()} returns
   * anything other than {@link FormatTools#INT8} or {@link FormatTools#UINT8},
   * this method will return null.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  void set8BitLookupTable(int iNo, byte[][] lut)
    throws FormatException, IOException;

  /**
   * Sets the 16-bit color lookup table associated with
   * the image at the given index's planes.
   * If no image planes have been opened, or if {@link #isIndexed()} returns
   * false, then this may return null. Also, if {@link #getPixelType()} returns
   * anything other than {@link FormatTools#INT16} or {@link
   * FormatTools#UINT16}, this method will return null.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  void set16BitLookupTable(int iNo, short[][] lut)
    throws FormatException, IOException;

  /** Gets the size of the X dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  void setSizeX(int iNo, int x);

  /** Gets the size of the Y dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  void setSizeY(int iNo, int y);

  /** Gets the size of the Z dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  void setSizeZ(int iNo, int z);

  /** Gets the size of the C dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  void setSizeC(int iNo, int c);

  /** Sets the size of the T dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  void setSizeT(int iNo, int t);

  /** 
   *  Set the size of the X dimension for the thumbnail.
   *  @param iNo the index of the desired image within the dataset
   */
  void setThumbSizeX(int iNo, int thumbX);

  /** 
   *  Set the size of the Y dimension for the thumbnail.
   *  @param iNo the index of the desired image within the dataset
   */
  void setThumbSizeY(int iNo, int thumbY);

  /**
   * Sets the pixel type of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   * @return the pixel type as an enumeration from {@link FormatTools}
   * <i>static</i> pixel types such as {@link FormatTools#INT8}.
   */
  void setPixelType(int iNo, int type);

  /**
   * Sets the # valid bits per pixel for the specified image.
   * @param iNo the index of the desired image within the dataset
   * @return the effective size C for the indicated image number
   */
  void setBitsPerPixel(int iNo, int bpp);

  /**
   * Sets the lengths of each subdimension of C,
   * in fastest-to-slowest rasterization order.
   * @param iNo the index of the desired image within the dataset
   */
  void setChannelDimLengths(int iNo, int[] cLengths);

  /**
   * Sets the name of each subdimension of C,
   * in fastest-to-slowest rasterization order.
   * Common subdimensional types are enumerated in {@link FormatTools}.
   * @param iNo the index of the desired image within the dataset
   */
  void setChannelDimTypes(int iNo, String[] cTypes);

  /**
   * Sets the order in which dimensions are stored.  Must be one of the following:<ul>
   *  <li>XYCZT</li>
   *  <li>XYCTZ</li>
   *  <li>XYZCT</li>
   *  <li>XYZTC</li>
   *  <li>XYTCZ</li>
   *  <li>XYTZC</li>
   * </ul>
   * @param iNo the index of the desired image within the dataset
   */
  void setDimensionOrder(int iNo, String order);

  /**
   * Sets whether the dimension order and sizes are known, or merely guesses.
   *  @param iNo the index of the desired image within the dataset
   */
  void setOrderCertain(int iNo, boolean orderCertain);

  /**
   * Sets whether the image planes in the file have more than one channel per
   * {@link #openBytes} call.
   * This method returns true if and only if {@link #getRGBChannelCount()}
   * returns a value greater than 1.
   * @param iNo the index of the desired image within the dataset
   */
  void setRGB(int iNo, boolean rgb);

  /** Sets whether the data of the image at the given index is 
   *  in little-endian format.
   *  @param iNo the index of the desired image within the dataset
   */
  void setLittleEndian(int iNo, boolean littleEndian);

  /**
   * Sets whether or not the channels of the image at the given index are interleaved.
   * This method exists because X and Y must appear first in the dimension order.
   * For interleaved data, {@link #getDimensionOrder()} returns XYCTZ or XYCZT,
   * and this method returns true.
   *
   * Note that this flag returns whether or not the data returned by
   * {@link #openBytes(int)} is interleaved.  In most cases, this will
   * match the interleaving in the original file, but for some formats (e.g.
   * TIFF) channel re-ordering is done internally and the return value of
   * this method will not match what is in the original file.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  void setInterleaved(int iNo, boolean interleaved);

  /**
   * Sets whether the planes of the image at the given index are indexed color.
   * This value has no impact on {@link #getSizeC()},
   * {@link #getEffectiveSizeC()} or {@link #getRGBChannelCount()}.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  void setIndexed(int iNo, boolean indexed);

  /**
   * Sets falseColor for the specified image index. False color should
   * return false if {@link #isIndexed()} is false, or if {@link #isIndexed()}
   * is true and the lookup table represents "real" color data. Returns true
   * if {@link #isIndexed()} is true and the lookup table is only present to aid
   * in visualization.
   * @param iNo the index of the desired image within the dataset
   */
  void setFalseColor(int iNo, boolean falseC);

  /** 
   * Sets whether or not this format's metadata is completely parsed for the specified image. 
   *  @param iNo the index of the desired image within the dataset
   */
  void setMetadataComplete(int iNo, boolean metadataComplete);

  /**
   * Sets the hashtable containing metadata field/value pairs from the
   * current image in the current file.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  void setImageMetadata(int iNo, Hashtable<String, Object> meta);

  /**
   * Sets whether the specified is a lower resolution copy of a different
   * image.
   *  @param iNo the index of the desired image within the dataset
   */
  void setThumbnailImage(int iNo, boolean thumbnail);

  /**
   * Gets the 8-bit color lookup table associated with
   * the image at the given index's planes.
   * If no image planes have been opened, or if {@link #isIndexed()} returns
   * false, then this may return null. Also, if {@link #getPixelType()} returns
   * anything other than {@link FormatTools#INT8} or {@link FormatTools#UINT8},
   * this method will return null.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  byte[][] get8BitLookupTable(int iNo) throws FormatException, IOException;

  /**
   * Gets the 16-bit color lookup table associated with
   * the image at the given index's planes.
   * If no image planes have been opened, or if {@link #isIndexed()} returns
   * false, then this may return null. Also, if {@link #getPixelType()} returns
   * anything other than {@link FormatTools#INT16} or {@link
   * FormatTools#UINT16}, this method will return null.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  short[][] get16BitLookupTable(int iNo) throws FormatException, IOException;

  /** Gets the size of the X dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  int getSizeX(int iNo);

  /** Gets the size of the Y dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  int getSizeY(int iNo);

  /** Gets the size of the Z dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  int getSizeZ(int iNo);

  /** Gets the size of the C dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  int getSizeC(int iNo);

  /** Gets the size of the T dimension of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   */
  int getSizeT(int iNo);

  /** 
   *  Get the size of the X dimension for the thumbnail.
   *  @param iNo the index of the desired image within the dataset
   */
  int getThumbSizeX(int iNo);

  /** 
   *  Get the size of the Y dimension for the thumbnail.
   *  @param iNo the index of the desired image within the dataset
   */
  int getThumbSizeY(int iNo);

  /**
   * Gets the pixel type of the image at the given index.
   * @param iNo the index of the desired image within the dataset
   * @return the pixel type as an enumeration from {@link FormatTools}
   * <i>static</i> pixel types such as {@link FormatTools#INT8}.
   */
  int getPixelType(int iNo);

  /**
   * Returns the # valid bits per pixel for the specified image.
   * @param iNo the index of the desired image within the dataset
   * @return the effective size C for the indicated image number
   */
  int getBitsPerPixel(int iNo);

  /**
   * Gets the lengths of each subdimension of C,
   * in fastest-to-slowest rasterization order.
   * @param iNo the index of the desired image within the dataset
   */
  int[] getChannelDimLengths(int iNo);

  /**
   * Gets the name of each subdimension of C,
   * in fastest-to-slowest rasterization order.
   * Common subdimensional types are enumerated in {@link FormatTools}.
   * @param iNo the index of the desired image within the dataset
   */
  String[] getChannelDimTypes(int iNo);

  /**
   * Order in which dimensions are stored.  Must be one of the following:<ul>
   *  <li>XYCZT</li>
   *  <li>XYCTZ</li>
   *  <li>XYZCT</li>
   *  <li>XYZTC</li>
   *  <li>XYTCZ</li>
   *  <li>XYTZC</li>
   * </ul>
   * @param iNo the index of the desired image within the dataset
   */
  String getDimensionOrder(int iNo);

  /**
   * Gets whether the dimension order and sizes are known, or merely guesses.
   *  @param iNo the index of the desired image within the dataset
   */
  boolean isOrderCertain(int iNo);

  /**
   * Checks if the image planes in the file have more than one channel per
   * {@link #openBytes} call.
   * This method returns true if and only if {@link #getRGBChannelCount()}
   * returns a value greater than 1.
   * @param iNo the index of the desired image within the dataset
   */
  boolean isRGB(int iNo);

  /** Gets whether the data of the image at the given index is 
   *  in little-endian format.
   *  @param iNo the index of the desired image within the dataset
   */
  boolean isLittleEndian(int iNo);

  /**
   * Gets whether or not the channels of the image at the given index are interleaved.
   * This method exists because X and Y must appear first in the dimension order.
   * For interleaved data, {@link #getDimensionOrder()} returns XYCTZ or XYCZT,
   * and this method returns true.
   *
   * Note that this flag returns whether or not the data returned by
   * {@link #openBytes(int)} is interleaved.  In most cases, this will
   * match the interleaving in the original file, but for some formats (e.g.
   * TIFF) channel re-ordering is done internally and the return value of
   * this method will not match what is in the original file.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  boolean isInterleaved(int iNo);

  /**
   * Gets whether the planes of the image at the given index are indexed color.
   * This value has no impact on {@link #getSizeC()},
   * {@link #getEffectiveSizeC()} or {@link #getRGBChannelCount()}.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  boolean isIndexed(int iNo);

  /**
   * Returns false if {@link #isIndexed()} is false, or if {@link #isIndexed()}
   * is true and the lookup table represents "real" color data. Returns true
   * if {@link #isIndexed()} is true and the lookup table is only present to aid
   * in visualization.
   * @param iNo the index of the desired image within the dataset
   */
  boolean isFalseColor(int iNo);

  /** 
   * Returns true if this format's metadata is completely parsed. 
   *  @param iNo the index of the desired image within the dataset
   */
  boolean isMetadataComplete(int iNo);

  /**
   * Obtains the hashtable containing metadata field/value pairs from the
   * specified in the current file.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  Hashtable<String, Object> getImageMetadata(int iNo);

  /**
   * Gets whether the specified image is a lower resolution copy of a different
   * image.
   *  @param iNo the index of the desired image within the dataset
   */
  boolean isThumbnailImage(int iNo);

  /**
   * Gets the number of channels returned with each call to openBytes.
   * The most common case where this value is greater than 1 is for interleaved
   * RGB data, such as a 24-bit color image plane. However, it is possible for
   * this value to be greater than 1 for non-interleaved data, such as an RGB
   * TIFF with Planar rather than Chunky configuration.
   * 
   * @param iNo the index of the desired image within the dataset
   */
  int getRGBChannelCount(int iNo);

  /**
   * Returns image count / (sizeZ * sizeT) for the specified image
   * in the current file.
   * @param iNo the index of the desired image within the dataset
   * @return the effective size C for the indicated image number
   */
  int getEffectiveSizeC(int iNo);

  /** Gets the number of images in this dataset. */
  int getImageCount();

  /**
   * Obtains the specified metadata field's value for the specified
   *  image in the current file.
   * @param field the name associated with the metadata field
   * @param iNo the index of the desired image within the dataset
   * @return the value, or null if the field doesn't exist
   */
  Object getImageMetadataValue(int iNo, String field);

  /**
   * Obtains the hashtable containing the metadata field/value pairs from
   * the current file.
   * @return the hashtable containing all non-image-specific metadata
   * from the file
   */
  Hashtable<String, Object> getGlobalMetadata();

  /**
   * Obtains the specified metadata field's value for the current file.
   * @param field the name associated with the metadata field
   * @param iNo the index of the desired image within the dataset
   * @return the value, or null if the field doesn't exist
   */
  Object getMetadataValue(int iNo, String field);

  /**
   * Resets this Metadata object's values as though it had just been
   * instantiated.
   */
  void resetMeta(Class<?> type);
}
