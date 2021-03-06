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

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineFragment;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineTextShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.TextShape;

public class ModalTextShapeFactoryForDesktop implements ModalTextShapeFactory {
		public TextShape buildTextShape(String text, AnnotatedFont font, Color color, float txtX, float txtY, AffineTransform trans) {
				return new TextShape(text, font, color, txtX, txtY, trans);
		}
		
		public MultiLineTextShape buildMultiLineTextShape(Color color, AnnotatedFont font, ArrayList<MultiLineFragment> fragments) {			
				MultiLineTextShape mlts = new MultiLineTextShape(color, font);
				
				for (MultiLineFragment fragment : fragments) {
						mlts.addFragment(fragment.frag_, fragment.tl_, fragment.x_, fragment.y_);
				}
				
				return mlts;
		}
}
