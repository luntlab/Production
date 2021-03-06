/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biotapestry.plugin;

import java.io.IOException;
import java.io.File;
import java.util.Set;
import java.util.HashSet;

import org.xml.sax.Attributes;

/***************************************************************************
**
** Used to specify plugins
*/
  
public class NodePlugInDirective extends AbstractPlugInDirective {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public static final String INTERNAL_NODE_DATA_DISPLAY_TAG = "InternalNodeDataDisplay";
  public static final String EXTERNAL_NODE_DATA_DISPLAY_TAG = "ExternalNodeDataDisplay";
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final String PLUGIN_TAG_ = "nodePlugIn";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor for internal use
  **
  */
  
  public NodePlugInDirective(String type, String className, String order, File jarFile) {
    super(type, className, order, jarFile);
  }  

  /***************************************************************************
  **
  ** Constructor for XML input
  **
  */
  
  private NodePlugInDirective() {
    super();
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Standard toString:
  **
  */
  
  public String toString() { 
    return ("NodePlugInDirective: " + mapToTypeTag(type_) + " " + className_ + " " + order_);
  }
  
  /***************************************************************************
  **
  ** Map types
  */

  public String mapToTypeTag(int val) {   
    switch (val) {
      case INTERNAL_DATA_DISPLAY:
        return (INTERNAL_NODE_DATA_DISPLAY_TAG); 
      case EXTERNAL_DATA_DISPLAY:
        return (EXTERNAL_NODE_DATA_DISPLAY_TAG); 
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Map types to values
  */

  public  int mapFromTypeTag(String tag) {
    if (tag.equalsIgnoreCase(INTERNAL_NODE_DATA_DISPLAY_TAG)) {
      return (INTERNAL_DATA_DISPLAY); 
    } else if (tag.equalsIgnoreCase(EXTERNAL_NODE_DATA_DISPLAY_TAG)) {
      return (EXTERNAL_DATA_DISPLAY);
    } else {
      throw new IllegalArgumentException();
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set keywordsOfInterest() {
    HashSet retval = new HashSet();
    retval.add(PLUGIN_TAG_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static NodePlugInDirective buildFromXML(String elemName, 
                                             Attributes attrs) throws IOException {
                                                
    if (!elemName.equals(PLUGIN_TAG_)) {
      throw new IllegalArgumentException();
    }
    NodePlugInDirective retval = new NodePlugInDirective();
    retval.stockCoreFromXML(elemName, attrs);
    return (retval); 
  }
}
