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
import java.util.List;

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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
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
import eu.aniketos.ParameterType;
import eu.aniketos.PerformType;
import eu.aniketos.ReactionType;
import eu.aniketos.ReactionType.Guard;
import eu.aniketos.RuleType;
import eu.aniketos.UpdateType;
import eu.aniketos.wp2.Expression;
import eu.aniketos.wp2.When;

/**
 * Create a dialogue box for editing ConSpec rules.
 * @author Aniketos Project; David Llewellyn-Jones, Liverpool John Moores University
 *
 */
public class RuleEdit extends TitleAreaDialog {
	/**
	 * Store the details of the rule being edited.
	 */
	private RuleType rule;
	private When when;
	/**
	 * Table for displaying the rule parameter list.
	 */
	private Table params;
	/**
	 * Table for displaying rule reactions.
	 */
	private Table reaction;
	/**
	 * Table for displaying the 'Else' updates.
	 */
	private Table reactionElse;
	/**
	 * Table viewers to allow us to attach context menus to the tables.
	 */
	private TableViewer paramsViewer;
	private TableViewer reactionViewer;
	private TableViewer elseViewer;
	private TableSelected contextSelection;
	
	/**
	 * Attributes for managing the user interface elements of the dialogue box.
	 */
	private Text name;
	private Combo comboWhen;
	private Text returnName;
	private Combo returnType;
	private Boolean contextMenu;
	/**
	 * Create delete actions (triggered by context menus).
	 */
	private Action deleteParam;
	private Action deleteReaction;
	private Action deleteElse;
	/**
	 * Manage variables needed by Eclipse for menus, dialogue boxes and so on.
	 */
	private IWorkbenchPartSite site;
	
	/**
	 * Constructor for the rule edit dialogie box.
	 * @param site The site that the dialogue box relates to.
	 * @param index The index (row) of the rule in the rule table of the editor.
	 * @param rule The details of the rule being edited.
	 */
	public RuleEdit(IWorkbenchPartSite site, int index, RuleType rule) {
		// Call the TitleAreaDialog constructor.
		super(site.getShell());
		// Store the information passed in for later use.
		this.site = site;
		// Make a copy of the rule details.
		// We can't edit the original rule, since if the user cancels we want our edits to be ignored.
		this.rule = copyRuleType(rule);

		// Ensure the rule has the required pieces, and if not create them.
		if (this.rule.getPerform() == null) {
			this.rule.setPerform(new PerformType());
		}
		if (this.rule.getPerform().getElse() == null) {
			this.rule.getPerform().setElse(new UpdateType());
		}

		// Set up a rule When helper object for handling the rule.
		when = new When(this.rule);

		contextSelection = new TableSelected(null, 0, 0);
	}
	
	/**
	 * Make a copy of a rule hierarchy. This is a 'deep' copy, so that referenced attribute objects will be copied too.
	 * @param ruleOrig The rule to copy.
	 * @return A new copy of the rule.
	 */
	private static RuleType copyRuleType (RuleType ruleOrig) {
		// It would be nice if we could use clone or some other recursive method
		// but unfortunately we don't have control over these classes.
		// Instead we marshal and unmarshal the XML
		// This is apparently approx. 50 times slower than using clone
		// http://stackoverflow.com/questions/930840/how-do-i-clone-a-jaxb-object

		// Create a new rule to copy the data into.
		RuleType ruleNew;
		// Marshal the original rule object into XML.
		StringWriter xml = new StringWriter();
		JAXB.marshal(ruleOrig, xml);
		// Unmarshal the generated XML back into our new class.
		StringReader reader = new StringReader(xml.toString());
		ruleNew = JAXB.unmarshal(reader, ruleOrig.getClass());
		
		// Which gives us a copy to pass back. It's not nice, but it works.
		return ruleNew;
	}
	
	/**
	 * Returns the edited copy of the rule.
	 * @return The copied version of the rule being edited, including any changes made.
	 */
	public RuleType getRule () {
		return rule;
	}

