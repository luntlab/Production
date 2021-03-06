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

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.SubGroupCreationDialogFactory;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Adding new Groups
*/

public class AddSubGroup extends AbstractControlFlow {
  
  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum Action {
    CREATE("groupPopup.CreateSubGroup", "groupPopup.CreateSubGroup", "FIXME24.gif", "groupPopup.CreateSubGroupMnem", null),
    INCLUDE("genePopup.EditTemporalInputMap", "genePopup.EditTemporalInputMap", "FIXME24.gif", "genePopup.EditTemporalInputMapMnem", null),
    ACTIVATE("genePopup.DeleteTemporalInput", "genePopup.DeleteTemporalInput", "FIXME24.gif", "genePopup.DeleteTemporalInputMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    Action(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    }  
     
    public String getName() {
      return (name_);      
    }
     
    public String getDesc() {
      return (desc_);      
    }
     
    public String getIcon() {
      return (icon_);      
    }
     
    public String getMnem() {
      return (mnem_);      
    }
     
    public String getAccel() {
      return (accel_);      
    }     
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private Action action_;
  
  private String parentGroup_;
  private String baseSubKey_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddSubGroup(BTState appState, Action action) {
    super(appState);
    if (action != Action.CREATE) {
      throw new IllegalArgumentException();
    }
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action; 
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddSubGroup(BTState appState, Action action, GroupPairArgs gpa) {
    super(appState);
    if (action == Action.CREATE) {
      throw new IllegalArgumentException();
    }
    name = gpa.getName();
    desc = gpa.getName();
    action_ = action; 
    parentGroup_ = gpa.getParentGroup();
    baseSubKey_ = gpa.getBaseSubKey();
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    switch (action_) {
      case CREATE: 
        return (cache.isNonDynamicInstance());
      case INCLUDE:
      case ACTIVATE:
        return (true);
      default:
        throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSpli, DataAccessContext rcx) { 
    switch (action_) {
      case CREATE: 
        GenomeInstance gi = rcx.getGenomeAsInstance();
        return (gi.getVfgParent() == null);
      case INCLUDE:
      case ACTIVATE:
        return (true);
      default:
        throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  
  @Override  
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    AddState retval = (action_ == Action.CREATE) ? new AddState(appState_, dacx) : new AddState(appState_, action_, parentGroup_, baseSubKey_, dacx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        throw new IllegalStateException();
      } else {
        AddState ans = (AddState)last.currStateX;
        if (ans.cfh == null) {
          ans.cfh = cfh;
        }
        if (ans.getNextStep().equals("stepBiWarning")) {
          next = ans.stepBiWarning();
        } else if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();           
        } else if (ans.getNextStep().equals("stepOne")) {
          next = ans.stepOne();
        } else if (ans.getNextStep().equals("stepDataExtract")) {
          next = ans.stepDataExtract(last);   
        } else if (ans.getNextStep().equals("stepFinishPlacement")) {
          next = ans.stepFinishPlacement();      
        } else if (ans.getNextStep().equals("stepActivate")) {
          next = ans.stepActivate();       
        } else if (ans.getNextStep().equals("stepInclude")) {
          next = ans.stepInclude();         
        } else {
          throw new IllegalStateException();
        }
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
  
  /***************************************************************************
  **
  ** Called as new X, Y info comes in to place the group
  */
  
  @Override
  public DialogAndInProcessCmd processClick(Point theClick, boolean isShifted, double pixDiam, DialogAndInProcessCmd.CmdState cmds) {
    AddState ans = (AddState)cmds;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    ans.nextStep_ = "stepFinishPlacement"; 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Running State.
  */
        
  public static class AddState implements DialogAndInProcessCmd.PopupCmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
   // private Genome targetGenome;
  //  private String genomeKey;
  //  private GenomeInstance targetGi;
  //  private Layout targetLayout;
    private DataAccessContext rcx;
    private Intersection intersect;  
    private Group newSubGroup;
    private int newSubGroupLayer;
    private ServerControlFlowHarness cfh;
    private int x;
    private int y;  
    private String nextStep_;     
    private BTState appState_;
    
    private String myParentGroup_;
    private String myBaseSubKey_;
         
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Init intersection
    */  
    
    public void setIntersection(Intersection intersect) {
      this.intersect = intersect;
      return;
    }  
    
    /***************************************************************************
    **
    ** Construct
    */
    
    public AddState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      rcx = dacx;
      nextStep_ = "stepBiWarning";
    }
    
    /***************************************************************************
    **
    ** mouse masking
    */
    
    public int getMask() {
      return (MainCommands.ALLOW_NAV_PUSH);
    }
    
    public boolean pushDisplay() {
      return (false);
    }
    
    public boolean noSubModels() {
      return (false);
    }
        
    public boolean noInstances() {
      return (false);
    }
            
    public boolean showBubbles() {
      return (false);
    }
    
    public boolean noRootModel() {
      return (true);
    }
       
    public boolean mustBeDynamic() {
      return (false);
    }
    
    public boolean cannotBeDynamic() {
      return (true);
    }
    
    public boolean hasTargetsAndOverlays() {
      return (false);
    }
    
    public Floater floaterMode() {
      return (DialogAndInProcessCmd.MouseClickCmdState.Floater.NO_FLOATER);
    }
    
    public void setFloaterPropsInLayout(Layout flay) {
      throw new IllegalStateException();
    }
  
    public Object getFloater(int x, int y) {
      throw new IllegalStateException();
    }
    
    public Color getFloaterColor() {
      throw new IllegalStateException();
    }
    
    /***************************************************************************
    **
    ** Construct
    */
    
    public AddState(BTState appState, Action action, String parentGroup, String baseSubKey, DataAccessContext dacx) {
      if (action == Action.CREATE) {
        throw new IllegalArgumentException();
      }
      appState_ = appState;
      rcx = dacx;
      myBaseSubKey_ = baseSubKey;
      myParentGroup_ = parentGroup;
      nextStep_ = (action == Action.ACTIVATE) ? "stepActivate" : "stepInclude";
    }
  
    /***************************************************************************
    **
    ** Warn of build instructions
    */
       
    private DialogAndInProcessCmd stepBiWarning() {
      DialogAndInProcessCmd doneval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
      if (rcx.getDBGenome().getID().equals(rcx.getGenomeID())) {  // Only works if not root genome
        return (doneval);
      }
      DialogAndInProcessCmd daipc;
      if (appState_.getDB().haveBuildInstructions()) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("instructWarning.message");
        String title = rMan.getString("instructWarning.title");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);      
       } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      }
      nextStep_ = "stepOne";
      return (daipc);     
    }
    
    /***************************************************************************
    **
    ** Handle the control flow for drawing into subset instance
    */
       
    private DialogAndInProcessCmd stepOne() {
      if (rcx.getGenomeAsInstance().getVfgParentRoot() != null) {
        throw new IllegalStateException();
      }
      String defaultName = appState_.getRMan().getString("sgcreate.defaultName");   // FIX ME generate unique suffix
      SubGroupCreationDialogFactory.SubGroupBuildArgs ba = 
        new SubGroupCreationDialogFactory.SubGroupBuildArgs(defaultName, rcx.getGenomeAsInstance().usedGroupNames());
      SubGroupCreationDialogFactory dgcdf = new SubGroupCreationDialogFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = dgcdf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepDataExtract";
      return (retval);
    } 
     
    /***************************************************************************
    **
    ** Handle the control flow for drawing into subset instance
    */
         
     private DialogAndInProcessCmd stepDataExtract(DialogAndInProcessCmd cmd) {
       SubGroupCreationDialogFactory.SubGroupRequest crq = (SubGroupCreationDialogFactory.SubGroupRequest)cmd.cfhui;   
       if (!crq.haveResults()) {
         DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
         return (retval);
       } 
       String id = rcx.getNextKey(); // Unique in root address space  
       String parentID = intersect.getObjectID();
       newSubGroup = new Group(rcx.rMan, id, crq.nameResult, parentID);
       newSubGroupLayer = crq.layerResult;
       nextStep_ = "stepSetToMode";
       DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
       return (retval);
     } 
  
    /***************************************************************************
    **
    ** Install a mouse handler
    */
     
    private DialogAndInProcessCmd stepSetToMode() {       
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = PanelCommands.Mode.ADD_SUB_GROUP;
      return (retval);
    }
      
    /***************************************************************************
    **
    ** Draw a group directly into an instance model
    */  
    
    private DialogAndInProcessCmd stepFinishPlacement() {  
 
      //
      // Undo/Redo support
      //
      
      UndoSupport support = new UndoSupport(appState_, "undo.addNewSubGroup");       
      
      GenomeChange gc = rcx.getGenomeAsInstance().addGroupWithExistingLabel(newSubGroup);
      if (gc != null) {
        support.addEdit(new GenomeChangeCmd(appState_, rcx, gc));
        support.addEvent(new ModelChangeEvent(rcx.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      } 
      
      String parent = newSubGroup.getParentID();  // Only works at VfA level!
      GroupProperties parProp = rcx.getLayout().getGroupProperties(parent);
      
      GroupProperties prop = new GroupProperties(newSubGroup.getID(), rcx.getLayout(), newSubGroupLayer, 
                                                 new Point2D.Double(x, y), parProp.getOrder(), rcx.cRes);
      Layout.PropChange[] lpc = new Layout.PropChange[1];    
      lpc[0] = rcx.getLayout().setGroupProperties(newSubGroup.getID(), prop);   
      if (lpc != null) {
        support.addEdit(new PropChangeCmd(appState_, rcx, lpc));
        support.addEvent(new LayoutChangeEvent(rcx.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));    
      }
      support.finish();      
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    }
     
    /***************************************************************************
    **
    ** Simple activate step
    */  
    
    private DialogAndInProcessCmd stepActivate() {   
      GroupCreationSupport handler = new GroupCreationSupport(appState_);
      handler.makeSubGroupActive(rcx, myParentGroup_, myBaseSubKey_, null);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Simple include step
    */  
    
    private DialogAndInProcessCmd stepInclude() {      
      GroupCreationSupport handler = new GroupCreationSupport(appState_);
      handler.includeSubGroup(rcx, myParentGroup_, myBaseSubKey_);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }   
  }
 
  /***************************************************************************
  **
  ** Arguments
  */
  
  public static class GroupPairArgs extends AbstractOptArgs {
    
    private static String[] keys_;
    
    static {
      keys_ = new String[] {"parentGroup", "baseSubKey", "name"};  
    }
    
    public String getParentGroup() {
      return (getValue(0));
    }
     
    public String getBaseSubKey() {
      return (getValue(1));
    }
   
    public String getName() {
      return (getValue(2));
    }
    
    public GroupPairArgs(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    public GroupPairArgs(String parentGroup, String baseSubKey, String name) {
      super();
      setValue(0, parentGroup);
      setValue(1, baseSubKey);
      setValue(2, name);
      bundle();
    }
  } 
}
