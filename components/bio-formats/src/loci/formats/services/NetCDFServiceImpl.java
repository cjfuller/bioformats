//
// NetCDFServiceImpl.java
//

/*
OME Bio-Formats package for reading and converting biological file formats.
Copyright (C) 2005-@year@ UW-Madison LOCI and Glencoe Software, Inc.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.formats.services;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import loci.common.Location;
import loci.common.services.AbstractService;
import loci.common.services.ServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Utility class for working with NetCDF/HDF files.  Uses reflection to
 * call the NetCDF Java library.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/services/NetCDFServiceImpl.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/services/NetCDFServiceImpl.java">SVN</a></dd></dl>
 */
public class NetCDFServiceImpl extends AbstractService
  implements NetCDFService {

  // -- Constants --

  public static final String NO_NETCDF_MSG =
    "NetCDF is required to read NetCDF/HDF variants.  Please obtain " +
    "the necessary JAR files from http://loci.wisc.edu/ome/formats.html.\n" +
    "Required JAR files are netcdf-4.0.jar and slf4j-jdk14.jar.";

  private static final Logger LOGGER =
    LoggerFactory.getLogger(NetCDFServiceImpl.class);

  // -- Fields --

  private String currentFile;

  private Vector<String> attributeList;

  private Vector<String> variableList;

  /** NetCDF file instance. */
  private NetcdfFile netCDFFile;

  /** Root of the NetCDF file. */
  private Group root;

  // -- NetCDFService API methods ---

  /**
   * Default constructor.
   */
  public NetCDFServiceImpl() {
    // One check from each package
    checkClassDependency(ucar.nc2.Attribute.class);
    checkClassDependency(ucar.ma2.Array.class);
  }

  /* (non-Javadoc)
   * @see loci.formats.NetCDFService#setFile(java.lang.String)
   */
  public void setFile(String file) throws IOException {
    this.currentFile = file;

    String currentId = Location.getMappedId(currentFile);
    netCDFFile = NetcdfFile.open(currentId);
    root = netCDFFile.getRootGroup();

    attributeList = new Vector<String>();
    variableList = new Vector<String>();
    List<Group> groups = new ArrayList<Group>();
    groups.add(root);
    parseAttributesAndVariables(groups);
  }

  /* (non-Javadoc)
   * @see loci.formats.NetCDFService#getFile()
   */
  public String getFile() {
    return currentFile;
  }

  /* (non-Javadoc)
   * @see loci.formats.NetCDFService#getAttributeList()
   */
  public Vector<String> getAttributeList() {
    return attributeList;
  }

  /* (non-Javadoc)
   * @see loci.formats.NetCDFService#getVariableList()
   */
  public Vector<String> getVariableList() {
    return variableList;
  }

  /* (non-Javadoc)
   * @see loci.formats.NetCDFService#getAttributeValue(java.lang.String)
   */
  public String getAttributeValue(String path) {
    String groupName = getDirectory(path);
    String attributeName = getName(path);
    Group group = getGroup(groupName);

    Attribute attribute = group.findAttribute(attributeName);
    if (attribute == null) return null;
    return arrayToString(attribute.getValues());
  }

  /* (non-Javadoc)
   * @see loci.formats.NetCDFService#getVariableValue(java.lang.String)
   */
  public Object getVariableValue(String name) throws ServiceException {
    return getArray(name, null, null);
  }

  /* (non-Javadoc)
   * @see loci.formats.NetCDFService#getArray(java.lang.String, int[], int[])
   */
  public Object getArray(String path, int[] origin, int[] shape)
    throws ServiceException
  {
    String groupName = getDirectory(path);
    String variableName = getName(path);
    Group group = getGroup(groupName);

    Variable variable = group.findVariable(variableName);
    try {
      if (origin != null && shape != null) {
        return variable.read(origin, shape).reduce().copyToNDJavaArray();
      }
      else {
        return variable.read().copyToNDJavaArray();
      }
    }
    catch (InvalidRangeException e) {
      throw new ServiceException(e);
    }
    catch (IOException e) {
      throw new ServiceException(e);
    }
  }

  /* (non-Javadoc)
   * @see loci.formats.NetCDFService#getVariableAttributes(java.lang.String)
   */
  public Hashtable<String, Object> getVariableAttributes(String name) {
    String groupName = getDirectory(name);
    String variableName = getName(name);
    Group group = getGroup(groupName);

    Variable variable = group.findVariable(variableName);
    List<Attribute> attributes = variable.getAttributes();
    Hashtable<String, Object> toReturn = new Hashtable<String, Object>();
    for (Attribute attribute: attributes) {
      toReturn.put(attribute.getName(), arrayToString(attribute.getValues()));
    }
    return toReturn;
  }

  public int getDimension(String name) {
    String groupName = getDirectory(name);
    String variableName = getName(name);
    Group group = getGroup(groupName);
    return group.findDimension(variableName).getLength();
  }

  /* (non-Javadoc)
   * @see loci.formats.NetCDFService#close()
   */
  public void close() throws IOException {
    netCDFFile.close();
    currentFile = null;
    attributeList = null;
    variableList = null;
    netCDFFile = null;
    root = null;
  }

  // -- Helper methods --

  /**
   * Recursively parses attribute and variable paths, filling
   * <code>attributeList</code> and <code>variableList</code>.
   * @param groups List of groups to recursively parse.
   */
  private void parseAttributesAndVariables(List<Group> groups) {
    for (Group group : groups) {
      String groupName = group.getName();
      List<Attribute> attributes = group.getAttributes();
      for (Attribute attribute : attributes) {
        String attributeName = attribute.getName();
        if (!groupName.endsWith("/")) attributeName = "/" + attributeName;
        attributeList.add(groupName + attributeName);
      }
      List<Variable> variables = group.getVariables();
      for (Variable variable : variables) {
        String variableName = variable.getName();
        if (!groupName.endsWith("/")) variableName = "/" + variableName;
        variableList.add(variableName);
      }
      groups = group.getGroups();
      parseAttributesAndVariables(groups);
    }
  }

  /**
   * Retrieves a group based on its fully qualified path.
   * @param path Fully qualified path to the group.
   * @return Group or <code>root</code> if the group cannot be found.
   */
  private Group getGroup(String path) {
    if (path.indexOf("/") == -1) {
      return root;
    }

    StringTokenizer tokens = new StringTokenizer(path, "/");
    Group parent = root;
    while (tokens.hasMoreTokens()) {
      parent = parent.findGroup(tokens.nextToken());
    }
    return parent == null? root : parent;
  }

  private String getDirectory(String path) {
    return path.substring(0, path.lastIndexOf("/"));
  }

  private String getName(String path) {
    return path.substring(path.lastIndexOf("/") + 1);
  }

  private String arrayToString(Array values) {
    Object v = values.copyTo1DJavaArray();
    if (v instanceof Object[]) {
      Object[] array = (Object[]) v;
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < array.length; i++) {
        sb.append((String) array[i]);
      }
      return sb.toString().trim();
    }
    return values.toString().trim();
  }

}