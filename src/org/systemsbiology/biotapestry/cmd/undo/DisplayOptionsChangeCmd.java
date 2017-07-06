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


package org.systemsbiology.biotapestry.cmd.undo;

import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.DisplayOptionsChange;

/****************************************************************************
**
** Handles undos of display option changes
*/

public class DisplayOptionsChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private DisplayOptionsChange restore_;
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the command. Use this if the change can use the current DACX
  */ 
  
  public DisplayOptionsChangeCmd(DisplayOptionsChange restore) {
    super();
    restore_ = restore;
  }  
  
  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public DisplayOptionsChangeCmd(DisplayOptionsChange restore, TabPinnedDynamicDataAccessContext tpdacx) {
    super(tpdacx);
    restore_ = restore;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Name to show
  */ 
  
  @Override
  public String getPresentationName() {
    return ("Display Option Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    if (restore_.oldOpts != null) {
      dacx_.getDisplayOptsSource().changeUndo(restore_);
    } else if (restore_.oldPertOpts != null) {
      dacx_.getExpDataSrc().getPertData().setPertDisplayOptionsForIO(restore_.oldPertOpts);
    } else {
      throw new IllegalStateException();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo the operation
  */ 
  
  @Override
  public void redo() {
    super.redo();
    if (restore_.newOpts != null) {
      dacx_.getDisplayOptsSource().changeRedo(restore_);
    } else if (restore_.newPertOpts != null) {
      dacx_.getExpDataSrc().getPertData().setPertDisplayOptionsForIO(restore_.newPertOpts);
    } else {
      throw new IllegalStateException();
    }
    return;
  }
}