	/**
	 * Returns the edited copy of the rule When part.
	 * @return The copied version of the rule When part being edited, including any changes made.
	 */
	public When getWhen () {
		return when;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		// Sets up the dialogue box.
		Control contents = super.createContents(parent);
		
		// Set the title of the dialogue box.
		setTitle("Edit ConSpec Rule");
		
		// Set some helpful text about the dialogue for the benefit of the user.
		setMessage("A ConSpec rule defines criteria which, when satisfied, will trigger an update. The rule trigger relates to a method in the service the policy applies to.");
		
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
		return new Point(512, 720);
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
		composite.setSize(512, 1024);

		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 2;

		// Label for the When part of the rule.
		Label labelWhen = new Label(composite, SWT.LEFT);
		labelWhen.setText("When:");
		labelWhen.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		// Combo box for selecting when the rule should be triggered.
		comboWhen = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		comboWhen.add("before");
		comboWhen.add("after");
		comboWhen.add("exceptional");
		comboWhen.setText(when.getType());
		comboWhen.setLayoutData(new GridData (SWT.FILL, SWT.TOP, true, false, 1, 1));

		// Add a listener to keep track of whether the user has edited the When value.
		comboWhen.addListener(SWT.Modify, new Listener () {
			@Override
			public void handleEvent(Event arg0) {
				// The when type changed.
				when.convertType(comboWhen.getText());
				// This requires a bit more work than usual, since different When values require different classes.
				// We therefore call a function to allow a new class to be generated and data copied across if necessary.
				// This is a consequence of the way JAXB generated the classes from the ConSpec XML Schema.
				rule.setBeforeOrAfterOrExceptional(when.getBeforeOrAfterOrException());
				updateReturnActiveState ();
			}
		});

		// Create a label for the identifier field.
		Label labelName = new Label(composite, SWT.LEFT);
		labelName.setText("Identifier:");
		labelName.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		// Create a text box to allow the user to edit the identifier.
		name = new Text(composite, SWT.BORDER);
		name.setText(when.getIdentifier());
		name.setLayoutData(new GridData (SWT.FILL, SWT.TOP, true, false, 1, 1));

		// Add a listener to check whether the user has changed the identifier.
		name.addListener(SWT.Modify, new Listener () {
			@Override
			public void handleEvent(Event arg0) {
				// The identifier changed.
				when.setIdentifier(name.getText());
			}
		});

		// Create a label for the parameters table.
		Label labelParams = new Label(composite, SWT.RIGHT);
		labelParams.setText("Parameters:");
		labelParams.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, true, false, 2, 1));
		
		// Create a table to list the parameters associated with the rulel.
		params = new Table(composite, SWT.SINGLE | SWT.BORDER);
		params.setLayoutData(new GridData (SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		// The table has two columns: type and name.
		TableColumn columnWhen = new TableColumn(params, SWT.LEFT);
		TableColumn columnName = new TableColumn(params, SWT.LEFT);

		columnWhen.setText("Type");
		columnName.setText("Name");
		columnWhen.setWidth(100);
		columnName.setWidth(380);
		params.setHeaderVisible(true);

		// Create a listener for when the user tries to open a context menu over the table.
		params.addListener(SWT.MenuDetect, new Listener () {
			@Override
			public void handleEvent(Event event) {
				// Ensure the menu only opens if the user clicks in a valid cell, rather than in the header for example.
				contextSelection.headerClear (params, event.x, event.y);
			}
		});

		// Set the values for the table from the rule parameter values in the ConSpec file.
		ParameterType param;
		Iterator<ParameterType> paramIter = when.getParameters().iterator();
		while (paramIter.hasNext()) {
			param = paramIter.next();
			TableItem item = new TableItem(params, SWT.NONE);
			String [] info = new String[2];
			
			info[0] = param.getType();
			info[1] = param.getIdentifier();

			item.setText(info);
		}
		new TableItem(params, SWT.NONE);

		// Create an editor so that the values in the table can be edited in-table.
		final TableEditor editor = new TableEditor(params);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;
		
		// Create a listener for when the user edits one of the parameter table cells.
		// Listen for when the user clicks in one of the table cells.
		params.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				// Find out which cell was selected by the user.
				contextSelection = new TableSelected (params, event.x, event.y);

				if (event.button == 1) {
					// Left mouse button.
					// Check whether the user actually clicked in a valid cell in the table.
					if (contextSelection.getFound()) {
						// Different columns require different actions.
						if (contextSelection.getColumn() == 0) {
							// Set up a listener to save the result back to the (internal) ConSpec structure.
							Combo combo = DeclarationComboListener.setupWidget(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), params, editor);
							DeclarationComboListener textListener = new DeclarationComboListener(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), combo, params) {
								public void valueChanged (String value, int row, int column) {
									// The callback function for when the value changed in a cell.
									// If the user edited a row beyond the actual number of parameter rows that exist, we need to create
									// new parameters to ensure the user actually has something to edit.
									while (when.getParameters().size() <= row) {
										when.getParameters().add(emptyParameter());
									}
									when.getParameters().get(row).setType(value);
								}
							};
							// Apply the listener
							DeclarationComboListener.setupListener(combo, textListener);
						}
						else {
							// Set up a listener to save the result back to the (internal) ConSpec structure.
							Text text = DeclarationTextListener.setupWidget(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), params, editor);
							DeclarationTextListener textListener = new DeclarationTextListener(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), text, params) {
								public void valueChanged (String value, int row, int column) {
									// The callback function for when the value changed in a cell.
									// If the user edited a row beyond the actual number of parameter rows that exist, we need to create
									// new parameters to ensure the user actually has something to edit.
									while (when.getParameters().size() <= row) {
										when.getParameters().add(emptyParameter());
									}
									when.getParameters().get(row).setIdentifier(value);
								}
							};
							// Apply the listener
							DeclarationTextListener.setupListener(text, textListener);
						}
					}
				}
			}
		});
		
		// Add a label for the return parameter.
		Label labelReturnType = new Label(composite, SWT.LEFT);
		labelReturnType.setText("Return type:");
		labelReturnType.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		// Create a combo box so the user can choose the appropriate return value.
		returnType = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		returnType.add("void");
		returnType.add("int");
		returnType.add("boolean");
		returnType.add("string");
		returnType.setLayoutData(new GridData (SWT.FILL, SWT.TOP, true, false, 1, 1));
		
		// Create a label for the return identifier.
		Label labelReturnName = new Label(composite, SWT.LEFT);
		labelReturnName.setText("Identifier:");
		labelReturnName.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		// Create a text box so the user can edit the return value identifier.
		returnName = new Text(composite, SWT.BORDER);
		returnName.setLayoutData(new GridData (SWT.FILL, SWT.TOP, true, false, 1, 1));

		// The return value is only valid if the update type is set to 'after'.
		// If the return value isn't valid, we'll be making these fields inactive.
		// If the return value is valid, we need to set it up with the appropriate values.
		boolean returnActive = updateReturnActiveState ();
		if (returnActive) {
			// Set the return fields to the values from the ConSpec file being edited.
			returnType.setText(when.getReturn().getType());
			returnName.setText(when.getReturn().getIdentifier());
		}
		else {
			// The return values aren't used, so set them to something suitably bland.
			returnType.setText("void");
			returnName.setText("");
		}

		// Add a listener to check whether the user has changed the return name.
		returnName.addListener(SWT.Modify, new Listener () {
			@Override
			public void handleEvent(Event arg0) {
				// The identifier changed.
				when.getReturn().setIdentifier(returnName.getText());
			}
		});

		// Add a listener to check whether the user has changed the return type.
		returnType.addListener(SWT.Modify, new Listener () {
			@Override
			public void handleEvent(Event arg0) {
				// The identifier changed.
				when.getReturn().setType(returnType.getText());
			}
		});
		
		// Create a label for the Reaction/Perform table. 
		Label labelReaction = new Label(composite, SWT.RIGHT);
		labelReaction.setText("Reaction:");
		labelReaction.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, true, false, 2, 1));
		
		// Create a table to list the reactions to perform for this rule.
		reaction = new Table(composite, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
		reaction.setLayoutData(new GridData (SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		// The reaction/perform table has two columns: guard and update.
		TableColumn columnGuard = new TableColumn(reaction, SWT.LEFT);
		TableColumn columnUpdate = new TableColumn(reaction, SWT.LEFT);

		columnGuard.setText("Guard");
		columnUpdate.setText("Update");
		columnGuard.setWidth(180);
		columnUpdate.setWidth(300);
		reaction.setHeaderVisible(true);
		
		// Create a listener for when the user tries to open a context menu over the table. 
		reaction.addListener(SWT.MenuDetect, new Listener () {
			@Override
			public void handleEvent(Event event) {
				// Ensure the menu only opens if the user clicks in a valid cell, rather than in the header for example.
				contextSelection.headerClear (reaction, event.x, event.y);
			}
		});

		// Set the values for the table from the reaction/perform values in the ConSpec file.
		PerformType perform = rule.getPerform();
		Iterator<ReactionType> reactIter = perform.getReaction().iterator();
		ReactionType react;
		while (reactIter.hasNext()) {
			react = reactIter.next();
			TableItem item = new TableItem(reaction, SWT.NONE);
			String [] info = new String[2];

			Guard guard = react.getGuard();
			Expression expression = new Expression(guard.getExpType());
			
			info[0] = expression.toString();
			info[1] = "";

			// The reaction may have lots of parameters that get updated.
			// We concatenate the identifiers of all parameters to be updated to be displayed in the table as a hint for the user. 
			AssignType assign;
			Iterator<AssignType> assignIter = react.getUpdate().getAssign().iterator();
			while (assignIter.hasNext()) {
				assign = assignIter.next();
				
				// Concatenate the identifier onto the end of the string.
				info[1] += assign.getIdentifier();
				if (assignIter.hasNext()) {
					info[1] += ", ";
				}
			}
			
			item.setText(info);
		}
		
		new TableItem(reaction, SWT.NONE);

		// Create a listener for when the user clicks on one of the reaction table cells.
		// This is for the purpose of figuring out whether a context menu is needed based on the mouse button the user clicked.
		reaction.addListener(SWT.MouseDown, new Listener () {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			 */
			@Override
			public void handleEvent(Event event) {
				// Find out which cell was selected.
				contextSelection = new TableSelected (reaction, event.x, event.y);

				if (event.button == 3) {
					// Right mouse button, so we want to open the context menu.
					contextMenu = true;
				}
				else {
					// Some other menu button, so no context menu is needed.
					contextMenu = false;
				}
			}
		});
		
		// Listen for when the user clicks in one of the table cells.
		// This will open a new dialogue box (since reactions are too complex to edit in-table),
		// or potentially open a context menu depending on which mouse button was clicked by the user.
		reaction.addSelectionListener(new SelectionListener() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent event) {
				// Check whether we should be opening a dialogue box or a context menu.
				if (contextMenu == false) {
					// Check whether the user actually selected a valid cell for editing.
					if (contextSelection.getFound()) {
						// Find out which row the user selected.
						int index = reaction.getSelectionIndex();
						// Get the list of reactions stored in the ConSpec file.
						List<ReactionType> reactList = rule.getPerform().getReaction();
						ReactionType react;
						// If the use clicked on a reaction that doesn't yet exist, make sure we create one for them to edit.
						if (reactList.size() <= index) {
							// Create a new empty reaction.
							react = new ReactionType();
							UpdateType update = new UpdateType();
							react.setUpdate(update);
							Guard guard = new Guard();
							// Set a suitably bland value for the guard expression for the new reaction.
							guard.setExpType(new Expression("0").getValue());
							react.setGuard(guard);
						}
						else {
							// The reaction already exists, so get its details.
							react = reactList.get(index);					
						}
						
						// Create a dialogue box for editing the reaction.
						ReactionEdit reactionShell = new ReactionEdit(site, index, react);
						
						// Open the dialogue box and store the value returned by it.
						// A return of 0 means the user selected the 'OK' dialogue button.
						int response = reactionShell.open();
						
						if (response == 0) {
							// The user selected 'OK' so we need to update the reaction based on the values the user entered.
							// If there aren't enough reactions already in the list, make sure we create enough of them so that the 
							// reaction the user is editing is valid.
							while (reactList.size() <= index) {
								// Create a new empty reaction.
								react = new ReactionType();
								UpdateType update = new UpdateType();
								react.setUpdate(update);
								Guard guard = new Guard();
								// Set a suitably bland value for the guard expression for the new reaction.
								guard.setExpType(new Expression("0").getValue());
								react.setGuard(guard);
								reactList.add(react);
							}

							// Set the new reaction values based on the data entered by the user in the dialogue box.
							// The previous reaction will eventually get cleaned up by the garbage collector.
							rule.getPerform().getReaction().set(index, reactionShell.getReaction());

							// Set the table entries to reflect the new reaction values entered by the user. 
							String [] info = new String[2];
							Expression expression = new Expression(reactionShell.getReaction().getGuard().getExpType());
							info[0] = expression.toString(); 
							info[1] = "";
		
							// The reaction may have lots of parameters that get updated.
							// We concatenate the identifiers of all parameters to be updated to be displayed in the table as a hint for the user. 
							AssignType assign;
							Iterator<AssignType> assignIter = reactionShell.getReaction().getUpdate().getAssign().iterator();
							while (assignIter.hasNext()) {
								assign = assignIter.next();
								
								// Concatenate the identifier onto the end of the string.
								info[1] += assign.getIdentifier();
								if (assignIter.hasNext()) {
									info[1] += ", ";
								}
							}
	
							// Set the new value
							reaction.getItem(index).setText(info);
							// If this is the end of the table, make sure we add a new blank line in case the user wants to add a new
							// reaction in the future.
							if (reaction.getItemCount() <= (index + 1)) {
								new TableItem(reaction, SWT.NONE);
							}
						}
					}
				}
			}
			
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// We're required to override this method, but we're not actually interested in it.
				// Maybe we should be though?
				// TODO: Find out if we need to be interested in this override.
			}
		});
		
		// Create a label for the 'Else' reactions table
		Label labelReactionElse = new Label(composite, SWT.RIGHT);
		labelReactionElse.setText("Else:");
		labelReactionElse.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, true, false, 2, 1));
		
		// Create a table to display the 'Else' reactions.
		reactionElse = new Table(composite, SWT.SINGLE | SWT.BORDER);
		reactionElse.setLayoutData(new GridData (SWT.FILL, SWT.FILL, true, true, 2, 1));

		// The Else table has two columns: name and expression.
		TableColumn columnParameter = new TableColumn(reactionElse, SWT.LEFT);
		TableColumn columnValue = new TableColumn(reactionElse, SWT.LEFT);

		columnParameter.setText("Name");
		columnValue.setText("Expression");
		columnParameter.setWidth(180);
		columnValue.setWidth(300);
		reactionElse.setHeaderVisible(true);
		
		// Create a listener for when the user tries to open a context menu over the table. 
		reactionElse.addListener(SWT.MenuDetect, new Listener () {
			@Override
			public void handleEvent(Event event) {
				// Ensure the menu only opens if the user clicks in a valid cell, rather than in the header for example.
				contextSelection.headerClear (reactionElse, event.x, event.y);
			}
		});

		// Set the values for the table from the Else values in the ConSpec file.
		AssignType reactElse;
		Iterator<AssignType> reactElseIter = rule.getPerform().getElse().getAssign().iterator();
		while (reactElseIter.hasNext()) {
			reactElse = reactElseIter.next();
			TableItem item = new TableItem(reactionElse, SWT.NONE);
			String [] info = new String[2];
			Expression expression = new Expression(reactElse.getValue().getExpType());
			
			info[0] = reactElse.getIdentifier();
			info[1] = expression.toString();
			
			item.setText(info);
		}
		new TableItem(reactionElse, SWT.NONE);
		
		// Create an editor so that the values in the table can be edited in-table.
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;
		
		// Create a listener for when the user edits one of the Else table cells.
		// Listen for when the user clicks in one of the table cells.
		reactionElse.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				// Find out which cell was selected by the user.
				contextSelection = new TableSelected (reactionElse, event.x, event.y);

				if (event.button == 1) {
					// Left mouse button.
					// Check whether the user actually clicked in a valid cell in the table.
					if (contextSelection.getFound()) {
						// Set up a listener to save the result back to the (internal) ConSpec structure 
						Text text = DeclarationTextListener.setupWidget(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), reactionElse, editor);
						DeclarationTextListener textListener = new DeclarationTextListener(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), text, reactionElse) {
							public void valueChanged (String value, int row, int column) {
								// The callback function for when the value changed in a cell.
								// If the user edited a row beyond the actual number of Else rows that exist, we need to create
								// new Else reactions to ensure the user actually has something to edit.
								while (rule.getPerform().getElse().getAssign().size() <= row) {
									// Create a new empty Else rection row.
									AssignType reactElse = new AssignType();
									reactElse.setIdentifier("");
									reactElse.setValue(new Value());
									reactElse.getValue().setExpType(new Expression("0").getValue());
									rule.getPerform().getElse().getAssign().add(reactElse);
								}
								
								// Check which column the user edited.
								switch (column) {
								case 0:
									// Set the new parameter/identifier value.
									rule.getPerform().getElse().getAssign().get(row).setIdentifier(value);
									break;
								case 1:
									// Set the new expression for updating the parameter.
									// Convert the expression string entered by the user into an expression object hierarchy.
									Expression expression = new Expression(value);
									// Update the Else reaction with the new expression.
									rule.getPerform().getElse().getAssign().get(row).getValue().setExpType(expression.getValue());
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

	/**
	 * Update the return value state, in case the user changed the 'When' value.
	 * If the rule is set to apply 'after', the return value must be active,
	 * otherwise it should be inactive. 
	 * @return Whether the return state is active.
	 */
	boolean updateReturnActiveState () {
		// Get the enabled state based on whether the 'When' value is set to 'after'.
		boolean enabled = when.getType().equals("after");
		// Set the dialogue widgets to be active or inactive based on the result.
		returnType.setEnabled(enabled);
		returnName.setEnabled(enabled);
		
		// Return whether the return state is active.
		return enabled;
	}
	
	/**
	 * Create an empty parameter object.
	 * Usually this would be performed by the ParameterType class constructor.
	 * However, the ParameterType class was autogenerated by JAXB, so we prefer not to make changes to it.
	 * Hence we have this method to do it instead.
	 * @return A new empty ParameterType object.
	 */
	ParameterType emptyParameter () {
		// Create a new instance of ParameterType. 
		ParameterType param = new ParameterType();
		// Set its default values to be suitably bland.
		param.setIdentifier("");
		param.setType("");
		
		// Return the new empty ParameterType object.
		return param;
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
	 * Callback for adding appropriate entries to the context menu for deleting parameter entries.
	 * @param manager The menu to add the menu entries to.
	 */
	private void fillContextMenuParam(IMenuManager manager) {
		// Check whether the context menu relates to a valid parameter cell.
		if (contextSelection.getFound()) {
			// It does, so add a delete option.
			manager.add(deleteParam);
		}
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/**
	 * Callback for adding appropriate entries to the context menu for deleting reaction entries.
	 * @param manager The menu to add the menu entries to.
	 */
	private void fillContextMenuReaction(IMenuManager manager) {
		// Check whether the context menu relates to a valid reaction cell.
		if (contextSelection.getFound()) {
			// It does, so add a delete option.
			manager.add(deleteReaction);
		}
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/**
	 * Callback for adding appropriate entries to the context menu for deleting 'Else' reaction entries.
	 * @param manager The menu to add the menu entries to.
	 */
	private void fillContextMenuElse(IMenuManager manager) {
		// Check whether the context menu relates to a valid 'Else' reaction cell.
		if (contextSelection.getFound()) {
			// It does, so add a delete option.
			manager.add(deleteElse);
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
		// Param context menu
		MenuManager menuMgrDeclaration = new MenuManager("#PopupMenu");
		menuMgrDeclaration.setRemoveAllWhenShown(true);
		// Set up a listener so we can add the relevant entries to the menu.
		menuMgrDeclaration.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				RuleEdit.this.fillContextMenuParam(manager);
			}
		});
		// Attach the menu to the appropriate places for Eclipse to handle it.
		menu = menuMgrDeclaration.createContextMenu(paramsViewer.getControl());
		paramsViewer.getControl().setMenu(menu);
		site.registerContextMenu(menuMgrDeclaration, paramsViewer);
		
		// Reaction context menu
		MenuManager menuMgrRule = new MenuManager("#PopupMenu");
		menuMgrRule.setRemoveAllWhenShown(true);
		// Set up a listener so we can add the relevant entries to the menu.
		menuMgrRule.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				RuleEdit.this.fillContextMenuReaction(manager);
			}
		});
		// Attach the menu to the appropriate places for Eclipse to handle it.
		menu = menuMgrRule.createContextMenu(reactionViewer.getControl());
		reactionViewer.getControl().setMenu(menu);
		site.registerContextMenu(menuMgrRule, reactionViewer);

		// Else context menu
		MenuManager menuMgrElse = new MenuManager("#PopupMenu");
		menuMgrElse.setRemoveAllWhenShown(true);
		// Set up a listener so we can add the relevant entries to the menu.
		menuMgrElse.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				RuleEdit.this.fillContextMenuElse(manager);
			}
		});
		// Attach the menu to the appropriate places for Eclipse to handle it.
		menu = menuMgrElse.createContextMenu(elseViewer.getControl());
		elseViewer.getControl().setMenu(menu);
		site.registerContextMenu(menuMgrElse, elseViewer);
}

	/**
	 * Set up callbacks for the various actions the user may perform
	 */
	private void makeActions() {
		paramsViewer = new TableViewer(params);
		reactionViewer = new TableViewer(reaction);
		elseViewer = new TableViewer(reactionElse);
		
		// Action of deleting a parameter using the context menu
		deleteParam = new Action() {
			public void run() {
				// Check whether the parameter actually exists
				if (contextSelection.getFound() && (contextSelection.getRow() < when.getParameters().size())) {
					// The parameter exists, so we remove it
					params.remove(contextSelection.getRow());
					contextSelection.getItem().dispose();
					when.getParameters().remove(contextSelection.getRow());
				}
			}
		};
		deleteParam.setText("Delete Parameter");
		deleteParam.setToolTipText("Delete parameter tooltip");
		deleteParam.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		
		// Action of deleting a reaction/perform using the context menu
		deleteReaction = new Action() {
			public void run() {
				// Check whether the reaction/perform actually exists
				if (contextSelection.getFound() && (contextSelection.getRow() < rule.getPerform().getReaction().size())) {
					// The reaction/perform exists, so we remove it
					reaction.remove(contextSelection.getRow());
					contextSelection.getItem().dispose();
					rule.getPerform().getReaction().remove(contextSelection.getRow());
				}
			}
		};
		deleteReaction.setText("Delete Reaction");
		deleteReaction.setToolTipText("Delete reaction tooltip");
		deleteReaction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));

		// Action of deleting an 'Else' reaction using the context menu
		deleteElse = new Action() {
			public void run() {
				// Check whether the 'Else' reaction actually exists
				if (contextSelection.getFound() && (contextSelection.getRow() < rule.getPerform().getElse().getAssign().size())) {
					// The 'Else' reaction exists, so we remove it
					reactionElse.remove(contextSelection.getRow());
					contextSelection.getItem().dispose();
					rule.getPerform().getElse().getAssign().remove(contextSelection.getRow());
				}
			}
		};
		deleteElse.setText("Delete Else Reaction");
		deleteElse.setToolTipText("Delete else reaction tooltip");
		deleteElse.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
	}
}
