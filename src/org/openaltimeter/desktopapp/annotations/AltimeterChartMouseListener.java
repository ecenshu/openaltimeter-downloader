/*
    openaltimeter -- an open-source altimeter for RC aircraft
    Copyright (C) 2010-2011  Jan Steidl, Jony Hudson
    http://openaltimeter.org

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openaltimeter.desktopapp.annotations;

import java.awt.event.InputEvent;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.XYPlot;

// Listens for mouse events on an altimeter chart. It is installed by an
// AltimeterAnnotationManager, and calls back up to its manager to add
// the annotations.
public final class AltimeterChartMouseListener implements ChartMouseListener 
{
	private final ChartPanel cp;
	private final AltimeterAnnotationManager am;
	private double lastAnnotationX = 0d;
	private double lastAnnotationY = 0d;
	
	public AltimeterChartMouseListener(ChartPanel cp, AltimeterAnnotationManager am) {
		this.cp = cp;
		this.am = am;
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent arg0) {
		return;
	}

	@Override
	public void chartMouseClicked(final ChartMouseEvent event) {
	    SwingUtilities.invokeLater(new Runnable() {
	       public void run()
	       {
	          XYPlot xyplot = (XYPlot) cp.getChart().getPlot();

	          double x, y;
	          x = xyplot.getDomainCrosshairValue();
	          y = xyplot.getRangeCrosshairValue();

	          am.addUserHeightAnnotation(x, y);
	                           	                  
	          int onmask = InputEvent.SHIFT_DOWN_MASK;
	          if ((event.getTrigger().getModifiersEx() & (onmask)) == onmask) 
	        	  am.addUserVarioAnnotation(lastAnnotationX, lastAnnotationY, x, y);
	     	          
	          lastAnnotationX = x;
	          lastAnnotationY = y;
	       }
	    });
	}
}