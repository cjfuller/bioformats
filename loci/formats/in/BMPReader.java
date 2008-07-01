//
// BMPReader.java
//

/*
OME Bio-Formats package for reading and converting biological file formats.
Copyright (C) 2005-@year@ UW-Madison LOCI and Glencoe Software, Inc.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.formats.in;

import java.io.IOException;
import loci.formats.*;
import loci.formats.codec.BitBuffer;
import loci.formats.meta.FilterMetadata;
import loci.formats.meta.MetadataStore;

/**
 * BMPReader is the file format reader for Microsoft Bitmap (BMP) files.
 * See http://astronomy.swin.edu.au/~pbourke/dataformats/bmp/ for a nice
 * description of the BMP file format.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/loci/formats/in/BMPReader.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/loci/formats/in/BMPReader.java">SVN</a></dd></dl>
 *
 * @author Melissa Linkert linkert at wisc.edu
 */
public class BMPReader extends FormatReader {

  // -- Constants --

  /** Compression types. */
  private static final int RAW = 0;
  private static final int RLE_8 = 1;
  private static final int RLE_4 = 2;
  private static final int RGB_MASK = 3;

  // -- Fields --

  /** Offset to the image data. */
  private int offset;

  /** Number of bits per pixel. */
  private int bpp;

  /** The palette for indexed color images. */
  private byte[][] palette;

  /** Compression type */
  private int compression;

  /** Offset to image data. */
  private long global;

  // -- Constructor --

  /** Constructs a new BMP reader. */
  public BMPReader() {
    super("Windows Bitmap", "bmp");
    blockCheckLen = 2;
  }

  // -- IFormatReader API methods --

  /* @see loci.formats.IFormatReader#isThisType(byte[]) */
  public boolean isThisType(byte[] block) {
    if (block.length < blockCheckLen) return false;
    return new String(block).startsWith("BM");
  }

  /* @see loci.formats.IFormatReader#get8BitLookupTable() */
  public byte[][] get8BitLookupTable() throws FormatException, IOException {
    FormatTools.assertId(currentId, true, 1);
    return palette;
  }

  /**
   * @see loci.formats.IFormatReader#openBytes(int, byte[], int, int, int, int)
   */
  public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
    throws FormatException, IOException
  {
    FormatTools.assertId(currentId, true, 1);
    FormatTools.checkPlaneNumber(this, no);
    FormatTools.checkBufferSize(this, buf.length, w, h);

    if (compression != RAW) {
      throw new FormatException("Compression type " + compression +
        " not supported");
    }

    int rowsToSkip = core.sizeY[0] - (h + y);
    int rowLength = core.sizeX[0] * (isIndexed() ? 1 : core.sizeC[0]);
    in.seek(global + rowsToSkip * rowLength);

    int planeSize = core.sizeX[0] * core.sizeY[0] * core.sizeC[0];
    if (bpp >= 8) planeSize *= (bpp / 8);
    else planeSize /= (8 / bpp);
    byte[] rawPlane = new byte[planeSize];
    in.read(rawPlane);

    BitBuffer bb = new BitBuffer(rawPlane);

    if ((palette != null && palette[0].length > 0) || core.sizeC[0] == 1) {
      for (int row=h-1; row>=0; row--) {
        bb.skipBits(x * bpp);
        for (int i=0; i<w; i++) {
          buf[row*w + i] = (byte) (bb.getBits(bpp) & 0xff);
        }
        bb.skipBits((core.sizeX[0] - w - x) * bpp);
      }
    }
    else {
      int len = core.sizeX[0] * core.sizeC[0];
      for (int row=h-1; row>=y; row--) {
        bb.skipBits(x * core.sizeC[0] * bpp);
        for (int i=0; i<w*core.sizeC[0]; i++) {
          buf[row*w*core.sizeC[0] + i] = (byte) (bb.getBits(bpp) & 0xff);
        }
        bb.skipBits(core.sizeC[0] * (core.sizeX[0] - w - x) * bpp);
      }
      for (int i=0; i<buf.length/core.sizeC[0]; i++) {
        byte tmp = buf[i*core.sizeC[0] + 2];
        buf[i*core.sizeC[0] + 2] = buf[i*core.sizeC[0]];
        buf[i*core.sizeC[0]] = tmp;
      }
    }
    return buf;
  }

  // -- IFormatHandler API methods --

  /* @see loci.formats.IFormatHandler#close() */
  public void close() throws IOException {
    super.close();
    offset = bpp = compression = 0;
    global = 0;
    palette = null;
  }

