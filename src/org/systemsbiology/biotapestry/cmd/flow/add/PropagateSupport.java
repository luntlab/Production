/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GroupChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GeneInstance;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupChange;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.BusDrop;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkBusDrop;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;


/****************************************************************************
**
** Collection of commands to add things
*/

public class PropagateSupport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor:
  */ 
  
  private PropagateSupport() {
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Change node color to match link color
  */  

  public static void changeNodeColor(BTState appState, DataAccessContext rcx, BusProperties bp, String linkID, UndoSupport support) { 
    //
    // Change node color here!
    //
    Linkage link = rcx.getGenome().getLinkage(linkID);
    String srcID = link.getSource();
    Node srcNode = rcx.getGenome().getNode(srcID);
    if (NodeProperties.setWithLinkColor(srcNode.getNodeType())) {
      String linkColor = bp.getColorName();            
      Layout.PropChange[] lpc = new Layout.PropChange[1];
      NodeProperties oldProps = rcx.getLayout().getNodeProperties(srcID);            
      NodeProperties newProps = oldProps.clone();
      if (srcNode.getNodeType() == Node.INTERCELL) {
        if (newProps.getColorName().equals(linkColor)) {
          newProps.setSecondColor(null);
        } else {
          newProps.setSecondColor(linkColor);
        }
      } else {
        newProps.setColor(linkColor);
      } 
      lpc[0] = rcx.getLayout().replaceNodeProperties(oldProps, newProps);
      if (lpc != null) {
        PropChangeCmd pcc = new PropChangeCmd(appState, rcx, lpc);
        support.addEdit(pcc);
      }
    }
    return;
  } 

  /***************************************************************************
  **
  ** Propagate Down.  Second group ID can be null.
  */
  
  public static void pullDownsFromFrame(BTState appState, Set<String> nodeIDs, Set<String> linkIDs, String groupID1, String groupID2) {
    String genomeKey = appState.getGenome();
    String layoutKey = appState.getLayoutKey();    
    propagateDownElementsFromRootToGroup(appState, nodeIDs, linkIDs, genomeKey, layoutKey, groupID1, groupID2);    
    appState.getSUPanel().drawModel(false);
    return;
  }

  /***************************************************************************
  **
  ** Add every element for a group in a subset instance
  */  
 
  public static boolean addAllElementsInGroupToSubsetInstance(BTState appState, DataAccessContext rcxT, //String genomeKey, 
                                                              GenomeInstance parent,
                                                              String groupID) {                                                       
    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState, "undo.addAllElementsInGroupToSubsetInstance"); 
    GenomeInstance gi = rcxT.getGenomeAsInstance();
    
    int genCount = parent.getGeneration();
    String baseID = Group.getBaseID(groupID);
    String inherit = Group.buildInheritedID(baseID, genCount);           
    Group parentGroup = parent.getGroup(inherit);    
    
    Iterator<Node> nit = parent.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      if (gi.getNode(node.getID()) != null) {
        continue;
      }
      if (parentGroup.isInGroup(node.getID(), parent)) {
        addNewNodeToSubsetInstance(appState, rcxT, (NodeInstance)node, support);
      }
    }
    //
    // Nodes are in, so let's crank the list of linkages to add to the subset:
    //
    
    Iterator<Linkage> lit = parent.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (gi.getLinkage(link.getID()) != null) {
        continue;
      }
      Group srcGrp = parent.getGroupForNode(link.getSource(), GenomeInstance.ALWAYS_MAIN_GROUP);
      if ((srcGrp == null) || !srcGrp.getID().equals(inherit)) {
        continue;
      }
      Group trgGrp = parent.getGroupForNode(link.getTarget(), GenomeInstance.ALWAYS_MAIN_GROUP);
      if ((trgGrp == null) || !trgGrp.getID().equals(inherit)) {
        continue;
      }      
      addNewLinkToSubsetInstance(appState, rcxT, (LinkageInstance)link, support);
    }
    
    support.addEvent(new ModelChangeEvent(rcxT.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));    
    support.finish();
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Add a new node to a VFN model by inheritance.
  */  
 
  public static boolean addNewNodeToSubsetInstance(BTState appState, DataAccessContext rcxT,  
                                                   NodeInstance useNode,
                                                   UndoSupport support) {
 
    GenomeInstance gi = rcxT.getGenomeAsInstance();
    
    GenomeInstance parent = gi.getVfgParent();
    if (parent == null) {
      throw new IllegalArgumentException();
    } 
    
    boolean doGene = (useNode.getNodeType() == Node.GENE);
  
    GenomeChange gc;
    if (doGene) {
      GeneInstance newInstance = new GeneInstance((GeneInstance)useNode);
      gc = gi.addGene(newInstance);
    } else {
      NodeInstance newInstance = new NodeInstance(useNode);
      gc = gi.addNode(newInstance);
    }
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcxT, gc);
      if (support == null) {
        throw new IllegalArgumentException();
      }
      support.addEdit(gcc);
      support.addEvent(new ModelChangeEvent(rcxT.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
    }

    return (true); 
  }    
 
  /***************************************************************************
  **
  ** Add a new link to a VFN model by inheritance.  Do this all as one undo operation.
  */  
 
  public static boolean addNewLinkWithNodesToSubsetInstance(BTState appState, DataAccessContext rcxT, //String genomeKey, 
                                                             GenomeInstance parent,
                                                             Set<LinkageInstance> pullSurvivors,
                                                             Set<String> nodesToAdd) {                                                       
    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState, "undo.addNewLinkWithNodesToSubsetInstance");                                                                
                                                       
    Iterator<String> ntait = nodesToAdd.iterator();
    while (ntait.hasNext()) {
      String nodeKey = ntait.next();
      NodeInstance pNode = (NodeInstance)parent.getNode(nodeKey);               
      addNewNodeToSubsetInstance(appState, rcxT, pNode, support);
    }
    //
    // Nodes are in, so let's crank the list of linkages to add to the subset:
    //
    Iterator<LinkageInstance> psit = pullSurvivors.iterator();
    while (psit.hasNext()) {
      LinkageInstance subsetLink = psit.next();
      addNewLinkToSubsetInstance(appState, rcxT, subsetLink, support);
    }
    support.addEvent(new ModelChangeEvent(rcxT.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));    
    support.finish();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Add a new link to a VFN model by inheritance.
  */  
 
  public static LinkageInstance addNewLinkToSubsetInstance(BTState appState, DataAccessContext rcxT, //String genomeKey, 
                                                           LinkageInstance useLink,
                                                           UndoSupport support) {
 
    GenomeInstance gi = rcxT.getGenomeAsInstance();
    
    GenomeInstance parent = gi.getVfgParent();
    if (parent == null) {
      throw new IllegalArgumentException();
    } 
  
    LinkageInstance newInstance = new LinkageInstance(useLink); 
    GenomeChange gc = gi.addLinkage(newInstance);
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcxT, gc);
      support.addEdit(gcc);
    }
    return (newInstance); 
  }
  
  /***************************************************************************
  **
  ** Propagate a LinkProperties to all interested layouts  This is for link
  ** properties that are not being merged into an existing tree.
  */  

  public static void propagateLinkProperties(BTState appState, DataAccessContext rcxT, //Database db, Genome genome,
                                              BusProperties lp, String linkID, UndoSupport support) {
    Iterator<Layout> loit = rcxT.lSrc.getLayoutIterator();
    String genomeKey = rcxT.getGenomeID();
    while (loit.hasNext()) {
      Layout lo = loit.next();
      String loTarg = lo.getTarget();
      if (loTarg.equals(genomeKey)) {
        BusProperties bp = new BusProperties(lp); 
        Layout.PropChange[] lpc = new Layout.PropChange[1];    
        lpc[0] = lo.setLinkPropertiesForUndo(linkID, bp, null);
        if (lpc != null) {
          PropChangeCmd pcc = new PropChangeCmd(appState, rcxT, lpc);
          support.addEdit(pcc);
        }
        //
        // Change node color here!
        //
        DataAccessContext rcxC = new DataAccessContext(rcxT);
        rcxC.setLayout(lo);
        changeNodeColor(appState, rcxC, bp, linkID, support);
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Propagate a LinkProperties to all layouts of a VFG
  */  

  public static void downPropagateLinkProperties(BTState appState, DataAccessContext rcx,
                                                 BusProperties lp, 
                                                 LinkageInstance newLink, Point2D srcPt,
                                                 UndoSupport support, String srcLinkID) {
                                         
    String linkID = newLink.getID();
    String source = newLink.getSource();
                
    //
    // From the root genome, we receive a LinkProperties.   If it is a single
    // property, just propagate it around.  If it is a tree, we need to extract
    // a single link from it, then try to merge it into any trees in Vfg layouts.
    //
    // FIX ME: Two single links to different target instances should be merged
    // into a tree!  (e.g. wnt8 to friz junction).  (I think this is fixed).

    Iterator<Layout> loit = rcx.lSrc.getLayoutIterator();
    String genomeKey = rcx.getGenomeID();
    while (loit.hasNext()) {
      Layout lo = loit.next();
      String loTarg = lo.getTarget();
      if (loTarg.equals(genomeKey)) {
        //
        // Find out if anybody else has the same source.  If so, use that
        // tree.  Otherwise, just stick the single link in.
        //
        NodeProperties snp = lo.getNodeProperties(source);
        Point2D startLoc = (Point2D)snp.getLocation().clone();
        Vector2D offset = new Vector2D(srcPt, startLoc);
          
        // Make a single-path tree:
        LinkBusDrop keepDrop = lp.getTargetDrop(srcLinkID);
        BusProperties spTree = new BusProperties(lp, (BusDrop)keepDrop);
        spTree = new BusProperties(spTree, source, linkID, offset);
        DataAccessContext rcxL = new DataAccessContext(rcx);
        rcxL.setLayout(lo);
        Layout.PropChange lpc = lo.foldInNewProperty(newLink, spTree, rcxL);        
        PropChangeCmd pcc = new PropChangeCmd(appState, rcxL, new Layout.PropChange[] {lpc});
        support.addEdit(pcc);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Propagate all the elements down from the full genome to the VfG
  */  
 
  public static boolean propagateDownEntireRootToGroup(BTState appState, DataAccessContext rcxT, DataAccessContext rcxR, String groupKey) {
  
    //
    // Build up lists of genes, nodes, and links that are going down.
    //
     
    HashSet<Node> nodes = new HashSet<Node>();    
    Group group = rcxT.getGenomeAsInstance().getGroup(groupKey);    
    
    Iterator<Node> nit = rcxR.getGenome().getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      if (group.instanceIsInGroup(node.getID())) {
        continue;
      }
      nodes.add(node);
    }
    
    //
    // Nodes first, then linkages, to insure our targets are there:
    //
    
    Set<String> linkages = new HashSet<String>();
    Iterator<Linkage> lit = rcxR.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      Set<String> ids = rcxT.getGenomeAsInstance().returnLinkInstanceIDsForBacking(link.getID());
      if (ids.isEmpty()) {
        linkages.add(link.getID());
      } else {
        boolean notThere = true;
        Iterator<String> sit = ids.iterator();
        while (sit.hasNext()) {
          String instanceID = sit.next();
          GenomeInstance.GroupTuple tup = rcxT.getGenomeAsInstance().getRegionTuple(instanceID);
          if (tup.getSourceGroup().equals(groupKey) && tup.getTargetGroup().equals(groupKey)) {
            notThere = false;
            break;
          }

        }
        if (notThere) {
          linkages.add(link.getID());
        }
      }
    }    

    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState, "undo.propagateAllDownToGroup");      
    
    //
    // Figure out the position offset to use:
    //

    Point2D existCentroid = rcxT.getLayout().getApproxCenterForGroup(rcxT.getGenomeAsInstance(), groupKey, false);
    Point2D existOrigCentroid = rcxR.getLayout().getApproxCenterForGroup(rcxT.getGenomeAsInstance(), groupKey, true);
    if ((existCentroid == null) || (existOrigCentroid == null)) {
      GroupProperties gp = rcxT.getLayout().getGroupProperties(groupKey);
      existCentroid = gp.getLabelLocation();      
      existOrigCentroid = rcxR.getLayout().getApproxCenter(rcxR.getGenome().getAllNodeIterator());
    }
    Vector2D offset;
    if ((existCentroid != null) && (existOrigCentroid != null)) {
      offset = new Vector2D(existOrigCentroid, existCentroid);       
    } else {
      offset = new Vector2D(0.0, 0.0);
    }
    
    //
    // We are given a list of selected items that we are to propagate
    // down. Crank through the list and propagate.
    //

    Iterator<Node> nlit = nodes.iterator();
    while (nlit.hasNext()) {
      Node node = nlit.next();
      propagateNode(appState, (node.getNodeType() == Node.GENE), rcxT, (DBNode)node, rcxR.getLayout(), offset, group, support);  
    }
    
    GenomeInstance.GroupTuple tup = new GenomeInstance.GroupTuple(groupKey, groupKey);    
    Iterator<String> sit = linkages.iterator();
    while (sit.hasNext()) {
      String lnkKey = sit.next();
      DBLinkage dbLink = (DBLinkage)rcxR.getGenome().getLinkage(lnkKey);
      propagateLinkage(appState, rcxT, dbLink, rcxR, tup, support);
    } 
    support.finish();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Propagate one element down from the full genome to the VfA
  */  
 
  public static boolean propagateDownElementsFromRootToGroup(BTState appState, Set<String> nodeIDs, Set<String> linkIDs, 
                                                             String genomeKey, String layoutKey, 
                                                             String groupKey1, String groupKey2) {

    Database db = appState.getDB();
    DataAccessContext rcxR = new DataAccessContext(appState); // Root genome
    DataAccessContext rcxT = new DataAccessContext(appState, db.getGenome(genomeKey), db.getLayout(layoutKey)); // target
    
    //
    // Get groups resolved:
    //
    
    if (groupKey2 == null) {
      groupKey2 = groupKey1;
    }
    boolean splitGroups = !groupKey2.equals(groupKey1);
    
    Group group1 = rcxT.getGenomeAsInstance().getGroup(groupKey1); 
    Group group2 = rcxT.getGenomeAsInstance().getGroup(groupKey2);   
    
    //
    // Build up sets of nodes and links that are going down.
    //
       
    HashSet<String> nodes = new HashSet<String>();
    Iterator<String> nit = nodeIDs.iterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      int iNum = rcxT.getGenomeAsInstance().getInstanceForNodeInGroup(nodeID, groupKey1);
      if (iNum == -1) {
        nodes.add(nodeID);
      }
    }
    
    HashSet<String> group2Nodes = new HashSet<String>();
    HashSet<String> linkages = new HashSet<String>();
    Iterator<String> lit = linkIDs.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      boolean added = false;
      Set<String> ids = rcxT.getGenomeAsInstance().returnLinkInstanceIDsForBacking(linkID);
      if (ids.isEmpty()) {
        linkages.add(linkID);
        added = true;
      } else {
        boolean notThere = true;
        Iterator<String> sit = ids.iterator();
        while (sit.hasNext()) {
          String instanceID = sit.next();
          GenomeInstance.GroupTuple tup = rcxT.getGenomeAsInstance().getRegionTuple(instanceID);
          if (tup.getSourceGroup().equals(groupKey1) && tup.getTargetGroup().equals(groupKey2)) {
            notThere = false;
            break;
          }
        }
        if (notThere) {
          linkages.add(linkID);
          added = true;
        }
      }
      //
      // Add in link source and target nodes dragged along for the ride:
      //
      if (added) {
        Linkage link = rcxR.getGenome().getLinkage(linkID);
        String src = link.getSource();
        int iNum = rcxT.getGenomeAsInstance().getInstanceForNodeInGroup(src, groupKey1);
        if (iNum == -1) {
          nodes.add(src);
        }
        String trg = link.getTarget();
        iNum = rcxT.getGenomeAsInstance().getInstanceForNodeInGroup(trg, groupKey2);
        if (iNum == -1) {
          if (splitGroups) {
            group2Nodes.add(trg);
          } else {
            nodes.add(trg);
          }
        }
      }
    }
 
    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState, "undo.propagateElementsDownToGroup");      
    
    //
    // Figure out the position offset to use:
    //

    Point2D existCentroid = rcxT.getLayout().getApproxCenterForGroup(rcxT.getGenomeAsInstance(), groupKey1, false);
    Point2D existOrigCentroid = rcxR.getLayout().getApproxCenterForGroup(rcxT.getGenomeAsInstance(), groupKey1, true);
    if ((existCentroid == null) || (existOrigCentroid == null)) {
      GroupProperties gp = rcxT.getLayout().getGroupProperties(groupKey1);
      existCentroid = gp.getLabelLocation();      
      existOrigCentroid = rcxR.getLayout().getApproxCenter(rcxR.getGenome().getAllNodeIterator());
    }
    Vector2D offset;
    if ((existCentroid != null) && (existOrigCentroid != null)) {
      offset = new Vector2D(existOrigCentroid, existCentroid);       
    } else {
      offset = new Vector2D(0.0, 0.0);
    }
        
    //
    // Nodes first, then linkages, to insure our targets are there:
    //    

    Iterator<String> nlit = nodes.iterator();
    while (nlit.hasNext()) {
      String nodeID = nlit.next();
      Node node = rcxR.getGenome().getNode(nodeID);
      propagateNode(appState, (node.getNodeType() == Node.GENE), rcxT, (DBNode)node, rcxR.getLayout(), offset, group1, support);  
    }
    
    Iterator<String> nlit2 = group2Nodes.iterator();
    while (nlit2.hasNext()) {
      String nodeID = nlit2.next();
      Node node = rcxR.getGenome().getNode(nodeID);
      propagateNode(appState, (node.getNodeType() == Node.GENE), rcxT, (DBNode)node, rcxR.getLayout(), offset, group2, support);  
    }    
    
    GenomeInstance.GroupTuple tup = new GenomeInstance.GroupTuple(groupKey1, groupKey2);    
    Iterator<String> sit = linkages.iterator();
    while (sit.hasNext()) {
      String lnkKey = sit.next();
      DBLinkage dbLink = (DBLinkage)rcxR.getGenome().getLinkage(lnkKey);
      propagateLinkage(appState, rcxT, dbLink, rcxR, tup, support);
    }
    
    support.addEvent(new ModelChangeEvent(genomeKey, ModelChangeEvent.UNSPECIFIED_CHANGE)); 
    support.finish();
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Propagate the gene down to the given target genome and region.
  */  
 
  public static boolean autoPropagateNodeDown(BTState appState, boolean doGene, DataAccessContext rcxR, DataAccessContext rcxT,
                                              String selectedGroupID, DBNode node, UndoSupport support) {

    Group selectedGroup = rcxT.getGenomeAsInstance().getGroup(selectedGroupID);
      
    Vector2D offset; 
    Point2D existCentroid = rcxT.getLayout().getApproxCenterForGroup(rcxT.getGenomeAsInstance(), selectedGroupID, false);
    Point2D existOrigCentroid = rcxR.getLayout().getApproxCenterForGroup(rcxT.getGenomeAsInstance(), selectedGroupID, true);
    if ((existCentroid == null) || (existOrigCentroid == null)) {
      offset = new Vector2D(0.0, 0.0);
    } else {
      offset = new Vector2D(existOrigCentroid, existCentroid);
    }
    
    propagateNode(appState, doGene, rcxT, node, rcxR.getLayout(), offset, selectedGroup, support);
    return (true);
  }  

  /***************************************************************************
  **
  ** Propagate the selected node down from the full genome to the VfA
  */  
 
  public static String propagateNode(BTState appState, boolean doGene, DataAccessContext rcxT, DBNode node, Layout layout, //<- Source layout!
                                     Vector2D offset, Group group, UndoSupport support) {
    Node newNode = propagateNodeNoLayout(appState, doGene, rcxT, node, group, support, null);
    propagateNodeLayoutProps(appState, rcxT, node, newNode, layout, offset, support);
    return (newNode.getID());
  }
    
  /***************************************************************************
  **
  ** Propagate the selected node layout
  */  
 
  public static void propagateNodeLayoutProps(BTState appState, DataAccessContext rcxT,
                                              Node node, Node newNode, 
                                              Layout srcLayout,
                                              Vector2D offset, 
                                              UndoSupport support) {
    // Find new location  

    NodeProperties oldProp = srcLayout.getNodeProperties(node.getID());
    Point2D loc = oldProp.getLocation();
    double xPos = loc.getX() + offset.getX();
    double yPos = loc.getY() + offset.getY();
    
    NodeProperties newProp = new NodeProperties(rcxT.cRes, rcxT.getLayout(), newNode.getNodeType(), 
                                                newNode.getID(), xPos, yPos, false);
    newProp.setColor(oldProp.getColorName());
    newProp.setSecondColor(oldProp.getSecondColorName());
    newProp.setOrientation(oldProp.getOrientation());
    String genomeKey = rcxT.getGenomeID();
    String loTarg = rcxT.getLayout().getTarget();
    if (!loTarg.equals(genomeKey)) {
      throw new IllegalStateException();
    }
    Layout.PropChange[] lpc = new Layout.PropChange[1]; 
    NodeProperties newProps = new NodeProperties(newProp, rcxT.getLayout(), newNode.getNodeType());
    lpc[0] = rcxT.getLayout().setNodeProperties(newNode.getID(), newProps);
    if (lpc != null) {
      PropChangeCmd pcc = new PropChangeCmd(appState, rcxT, lpc);
      support.addEdit(pcc);
    }        
    return; // Previously returned a boolean, but it was ALWAYS true.
  }
  
  /***************************************************************************
  **
  ** Propagate the selected node down from the full genome to the VfG, without
  ** layout support
  */  
 
  public static NodeInstance propagateNodeNoLayout(BTState appState, boolean doGene, DataAccessContext rcxI,
                                                   DBNode node,
                                                   Group group, UndoSupport support,
                                                   String instanceID) {
                                              
    return (propagateOldOrNewNodeNoLayout(appState, doGene, rcxI, null, node, group, support, instanceID, null));
  }
  
  /***************************************************************************
  **
  ** Propagate the selected node down from the full genome to the VfG, without
  ** layout support
  */  
 
  public static NodeInstance propagateOldOrNewNodeNoLayout(BTState appState, boolean doGene,
                                                           DataAccessContext rcxI,
                                                           GenomeInstance oldGi,
                                                           DBNode node, Group group, UndoSupport support,
                                                           String instanceID, Map<String, Integer> topNodeInstanceNums) {                                                                                                     
    int instanceCount;
    String nodeID = node.getID();
    if (instanceID == null) {
      if (topNodeInstanceNums != null) {
        Integer topNum = topNodeInstanceNums.get(nodeID);
        Integer useNum = (topNum == null) ? new Integer(0) : new Integer(topNum.intValue() + 1);
        topNodeInstanceNums.put(nodeID, useNum);
        instanceCount = useNum.intValue(); 
      } else {
        instanceCount = rcxI.getGenomeAsInstance().getNextNodeInstanceNumber(nodeID);
      }
    } else {
      instanceCount = GenomeItemInstance.getInstanceID(instanceID);
    }
    
    Node oldNode = null;
    if ((instanceID != null) && (oldGi != null)) {
      oldNode = oldGi.getNode(GenomeItemInstance.getCombinedID(nodeID, instanceID));
    }
    
    GenomeChange gc;
    NodeInstance newNode;
    if (doGene) {
      newNode = (oldNode == null) ? new GeneInstance(appState, node, instanceCount, null, GeneInstance.ACTIVE)
                                  : new GeneInstance((GeneInstance)oldNode);                                         
      gc = rcxI.getGenome().addGene((Gene)newNode);          
    } else {    
      newNode = (oldNode == null) ? new NodeInstance(appState, node, node.getNodeType(), instanceCount, null, GeneInstance.ACTIVE)
                                  : new NodeInstance((NodeInstance)oldNode);                                         
      gc = rcxI.getGenome().addNode(newNode);
    }
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcxI, gc);
      support.addEdit(gcc);
    }    

    GroupChange grc = group.addMember(new GroupMember(newNode.getID()), rcxI.getGenomeID());
    if (grc != null) {
      GroupChangeCmd grcc = new GroupChangeCmd(appState, rcxI, grc);
      support.addEdit(grcc);
    } 
    
    return (newNode);
  }  
 
  /***************************************************************************
  **
  ** Propagate the selected link down from the full genome to the VfG
  */  
 
  public static boolean propagateLinkage(BTState appState, DataAccessContext rcxT,
                                          DBLinkage link, 
                                          DataAccessContext rcxR, 
                                          GenomeInstance.GroupTuple groupTup,
                                          UndoSupport support) {

                                    
    LinkageInstance newLink = propagateLinkageNoLayout(appState, rcxT, link, rcxR, groupTup, support, null);                                    
                                     
    String sourceID = link.getSource();
    BusProperties lp = rcxR.getLayout().getLinkProperties(link.getID());
    NodeProperties nps = rcxR.getLayout().getNodeProperties(sourceID);
    Point2D srcPt = nps.getLocation();
    
    String srcLinkID = GenomeItemInstance.getBaseID(link.getID());
    downPropagateLinkProperties(appState, rcxT, lp, newLink, srcPt, support, srcLinkID);

    return (true);
  }

  /***************************************************************************
  **
  ** Propagate the selected link down from the full genome to the VfG with no layout
  */  
 
  public static LinkageInstance propagateLinkageNoLayout(BTState appState, DataAccessContext rcxT, DBLinkage link, 
                                                          DataAccessContext rcxR, // Genome genome,
                                                          GenomeInstance.GroupTuple groupTup, 
                                                          UndoSupport support, String instanceID) {
    return (propagateOldOrNewLinkageNoLayout(appState, rcxT, null, link, rcxR, groupTup, support, instanceID, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Propagate the selected link down from the full genome to the VfA with no layout
  */  
 
  public static LinkageInstance propagateOldOrNewLinkageNoLayout(BTState appState, DataAccessContext rcxT, //GenomeInstance gi, 
                                                                  GenomeInstance oldGi,
                                                                  
                                                                  DBLinkage link, DataAccessContext rcxR, //Genome gexxnome, //Layout layout,
                                                                  GenomeInstance.GroupTuple groupTup, 
                                                                  UndoSupport support, String instanceID, 
                                                                  Map<String, Integer> topLinkInstanceNums, 
                                                                  PadCalculatorToo.PadCache padCache,
                                                                  Map<String, Map<String, Integer>> nodeGroupMapCache) {

    //
    // We need to find the source and target instance numbers to use from
    // the group tuple.
    //
                                     
    String sourceID = link.getSource();
    String targID = link.getTarget();                                 
        
    int srcInstance;
    int trgInstance;
    
    if (nodeGroupMapCache != null) {
      Map<String, Integer> forSrc = nodeGroupMapCache.get(groupTup.getSourceGroup());
      srcInstance = forSrc.get(sourceID).intValue();
      Map<String, Integer> forTrg = nodeGroupMapCache.get(groupTup.getTargetGroup());
      trgInstance = forTrg.get(targID).intValue();      
    } else {
      // EXPENSIVE FOR BATCH LOADS!
      srcInstance = rcxT.getGenomeAsInstance().getInstanceForNodeInGroup(sourceID, groupTup.getSourceGroup());
      trgInstance = rcxT.getGenomeAsInstance().getInstanceForNodeInGroup(targID, groupTup.getTargetGroup());
    }

    //
    // Make sure we constrain launch/landing pads
    //
    
    String srcID = GenomeItemInstance.getCombinedID(sourceID, Integer.toString(srcInstance));
    Integer srcPad = findSourcePad(srcID, link.getLaunchPad(), rcxT, rcxR, padCache);

    //
    // We have the source and target instance numbers!  Pass them to the constructor
    // to build the new linkage instance.
    //
    
    String linkID = link.getID();
    int instanceCount;
    if (instanceID == null) {
      if (topLinkInstanceNums != null) {
        Integer topNum = topLinkInstanceNums.get(linkID);
        Integer useNum = (topNum == null) ? new Integer(0) : new Integer(topNum.intValue() + 1);
        topLinkInstanceNums.put(linkID, useNum);
        instanceCount = useNum.intValue(); 
      } else {
        instanceCount = rcxT.getGenomeAsInstance().getNextLinkInstanceNumber(link.getID());
      }
    } else {
      instanceCount = GenomeItemInstance.getInstanceID(instanceID);
    }
    
    Linkage oldLink = null;
    if ((instanceID != null) && (oldGi != null)) {
      oldLink = oldGi.getLinkage(GenomeItemInstance.getCombinedID(linkID, instanceID));
    }
    
    LinkageInstance newLink = (oldLink == null) ? new LinkageInstance(appState, link, instanceCount, srcInstance, trgInstance)
                                                : new LinkageInstance((LinkageInstance)oldLink);     

    //
    // Setting launch and landing pads is only valid if we are not dealing with an existing link:
    //

    if (oldLink == null) {
      if (srcPad != null) {
        newLink.setLaunchPad(srcPad.intValue());
      } else {
        emergencyPadForce(appState, srcID, rcxT, support);
        newLink.setLaunchPad(link.getLaunchPad());
        if (padCache != null) {
          padCache.addSource(srcID, link.getLaunchPad());
        }
      }
      String trgID = GenomeItemInstance.getCombinedID(targID, Integer.toString(trgInstance));
      int landPad = findLandingPad(trgID, link.getLandingPad(), rcxT, rcxR, padCache);
      newLink.setLandingPad(landPad);
    }
    
    GenomeChange gc = rcxT.getGenome().addLinkage(newLink);
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcxT, gc);
      support.addEdit(gcc);
    } 

    return (newLink);
  }
  
  /***************************************************************************
  **
  ** Do pad forcing
  */  
 
  public static void emergencyPadForce(BTState appState, String srcID, DataAccessContext rcxT, UndoSupport support) {
    //
    // Gotta force source node to parent configuration to free up source pad
    //
    // FIX ME!  Subset models hold pad info and must be changed to stay in sync with root
    // instance-> this info should be inferred from the root instance.
    //
    Iterator<GenomeInstance> git = rcxT.getGenomeSource().getInstanceIterator();
    boolean doGeneral = false;
    while (git.hasNext()) {
      GenomeInstance cgi = git.next();
      if (cgi.isAncestor(rcxT.getGenomeAsInstance())) {
        GenomeChange[] gca = cgi.revertPadsToRoot(srcID);
        if (gca != null) {
          for (int i = 0; i < gca.length; i++) {
            if (gca[i] != null) {
              GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcxT, gca[i]);
              support.addEdit(gcc);
              support.addEvent(new ModelChangeEvent(cgi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
              doGeneral = true;
            }              
          }
        }
      }
    }
    if (doGeneral) { // Flushes dynamic instances
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.UNSPECIFIED_CHANGE));
    }
    return;
  }  

  /***************************************************************************
  **
  ** Find a source pad for the given node.  If no source pad is found, return
  ** null
  */  
 
  public static Integer findSourcePad(String srcID, int parentSource, DataAccessContext rcxT, DataAccessContext rcxR, PadCalculatorToo.PadCache padCache) {
    //
    // Four cases:  
    // 1) Source pad already defined
    // 2) Not defined, but we can use parent since namespace is not shared
    // 3) No source pad yet, but all pads are taken.
    // 4) No source pad yet, but pads are available.
    //        

    // Case 1:
    // Not having the pad cache is a big performance hit of O(l):
    Integer srcPad = (padCache != null) ? padCache.getSource(srcID) : rcxT.getGenome().getSourcePad(srcID);
    if (srcPad != null) {
      return (srcPad);
    }
    
    //
    // No source pad yet.  See what is available:
    //

    Node node = rcxT.getGenome().getNode(srcID);
    INodeRenderer srcRenderer = rcxR.getLayout().getNodeProperties(GenomeItemInstance.getBaseID(srcID)).getRenderer();
    
    if (!srcRenderer.sharedPadNamespaces()) {  // No conflicts
      if (padCache != null) {
        padCache.addSource(srcID, parentSource);
      }
      return (new Integer(parentSource));
    }
    
    Integer psObj = new Integer(parentSource);
    MinMax padRange = srcRenderer.getSourcePadRange(node);    

    //
    // See what is grabbed by targets.
    //
    
    TreeSet<Integer> srcPads = new TreeSet<Integer>();    
    for (int i = padRange.min; i <= padRange.max; i++) {
      srcPads.add(new Integer(i));
    } 
    
    if (padCache != null) {
      Set<Integer> targs = padCache.getTargets(srcID);
      if (targs != null) {
        srcPads.removeAll(targs);
      }
    } else {
      Iterator<Linkage> lit = rcxT.getGenome().getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        String trg = link.getTarget();
        if (trg.equals(srcID)) {
          int lpad = link.getLandingPad();
          srcPads.remove(new Integer(lpad));        
        }
      }
    }
    
    //
    // Nobody available:
    //
    
    if (srcPads.isEmpty()) {
      return (null);
    }
    
    //
    // Use parent:
    //
    
    if (srcPads.contains(psObj)) {
      if (padCache != null) {
        padCache.addSource(srcID, parentSource);
      }
      return (psObj);
    }
    
    //
    // Use somebody:
    //
    
    if (padCache != null) {
      padCache.addSource(srcID, srcPads.first().intValue());
    }
    return (srcPads.first()); 
  }
  
  /***************************************************************************
  **
  ** Find a landing pad for the given node.
  */  
 
  public static int findLandingPad(String trgID, int parentTarget, DataAccessContext rcxT, DataAccessContext rcxR, PadCalculatorToo.PadCache padCache) {
    //
    // Since we can overload landing pads, we should not have a problem finding one.
    // However, we CANNOT land on a source pad, but there is at most one.  
    //        

    Node node = rcxT.getGenome().getNode(trgID);
    INodeRenderer trgRenderer = rcxR.getLayout().getNodeProperties(GenomeItemInstance.getBaseID(trgID)).getRenderer();

    if (!trgRenderer.sharedPadNamespaces()) {  // no conflicts
      if (padCache != null) {
        padCache.addTarget(trgID, parentTarget);
      }
      return (parentTarget);
    }
    
    Integer srcPadObj = (padCache != null) ? padCache.getSource(trgID) : rcxT.getGenome().getSourcePad(trgID);
     
    if ((srcPadObj == null) || (srcPadObj.intValue() != parentTarget)) {
      if (padCache != null) {
        padCache.addTarget(trgID, parentTarget);
      }
      return (parentTarget);
    }
    int srcPad = srcPadObj.intValue();

    //
    // Landing pad matches existing launch pad.  Find an available landing pad.
    //
    
    //
    // See what is grabbed by targets.
    //
    
    MinMax padRange = trgRenderer.getTargetPadRange(node);        
    
    TreeSet<Integer> trgPads = new TreeSet<Integer>();    
    for (int i = padRange.min; i <= padRange.max; i++) {
      trgPads.add(new Integer(i));
    }
     
    if (padCache != null) {
      Set<Integer> targs = padCache.getTargets(trgID);
      if (targs != null) {
        trgPads.removeAll(targs);
      }
    } else {
      Iterator<Linkage> lit = rcxT.getGenome().getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        String trg = link.getTarget();
        if (trg.equals(trgID)) {
          int lpad = link.getLandingPad();
          trgPads.remove(new Integer(lpad));        
        }
      }
    }

    if (trgRenderer.sharedPadNamespaces()) {  // A conflict
      trgPads.remove(new Integer(srcPad));
    }      
        
    //
    // Nobody available, just go with a non-source pad.
    //
    
    if (trgPads.isEmpty()) {
      int retval = (srcPad == 0) ? 1 : 0;
      if (padCache != null) {
        padCache.addTarget(trgID, retval);
      }   
      return (retval);
    }
   
    
    if (padCache != null) {
      padCache.addTarget(trgID, trgPads.first().intValue());
    }
    return (trgPads.first().intValue());
  }  
 
  /***************************************************************************
  **
  ** Used to propagate layout down.  Remembers from invocation to invocation what
  ** the state is;
  */
  
  public static class DownPropState {
    public String lastGI;
    
    public DownPropState() {
      this.lastGI = null;
    } 
  }     
}
