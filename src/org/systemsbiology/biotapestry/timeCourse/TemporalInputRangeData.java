/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.timeCourse;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** This holds TemporalInputRangeData
*/

public class TemporalInputRangeData {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private DataAccessContext dacx_;
  private ArrayList<TemporalRange> entries_;
  private HashMap<String, List<String>> trEntryMap_;
  private HashMap<String, List<String>> trSourceMap_;  
  private HashMap<String, List<GroupUsage>> groupMap_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TemporalInputRangeData(DataAccessContext dacx) {
    dacx_ = dacx;
    entries_ = new ArrayList<TemporalRange>();
    trEntryMap_ = new HashMap<String, List<String>>();
    trSourceMap_ = new HashMap<String, List<String>>();    
    groupMap_ = new HashMap<String, List<GroupUsage>>();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone
  **
  */  
  
  @Override
  public TemporalInputRangeData clone() { 
    try {
      TemporalInputRangeData newVal = (TemporalInputRangeData)super.clone();
      newVal.entries_ = new ArrayList<TemporalRange>();
      int size = this.entries_.size();
      for (int i = 0; i < size; i++) {
        TemporalRange itr = this.entries_.get(i);
        newVal.entries_.add(itr.clone());
      }
      newVal.mapCopy(this);
      return (newVal);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  } 

  /***************************************************************************
  **
  ** Just get the maps extracted
  **
  */  
  
  public TemporalInputRangeData extractOnlyMaps() { 
    TemporalInputRangeData retval = new TemporalInputRangeData(this.dacx_);
    retval.mapCopy(this);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Just get the maps extracted into is
  **
  */  
  
  private void mapCopy(TemporalInputRangeData other) {     
    this.trEntryMap_ = new HashMap<String, List<String>>();
    for (String temKey : other.trEntryMap_.keySet()) {
      List<String> tml = other.trEntryMap_.get(temKey);
      this.trEntryMap_.put(temKey, new ArrayList<String>(tml));
    }
    this.trSourceMap_ = new HashMap<String, List<String>>();
    for (String tsmKey : other.trSourceMap_.keySet()) {
      List<String> tml = other.trSourceMap_.get(tsmKey);
      this.trSourceMap_.put(tsmKey, new ArrayList<String>(tml));
    }   
    this.groupMap_ = new HashMap<String, List<GroupUsage>>();
    for (String gmKey : other.groupMap_.keySet()) {
      List<GroupUsage> gml = other.groupMap_.get(gmKey);
      ArrayList<GroupUsage> nvgm = new ArrayList<GroupUsage>();        
      this.groupMap_.put(gmKey, nvgm);
      for (GroupUsage gu : gml) {
        nvgm.add(gu.clone());
      }
    } 
    return;
  } 
  
  /***************************************************************************
  **
  ** Answer if we have data
  */
  
  public boolean haveData() {
    if (!entries_.isEmpty()) {
      return (true);
    }
    return (haveMaps());
  }
  
  /***************************************************************************
  **
  ** Answer if we have maps
  */
  
  public boolean haveMaps() {
    if (!trEntryMap_.isEmpty()) {
      return (true);
    }
    if (!trSourceMap_.isEmpty()) {
      return (true);
    }    
    if (!groupMap_.isEmpty()) {
      return (true);
    }
    return (false);
  }

  /***************************************************************************
  ** 
  ** Copy our maps from the established TCD data
  */

  public void buildMapsFromTCDMaps(DataAccessContext dac) {
    TimeCourseDataMaps tcdm = dac.getDataMapSrc().getTimeCourseDataMaps();
    groupMap_ = new HashMap<String, List<GroupUsage>>(tcdm.getGroupMapCopy());
    Map<String, List<TimeCourseDataMaps.TCMapping>> tcmc = tcdm.getTCMapCopy();
    trEntryMap_.clear();
    trSourceMap_.clear();
    Iterator<String> tcmit = tcmc.keySet().iterator();
    while (tcmit.hasNext()) {
      String mapKey = tcmit.next();
      List<TimeCourseDataMaps.TCMapping> targList = tcmc.get(mapKey);
      ArrayList<String> emaplist = new ArrayList<String>();
      ArrayList<String> smaplist = new ArrayList<String>();
      trEntryMap_.put(mapKey, emaplist);
      trSourceMap_.put(mapKey, smaplist);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        TimeCourseDataMaps.TCMapping targ = targList.get(i);
        emaplist.add(targ.name);
        smaplist.add(targ.name);
      }
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Prepare for building the temporal input range data purely from the time course data.
  */

  public List<HoldForBuild> prepForBuildFromTCD(DataAccessContext dac) {
    
    //
    // OK, we can have an issue if the user has mapped a model node to more than one
    // underlying TIR source or entry. Which entry to build? Which source to use? So
    // if we have one-to-many maps, we would be creating a whole slew of entries. For
    // now, just refuse to do it if we encounter anything more complex than one-to-one
    // maps. But note we are OK with time course one-to-many maps, since those will 
    // just combine in an "OR" fashion.
    // Don't want to abort the procedure part-way thru, so we will do the checking first
    // and hold onto the results.
    //
    
    TimeCourseDataMaps tcdm = dac.getDataMapSrc().getTimeCourseDataMaps();
    DBGenome genome = dac.getDBGenome();
    
    ArrayList<HoldForBuild> hfbs = new ArrayList<HoldForBuild>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      HoldForBuild hfb = new HoldForBuild();
      hfb.link = lit.next();
      String src = hfb.link.getSource();
      String trg = hfb.link.getTarget();
      String srcNameT = genome.getNode(src).getRootName();
      String trgNameT = genome.getNode(trg).getRootName();
      hfb.stcm = tcdm.getTimeCourseTCMDataKeysWithDefault(src, dac.getGenomeSource());
      hfb.tisks = getTemporalInputRangeSourceKeysWithDefaultGivenName(src, srcNameT);   
      hfb.tirks = getTemporalInputRangeEntryKeysWithDefaultGivenName(trg, trgNameT);
      if ((hfb.tisks.size() > 1) || (hfb.tirks.size() > 1)) {
        return (null);
      }
      hfbs.add(hfb);
    }
    return (hfbs);
  }

  /***************************************************************************
  ** 
  ** Build the temporal input range data purely from the time course data. Do this inside an undo transaction
  */

  public void buildFromTCD(DataAccessContext dac, List<HoldForBuild> hfbs) {
    
    TimeCourseData tcd = dac.getExpDataSrc().getTimeCourseData();
    double weak = dac.getDisplayOptsSource().getDisplayOptions().getWeakExpressionLevel();
   
    //
    // OK, we can have an issue if the user has mapped a model node to more than one
    // underlying TIR source or entry. Which entry to build? Which source to use? So
    // if we have one-to-many maps, we would be creating a whole slew of entries. For
    // now, just refuse to do it if we encounter anything more complex than one-to-one
    // maps. But note we are OK with time course one-to-many maps, since those will 
    // just combine in an "OR" fashion.
    // Don't want to abort the procedure part-way thru, so we will do the checking first
    // and hold onto the results.
    //
     
    Set<Integer> times = tcd.getInterestingTimes();
    SortedSet<Integer> sortedTimes = new TreeSet<Integer>(times);
    sortedTimes = DataUtil.fillOutHourly(sortedTimes);
    
    Map<String, Set<CrossRegionTuple>> xRegLinks = dac.getFGHO().getAllCrossLinks(this);
       
    for (HoldForBuild hfb : hfbs) {
      if (hfb.tisks.isEmpty() || hfb.tirks.isEmpty()) {
        continue; // Should not happen
      }
      String trgName = hfb.tirks.get(0);
      String srcName = hfb.tisks.get(0);
      TemporalRange tr = getRange(trgName);
      InputTimeRange itr = (tr == null) ? null : tr.getTimeRange(srcName);
      Set<CrossRegionTuple> xrts = xRegLinks.get(GenomeItemInstance.getBaseID(hfb.link.getID()));
      
      for (TimeCourseDataMaps.TCMapping tcm : hfb.stcm) {            
        TimeCourseGene tcg = tcd.getTimeCourseData(tcm.name);
        // Guys in VfG but no VfA will show up here:
        if (tcg == null) {
          continue;
        }
        Set<String> regions = tcg.expressesInRegions(tcm.channel, true);
        
        Iterator<Integer> tmit = sortedTimes.iterator();
        while (tmit.hasNext()) {
          Integer time = tmit.next();
          Iterator<String> rit = regions.iterator();
          while (rit.hasNext()) {
            String region = rit.next();
            TimeCourseGene.VariableLevel vl = new TimeCourseGene.VariableLevel();
            boolean expressesAtTime = false;
            int exLev = tcg.getExpressionLevelForSource(region, time, tcm.channel, vl, weak);
            if ((exLev == ExpressionEntry.EXPRESSED) ||
                (exLev == ExpressionEntry.WEAK_EXPRESSION)) {
              expressesAtTime = true;
            } else if (exLev == ExpressionEntry.VARIABLE) {
              if (vl.level > 0.0) {
                expressesAtTime = true;
              }
            }
            if (expressesAtTime) {
              //
              // New entry? New input range for source? Add it:
              //
              if (tr == null) {
                tr = new TemporalRange(trgName, null, true);
                addEntry(tr);
              }
              if (itr == null) {
                itr = new InputTimeRange(srcName);
                tr.addTimeRange(itr);
              }
              //
              // Get cross-region tuples that are sourced in the current region.
              // Add in the mono-region tuple to the cross-regions:
              //
              
              HashSet<CrossRegionTuple> genXrts = new HashSet<CrossRegionTuple>();
              genXrts.add(new CrossRegionTuple(region, region));
              if (xrts != null) {
                for (CrossRegionTuple xrtu : xrts) {
                  if (xrtu.getSourceRegion().equals(region)) {
                    genXrts.add(xrtu.clone());
                  }
                }
              }
              //
              // For each src-trg region tuple:
              //
              for (CrossRegionTuple gxrtu : genXrts) {
                //
                // See if we have a matching region and range to work with:
                //
                RegionAndRange rar = null;
                Iterator<RegionAndRange> itrar = itr.getRanges();
                int rarSign = RegionAndRange.signForSign(hfb.link.getSign());
                while (itrar.hasNext()) {
                  RegionAndRange range = itrar.next();                
                  String reg = range.getRegion();
                  if ((reg != null) && DataUtil.keysEqual(gxrtu.getTargetRegion(), reg) && (rarSign == range.getSign())) {
                    rar = range;
                    break;
                  }
                }
                // Nothing existing? Add it. Else add the time:
                int tiv = time.intValue();
                if (rar == null) {
                  String srcReg = (gxrtu.isCross()) ? gxrtu.getSourceRegion() : null;
                  rar = new RegionAndRange(gxrtu.getTargetRegion(), tiv, tiv, RegionAndRange.signForSign(hfb.link.getSign()), null, srcReg);
                  itr.add(rar);                
                } else {
                  rar.addTime(tiv);
                }
              }
            }
          }
        }
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Answers if we have a tir mapping
  **
  */
  
  public boolean haveCustomMapForNode(String nodeID) {
    return (haveCustomSourceMapForNode(nodeID) || haveCustomEntryMapForNode(nodeID));
  } 
  
  /***************************************************************************
  **
  ** Answers if we have a temporal input region mapping
  **
  */
  
  public boolean haveCustomMapForRegion(String groupId) {
    if (!haveData()) {
      return (false);
    }
    List<GroupUsage> mapped = groupMap_.get(groupId);
    if ((mapped == null) || (mapped.size() == 0))  {
      return (false);
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Answers if we have a temporal input mapping
  **
  */
  
  public boolean haveCustomEntryMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = trEntryMap_.get(nodeID);
    if ((mapped == null) || (mapped.size() == 0))  {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if we have a temporal input mapping
  **
  */
  
  public boolean haveCustomSourceMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = trSourceMap_.get(nodeID);
    if ((mapped == null) || (mapped.size() == 0))  {
      return (false);
    }
    return (true);
  }  

  /***************************************************************************
  **
  ** Answers if we have time course data for the given node
  **
  */
  
  public boolean haveDataForNode(String nodeID, GenomeSource db) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = getTemporalInputRangeEntryKeysWithDefault(nodeID, db);
    if (mapped == null) {
      return (false);
    }
    Iterator<String> mit = mapped.iterator();
    while (mit.hasNext()) {
      String name = mit.next();
      if (getRange(name) != null) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers if we have time course data for the given node
  **
  */
  
  public boolean haveDataForNodeOrName(String nodeID, String nodeName) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = getTemporalInputRangeEntryKeysWithDefaultGivenName(nodeID, nodeName);
    if (mapped == null) {
      return (false);
    }
    Iterator<String> mit = mapped.iterator();
    while (mit.hasNext()) {
      String name = mit.next();
      if (getRange(name) != null) {
        return (true);
      }
    }
    return (false);
  }  

  /***************************************************************************
  **
  ** Get the node IDs that target the given table entry
  */

  public Set<String> getTemporalInputEntryKeyInverse(String name) {
    name = DataUtil.normKey(name);
    HashSet<String> retval = new HashSet<String>();
    //
    // If there is anybody out there with the same name _and no custom map_, it
    // will map by default:
    //
    
    DBGenome genome = dacx_.getDBGenome();
    Node node = genome.getGeneWithName(name);
    if (node != null) {
      if (!haveCustomEntryMapForNode(node.getID())) {
        retval.add(node.getID());
      }
    }
    
    Set<Node> nodes = genome.getNodesWithName(name);
    if (!nodes.isEmpty()) {
      Iterator<Node> sit = nodes.iterator();
      while (sit.hasNext()) {
        node = sit.next();
        if (!haveCustomEntryMapForNode(node.getID())) {
          retval.add(node.getID());
        }
      }
    }

    Iterator<String> kit = trEntryMap_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<String> targs = trEntryMap_.get(key);
      Iterator<String> trit = targs.iterator();
      while (trit.hasNext()) {
        String testName = trit.next();
        if (DataUtil.keysEqual(testName, name)) {
          retval.add(key);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the node IDs mapped to the given perturbation source name
  */

  public Set<String> getTemporalInputSourceKeyInverse(String name) {
    name = DataUtil.normKey(name);
    HashSet<String> retval = new HashSet<String>();
    //
    // Blank names cannot be inverted, and blank names are handed to us when
    // new entries are added to the tables. (Bug BT-08-18-04:1)
    //
    if (name.equals("")) {
      return (retval);
    }
    //
    // If there is anybody out there with the same name _and no custom map_, it
    // will map by default:
    //
    
    DBGenome genome = dacx_.getDBGenome();
    Node node = genome.getGeneWithName(name);
    if (node != null) {
      if (!haveCustomSourceMapForNode(node.getID())) {
        retval.add(node.getID());
      }
    }
    
    Set<Node> nodes = genome.getNodesWithName(name);
    if (!nodes.isEmpty()) {
      Iterator<Node> sit = nodes.iterator();
      while (sit.hasNext()) {
        node = sit.next();
        if (!haveCustomSourceMapForNode(node.getID())) {
          retval.add(node.getID());
        }
      }
    }

    Iterator<String> kit = trSourceMap_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<String> targs = trSourceMap_.get(key);
      Iterator<String> trit = targs.iterator();
      while (trit.hasNext()) {
        String testName = trit.next();
        if (DataUtil.keysEqual(testName, name)) {
          retval.add(key);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Gets input sources that do not appear as targets
  */
  
  public Set<String> getNonTargetSources() {
    HashSet<String> retval = new HashSet<String>();
    HashSet<String> targetNames = new HashSet<String>();
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      targetNames.add(DataUtil.normKey(trg.getName()));
    }
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      Iterator<InputTimeRange> itrs = trg.getTimeRanges();
      while (itrs.hasNext()) {
        InputTimeRange itr = itrs.next();
        String itrName = itr.getName();
        if (!DataUtil.containsKey(targetNames, itrName)) {
          retval.add(itrName);
        }
      }
    }
    
    return (retval);
  }

  /***************************************************************************
  **
  ** Gets all input sources
  */
  
  public Set<String> getAllSources() {
    HashSet<String> retval = new HashSet<String>();
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      Iterator<InputTimeRange> itrs = trg.getTimeRanges();
      while (itrs.hasNext()) {
        InputTimeRange itr = itrs.next();
        String itrName = itr.getName();
        if (!DataUtil.containsKey(retval, itrName)) {
          retval.add(itrName);
        }
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Answer if the given name shows up as a perturbation source:
  */
  
  public boolean isPerturbationSourceName(String name) {
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      Iterator<InputTimeRange> itrs = trg.getTimeRanges();
      while (itrs.hasNext()) {
        InputTimeRange itr = itrs.next();
        String itrName = itr.getName();
        if (DataUtil.keysEqual(name, itrName)) {
          return (true);
        }
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(TemporalInputChange undo) {
    if ((undo.entryMapListOrig != null) || (undo.entryMapListNew != null)) {
      entryMapChangeUndo(undo);
    } else if ((undo.sourceMapListOrig != null) || (undo.sourceMapListNew != null)) {
      sourceMapChangeUndo(undo);      
    } else if ((undo.groupMapListOrig != null) || (undo.groupMapListNew != null)) {
      groupMapChangeUndo(undo);      
    } else if ((undo.eOrig != null) || (undo.eNew != null)) {
      entryChangeUndo(undo);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(TemporalInputChange undo) {
    if ((undo.entryMapListOrig != null) || (undo.entryMapListNew != null)) {
      entryMapChangeRedo(undo);
    } else if ((undo.sourceMapListOrig != null) || (undo.sourceMapListNew != null)) {
      sourceMapChangeRedo(undo);
    } else if ((undo.groupMapListOrig != null) || (undo.groupMapListNew != null)) {
      groupMapChangeRedo(undo);
    } else if ((undo.eOrig != null) || (undo.eNew != null)) {
      entryChangeRedo(undo);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public TemporalInputChange startRangeUndoTransaction(String targetName) {
    TemporalInputChange retval = new TemporalInputChange();
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      String name = trg.getName();
      if (DataUtil.keysEqual(name, targetName)) {
        retval.eOrig = new TemporalRange(trg);
        retval.entryPos = i;
        return (retval);
      }
    }
    throw new IllegalArgumentException();
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public TemporalInputChange finishRangeUndoTransaction(TemporalInputChange change) {
    TemporalRange trg = entries_.get(change.entryPos);
    change.eNew = new TemporalRange(trg);
    return (change);
  }
  
  /***************************************************************************
  **
  ** Answer if the data is empty
  */
  
  public boolean isEmpty() {
    return (entries_.isEmpty());
  } 
  
  /***************************************************************************
  **
  ** Add an entry
  */
  
  public TemporalInputChange addEntry(TemporalRange range) {
    TemporalInputChange retval = new TemporalInputChange();
    retval.entryPos = entries_.size();      
    entries_.add(range);
    retval.eNew = new TemporalRange(range);
    retval.eOrig = null;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the entries
  */
  
  public Iterator<TemporalRange> getEntries() {
    return (entries_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get nth entry
  */
  
  public TemporalRange getEntry(int n) {
    return (entries_.get(n));
  }
  /***************************************************************************
  **
  ** Drop maps that resolve to the given input range entry
  */
  
  public TemporalInputChange[] dropMapsTo(String entryID) {
    //
    // Crank through the maps and look for entries that target the 
    // given entry.  Delete it.
    //
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    Iterator<String> mit = new HashSet<String>(trEntryMap_.keySet()).iterator();
    while (mit.hasNext()) {
      String mapKey = mit.next();
      List<String> targList = trEntryMap_.get(mapKey);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        String targ = targList.get(i);
        if (DataUtil.keysEqual(targ, entryID)) {
          TemporalInputChange tic = new TemporalInputChange();
          tic.mapKey = mapKey;
          tic.entryMapListOrig = new ArrayList<String>(targList);
          targList.remove(i);
          if (targList.isEmpty()) {
            tic.entryMapListNew = null;
            trEntryMap_.remove(mapKey);
          } else {
            tic.entryMapListNew = new ArrayList<String>(targList);
          }
          retvalList.add(tic);
          break;
        }
      }
    }
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  }
 
  /***************************************************************************
  **
  ** Fixup all maps to lose dangling references
  */

  /*
  public TemporalInputChange[] mapFixups() {

    ArrayList retvalList = new ArrayList();
 
    //
    // Make a set of all entry names and row names, and a set of all region 
    // names.  Go through the maps and drop any mapping to a name not in those
    // sets.
    //
    

    Iterator mit = new HashSet(trMap_.keySet()).iterator();
    while (mit.hasNext()) {
      String mapKey = (String)mit.next();
      List targList = (List)trMap_.get(mapKey);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        String targ = (String)targList.get(i);
        if (targ.equals(entryID)) {
          TemporalInputChange tic = new TemporalInputChange();
          tic.mapKey = mapKey;
          tic.mapListOrig = new ArrayList(targList);
          targList.remove(i);
          if (targList.isEmpty()) {
            tic.mapListNew = null;
            trMap_.remove(mapKey);
          } else {
            tic.mapListNew = new ArrayList(targList);
          }
          retvalList.add(tic);
          break;
        }
      }
    }
    return ((TemporalInputChange[])retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  } 
  
  /***************************************************************************
  **
  ** When changing an entry name, we need to change one or more maps to keep
  ** them consistent.  Do this operation. Return empty array if no changes occurred.
  */
  
  public TemporalInputChange[] changeDataMapsToName(String oldName, String newName) {
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    Iterator<String> tmit = trEntryMap_.keySet().iterator();
    while (tmit.hasNext()) {
      String mkey = tmit.next();
      List<String> keys = trEntryMap_.get(mkey);
      if (DataUtil.containsKey(keys, oldName)) {
        TemporalInputChange retval = new TemporalInputChange();
        retval.mapKey = mkey;
        retval.entryMapListOrig = new ArrayList<String>(keys);
        keys.remove(oldName);        
        keys.add(newName);
        retval.entryMapListNew = new ArrayList<String>(keys);
        retvalList.add(retval);
      }
    }
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  }

  /***************************************************************************
  **
  ** We are changing the name of a node or gene, and have been told to modify the
  ** data as needed.  We need to change the entry (if any) to the given name, the
  ** source rows, and any custom maps to that name.
  */
  
  public TemporalInputChange[] changeName(String oldName, String newName) {
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    
    if (!DataUtil.keysEqual(oldName, newName)) {
      if ((getRange(newName) != null) || isPerturbationSourceName(newName)) {
        throw new IllegalArgumentException();
      }
    }
     
    //
    // Do all the rows in other entries:
    //
    
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange tr = entries_.get(i);
      Iterator<InputTimeRange> perts = tr.getTimeRanges();
      while (perts.hasNext()) {
        InputTimeRange itr = perts.next();
        String pertName = itr.getName();
        if (DataUtil.keysEqual(oldName, pertName)) {
          TemporalInputChange tic = startRangeUndoTransaction(tr.getName());
          itr.setName(newName);
          tic = finishRangeUndoTransaction(tic);
          retvalList.add(tic);
        }
      }
    }
    
    //
    // Do the entry:
    //
    
    TemporalRange range = getRange(oldName); 
    if (range != null) {
      TemporalInputChange tic = startRangeUndoTransaction(oldName);
      range.setName(newName);
      tic = finishRangeUndoTransaction(tic);
      retvalList.add(tic);
    }    
    
    //
    // Fix any custom entry maps
    //
    
    Iterator<String> dmit = trEntryMap_.keySet().iterator();
    while (dmit.hasNext()) {
      String mkey = dmit.next();
      List<String> keys = trEntryMap_.get(mkey);
      if (DataUtil.containsKey(keys, oldName)) {
        TemporalInputChange retval = new TemporalInputChange();
        retval.mapKey = mkey;
        retval.entryMapListOrig = new ArrayList<String>(keys);
        keys.remove(oldName);
        keys.add(newName);
        retval.entryMapListNew = new ArrayList<String>(keys);
        retvalList.add(retval);
      }
    }
    
    //
    // Fix any custom source maps
    // 
    
    dmit = trSourceMap_.keySet().iterator();
    while (dmit.hasNext()) {
      String mkey = dmit.next();
      List<String> keys = trSourceMap_.get(mkey);
      if (DataUtil.containsKey(keys, oldName)) {
        TemporalInputChange retval = new TemporalInputChange();
        retval.mapKey = mkey;
        retval.sourceMapListOrig = new ArrayList<String>(keys);
        keys.remove(oldName);
        keys.add(newName);
        retval.sourceMapListNew = new ArrayList<String>(keys);
        retvalList.add(retval);
      }
    }

    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  } 
  
  /***************************************************************************
  **
  ** We are changing the name of a region, and have been told to modify the
  ** data as needed.  We need to change the regions.  
  */
  
  public TemporalInputChange[] changeRegionName(String oldName, String newName) {
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    
    //
    // Do all the rows in the entries:
    //
    
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange tr = entries_.get(i);
      Iterator<InputTimeRange> perts = tr.getTimeRanges();
      while (perts.hasNext()) {
        InputTimeRange itr = perts.next();
        Iterator<RegionAndRange> rit = itr.getRanges();
        while (rit.hasNext()) {
          RegionAndRange rar = rit.next();
          boolean regionMatch = (rar.getRegion() != null) && DataUtil.keysEqual(rar.getRegion(), oldName);
          boolean sourceMatch = (rar.getRestrictedSource() != null) && DataUtil.keysEqual(rar.getRestrictedSource(), oldName);
          if (regionMatch || sourceMatch) {
            TemporalInputChange tic = startRangeUndoTransaction(tr.getName());
            if (regionMatch) {
              rar.setRegion(newName);
            }
            if (sourceMatch) {
              rar.setRestrictedSource(newName);
            }
            tic = finishRangeUndoTransaction(tic);
            retvalList.add(tic);
          }
        }
      }
    }

    //
    // Fix any custom entry maps
    //
    
    Iterator<String> dmit = groupMap_.keySet().iterator();
    while (dmit.hasNext()) {
      String mkey = dmit.next();
      List<GroupUsage> currentMap = groupMap_.get(mkey);
      List<GroupUsage> newMap = new ArrayList<GroupUsage>();
      boolean haveChange = false;
      int csize = currentMap.size();
      for (int i = 0; i < csize; i++) {
        GroupUsage gu = currentMap.get(i);
        GroupUsage newgu  = new GroupUsage(gu);
        if (DataUtil.keysEqual(gu.mappedGroup, oldName)) {
          haveChange = true;
          newgu.mappedGroup = newName;
        }
        newMap.add(newgu);
      }
      if (haveChange) {
        TemporalInputChange tic = new TemporalInputChange();
        retvalList.add(tic);
        tic.mapKey = mkey;
        tic.groupMapListOrig = deepCopyGroupMap(currentMap);
        groupMap_.put(mkey, newMap);
        tic.groupMapListNew = deepCopyGroupMap(newMap);      
      }  
    }
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  }
 
  /***************************************************************************
  **
  ** Return a list of the regions 
  */
  
  public Set<String> getRegions() {
    HashSet<String> retval = new HashSet<String>();
    
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange tr = entries_.get(i);
      Iterator<InputTimeRange> perts = tr.getTimeRanges();
      while (perts.hasNext()) {
        InputTimeRange itr = perts.next();
        Iterator<RegionAndRange> rit = itr.getRanges();
        while (rit.hasNext()) {
          RegionAndRange rar = rit.next();
          String region = rar.getRegion();
          if (region != null) {
            retval.add(region);
          }
        }
      }
    }    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Drop nth entry 
  */
  
  public TemporalInputChange dropEntry(int n) {
    TemporalInputChange retval = new TemporalInputChange();
    TemporalRange entry = entries_.get(n);
    retval.eOrig = new TemporalRange(entry);
    retval.eNew = null;
    retval.entryPos = n;
    entries_.remove(n);    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop the entry
  */
  
  public TemporalInputChange dropEntry(String entryID) {
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange entry = entries_.get(i);
      if (entry.getName().equals(entryID)) {
        TemporalInputChange retval = new TemporalInputChange();
        retval.eOrig = new TemporalRange(entry);
        retval.eNew = null;
        retval.entryPos = i;
        entries_.remove(i);
        return (retval);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Undo an entry change
  */
  
  private void entryChangeUndo(TemporalInputChange undo) {
    if ((undo.eOrig != null) && (undo.eNew != null)) {
      entries_.set(undo.entryPos, undo.eOrig);
    } else if (undo.eOrig == null) {
      entries_.remove(undo.entryPos);
    } else {
      entries_.add(undo.entryPos, undo.eOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo an entry change
  */
  
  private void entryChangeRedo(TemporalInputChange undo) {
    if ((undo.eOrig != null) && (undo.eNew != null)) {
      entries_.set(undo.entryPos, undo.eNew);
    } else if (undo.eNew == null) {
      entries_.remove(undo.entryPos);
    } else {
      entries_.add(undo.entryPos, undo.eNew);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the temporal range for the name.
  */
  
  public TemporalRange getRange(String targetName) {
    targetName = targetName.replaceAll(" ", "");
    Iterator<TemporalRange> trgit = entries_.iterator();  // FIX ME: use a hash map
    while (trgit.hasNext()) {
      TemporalRange trg = trgit.next();
      String name = trg.getName().replaceAll(" ", "");
      if (name.equalsIgnoreCase(targetName)) {
        return (trg);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get the set of "interesting times" (Integers)
  **
  */
  
  public Set<Integer> getInterestingTimes() {
    HashSet<Integer> retval = new HashSet<Integer>();
    Iterator<TemporalRange> eit = getEntries();
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      tr.getInterestingTimes(retval);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a set of hour values that are covered by the data
  */
  
  public Set<Integer> getHours() {
    HashSet<Integer> retval = new HashSet<Integer>();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add source and target maps
  */

  public TemporalInputChange[] addTemporalInputRangeMaps(String key, List<String> entries, List<String> sources) {
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    TemporalInputChange retval;
    if ((entries != null) && (entries.size() > 0)) {
      retval = new TemporalInputChange();
      retval.mapKey = key;
      retval.entryMapListNew = new ArrayList<String>(entries);
      List<String> orig = trEntryMap_.get(key);
      retval.entryMapListOrig = (orig == null) ? null : new ArrayList<String>(orig);
      trEntryMap_.put(key, entries);
      retvalList.add(retval);
    }
    if ((sources != null) && (sources.size() > 0)) {    
      retval = new TemporalInputChange(); 
      retval.mapKey = key;    
      retval.sourceMapListNew = new ArrayList<String>(sources);
      List<String> orig = trSourceMap_.get(key);
      retval.sourceMapListOrig = (orig == null) ? null : new ArrayList<String>(orig);
      trSourceMap_.put(key, sources);
      retvalList.add(retval);
    }
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));    
  }  

  /***************************************************************************
  **
  ** Add combined source and target maps
  */
  
  public void addCombinedTemporalInputRangeMaps(String key, List<TirMapResult> mapSets) {
    ArrayList<String> sourceList = new ArrayList<String>();
    ArrayList<String> entryList = new ArrayList<String>();
    Iterator<TirMapResult> mit = mapSets.iterator();
    while (mit.hasNext()) {
      TirMapResult res = mit.next();
      if (res.type == TirMapResult.Type.ENTRY_MAP) {
        entryList.add(res.name);
      } else {
        sourceList.add(res.name);
      }
    }
    if (entryList.size() > 0) {
      trEntryMap_.put(key, entryList);
    }
    if (sourceList.size() > 0) {
      trSourceMap_.put(key, sourceList);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Get the list of entry names for the node ID.  May be empty.
  */
  
  public List<String> getTemporalInputRangeEntryKeysWithDefault(String nodeId, GenomeSource db) {
    List<String> retval = trEntryMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      String defMap = getTemporalInputRangeDefaultMap(nodeId, db);
      if (defMap == null) {
        return (retval);
      }
      retval.add(defMap);
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the list of entry names for the node ID.  May be empty.
  */
  
  public List<String> getTemporalInputRangeEntryKeysWithDefaultGivenName(String nodeId, String nodeName) {
    List<String> retval = trEntryMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      if ((nodeName == null) || (nodeName.trim().equals(""))) {
        return (retval);
      }
      retval.add(nodeName);
    }    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the list of entry names for the node ID.  May be null.
  */
  
  public List<String> getCustomTemporalInputRangeEntryKeys(String nodeId) {
    return (trEntryMap_.get(nodeId));
  }
 
  /***************************************************************************
  **
  ** Get the list of source names for the node ID.  May be empty.
  */
  
  public List<String> getTemporalInputRangeSourceKeysWithDefault(String nodeId, GenomeSource db) {
    List<String> retval = trSourceMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      String defMap = getTemporalInputRangeDefaultMap(nodeId, db);
      if (defMap == null) {
        return (retval);
      }
      retval.add(defMap);
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the default target name for the node ID.  May be null.
  */
  
  public String getTemporalInputRangeDefaultMap(String nodeId, GenomeSource db) {
    Node node = db.getRootDBGenome().getNode(nodeId);      
    if (node == null) { // for when node has been already deleted...
      throw new IllegalStateException();
    }      
    String nodeName = node.getRootName();
    if ((nodeName == null) || (nodeName.trim().equals(""))) {
      return (null);
    } 
    return (nodeName);
  }
  
  /***************************************************************************
  **
  ** Get the list of source names for the node ID.  May be empty.
  */
  
  public List<String> getTemporalInputRangeSourceKeysWithDefaultGivenName(String nodeId, String nodeName) {
    List<String> retval = trSourceMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      if ((nodeName == null) || (nodeName.trim().equals(""))) {
        return (retval);
      }
      retval.add(nodeName);
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the list of source names for the node ID.  May be null.
  */
  
  public List<String> getCustomTemporalInputRangeSourceKeys(String nodeId) {
    return (trSourceMap_.get(nodeId));
  }  

  /***************************************************************************
  **
  ** Delete the key mapping
  */

  public TemporalInputChange dropDataEntryKeys(String geneId) {
    if (trEntryMap_.get(geneId) == null) {
      return (null);
    }    
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = geneId;
    retval.entryMapListNew = null;
    // FIX ME!!!! Clone
    retval.entryMapListOrig = trEntryMap_.remove(geneId);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete the key mapping
  */

  public TemporalInputChange dropDataSourceKeys(String geneId) {
    if (trSourceMap_.get(geneId) == null) {
      return (null);
    }    
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = geneId;
    retval.sourceMapListNew = null;
    // FIX ME!!!! Clone
    retval.sourceMapListOrig = trSourceMap_.remove(geneId);
    return (retval);
  }  
   
  /***************************************************************************
  **
  ** Undo a map change
  */
  
  private void sourceMapChangeUndo(TemporalInputChange undo) {
    if ((undo.sourceMapListOrig != null) && (undo.sourceMapListNew != null)) {
      trSourceMap_.put(undo.mapKey, undo.sourceMapListOrig);
    } else if (undo.sourceMapListOrig == null) {
      trSourceMap_.remove(undo.mapKey);
    } else {
      trSourceMap_.put(undo.mapKey, undo.sourceMapListOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a map change
  */
  
  private void sourceMapChangeRedo(TemporalInputChange undo) {
    if ((undo.sourceMapListOrig != null) && (undo.sourceMapListNew != null)) {
      trSourceMap_.put(undo.mapKey, undo.sourceMapListNew);
    } else if (undo.sourceMapListNew == null) {
      trSourceMap_.remove(undo.mapKey);
    } else {
      trSourceMap_.put(undo.mapKey, undo.sourceMapListNew);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a map change
  */
  
  private void entryMapChangeUndo(TemporalInputChange undo) {
    if ((undo.entryMapListOrig != null) && (undo.entryMapListNew != null)) {
      trEntryMap_.put(undo.mapKey, undo.entryMapListOrig);
    } else if (undo.entryMapListOrig == null) {
      trEntryMap_.remove(undo.mapKey);
    } else {
      trEntryMap_.put(undo.mapKey, undo.entryMapListOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a map change
  */
  
  private void entryMapChangeRedo(TemporalInputChange undo) {
    if ((undo.entryMapListOrig != null) && (undo.entryMapListNew != null)) {
      trEntryMap_.put(undo.mapKey, undo.entryMapListNew);
    } else if (undo.entryMapListNew == null) {
      trEntryMap_.remove(undo.mapKey);
    } else {
      trEntryMap_.put(undo.mapKey, undo.entryMapListNew);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Add a map from a group to a list of target groups
  */
  
  public TemporalInputChange setTemporalRangeGroupMap(String key, List<GroupUsage> mapSets) {
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = key;
    retval.groupMapListNew = deepCopyGroupMap(mapSets);
    retval.groupMapListOrig = deepCopyGroupMap(groupMap_.get(key));
    groupMap_.put(key, mapSets);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Duplicate a map for genome duplications
  */
  
  public TemporalInputChange copyTemporalRangeGroupMapForDuplicateGroup(String oldKey, String newKey, Map<String, String> modelMap) {
    List<GroupUsage> oldMapList = groupMap_.get(oldKey);
    if (oldMapList == null) {
      return (null);
    }
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = newKey;
    retval.groupMapListNew = deepCopyGroupMapMappingUsage(oldMapList, modelMap, oldKey.equals(newKey));
    retval.groupMapListOrig = null;
    groupMap_.put(newKey, deepCopyGroupMap(retval.groupMapListNew));
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the list of target names for the group.  May be empty for a group
  ** with no name.
  */
  
  public List<GroupUsage> getTemporalRangeGroupKeysWithDefault(String groupId, String groupName) {
    List<GroupUsage> retval = groupMap_.get(groupId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<GroupUsage>();
      if ((groupName == null) || (groupName.trim().equals(""))) {
        return (retval);
      }
      retval.add(new GroupUsage(groupName.toUpperCase().replaceAll(" ", ""), null));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the list of targets names for the node ID.  May be null.
  */
  
  public List<GroupUsage> getCustomTemporalRangeGroupKeys(String groupId) {
    return (groupMap_.get(groupId));
  }
  
  /***************************************************************************
  **
  ** Delete the group mapping
  */

  public TemporalInputChange dropGroupMap(String groupId) {
    if (groupMap_.get(groupId) == null) {
      return (null);
    }    
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = groupId;
    retval.groupMapListNew = null;
    retval.groupMapListOrig = deepCopyGroupMap(groupMap_.remove(groupId));
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get an iterator over all the group map keys
  */

  public Iterator<String> getGroupMapKeys() {
    return (groupMap_.keySet().iterator());
  }  
  
  /***************************************************************************
  **
  ** Delete the group mappings referencing the given proxy
  */

  public TemporalInputChange[] dropGroupMapsForProxy(String proxyId) {

    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    Iterator<String> gmit = new HashSet<String>(groupMap_.keySet()).iterator();
    while (gmit.hasNext()) {
      String key = gmit.next();
      List<GroupUsage> currentMap = groupMap_.get(key);
      int mSize = currentMap.size();
      ArrayList<GroupUsage> newMap = new ArrayList<GroupUsage>();
      for (int i = 0; i < mSize; i++) {
        GroupUsage gu = currentMap.get(i);
        if ((gu.usage == null) || (!gu.usage.equals(proxyId))) {
          newMap.add(gu);
        }
      }
      if (newMap.size() < mSize) {
        TemporalInputChange tic = new TemporalInputChange();      
        retvalList.add(tic);
        tic.mapKey = key;
        tic.groupMapListOrig = deepCopyGroupMap(currentMap);
        if (newMap.size() > 0) {
          groupMap_.put(key, newMap);
          tic.groupMapListNew = deepCopyGroupMap(newMap);        
        } else {
          groupMap_.remove(key);
          tic.groupMapListNew = null;
        }
      }
    }
    
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  }
  
  /***************************************************************************
  **
  ** Deep copy a group mapping
  */
  
  private List<GroupUsage> deepCopyGroupMap(List<GroupUsage> oldMap) {
    if (oldMap == null) {
      return (null);
    }
    ArrayList<GroupUsage> retval = new ArrayList<GroupUsage>();
    int size = oldMap.size();
    for (int i = 0; i < size; i++) {
      retval.add(oldMap.get(i).clone());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Deep copy a group mapping, mapping model usage as we go
  */
  
  private List<GroupUsage> deepCopyGroupMapMappingUsage(List<GroupUsage> oldMap, Map<String, String> modelMaps, boolean append) {
    if (oldMap == null) {
      return (null);
    }
    ArrayList<GroupUsage> retval = new ArrayList<GroupUsage>();
    int size = oldMap.size();
    for (int i = 0; i < size; i++) {
      GroupUsage copied = oldMap.get(i).clone();
      String modelID = modelMaps.get(copied.usage);
      if (modelID != null) {
        if (append) {
          retval.add(copied);
          copied = copied.clone();
        }
        copied.usage = modelID;
      }
      retval.add(copied);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a map change
  */
  
  private void groupMapChangeUndo(TemporalInputChange undo) {
    if ((undo.groupMapListOrig != null) && (undo.groupMapListNew != null)) {
      groupMap_.put(undo.mapKey, undo.groupMapListOrig);
    } else if (undo.groupMapListOrig == null) {
      groupMap_.remove(undo.mapKey);
    } else {
      groupMap_.put(undo.mapKey, undo.groupMapListOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a map change
  */
  
  private void groupMapChangeRedo(TemporalInputChange undo) {
    if ((undo.groupMapListOrig != null) && (undo.groupMapListNew != null)) {
      groupMap_.put(undo.mapKey, undo.groupMapListNew);
    } else if (undo.groupMapListNew == null) {
      groupMap_.remove(undo.mapKey);
    } else {
      groupMap_.put(undo.mapKey, undo.groupMapListNew);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the list of all regions used in the range data.
  */
  
  public Set<String> getAllRegions() {
    TreeSet<String> retval = new TreeSet<String>();
    //
    // Get regions present in entries, but maybe not mapped:
    //
    Iterator<TemporalRange> eit = getEntries();
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      tr.getAllRegions(retval);
    }
    //
    // Get regions in maps, but maybe not in entries:
    
    Iterator<List<GroupUsage>> gmit = groupMap_.values().iterator();
    while (gmit.hasNext()) {
      List<GroupUsage> groupsUsed = gmit.next();
      Iterator<GroupUsage> guit = groupsUsed.iterator();
      while (guit.hasNext()) {
        GroupUsage groupUse = guit.next();
        retval.add(groupUse.mappedGroup);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the set of all current map targets
  */
  
  public Set<String> getAllIdentifiers() {
    TreeSet<String> retval = new TreeSet<String>();
    //
    // Get regions present in entries, but maybe not mapped:
    //
    Iterator<TemporalRange> eit = getEntries();
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      retval.add(tr.getName());
      Iterator<InputTimeRange> pit = tr.getTimeRanges();
      while (pit.hasNext()) {
        InputTimeRange itr = pit.next();
        retval.add(itr.getName());
      }
    }
    //
    // Get targets in maps:
    //
    Iterator<List<String>> tmit = trEntryMap_.values().iterator();
    while (tmit.hasNext()) {
      List<String> idsUsed = tmit.next();
      retval.addAll(idsUsed);
    }
    tmit = trSourceMap_.values().iterator();
    while (tmit.hasNext()) {
      List<String> idsUsed = tmit.next();
      retval.addAll(idsUsed);
    }    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Answers if a range name is unique
  */
  
  public boolean nameIsUnique(String targName) {
    Iterator<TemporalRange> eit = getEntries();  
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      if (DataUtil.keysEqual(tr.getName(), targName)) {
        return (false);
      }
    // FIX ME ???
    }  
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if a range name shows up NO MORE THAN ONCE
  */
  
  public boolean nameAppearsZeroOrOne(String targName) {
    int hitCount = 0;
    Iterator<TemporalRange> eit = getEntries();  
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      if (DataUtil.keysEqual(tr.getName(), targName)) {
        if (++hitCount > 1) {
          return (false);
        }
      }
    }  
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if a range name shows up ONCE
  */
  
  public boolean nameAppearsOnce(String targName) {
    int hitCount = 0;
    Iterator<TemporalRange> eit = getEntries();  
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      if (DataUtil.keysEqual(tr.getName(), targName)) {
        if (++hitCount > 1) {
          return (false);
        }
      }
    }  
    return (hitCount != 0);
  }
  
  /***************************************************************************
  **
  ** Write the Temporal Input Range Data to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<TemporalInputRangeData>");
    if (entries_.size() > 0) {
      Iterator<TemporalRange> eit = getEntries();
      ind.up();    
      while (eit.hasNext()) {
        TemporalRange tr = eit.next();
        tr.writeXML(out, ind);
      }
      ind.down(); 
    }
    if ((trEntryMap_.size() > 0) || (trSourceMap_.size() > 0)) {
      writeTrMap(out, ind);
    } 
    if (groupMap_.size() > 0) {
      writeGroupMap(out, ind);
    }    
    ind.indent();
    out.println("</TemporalInputRangeData>");
    return;
  }
  
  /***************************************************************************
  **
  ** Get an HTML inputs table suitable for display.
  */
  
  public String getInputsTable(String targetName, Set<String> srcNames) {
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);    
    Iterator<TemporalRange> trit = entries_.iterator();  // FIX ME: use a hash map
    while (trit.hasNext()) {
      TemporalRange tr = trit.next();
      String name = tr.getName();
      if (DataUtil.keysEqual(name, targetName)) {
        if (!tr.isInternalOnly()) {
          tr.getInputsTable(dacx_, out, srcNames);
        }
      }
    }
    return (sw.toString());
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  @Override
  public String toString() {
    return ("TemporalInputRangeData: " + " entries = " + entries_);
  }

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /***************************************************************************
  **
  ** Utility for TIRD building
  */
  
  public static class HoldForBuild {
    Linkage link;
    List<TimeCourseDataMaps.TCMapping> stcm;
    List<String> tisks;   
    List<String> tirks;
  }
  
  /***************************************************************************
  **
  ** Used to return group tuples
  */
  
  public static class CrossRegionTuple implements Cloneable {
    private String srcReg_;
    private String trgReg_;
    
    public CrossRegionTuple(String srcReg, String trgReg) {
      if ((srcReg == null) || (trgReg == null)) {
        throw new IllegalArgumentException();
      }
      srcReg_ = srcReg;
      trgReg_ = trgReg;
    }
   
    @Override
    public CrossRegionTuple clone() { 
      try {
        return ((CrossRegionTuple)super.clone());
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    } 
  
    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof CrossRegionTuple)) {
        return (false);
      }
      CrossRegionTuple otherGT = (CrossRegionTuple)other;
      if (!this.srcReg_.equals(otherGT.srcReg_)) {
        return (false);
      }
      return (this.trgReg_.equals(otherGT.trgReg_));
    }
    
    public boolean isCross() {
      return (!srcReg_.equals(trgReg_));
    }
  
    public String getSourceRegion() {
      return (srcReg_);
    }
    
    public String getTargetRegion() {
      return (trgReg_);
    } 
    
    @Override
    public int hashCode() {
      return ((srcReg_.hashCode() * 3) + trgReg_.hashCode());
    }
    
    @Override
    public String toString() {
      return ("CrossRegionTuple : " + srcReg_ + " -> " + trgReg_);
    }
  }   

  /***************************************************************************
  **
  ** Used to return QPCR map results
  **
  */
  
  public static class TirMapResult {
    public String name;
    public Type type;
    
    public enum Type {ENTRY_MAP, SOURCE_MAP};

    public TirMapResult(String name, Type type) {
      this.name = name;
      this.type = type;
    }
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TemporalInputRangeWorker extends AbstractFactoryClient {
    
    private DataAccessContext dacx_;
    private TemporalRange.TemporalRangeWorker trw_;
    private TemporalInputMapWorker timw_;
    private TemporalGroupMapWorker tgmw_;
    
    public TemporalInputRangeWorker(FactoryWhiteboard whiteboard, boolean mapsAreIllegal) {
      super(whiteboard);
      myKeys_.add("TemporalInputRangeData");
      trw_ = new TemporalRange.TemporalRangeWorker(whiteboard);
      installWorker(trw_, new MyTemporalRangeGlue());
      timw_ = new TemporalInputMapWorker(whiteboard, mapsAreIllegal);
      installWorker(timw_, null);
      tgmw_ = new TemporalGroupMapWorker(whiteboard, mapsAreIllegal);
      installWorker(tgmw_, null);
    }
    

    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      trw_.installContext(dacx_);
      timw_.installContext(dacx_);
      tgmw_.installContext(dacx_);
      return;
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("TemporalInputRangeData")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tird = buildFromXML(elemName, attrs);
        retval = board.tird;
        if (retval != null) {
          dacx_.getTemporalRangeSrc().setTemporalInputRangeData(board.tird);
        }
      }
      return (retval);     
    }  
    
    private TemporalInputRangeData buildFromXML(String elemName, Attributes attrs) throws IOException {  
      return (new TemporalInputRangeData(dacx_));
    }
  }
  
  public static class MyTemporalRangeGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.tird.addEntry(board.temporalRange);
      return (null);
    }
  }

  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TemporalInputMapWorker extends AbstractFactoryClient {
    
    private DataAccessContext dacx_;
    private boolean mapsAreIllegal_;
    private TemporalInputMapEntryWorker timew_;
    
    public TemporalInputMapWorker(FactoryWhiteboard whiteboard, boolean mapsAreIllegal) {
      super(whiteboard);
      myKeys_.add("trMap");
      mapsAreIllegal_ = mapsAreIllegal;
      timew_ = new TemporalInputMapEntryWorker(whiteboard);
      installWorker(timew_, new MyMapEntryGlue());
    }
    
    public void installContext(DataAccessContext dacx) {
      dacx_ = dacx;
      timew_.installContext(dacx_);
      return;
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      if (elemName.equals("trMap")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tmrKey = buildFromXML(elemName, attrs);
        board.tmrList = new ArrayList<TemporalInputRangeData.TirMapResult>();
        retval = board.tmrList;
      }
      return (retval);     
    }
    //
    // This list addition can only happen when the list is complete! No glue stick! 
    //
    @Override
    protected void localFinishElement(String elemName) throws IOException {
      if (elemName.equals("trMap")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tird.addCombinedTemporalInputRangeMaps(board.tmrKey, board.tmrList);
        board.tmrKey = null;
        board.tmrList = null;
      } 
      return;
    }
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String key = AttributeExtractor.extractAttribute(elemName, attrs, "trMap", "key", true);
      return (key);
    }
  } 
  
  public static class MyMapEntryGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.tmrList.add(board.tmres);
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TemporalInputMapEntryWorker extends AbstractFactoryClient {
 
    @SuppressWarnings("unused")
    private DataAccessContext dacx_;
    
    public TemporalInputMapEntryWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("useTr");
    }
    
    public void installContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }
 
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("useTr")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tmres = buildFromXML(elemName, attrs);
        retval = board.tmres;
      }
      return (retval);     
    } 
    
    private TirMapResult buildFromXML(String elemName, Attributes attrs) throws IOException {     
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "useTr", "name", true);
      String type = AttributeExtractor.extractAttribute(elemName, attrs, "useTr", "type", true);
      TirMapResult.Type mapType = (type.equals("entry")) ? TirMapResult.Type.ENTRY_MAP : TirMapResult.Type.SOURCE_MAP;
      return (new TirMapResult(name, mapType));   
    }
  }

  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TemporalGroupMapWorker extends AbstractFactoryClient {
    
    private boolean mapsAreIllegal_;
    private DataAccessContext dacx_;
    private TemporalGroupMapEntryWorker tgmew_;
    
    public TemporalGroupMapWorker(FactoryWhiteboard whiteboard, boolean mapsAreIllegal) {
      super(whiteboard);
      myKeys_.add("trGroupMap");
      mapsAreIllegal_ = mapsAreIllegal;
      tgmew_ = new TemporalGroupMapEntryWorker(whiteboard);
      installWorker(tgmew_, new MyGroupMapEntryGlue());
    }
    
    public void installContext(DataAccessContext dacx) {
      dacx_ = dacx;
      tgmew_.installContext(dacx_);
      return;
    }
       
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      if (elemName.equals("trGroupMap")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tirdguKey = buildFromXML(elemName, attrs);
        board.tirdguList = new ArrayList<GroupUsage>();
        retval = board.tirdguList;
      }
      return (retval);     
    } 
    
    //
    // This list addition can only happen when the list is complete! No glue stick! 
    //
    @Override
    protected void localFinishElement(String elemName) throws IOException {
      if (elemName.equals("trGroupMap")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tird.setTemporalRangeGroupMap(board.tirdguKey, board.tirdguList);
        board.tirdguKey = null;
        board.tirdguList = null;
      } 
      return;
    }

    private String buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String key = AttributeExtractor.extractAttribute(elemName, attrs, "trGroupMap", "key", true);
      return (key);
    }
  } 
  
  public static class MyGroupMapEntryGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.tirdguList.add(board.tirdgu);
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TemporalGroupMapEntryWorker extends AbstractFactoryClient {
 
    @SuppressWarnings("unused")
    private DataAccessContext dacx_;
    
    public TemporalGroupMapEntryWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("trUseGroup");
    }
    
    public void installContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("trUseGroup")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tirdgu = buildFromXML(elemName, attrs);
        retval = board.tirdgu;
      }
      return (retval);     
    } 
    
    private GroupUsage buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String mappedGroup = AttributeExtractor.extractAttribute(elemName, attrs, "trUseGroup", "name", true);
      String usage = AttributeExtractor.extractAttribute(elemName, attrs, "trUseGroup", "useFor", false);    
      return (new GroupUsage(mappedGroup, usage));   
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Write the trmap to XML
  **
  */
  
  private void writeTrMap(PrintWriter out, Indenter ind) {
    ind.up().indent();    
    out.println("<trMaps>");
    TreeSet<String> sorted = new TreeSet<String>();
    sorted.addAll(trEntryMap_.keySet());
    sorted.addAll(trSourceMap_.keySet());    
    Iterator<String> mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = mapKeys.next();     
      List<String> elist = trEntryMap_.get(key);
      List<String> slist = trSourceMap_.get(key);
      ind.indent();
      out.print("<trMap key=\"");
      out.print(key);
      out.println("\">");
      ind.up();
      if (elist != null) {
        Iterator<String> lit = elist.iterator();
        while (lit.hasNext()) {
          String useTr = lit.next();      
          ind.indent();
          out.print("<useTr type=\"entry\" name=\"");
          out.print(useTr);
          out.println("\"/>");
        }
      }
      if (slist != null) {
        Iterator<String> lit = slist.iterator(); 
        while (lit.hasNext()) {
          String useTr = lit.next();      
          ind.indent();
          out.print("<useTr type=\"source\" name=\"");
          out.print(useTr);
          out.println("\"/>");
        }
      }
      ind.down().indent(); 
      out.println("</trMap>");
    }
    ind.down().indent(); 
    out.println("</trMaps>");
    return;
  }  

  /***************************************************************************
  **
  ** Write the group map to XML
  **
  */
  
  private void writeGroupMap(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<trGroupMaps>");
    TreeSet<String> sorted = new TreeSet<String>();
    sorted.addAll(groupMap_.keySet());
    Iterator<String> mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = mapKeys.next();     
      List<GroupUsage> list = groupMap_.get(key);
      ind.indent();
      out.print("<trGroupMap key=\"");
      out.print(key);
      out.println("\">");
      Iterator<GroupUsage> lit = list.iterator();
      ind.up();
      while (lit.hasNext()) {
        GroupUsage usegr = lit.next();      
        ind.indent();
        out.print("<trUseGroup name=\"");
        out.print(usegr.mappedGroup);
        if (usegr.usage != null) {
          out.print("\" useFor=\""); 
          out.print(usegr.usage);          
        }
        out.println("\"/>");
      }
      ind.down().indent(); 
      out.println("</trGroupMap>");
    }
    ind.down().indent(); 
    out.println("</trGroupMaps>");
    return;
  }
}
