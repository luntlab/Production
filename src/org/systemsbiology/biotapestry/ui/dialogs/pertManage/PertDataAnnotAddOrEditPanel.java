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

package org.systemsbiology.biotapestry.ui.dialogs.pertManage;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for editing or creating annotation list for a data point
*/

public class PertDataAnnotAddOrEditPanel extends AnimatedSplitEditPanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private List<String> currAnnots_;
  private String parentCurrKey_;
  private List<String> annotsResult_;
  private PerturbationData pd_;
  private List annotList_;
  private EditableTable estAnnot_;
  private PertManageHelper pmh_;
  
  private static final long serialVersionUID = 1L;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PertDataAnnotAddOrEditPanel(UIComponentSource uics, JFrame parent, PerturbationData pd,
                                     TimeCourseData tcd, PendingEditTracker pet, String myKey) {
    super(uics, parent, pet, myKey, 1);
    pd_ = pd;
    pmh_ = new PertManageHelper(uics, parent, pd, tcd, rMan_, gbc_, pet_);
  
    annotList_ = new ArrayList(); 
    
    //
    // Build the values table tabs.
    //

    estAnnot_ = new EditableTable(uics, new EditableTable.OneEnumTableModel(uics, "psAddEditDataAnnot.annot", annotList_), parent_);
    EditableTable.TableParams etp = pmh_.tableParamsForAnnot(annotList_);
    JPanel annotTablePan = estAnnot_.buildEditableTable(etp);
    JPanel annotTableWithButton = pmh_.addEditButton(annotTablePan, "psAddEditDataAnnot.annotEdit", true, new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          String who = pmh_.getSelectedEnumVal(estAnnot_);        
          pet_.jumpToRemoteEdit(PertAnnotManagePanel.MANAGER_KEY, PertAnnotManagePanel.ANNOT_KEY, who);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    //
    // Add the panel:
    //
    
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);       
    add(annotTableWithButton, gbc_);
    rowNum_ += 10;
     
    finishConstruction();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Clear out the editor:
  */  
   
  public void closeAction() {
    estAnnot_.stopTheEditing(false);
    super.closeAction();
    return;
  } 
  
  /***************************************************************************
  **
  ** Set the new annotations
  ** 
  */
  
  public void setAnnots(String parentKey, List<String> annots, int mode) {
    mode_ = mode;
    currAnnots_ = annots;
    parentCurrKey_ = parentKey;
    displayProperties();
    return;
  }
 
  /***************************************************************************
  **
  ** Get the annotation result
  ** 
  */
  
  public List<String> getResult() {
    return (annotsResult_);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get freeze-dried state:
  */ 
  
  protected FreezeDried getFreezeDriedState() {
    return (new MyFreezeDried());
  }  
   
  /***************************************************************************
  **
  ** Update the options in our UI components
  ** 
  */
  
  protected void updateOptions() {
    annotList_ = pmh_.buildAnnotEnum();
    HashMap<Integer, EditableTable.EnumCellInfo> perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, annotList_, EnumCell.class));      
    estAnnot_.refreshEditorsAndRenderers(perColumnEnums);
    ((EditableTable.OneEnumTableModel)estAnnot_.getModel()).setCurrentEnums(annotList_);   
    return;
  }
  
  /***************************************************************************
  **
  ** Stash our UI values
  ** 
  */
  
   protected boolean stashResults() { 
    Iterator<EditableTable.OneEnumTableModel.TableRow> tdit = estAnnot_.getModel().getValuesFromTable().iterator();
    annotsResult_ = (tdit.hasNext()) ? new ArrayList<String>() : null;
    while (tdit.hasNext()) {
      EditableTable.OneEnumTableModel.TableRow ent = tdit.next();
      EnumCell ec = ent.enumChoice;
      annotsResult_.add(ec.internal);
    }
    estAnnot_.stopTheEditing(false);
    return (true);
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {

    updateOptions();
    
    if (currAnnots_ == null) {
      currAnnots_ = new ArrayList();
    }
    ((EditableTable.OneEnumTableModel)estAnnot_.getModel()).setCurrentEnums(annotList_);       
    List annotRows = pmh_.buildAnnotDisplayList(currAnnots_, estAnnot_, annotList_, false);
    estAnnot_.updateTable(true, annotRows);
    return;
  } 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  /***************************************************************************
  **
  ** Freeze dried state for hot updates
  */

  public class MyFreezeDried implements FreezeDried {
  
    private List estAnnotValues;
    private ArrayList frozenAnnots;
    
    MyFreezeDried() {
      frozenAnnots = (currAnnots_ == null) ? null : new ArrayList(currAnnots_);
      estAnnotValues = estAnnot_.getModel().getValuesFromTable();
    }   
    
    public boolean needToCancel() {
      if ((parentCurrKey_ != null) && (mode_ == EDIT_MODE)) {
        if (pd_.getDataPoint(parentCurrKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    }
 
    public void reInstall() {
      currAnnots_ = pmh_.fixupFrozenAnnot(frozenAnnots);
      List annotRows = pmh_.buildAnnotDisplayList(estAnnotValues, estAnnot_, annotList_, true);  
      estAnnot_.updateTable(true, annotRows);
      return;
    }
  }
}
