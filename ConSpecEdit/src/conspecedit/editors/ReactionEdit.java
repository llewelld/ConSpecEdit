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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.bind.JAXB;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;

import conspecedit.Activator;
import eu.aniketos.AssignType;
import eu.aniketos.AssignType.Value;
import eu.aniketos.ReactionType;
import eu.aniketos.UpdateType;
import eu.aniketos.wp2.Expression;

/**
 * Create a dialogue box for editing ConSpec reactions.
 * @author Aniketos Project; David Llewellyn-Jones, Liverpool John Moores University
 *
 */
public class ReactionEdit extends TitleAreaDialog {
	/**
	 * Store the details of the reaction being edited.
	 */
	private ReactionType reaction;
	/**
	 * Store details of the table to show the update information for the reaction being edited.
	 */
	private Table update;
	private TableSelected contextSelection;
	private TableViewer updateViewer;
	/**
	 * Store details about the guard for the reaction.
	 */
	private Text guard;
	/**
	 * Create delete actions (triggered by context menus).
	 */
	private Action deleteUpdate;
	/**
	 * Manage variables needed by Eclipse for menus, dialogue boxes and so on.
	 */
	private IWorkbenchPartSite site;
	
	/**
	 * Constructor for the reaction edit dialogue box.
	 * @param site The site that the dialogue box relates to.
	 * @param index The index (row) of the reaction in the reaction table of the Rules dialogue box.
	 * @param reaction The details of the reaction being edited.
	 */
	public ReactionEdit(IWorkbenchPartSite site, int index, ReactionType reaction) {
		// Call the TitleAreaDialog constructor.
		super(site.getShell());
		// Store the information passed in for later use.
		this.site = site;
		// Make a copy of the reaction details.
		// We can't edit the original reaction, since if the user cancels we want our edits to be ignored.
		this.reaction = copyReactionType(reaction);
		// If the reaction doesn't already have an update we have to create one, so that the user can edit it.
		if (this.reaction.getUpdate() == null) {
			UpdateType update = new UpdateType();
			this.reaction.setUpdate(update);
		}
		contextSelection = new TableSelected(null, 0, 0);
	}
	
	/**
	 * Make a copy of a reaction hierarchy. This is a 'deep' copy, so that referenced attribute objects will be copied too.
	 * @param reactionOrig The reaction to copy.
	 * @return A new copy of the reaction.
	 */
	private static ReactionType copyReactionType (ReactionType reactionOrig) {
		// It would be nice if we could use clone or some other recursive method
		// but unfortunately we don't have control over these classes.
		// Instead we marshal and unmarshal the XML
		// This is apparently approx. 50 times slower than using clone
		// http://stackoverflow.com/questions/930840/how-do-i-clone-a-jaxb-object

		// Create a new reaction to copy the data into.
		ReactionType reactionNew;
		// Marshal the original reaction object into XML.
		StringWriter xml = new StringWriter();
		JAXB.marshal(reactionOrig, xml);
		// Unmarshal the generated XML back into our new class.
		StringReader reader = new StringReader(xml.toString());
		reactionNew = JAXB.unmarshal(reader, reactionOrig.getClass());
		
		// Which gives us a copy to pass back. It's not nice, but it works.
		return reactionNew;
	}
	
