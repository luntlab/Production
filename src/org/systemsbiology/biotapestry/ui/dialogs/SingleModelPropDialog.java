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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.ui.DataLocator;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;

/****************************************************************************
**
** Dialog box for editing single model property data
*/

public class SingleModelPropDialog extends JDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField longNameField_;
  private JTextField descripField_;
  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  
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
  
  public SingleModelPropDialog(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("smprop.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    if (!dacx_.currentGenomeIsRootDBGenome()) {
      throw new IllegalArgumentException();
    }
        
    ResourceManager rMan = dacx_.getRMan();    
    setSize(700, 200);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    
    int rowNum = 0;
    
    //
    // Build the long name panel:
    //

    JLabel label = new JLabel(rMan.getString("smprop.longName"));
    longNameField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(longNameField_, gbc);
    
    //
    // Build the description panel:
    //

    label = new JLabel(rMan.getString("smprop.descrip"));
    descripField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(descripField_, gbc);
        
    //
    // Build the button panel:
    //
 
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          applyProperties();
          SingleModelPropDialog.this.setVisible(false);
          SingleModelPropDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          SingleModelPropDialog.this.setVisible(false);
          SingleModelPropDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, rowNum, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
    displayProperties();
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the current model property values to our UI components
  ** 
  */
  
  private void displayProperties() {

    longNameField_.setText(dacx_.getCurrentGenome().getLongName());
    descripField_.setText(dacx_.getCurrentGenome().getDescription());    
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the model
  ** 
  */
  
  private void applyProperties() {
    
    //
    // Undo/Redo support
    //
    
    UndoSupport support = uFac_.provideUndoSupport("undo.smprop", dacx_);  
    String longName = longNameField_.getText();
    GenomeChange gc = dacx_.getCurrentGenomeAsDBGenome().setProperties(dacx_.getCurrentGenome().getName(),  
                                                                longName, 
                                                                descripField_.getText());
    GenomeChangeCmd cmd = new GenomeChangeCmd(gc);
    support.addEdit(cmd);
    
    if (!longName.trim().equals("")) {
      new DataLocator(uics_.getGenomePresentation(), dacx_).setTitleLocation(support, dacx_.getCurrentGenomeID(), longName);
    }
    
    ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
    support.addEvent(mcev);    
    mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.PROPERTY_CHANGE);
    support.addEvent(mcev);
    
    support.finish();    
    return;
  }  
}
