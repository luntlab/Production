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

package org.systemsbiology.biotapestry.db;

/****************************************************************************
**
** Serves as a workspace source for non-database workspaces
*/
      
public class LocalWorkspaceSource implements WorkspaceSource {

  private Workspace workspace_;

  public LocalWorkspaceSource() {
    workspace_ = new Workspace();
  }

  /***************************************************************************
  ** 
  ** Get the workspace definition
  */

  public Workspace getWorkspace() {
    return (workspace_);
  }

  /***************************************************************************
  ** 
  ** Set the workspace definition
  */

  public void simpleSetWorkspace(Workspace workspace) {
    workspace_ = workspace;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Set the workspace definition
  */

  public DatabaseChange setWorkspace(Workspace workspace) {
    workspace_ = workspace;
    return (null);
  }
}