	/**
	 * Returns the edited copy of the reaction.
	 * @return The copied version of the reaction being edited, including any changes made.
	 */
	public ReactionType getReaction () {
		return reaction;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		// Sets up the dialogue box.
		Control contents = super.createContents(parent);
		
		// Set the title of the dialogue box.
		setTitle("ConSpec Rule Reaction");
		
		// Set some helpful text about the dialogue for the benefit of the user.
		setMessage("A ConSpec reaction specifies how to update the variables in the security state when a rule is triggered.");

		// Give the dialogue box an attractive Aniketos header.
	    ImageDescriptor image = Activator.getImageDescriptor("icons/header.png");
		setTitleImage(image.createImage());

		return contents;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#getInitialSize()
	 */
	protected Point getInitialSize() {
		// Set the initial size of the dialogue box.
		return new Point(412, 400);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#setShellStyle(int)
	 */
	protected void setShellStyle(int newShellStyle) {
		// Set the style for the dialogue box.
		// Allow resizing and maximising.
		super.setShellStyle(newShellStyle | SWT.RESIZE | SWT.MAX);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		// Add the controls into the dialogue box.
		// Set up the SWT layout for rendering the form.
		Composite container = (Composite)super.createDialogArea(parent);
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 2;

		// Label for the text box for entering the guard expression.
		Label labelGuard = new Label(composite, SWT.LEFT);
		labelGuard.setText("Guard expression:");
		labelGuard.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		// Create a guard text box to allow the user to edit the guard expression.
		guard = new Text(composite, SWT.BORDER);
		// Convert the structured guard expression object hierarchy into a string.
		Expression expression = new Expression (reaction.getGuard().getExpType());

		// Set up the guard text box with the expression string.
		guard.setText(expression.toString());
		guard.setLayoutData(new GridData (SWT.FILL, SWT.TOP, true, false, 1, 1));

		// Add a listener to keep track of whether the user has edited the guard expression.
		guard.addListener(SWT.Modify, new Listener () {
			@Override
			public void handleEvent(Event arg0) {
				// The guard expression changed.
				// Convert the expression string back into an object hierarchy.
				Expression expression = new Expression(guard.getText());
				// Store the new guard expression in the ConSpec file.
				reaction.getGuard().setExpType(expression.getValue());
			}
		});
		
		// Label for the update table.
		Label labelUpdate = new Label(composite, SWT.RIGHT);
		labelUpdate.setText("Update:");
		labelUpdate.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, true, false, 2, 1));
		
		// Create a table for listing and editing the update values.
		update = new Table(composite, SWT.SINGLE | SWT.BORDER);
		update.setLayoutData(new GridData (SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		// The table has two columns: parameter and expression.
		TableColumn columnParam = new TableColumn(update, SWT.LEFT);
		TableColumn columnValue = new TableColumn(update, SWT.LEFT);

		columnParam.setText("Parameter");
		columnValue.setText("Expression");
		columnParam.setWidth(120);
		columnValue.setWidth(240);
		update.setHeaderVisible(true);
		
		// Create a listener for when the user tries to open a context menu over the table. 
		update.addListener(SWT.MenuDetect, new Listener () {
			@Override
			public void handleEvent(Event event) {
				// Ensure the menu only opens if the user clicks in a valid cell, rather than in the header for example.
				contextSelection.headerClear (update, event.x, event.y);
			}
		});

		// Set the values for the table from the update values in the ConSpec file.
		AssignType assign;
		Iterator<AssignType> assignIter = reaction.getUpdate().getAssign().iterator();
		while (assignIter.hasNext()) {
			assign = assignIter.next();
			String info[] = new String[2];
			Expression value = new Expression(assign.getValue().getExpType());
			TableItem item = new TableItem(update, SWT.NONE);

			info[0] = assign.getIdentifier();
			info[1] = value.toString();
			item.setText(info);
		}
		new TableItem(update, SWT.NONE);

		// Create an editor so that the values in the table can be edited in-table.
		final TableEditor editor = new TableEditor(update);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;
		
		// Create a listener for when the user edits one of the update table cells.
		// Listen for when the user clicks in one of the table cells.
		update.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				// Find out which cell was selected by the user.
				contextSelection = new TableSelected (update, event.x, event.y);
				
				if (event.button == 1) {
					// Left mouse button.
					// Check whether the user actually clicked in a valid cell in the table.
					if (contextSelection.getFound()) {
						// Set up a listener to save the result back to the (internal) ConSpec structure. 
						Text text = DeclarationTextListener.setupWidget(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), update, editor);
						DeclarationTextListener textListener = new DeclarationTextListener(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), text, update) {
							public void valueChanged (String value, int row, int column) {
								// The callback function for when the value changed in a cell.
								// If the user edited a row beyond the actual number of update rows that exist, we need to create
								// new updates to ensure the user actually has something to edit.
								while (reaction.getUpdate().getAssign().size() <= row) {
									// Create a new empty update row.
									AssignType reactElse = new AssignType();
									reactElse.setIdentifier("");
									reactElse.setValue(new Value());
									reactElse.getValue().setExpType(new Expression("0").getValue());
									reaction.getUpdate().getAssign().add(reactElse);
								}

								// Check which column the user edited.
								switch (column) {
								case 0:
									// Set the new parameter/identifier value.
									reaction.getUpdate().getAssign().get(row).setIdentifier(value);
									break;
								case 1:
									// Set the new expression for updating the parameter.
									// Convert the expression string entered by the user into an expression object hierarchy.
									Expression expression = new Expression(value);
									// Update the reaction update with the new expression.
									reaction.getUpdate().getAssign().get(row).getValue().setExpType(expression.getValue());
									break;
								}
							}
						};
						// Apply the listener
						DeclarationTextListener.setupListener(text, textListener);
					}
				}
			}
		});

		// Hook up the actions and context menus for the dialogue box.
		makeActions ();
		hookContextMenu ();
		
		// Return the contents of the dialogue box.
		return composite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// Add buttons for OK and Cancel to the dialogue box.
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Callback for adding appropriate entries to the context menu for deleting update entries.
	 * @param manager The menu to add the menu entries to.
	 */
	private void fillContextMenuUpdate(IMenuManager manager) {
		// Check whether the context menu relates to a valid update cell.
		if (contextSelection.getFound()) {
			// It does, so add a delete option.
			manager.add(deleteUpdate);
		}
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/**
	 * Set up the context menu for the dialogue box.
	 */
	private void hookContextMenu() {
		// The menu to be added.
		Menu menu;

		// Param context menu.
		MenuManager menuMgrDeclaration = new MenuManager("#PopupMenu");
		menuMgrDeclaration.setRemoveAllWhenShown(true);
		// Set up a listener so we can add the relevant entries to the menu.
		menuMgrDeclaration.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ReactionEdit.this.fillContextMenuUpdate(manager);
			}
		});
		// Attach the menu to the appropriate places for Eclipse to handle it.
		menu = menuMgrDeclaration.createContextMenu(updateViewer.getControl());
		updateViewer.getControl().setMenu(menu);
		site.registerContextMenu(menuMgrDeclaration, updateViewer);
	}

	/**
	 * Set up callbacks for the various actions the user may perform
	 */
	private void makeActions() {
		updateViewer = new TableViewer(update);
		
		// Action of deleting an update using the context menu
		deleteUpdate = new Action() {
			public void run() {
				// Check whether the update actually exists
				if (contextSelection.getFound() && (contextSelection.getRow() < reaction.getUpdate().getAssign().size())) {
					// The update exists, so we remove it
					update.remove(contextSelection.getRow());
					contextSelection.getItem().dispose();
					reaction.getUpdate().getAssign().remove(contextSelection.getRow());
				}
			}
		};
		deleteUpdate.setText("Delete Parameter");
		deleteUpdate.setToolTipText("Delete parameter tooltip");
		deleteUpdate.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
	}
}