  // -- Internel FormatReader API methods --

  /* @see loci.formats.FormatReader#initFile(String) */
  protected void initFile(String id) throws FormatException, IOException {
    if (debug) debug("BMPReader.initFile(" + id + ")");
    super.initFile(id);
    in = new RandomAccessStream(id);

    status("Reading bitmap header");

    in.order(true);

    // read the first header - 14 bytes

    addMeta("Magic identifier", in.readString(2));

    addMeta("File size (in bytes)", "" + in.readInt());
    in.skipBytes(4); // reserved

    // read the offset to the image data
    offset = in.readInt();

    // read the second header - 40 bytes

    in.skipBytes(4);

    // get the dimensions

    core.sizeX[0] = in.readInt();
    //while ((core.sizeX[0] % 2) != 0) core.sizeX[0]++;
    core.sizeY[0] = in.readInt();

    if (core.sizeX[0] < 1 || core.sizeY[0] < 1) {
      throw new FormatException("Invalid image dimensions: " +
        core.sizeX[0] + " x " + core.sizeY[0]);
    }
    addMeta("Image width", "" + core.sizeX[0]);
    addMeta("Image height", "" + core.sizeY[0]);

    addMeta("Color planes", "" + in.readShort());
    bpp = in.readShort();
    addMeta("Bits per pixel", "" + bpp);

    compression = in.readInt();
    String comp = "invalid";

    switch (compression) {
      case RAW:
        comp = "None";
        break;
      case RLE_8:
        comp = "8 bit run length encoding";
        break;
      case RLE_4:
        comp = "4 bit run length encoding";
        break;
      case RGB_MASK:
        comp = "RGB bitmap with mask";
        break;
    }

    addMeta("Compression type", comp);

    in.skipBytes(4);
    float pixelSizeX = (float) in.readInt();
    float pixelSizeY = (float) in.readInt();
    addMeta("X resolution", "" + pixelSizeX);
    addMeta("Y resolution", "" + pixelSizeY);
    int nColors = in.readInt();
    if (nColors == 0 && bpp != 32 && bpp != 24) {
      nColors = bpp < 8 ? 1 << bpp : 256;
    }
    in.skipBytes(4);

    // read the palette, if it exists

    if (nColors != 0 && bpp == 8) {
      palette = new byte[3][256];

      for (int i=0; i<nColors; i++) {
        for (int j=palette.length-1; j>=0; j--) {
          palette[j][i] = in.readByte();
        }
        in.skipBytes(1);
      }
    }
    else if (nColors != 0) in.skipBytes(nColors * 4);

    global = in.getFilePointer();

    addMeta("Indexed color", String.valueOf(palette != null));

    status("Populating metadata");

    core.sizeC[0] = bpp != 24 ? 1 : 3;
    if (bpp == 32) core.sizeC[0] = 4;
    if (bpp > 8) bpp /= core.sizeC[0];
    //while (bpp % 8 != 0) bpp++;

    switch (bpp) {
      case 16:
        core.pixelType[0] = FormatTools.UINT16;
        break;
      case 32:
        core.pixelType[0] = FormatTools.UINT32;
        break;
      default:
        core.pixelType[0] = FormatTools.UINT8;
    }

    core.rgb[0] = core.sizeC[0] > 1;
    core.littleEndian[0] = true;
    core.interleaved[0] = true;
    core.imageCount[0] = 1;
    core.sizeZ[0] = 1;
    core.sizeT[0] = 1;
    core.currentOrder[0] = "XYCTZ";
    core.metadataComplete[0] = true;
    core.indexed[0] = palette != null;
    if (core.indexed[0]) {
      core.sizeC[0] = 1;
      core.rgb[0] = false;
    }
    core.falseColor[0] = false;

    // Populate metadata store.

    // The metadata store we're working with.
    MetadataStore store =
      new FilterMetadata(getMetadataStore(), isMetadataFiltered());
    store.setImageName("", 0);
    MetadataTools.setDefaultCreationDate(store, id, 0);
    MetadataTools.populatePixels(store, this);

    // resolution is stored as pixels per meter; we want to convert to
    // microns per pixel

    float correctedX = pixelSizeX == 0f ? 0f : 1000000 / pixelSizeX;
    float correctedY = pixelSizeY == 0f ? 0f : 1000000 / pixelSizeY;

    store.setDimensionsPhysicalSizeX(new Float(correctedX), 0, 0);
    store.setDimensionsPhysicalSizeY(new Float(correctedY), 0, 0);
  }

}
