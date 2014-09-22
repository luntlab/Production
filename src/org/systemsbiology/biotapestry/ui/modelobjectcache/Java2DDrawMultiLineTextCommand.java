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

package org.systemsbiology.biotapestry.ui.modelobjectcache;

import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.LinkedList;

import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineTextShape;

public class Java2DDrawMultiLineTextCommand implements ConcreteRenderCommand {
	private MultiLineTextShape mlt_;

	public Java2DDrawMultiLineTextCommand(MultiLineTextShape mlt) {
		mlt_ = mlt;
	}

	public void execute(Graphics2D g2, LinkedList<AffineTransform> transformStack) {
		AffineTransform saveTrans = g2.getTransform();
		
		for (MultiLineTextShape.Fragment frag : mlt_.fragments_) {
			TextLayout tl = frag.tl_;
			tl.draw(g2, frag.x_, frag.y_);
		}
		
		g2.setTransform(saveTrans);
	}
}
