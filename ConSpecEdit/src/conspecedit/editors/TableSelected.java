/**
 * Copyright 2012  David Llewellyn-Jones <D.Llewellyn-Jones@ljmu.ac.uk>
 * Liverpool John Moores University <http://www.ljmu.ac.uk/cmp/>
 * Aniketos Project <http://www.aniketos.eu>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package conspecedit.editors;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * Helper class for figuring out which cell in a table was selected based on a given set of mouse coordinates.
 * The column and row of the cell are also calculated.
 * Can be used with any Eclipse/SWT table.
 * Surely this should be possible using one of the build-in table methods?
 * @author Aniketos Project; David Llewellyn-Jones, Liverpool John Moores University
 *
 */
public class TableSelected {
	/**
	 * The row of the cell selected in the table, or -1 if there's no valid cell at the given position.
	 */
	public int row;
	/**
	 * The column of the cell selected in the table, or -1 if there's no valid cell at the given position.
	 */
	public int column;
	/**
	 * The cell entry selected in the table, or null if there's no valid cell at the given position.
	 */
	public TableItem item;
	/**
	 * Whether the coordinates relate to a valid cell.
	 */
	public boolean found;
	
	/**
	 * Constructor for initialising the class.
	 * Sets up the class attributes that specify which cell is selected based on the coordinates provided.
	 * @param table The table to find the cell within.
	 * @param x The x position of the selection.
	 * @param y The y position of the selection.
	 */
	public TableSelected (Table table, int x, int y) {
		// Call the method for finding the cell based on the given coordinates. 
		set (table, x, y);
	}
	
	/**
	 * Sets up the class attributes that specify which cell is selected based on the coordinates provided.
	 * @param table The table to find the cell within.
	 * @param x The x position of the selection.
	 * @param y The y position of the selection.
	 */
	public void set (Table table, int x, int y) {
		// Reset the class attributes to represent an invalid cell.
		clear();

		// Check whether the table exists.
		if (table != null){
			boolean visible = false;
			// Get the client area for the table.
			Rectangle clientArea = table.getClientArea();
			// Set up the point selected.
			Point point = new Point(x, y);
			// Loop through each row of the table, starting from the top, to find out which row was selected.
			row = table.getTopIndex();
			found = false;
			column = -1;
			item = null;
			while ((row < table.getItemCount()) && (!found)) {
				visible = false;
				item = table.getItem(row);
				// Cycle through each column of the table to find out which column was selected. 
				column = 0;
				while ((column < table.getColumnCount()) && (!found)) {
					// Find the bounds of the current item
					Rectangle bounds = item.getBounds(column);
					// Check whether the point falls inside the bounds of the item.
					if (bounds.contains(point)) {
						// The bounds do fall inside, so we've found what we're looking for.
						found = true;
					}
					// Check whether the item is actually visible on screen
					if (!visible && bounds.intersects(clientArea)) {
						visible = true;
					}
					column++;
				}
				if (!visible) {
					found = true;
				}
				row++;
			}
	
			if (!visible) {
				// We didn't find anything or the item found wasn't actually visible.
				found = false;
				row = -1;
				column = -1;
				item = null;
			}
			else {
				// We did find something, but need to fix the row and column positions
				// (since the loop will have incremented them one time too many).
				row--;
				column--;
			}
		}
	}
	
	/**
	 * Check whether or not a particular point falls inside the header of the table.
	 * If it does, ensure that the item selected is cleared. 
	 * @param table The table to check against.
	 * @param x The x position of the selection.
	 * @param y The y position of the selection.
	 */
	public void headerClear(Table table, int x, int y) {
		// Map the point inside the table.
		Point point = table.getParent().getDisplay().map(null, table, new Point (x, y));
		// Subtract the height of the header from the y position.
		point.y -= table.getHeaderHeight();
		// Check whether the resulting point lies within the client area.
		Rectangle clientArea = table.getClientArea();
		if (!clientArea.contains(point)) {
			// If it doesn't clear the attributes to reflect that no cell was selected.
			clear();
		}
	}
	
	/**
	 * Clear the object attributes to reflect no cell being selected.
	 */
	public void clear () {
		found = false;
		row = -1;
		column = -1;
		item = null;
	}
	
	/**
	 * Get whether a cell is selected.
	 * @return Whether a cell was selected. True if there was a cell selected, false otherwise.
	 */
	public boolean getFound () {
		return found;
	}
	
	/**
	 * Get the row of the cell selected.
	 * @return The row of the cell selected, or -1 if there is none.
	 */
	public int getRow () {
		return row;
	}
	
	/**
	 * Get the column of the cell selected.
	 * @return The column of the cell selected, or -1 if there is none.
	 */
	public int getColumn () {
		return column;
	}
	
	/**
	 * Get the item selected.
	 * @return The item selected, or null if there is none.
	 */
	public TableItem getItem () {
		return item;
	}
}
