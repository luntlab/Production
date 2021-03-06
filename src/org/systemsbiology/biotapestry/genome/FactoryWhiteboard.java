/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.genome;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.db.StartupView;
import org.systemsbiology.biotapestry.nav.UserTreePath;
import org.systemsbiology.biotapestry.nav.UserTreePathStop;
import org.systemsbiology.biotapestry.perturb.ConditionDictionary;
import org.systemsbiology.biotapestry.perturb.ExperimentConditions;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.MeasureProps;
import org.systemsbiology.biotapestry.perturb.NameMapper;
import org.systemsbiology.biotapestry.perturb.PertAnnotations;
import org.systemsbiology.biotapestry.perturb.PertDataPoint;
import org.systemsbiology.biotapestry.perturb.PertDictionary;
import org.systemsbiology.biotapestry.perturb.PertProperties;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.Experiment;
import org.systemsbiology.biotapestry.perturb.ExperimentControl;
import org.systemsbiology.biotapestry.perturb.MeasureScale;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.BusDrop;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.CustomEvidenceDrawStyle;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.NetModuleBusDrop;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.PerLinkDrawStyle;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.NameValuePairList;
import org.systemsbiology.biotapestry.util.FactoryUtilWhiteboard;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;

  
/***************************************************************************
**
** Info needed during XML construction by abstract factory 
** clients and glue sticks
**
*/
  
public class FactoryWhiteboard extends FactoryUtilWhiteboard {
  public Genome genome;
  public int genomeType;
  public NetworkOverlay netOvr;
  public NetModule netMod;
  public NetModuleLinkage netModLink;
  public DynamicInstanceProxy prox;
  public NameValuePairList nvPairList;  
  public NameValuePair nvPair;
  
  public UserTreePath userTreePath;
  public UserTreePathStop userTreePathStop;
  
  public StartupView startupView;
    
  public Layout layout;
  public NetOverlayProperties netOvrProps;  
  public NetModuleProperties netModProps;
  public NetModuleBusDrop nmBusDrop;
  public NetModuleLinkageProperties netModLinkProps;
  public Rectangle2D nmpShape;
  
  public NodeProperties nodeProps;
  
  public BusProperties busProps;
  public LinkSegment linkSegment;
  public BusDrop busDrop;
  public PerLinkDrawStyle perLinkSty;
  public SuggestedDrawStyle suggSty;
  
  public CustomEvidenceDrawStyle evidenceDrawSty;
  public DisplayOptions displayOptions;
  public MinMax column;
  public NameValuePair pertColorPair;
  
  public PerturbationData pertData;
  public PerturbationData.Invest invest;
  public PerturbationData.SourceName srcName;
  public PertAnnotations pa;
  public PertAnnotations.Annot annot;
  public PertDataPoint.AugPertDataPoint augPertDataPt;
  public Experiment pertSrcInfo;
  public PertSources pertSrcs;
  public PertSource pertSrc;
  public PertDictionary pDict;
  public PerturbationData.AugTarget augTarg;
  public PertProperties pertProps;
  public MeasureDictionary mDict;
  public MeasureProps measProps;
  public MeasureScale measScale;
  public PerturbationData.AugRegRestrict augRegRes;
  public String reg;
  public ConditionDictionary cDict;
  public ExperimentConditions experCond;
  public ExperimentControl expControl;
  public PerturbationData.AugUserData augUserData;
  public String userFieldName;
  
  public NameMapper currPertDataMap;
  public String currPertMapFrom;
  public String currPertMapTo;
  
  public Map<String, List<String>> currentMap;
  public String currentMapKey;
  public String currentMapTarget;
  
  public TimeCourseData.TimeBoundedRegion currTimeBoundRegion;
  public PerturbationData.SerialNumberSet sns;
  
}
