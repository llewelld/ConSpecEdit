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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * Manage the listener for the text box widget used for editing
 * the text field of the declarations table in the ConSpec Editor.
 * @author Aniketos Project; David Llewellyn-Jones, Liverpool John Moores University
 *
 */
public abstract class DeclarationTextListener implements Listener {
	/**
	 * The row of the table the text box relates to.
	 */
	private int index;
	/**
	 * The item in the table the text box relates to.
	 */
	private TableItem item;
	/**
	 * The column of the table the text box relates to.
	 */
	private int column;
	/**
	 * The text box widget that represents the control.
	 */
	private Text text;
	/**
	 * The declarations table that the cell belongs to.
	 */
	private Table declarations;
	
	/**
	 * Constructor for creating a text box for in-table editing of declaration entries. 
	 * @param index The row of the cell in the declarations table being edited.
	 * @param item The item of the cell in the declarations table being edited.
	 * @param column The column of the cell in the declarations table being edited.
	 * @param combo The text box used for editing.
	 * @param declarations The table of declarations in which the cell belongs.
	 */
	public DeclarationTextListener (int index, TableItem item, int column, Text text, Table declarations) {
		// Call the Listener constructor.
		super();
		// Set the attributes passed in.
		this.index = index;
		this.item = item;
		this.column = column;
		this.text = text;
		this.declarations = declarations;
	}

	/**
	 * Set up a widget with appropriate values for editing
	 * @param index The row of the cell in the declarations table being edited.
	 * @param item The item of the cell in the declarations table being edited.
	 * @param column The column of the cell in the declarations table being edited.
	 * @param declarations The declarations table in which the cell belongs.
	 * @param editor The editor to use for editing the cell.
	 * @return The text box widget that will be used for editing.
	 */
	public static Text setupWidget (int index, TableItem item, int column, Table declarations, TableEditor editor) {
		Text text = new Text (declarations, SWT.NONE);
		// Set the text box to contain the same value as that held in the appropriate table cell.
		text.setText(item.getText(column));

		// Set the editor and focus for the text box widget.
		editor.setEditor(text, item, column);
		text.selectAll();
		text.setFocus();

		// Return the text box widget that will be used for editing.
		return text;
	}

	/**
	 * Set up the relevant listeners for the text box widget.
	 * @param text The text box widget being used for editing.
	 * @param textListener The DeclarationComboListener class that will be used to keep track of the actions.
	 */
	public static void setupListener(Text text, DeclarationTextListener textListener) {
		text.addListener (SWT.FocusOut, textListener);
		text.addListener (SWT.Traverse, textListener);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void handleEvent(Event e) {
		// Handle an event on the text box widget.
		switch (e.type){
		case SWT.FocusOut:
			// If the focus leaves the widget, update the text in the table.
			updateText();
			break;
		case SWT.Traverse:
			// Moving around the table
			switch (e.detail){
			case SWT.TRAVERSE_RETURN:
				// The user hit the Return key, so update the text in the table.
				updateText();
				e.doit = false;
				break;
			case SWT.TRAVERSE_ESCAPE:
				// The user hit the Escape key, so don't update the text in the table
				// so that it reverts to the original value.
				text.dispose();
				e.doit = false;
				break;
			}
		}
		
	}

	/**
	 * Once the user is done, this method can be used to update the text in the original table based on
	 * what the user entered in the text box widget.
	 */
	private void updateText () {
		// Get the text entered by the user in the text box widget.
		String value = new String(text.getText());
		item.setText(column, text.getText());
		// If this is the last declaration in the list, we have to be careful to add a new empty line in afterwards.
		// This is to ensure the user always has a blank line to edit, in case they want to add in new declarations.
		if (index == (declarations.getItemCount() - 1)) {
			// Check to see whether there are any other values already stored in this row
			boolean empty = true;
			for (int i = 0; i < declarations.getColumnCount(); i++) {
				if (item.getText(i) != "") {
					empty = false;
				}
			}
			// If the row isn't empty, add a new one after it.
			if (!empty) {
				new TableItem(declarations, SWT.NONE);
			}
		}
		text.dispose();
		// Call the callback so that the creating method can do other things after the value has been updated.
		valueChanged(value, index, column);
	}
	
	/**
	 * Callback for when a cell has been updated. This can be used to perform other tasks whenever an entry
	 * in the table is updated.
	 * @param value The new value for the cell.
	 * @param row The row of the cell that has changed in the declarations table.
	 * @param column The column of the cell that has changed in the declarations table.
	 */
	abstract public void valueChanged (String value, int row, int column);
}
