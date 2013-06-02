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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;

import eu.aniketos.AssignType;
import eu.aniketos.DeclType;
import eu.aniketos.DeclType.Value;
import eu.aniketos.ParameterType;
import eu.aniketos.PerformType;
import eu.aniketos.ReactionType;
import eu.aniketos.RuleType;
import eu.aniketos.Specification;
import eu.aniketos.StateType;
import eu.aniketos.UpdateType;
import eu.aniketos.WhenType;
import eu.aniketos.wp2.Declaration;
import eu.aniketos.wp2.Expression;
import eu.aniketos.wp2.When;

/**
 * Aniketos ConSpec Editor class. This creates pages for an Eclipse plug-in that will
 * allow the editing of ConSpec XML files.
 * 
 * The editor has four pages.
 * <ul>
 * <li/>page 0: Specification editor.
 * <li/>page 1: Declarations editor.
 * <li/>page 2: Rules editor.
 * <li/>page 3: Visual representation of the state space (not yet implemented).
 * </ul>
 * @author Aniketos Project; David Llewellyn-Jones, Liverpool John Moores University
 *
 */
public class ConSpecEditor extends MultiPageEditorPart implements IResourceChangeListener{

	/**
	 * The details of the ConSpec file being edited.
	 */
	private IFile file;
	/**
	 * The JAXB context used for marshalling and unmarshalling the ConSpec XML file.
	 */
	private JAXBContext conspecContext;
	/**
	 * The unmarshaller used for unmarshalling the ConSpec XML file.
	 */
	private Unmarshaller conspecUnmarshal;
	/**
	 * The spec represents the root of the unmarshalled ConSpec file. It is the start of the ConSpec policy as stored in memory.
	 */
	private Specification spec;

	private CCombo scope;
	private Spinner maxInt;
	private Spinner maxString;
	/**
	 * The dirty attributed represents whether the file has actually changed, and so needs to be re-saved.
	 * This should be accessed using the setDirty() and setClean() methods.
	 */
	private boolean dirty;
	/**
	 * Table for storing the list of ConSpec declarations.
	 */
	private Table declarations;
	private TableViewer declarationsViewer;
	/**
	 * Table for storing the list of ConSpec rules.
	 */
	private Table rules;
	private TableViewer rulesViewer;
	private Action deleteDeclaration;
	private Action deleteRule;
	private Boolean contextMenu;
	private TableSelected contextSelection;
	
	/** 
	 * Details of the text widget for previewing the ConSpec file.
	 */
	private StyledText text;
	private int previewPage;

	/**
	 * ConSpecEditor constructor. Sets up the initial attribute states.
	 */
	public ConSpecEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		dirty = false;
		contextSelection = new TableSelected(null, 0, 0);
	}

	/**
	 * Check the currently loaded ConSpec object hierarchy fulfils the minimal requirements and 
	 * contains the minimal set of classes that will allow the user to edit it.
	 */
	private void ensureMinimumConSpecData() {
		if (spec == null) {
			// If there's no specification at all, create a new Specification object to edit.
			spec = new Specification();
		}
		if (spec.getMaxint() == null) {
			// If there's no maximum integer value set, create one and set it to a default value
			spec.setMaxint(new BigInteger("255"));
		}
		if (spec.getMaxlen() == null) {
			// If there's no maximum string value set, create one and set it to a default value
			spec.setMaxlen(new BigInteger("255"));
		}
		if (spec.getScope() == null) {
			// If there's no scope value set, create one and set it to a default value
			spec.setScope("session");
		}
		if (spec.getSecuritystate() == null) {
			// If there's no security state object, create a new one
			StateType state = new StateType();
			spec.setSecuritystate(state);
		}
	}
	

	/**
	 * Creates ConSpec specification editor page.
	 */
	void createPageSpecification() {
		// Set up the SWT layout for rendering the form.
		Composite composite = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 2;

		// Ensure the minimal requirements for a file are fulfilled.
		ensureMinimumConSpecData();

		// Provide some help text to the user.
		Label LabelInfo = new Label(composite, SWT.WRAP);
		LabelInfo.setText("Set global details that apply throughout the policy.");
		LabelInfo.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, true, false, 2, 1));

		// Create a scope widget for the user to change the scope of the ConSpec file.
		Label labelScope = new Label(composite, SWT.LEFT);
		labelScope.setText("Scope:");
		labelScope.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));
		scope = new CCombo(composite, SWT.DROP_DOWN | SWT.BORDER);
		scope.add("session");
		scope.add("multiSession");
		scope.add("global");
		scope.setText(spec.getScope());
		scope.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));

		// If the scope widget is changed, the details need to be updated.
		scope.addListener(SWT.Modify, new Listener () {
			@Override
			public void handleEvent(Event arg0) {
				// The scope changed
				spec.setScope(scope.getText());
				setDirty();
			}
		});
		
		// Create a maxInt widget to allow the user to alter the maximum size of an integer in the ConSpec file.
		Label labelMaxInt = new Label(composite, SWT.LEFT);
		labelMaxInt.setText("Maximum integer size:");
		labelMaxInt.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));
		maxInt = new Spinner(composite, SWT.BORDER);
		maxInt.setValues(spec.getMaxint().intValue(), 0, Integer.MAX_VALUE, 0, 8, 256);
		maxInt.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));

		// If the maxInt widget is changed, the details need to be updated
		maxInt.addListener(SWT.Modify, new Listener () {
			@Override
			public void handleEvent(Event arg0) {
				// The scope changed
				spec.setMaxint(BigInteger.valueOf(maxInt.getSelection()));
				setDirty();
			}
		});
		
		// Create a MaxString widget to allow the user to alter the maximum length of a string in the ConSpec file.
		Label labelMaxString = new Label(composite, SWT.LEFT);
		labelMaxString.setText("Maximum string size:");
		labelMaxString.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));
		maxString = new Spinner(composite, SWT.BORDER);
		maxString.setValues(spec.getMaxlen().intValue(), 0, Integer.MAX_VALUE, 0, 1, 8);
		maxString.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, false, false, 1, 1));

		// If the maxString widget is changed, the details need to be updated
		maxString.addListener(SWT.Modify, new Listener () {
			@Override
			public void handleEvent(Event arg0) {
				// The scope changed
				spec.setMaxlen(BigInteger.valueOf(maxString.getSelection()));
				setDirty();
			}
		});
		
		// Add the page to the interface.
		int index = addPage(composite);
		setPageText(index, "Specification");
	}
	
	/**
	 * Creates ConSpec declarations editor page.
	 */
	void createPageDeclarations() {
		// Set up the SWT layout for rendering the form.
		Composite composite = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 2;

		// Provide some help text to the user.
		Label LabelInfo = new Label(composite, SWT.WRAP);
		LabelInfo.setText("Define the security state. These 'global' variables are initialised with the given values and persist throughout the policy scope.");
		LabelInfo.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, true, false, 2, 1));

		// Provide some help text to the user.
		Label labelDeclarations = new Label(composite, SWT.RIGHT);
		labelDeclarations.setText("Declarations:");
		labelDeclarations.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, true, false, 2, 1));

		// Create a table for listing the declarations.
		declarations = new Table(composite, SWT.SINGLE | SWT.BORDER);
		declarations.setLayoutData(new GridData (SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		// The table has three columns: type, name and expression.
		TableColumn columnType = new TableColumn(declarations, SWT.LEFT);
		TableColumn columnName = new TableColumn(declarations, SWT.LEFT);
		TableColumn columnValue = new TableColumn(declarations, SWT.LEFT);

		columnType.setText("Type");
		columnName.setText("Name");
		columnValue.setText("Expression");
		columnType.setWidth(100);
		columnName.setWidth(100);
		columnValue.setWidth(300);
		declarations.setHeaderVisible(true);
		
		// Set up a listener in case the user tries to create a context menu over one of the header items.
		// We need to catch this to avoid creating the usual menu in this case.
		declarations.addListener(SWT.MenuDetect, new Listener () {
			@Override
			public void handleEvent(Event event) {
				contextSelection.headerClear (declarations, event.x, event.y);
			}
		});

		// Find the actual declaration list that's contained in the currently loaded ConSpec file.
		List<DeclType> decList = spec.getSecuritystate().getDeclaration();

		// Iterate through the declarations and store appropriate values from them in the table.
		Iterator<DeclType> decIter = decList.iterator();
		while (decIter.hasNext()) {
			Declaration decl = new Declaration(decIter.next());
			TableItem declaration = new TableItem(declarations, SWT.NONE);
			String [] info = new String[3];
			info[0] = decl.getType();
			info[1] = decl.getIdentifier();
			info[2] = (decl.getExpression().toString());
			
			declaration.setText(info);
		}
		new TableItem(declarations, SWT.NONE);

		// The declarations can be edited in-place, so we need to create a TableEditor to allow this.
		final TableEditor editor = new TableEditor(declarations);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;
		
		// The editing happens in callbacks/listeners for the table.
		// We're interested in the user clicking on one of the table cells.
		declarations.addListener(SWT.MouseDown, new Listener() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			 */
			public void handleEvent(Event event) {
				// Method called when the user clicks on one of the table cells.
				// Find out which cell was selected.
				contextSelection = new TableSelected (declarations, event.x, event.y);

				// Check which mouse button was pressed.
				if (event.button == 1) {
					// Left mouse button.
					if (contextSelection.getFound()) {
						// The user clicked on a cell (rather than on a header, or something else).
						if (contextSelection.getColumn() == 0) {
							// Set up a listener to save the result back to the (internal) ConSpec structure. 
							Combo combo = DeclarationComboListener.setupWidget(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), declarations, editor);
							// Create an object to handle changes to the drop-down box
							DeclarationComboListener comboListener = new DeclarationComboListener(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), combo, declarations) {
								/* (non-Javadoc)
								 * @see conspecedit.editors.DeclarationComboListener#valueChanged(java.lang.String, int, int)
								 */
								public void valueChanged (String value, int row, int column) {
									// In case the user clicked in an empty row (beyond the list of declarations that actually exist)
									// add new (empty) declarations until we have enough 
									while (spec.getSecuritystate().getDeclaration().size() <= row) {
										spec.getSecuritystate().getDeclaration().add(emptyDeclaration());
									}
									// Set the value of the declaration type as chosen by the user
									spec.getSecuritystate().getDeclaration().get(row).setType(value);
									setDirty();
								}
							};
							DeclarationComboListener.setupListener(combo, comboListener);
						}
						else {
							// Set up a listener to save the result back to the (internal) ConSpec structure 
							Text text = DeclarationTextListener.setupWidget(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), declarations, editor);
							// Create an object to handle changes to the text box
							DeclarationTextListener textListener = new DeclarationTextListener(contextSelection.getRow(), contextSelection.getItem(), contextSelection.getColumn(), text, declarations) {
								public void valueChanged (String value, int row, int column) {
									// In case the user clicked in an empty row (beyond the list of declarations that actually exist)
									// add new (empty) declarations until we have enough 
									while (spec.getSecuritystate().getDeclaration().size() <= row) {
										spec.getSecuritystate().getDeclaration().add(emptyDeclaration());
									}
									// Set the value of the declaration as chosen by the user, depending on which column is being edited
									switch (column) {
									case 1:
										// Update the identifier of the declaration
										spec.getSecuritystate().getDeclaration().get(row).setIdentifier(value);
										break;
									case 2:
										// Update the expression of the declaration
										Expression expression = new Expression(value);
										spec.getSecuritystate().getDeclaration().get(row).getValue().setExpType(expression.getValue());
										break;
									}
									setDirty();
								}
							};
							DeclarationTextListener.setupListener(text, textListener);
						}
					}
				}
			}
		});
		
		// Add the page to the interface.
		int index = addPage(composite);
		setPageText(index, "Declarations");
	}
	
	/**
	 * Creates ConSpec rules editor page.
	 */
	void createPageRules() {
		// Set up the SWT layout for rendering the form.
		Composite composite = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 2;

		// Provide some help text to the user.
		Label LabelInfo = new Label(composite, SWT.WRAP);
		LabelInfo.setText("Define the rules that will trigger the security state to be updated. Triggers are tied to methods executed within the service the policy applies to.");
		LabelInfo.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, true, false, 2, 1));

		// Provide some help text to the user.
		Label labelDeclarations = new Label(composite, SWT.RIGHT);
		labelDeclarations.setText("Rules:");
		labelDeclarations.setLayoutData(new GridData (SWT.LEFT, SWT.TOP, true, false, 2, 1));
		
		// Create a table for listing the rules.
		rules = new Table(composite, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
		rules.setLayoutData(new GridData (SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		// The table has three columns: when, name and parameters.
		TableColumn columnWhen = new TableColumn(rules, SWT.LEFT);
		TableColumn columnName = new TableColumn(rules, SWT.LEFT);
		TableColumn columnParams = new TableColumn(rules, SWT.LEFT);

		columnWhen.setText("When");
		columnName.setText("Name");
		columnParams.setText("Parameters");
		columnWhen.setWidth(100);
		columnName.setWidth(200);
		columnParams.setWidth(200);
		rules.setHeaderVisible(true);

		// Set up a listener in case the user tries to create a context menu over one of the header items.
		// We need to catch this to avoid creating the usual menu in this case.
		rules.addListener(SWT.MenuDetect, new Listener () {
			@Override
			public void handleEvent(Event event) {
				contextSelection.headerClear (rules, event.x, event.y);
			}
		});

		// Get the list of rules from the ConSpec file being edited.
		List<RuleType> ruleList = spec.getRule();
		
		// Iterate through the rules and add their details to the table.
		Iterator<RuleType> ruleIter = ruleList.iterator();
		while (ruleIter.hasNext()) {
			RuleType rule = ruleIter.next();
			// We need a unique rule id
			When when = new When(rule);

			TableItem item = new TableItem(rules, SWT.NONE);
			String [] info = new String[3];
			info[0] = when.getType();
			info[1] = when.getIdentifier();
			info[2] = "";

			// Each rule can have multiple parameters, and we summarise them in the table cell.
			ParameterType param;
			
			// Iterate through each parameter and concatenate the identifier to form the string to be displayed.
			Iterator<ParameterType> paramIter = when.getParameters().iterator();
			while (paramIter.hasNext()) {
				param = paramIter.next();
				info[2] += param.getType() + " " + param.getIdentifier();
				if (paramIter.hasNext()) {
					info[2] += ", ";
				}
			}
			
			// Add the resulting concatenated string to the cell.
			item.setText(info);
		}
		new TableItem(rules, SWT.NONE);

		// Create a listener to handle the context menu for a cell.
		// The (right-click) context menu allows rules to be deleted 
		rules.addListener(SWT.MouseDown, new Listener() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			 */
			@Override
			public void handleEvent(Event event) {
				// Work out which cell was clicked on
				contextSelection = new TableSelected (rules, event.x, event.y);

				// If the right mouse button was clicked, we want to open up a context menu.
				// The actual menu is opened in response to the widgetSelected SelectionListener callback.
				if (event.button == 3) {
					// Right mouse button.
					contextMenu = true;
				}
				else {
					// Some other mouse button.
					contextMenu = false;
				}
			}
		});
		
		// Clicking on a row in the table will open up a new dialogue box to edit the values.
		// Clicking with the right mouse button will open a context menu
		rules.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// Check whether or not we should open the context menu, or the rule editor dialogue box.
				if (contextMenu == false) {
					// Open the rule editor dialogue box.
					// Find out which rule is selected.
					int index = rules.getSelectionIndex();
					List<RuleType> ruleList = spec.getRule();
					RuleType rule;
					if (ruleList.size() <= index) {
						// The user clicked beyond the rule list, so we need to create a new empty rule for editing.
						rule = new RuleType();
						PerformType perform = new PerformType();
						UpdateType update = new UpdateType();
						perform.setElse(update);
						rule.setPerform(perform);
						WhenType when = new WhenType();
						when.setIdentifier("");
						QName name = new QName("before");
						JAXBElement<WhenType> whenAdd = new JAXBElement<WhenType>(name, WhenType.class, when); 
						rule.setBeforeOrAfterOrExceptional(whenAdd);
					}
					else {
						// The user clicked inside the rule lost, so we need to get the details.
						rule = ruleList.get(index);
					}

					// Create the dialogue box to edit the rule.
					RuleEdit ruleShell = new RuleEdit(getSite(), index, rule);
					
					// Display the dialogue box and capture the returned result.
					// A result of 0 means the user clicked 'OK'.
					int result = ruleShell.open();
					if (result == 0) {
						// The user clicked 'OK', so we need to update the value of the rule.
						// In case there aren't enough rules, create empty rules until we have enough.
						while (spec.getRule().size() <= index) {
							// Create a new empty rule.
							rule = new RuleType();
							PerformType perform = new PerformType();
							UpdateType update = new UpdateType();
							perform.setElse(update);
							rule.setPerform(perform);
							WhenType when = new WhenType();
							when.setIdentifier("");
							QName name = new QName("before");
							JAXBElement<WhenType> whenAdd = new JAXBElement<WhenType>(name, WhenType.class, when); 
							rule.setBeforeOrAfterOrExceptional(whenAdd);
							// Add the rule to the ConSpec file
							spec.getRule().add(rule);
						}

						// Set the details of the new rule.
						// The previous rule will eventually get cleaned up by the garbage collector.
						spec.getRule().set(index, ruleShell.getRule());
						
						// Update the details in the table to reflect the new values for the rule.
						String [] info = new String[3];
						info[0] = ruleShell.getWhen().getType();
						info[1] = ruleShell.getWhen().getIdentifier();
						info[2] = "";
						
						// Create string to summarise the parameters for the rule, so that it can be displayed in the table. 
						ParameterType param;
						Iterator<ParameterType> paramIter = ruleShell.getWhen().getParameters().iterator();
						while (paramIter.hasNext()) {
							param = paramIter.next();
							info[2] += param.getType() + " " + param.getIdentifier();
							if (paramIter.hasNext()) {
								info[2] += ", ";
							}
						}
	
						rules.getItem(index).setText(info);
	
						// If the user just edited the last rule in the table, add another blank line in the table,
						// so that there's a new blank rule for the user to edit in the future.
						if (rules.getItemCount() <= (index + 1)) {
							new TableItem(rules, SWT.NONE);
						}
						
						// The file changed, so set it to being dirty.
						setDirty();
					}
				}
			}
			
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// Do nothing
			}
		});
		
		// Add the page to the interface.
		composite.pack();
		int index = addPage(composite);
		setPageText(index, "Rules");
	}


	/**
	 * Creates page showing the ConSpec data in text format.
	 */
	void createPageConSpecPreview() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		FillLayout layout = new FillLayout();
		composite.setLayout(layout);
		text = new StyledText(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		text.setEditable(false);

		previewPage = addPage(composite);
		setPageText(previewPage, "Preview");
	}

	private Graph graph;
	//private int layout = 1;
	/**
	 * Creates ConSpec state diagram page.
	 */
	void createPageStateDiagram() {
		// Set up the SWT layout for rendering the form.
		Composite composite = new Composite(getContainer(), SWT.NONE);

		composite.setLayout(new FillLayout());
		
		// Currently there's nothing on this page.
		// TODO: Implement the state diagram visualisation.
		graph = new Graph(composite, SWT.NONE);

		
		GraphNode node1 = new GraphNode(graph, SWT.NONE, "Initial state");
		GraphNode node2 = new GraphNode(graph, SWT.NONE, "State 1");
		GraphNode node3 = new GraphNode(graph, SWT.NONE, "State 2");
		GraphNode node4 = new GraphNode(graph, SWT.NONE, "State 3");

		new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, node1, node2);
		new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, node1, node3);
		GraphConnection graphConnection = new GraphConnection(graph, SWT.NONE, node1, node4);
	
		graphConnection.changeLineColor(composite.getDisplay().getSystemColor(SWT.COLOR_GREEN));
		graphConnection.setText("Condition");
		graphConnection.setHighlightColor(composite.getDisplay().getSystemColor(SWT.COLOR_RED));
		
		graph.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
		graph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println(e);
			}
		});

		
		
		
		// Add the page to the interface.
		int index = addPage(composite);
		setPageText(index, "State Diagram");
	}
	
	/**
	 * Create a new empty declaration.
	 * Usually this would be created as a constructor for the DeclType class, but this class
	 * was autogenerated by JAXB, so it's preferable not to make changes to it.
	 * @return the new empty declaration.
	 */
	public DeclType emptyDeclaration () {
		DeclType decl = new DeclType();
		decl.setIdentifier("");
		decl.setType("int");
		decl.setValue(new Value());
		decl.getValue().setExpType(new Expression("0").getValue());
		
		return decl;
	}
	
	/**
	 * Creates the pages of the multi-page ConSpec editor.
	 */
	protected void createPages() {
		// Page for editing the ConSpec specification.
		createPageSpecification();
		// Page for editing the ConSpec declarations (the automaton security state).
		createPageDeclarations();
		// Page for editing the ConSpec rules (the automaton update rules).
		createPageRules();
		// Page for displaying a text preview of the ConSpec file
		createPageConSpecPreview();
		// Page for displaying the states of the FSA.
		createPageStateDiagram();

		// Set things up so that the user can interact with the pages
		makeActions();
		hookContextMenu();
	}

	/**
	 * Add the 'Delete' option to the context menu for deleting declarations.
	 * @param manager The IMenuManager for adding the new menu entry.
	 */
	private void fillContextMenuDeclaration(IMenuManager manager) {
		// Only add the menu item if the user actually clicked on a relevant cell in the declarations table.
		if (contextSelection.getFound()) {
			manager.add(deleteDeclaration);
		}
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/**
	 * Add the 'Delete' option to the context menu for deleting rules.
	 * @param manager The IMenuManager for adding the new menu entry.
	 */
	private void fillContextMenuRule(IMenuManager manager) {
		// Only add the menu item if the user actually clicked on a relevant cell in the rules table.
		if (contextSelection.getFound()) {
			manager.add(deleteRule);
		}
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/**
	 * Hook the context menu into the Eclipse processes.
	 */
	private void hookContextMenu() {
		// The menu to be added.
		Menu menu;

		// Declaration context menu.
		MenuManager menuMgrDeclaration = new MenuManager("#PopupMenu");
		menuMgrDeclaration.setRemoveAllWhenShown(true);
		// Set up a listener so we can add the relevant entries to the menu.
		menuMgrDeclaration.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ConSpecEditor.this.fillContextMenuDeclaration(manager);
			}
		});
		// Attach the menu to the appropriate places for Eclipse to handle it.
		menu = menuMgrDeclaration.createContextMenu(declarationsViewer.getControl());
		declarationsViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgrDeclaration, declarationsViewer);
		
		// Rule context menu
		MenuManager menuMgrRule = new MenuManager("#PopupMenu");
		menuMgrRule.setRemoveAllWhenShown(true);
		// Set up a listener so we can add the relevant entries to the menu.
		menuMgrRule.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ConSpecEditor.this.fillContextMenuRule(manager);
			}
		});
		// Attach the menu to the appropriate places for Eclipse to handle it.
		menu = menuMgrRule.createContextMenu(rulesViewer.getControl());
		rulesViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgrRule, rulesViewer);
	}

	/**
	 * Set up callbacks for the various actions the user may perform
	 */
	private void makeActions() {
		declarationsViewer = new TableViewer(declarations);
		rulesViewer = new TableViewer(rules);

		// Action of deleting a declaration using the context menu
		deleteDeclaration = new Action() {
			public void run() {
				// Check whether the declaration actually exists
				if (contextSelection.getFound() && (contextSelection.getRow() < spec.getSecuritystate().getDeclaration().size())) {
					// The declaration exists, so we remove it
					declarations.remove(contextSelection.getRow());
					contextSelection.getItem().dispose();
					spec.getSecuritystate().getDeclaration().remove(contextSelection.getRow());
					setDirty();
				}
			}
		};
		deleteDeclaration.setText("Delete Declaration");
		deleteDeclaration.setToolTipText("Delete the selected ConSpec declaration.");
		deleteDeclaration.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		
		// Action of deleting a rule using the context menu
		deleteRule = new Action() {
			public void run() {
				// Check whether the rule actually exists
				if (contextSelection.getFound() && (contextSelection.getRow() < spec.getRule().size())) {
					// The rule exists, so we remove it
					rules.remove(contextSelection.getRow());
					contextSelection.getItem().dispose();
					spec.getRule().remove(contextSelection.getRow());
					setDirty();
				}
			}
		};
		deleteRule.setText("Delete Rule");
		deleteRule.setToolTipText("Delete the selected ConSpec rule.");
		deleteRule.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
	}
	
//	private void showMessage(String message) {
//		MessageDialog.openInformation(getContainer().getShell().getShell(), "ConSpec Editor", message);
//	}
	
	/**
	 * The <code>MultiPageEditorPart</code> implementation of this 
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}
	
	/**
	 * Sets the file to being dirty. This should be called whenever a change is made to the file, 
	 * so that Eclipse knows the file has changed and needs to be saved.
	 */
	private void setDirty() {
		if (!dirty) {
			dirty = true;
			// Tell Eclipse that it needs to note the change in status.
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}
	
	/**
	 * Sets the file to being clean. This should be called whenever the file is saved, 
	 * so that Eclipse knows the file hasn't changed and doesn't need to be saved.
	 */
	private void setClean() {
		if (dirty) {
			dirty = false;
			// Tell Eclipse that it needs to note the change in status.
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#isDirty()
	 */
	public boolean isDirty() {
		// Return whether or not the file is dirty (i.e. has been changed by the user and therefore needs to be saved).
		return dirty;
	}
	
	/**
	 * Saves the multi-page editor's document.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		// Marshal the file for output
		// If any changes are made to the ConSpec file this will turn it back in to XML.
		Marshaller conspecMarhaller;
		try {
			// Set up the JAXB marshaller.
			conspecMarhaller = conspecContext.createMarshaller();
			conspecMarhaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			// Output to a bytearray in memory to suit the way Eclipse handles files.
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			// Actually marshal the output.
			conspecMarhaller.marshal(spec, output);
			InputStream getOutput = new ByteArrayInputStream(output.toByteArray());
			// Set the new contents of the file.
			file.setContents(getOutput, IFile.KEEP_HISTORY, monitor);
			// The file has been saved, so we set it to being clean.
			setClean();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			monitor.isCanceled();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			monitor.isCanceled();
		}
	}
	/**
	 * Saves the multi-page editor's document as another file.
	 * Also updates the text for page 0's tab, and updates this multi-page editor's input
	 * to correspond to the nested editor's.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
//		IEditorPart editor = getEditor(0);
//		editor.doSaveAs();
//		setPageText(0, editor.getTitle());
//		setInput(editor.getEditorInput());

		// Open a Save As dialogue box.
		SaveAsDialog save = new SaveAsDialog(getContainer().getShell());
		save.setOriginalFile(file);
		// Open the dialogue box and capture the result.
		// A result of 0 means the user selected 'OK'.
		int saveResult = save.open();
		
		if (saveResult == 0) {
			// The user selected 'OK', so save out the file.
			IPath path = save.getResult();
			// Figure out where the file should be saved. This is within the Eclipse workspace, rather than to disc.
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot root = workspace.getRoot();
			file = root.getFile(path);
			try {
				// Check whether the file already exists. If it doesn't we need to create it in the workspace.
				if (file.exists() == false) {
					file.create(null, true, null);
				}

				// We save it out using an progress monitor, so it can run in the background if necessary.
				// In fact, it should save so fast that this isn't necessary, but we leave it in since it's good practice,
				// and the time taken to save out might grow in the future (you never know).
				Job job = new Job("Saving ConSpec File") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Saving ConSpec File ...", 100);
						doSave(monitor);
						monitor.done();
						return Status.OK_STATUS;
					}
				};
				job.schedule();
				setPartName(file.getName());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Go to a marker in the file. We don't really support this yet. 
	 * @param marker The marker to jump to.
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(0);
		IDE.gotoMarker(getEditor(0), marker);
	}
	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method
	 * checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput editorInput)
		throws PartInitException {
		if (editorInput instanceof IFileEditorInput) {
			// Set up the page titles, etc.
			setSite(site);
			setInput(editorInput);
			
			// Read in the file
			file = ((IFileEditorInput) editorInput).getFile();
			setPartName(file.getName());

			// Unmarshal the ConSpec XML using JAXB
			try {
				conspecContext = JAXBContext.newInstance("eu.aniketos");
				conspecUnmarshal = conspecContext.createUnmarshaller();
				spec = (Specification) conspecUnmarshal.unmarshal(file.getContents());
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Ensure the minimal requirements for a file are fulfilled.
			ensureMinimumConSpecData();
		}
		else {
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		}
		super.init(site, editorInput);
	}

	/* (non-Javadoc)
	 * Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		// We always allow the file to be saved out with a new filename.
		return true;
	}

	/**
	 * Calculates the contents of page 2 when the it is activated.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#pageChange(int)
	 */
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (newPageIndex == previewPage) {
			generatePreview();
		}
	}


	/**
	 * Generates the text-based preview of the ConSpec file 
	 */
	void generatePreview() {
		String preview = "";
		
		// Generate the ConSpec header information.
		preview += "SCOPE " + spec.getScope() + ";\n";
		preview += "maxint = " + spec.getMaxint() + ";\n"; 
		preview += "maxlen = " + spec.getMaxlen() + ";\n";
		preview += "\n";
		
		// Generate the security state.
		preview += "SECURITY STATE" + "\n";
		Iterator<DeclType> declIter = spec.getSecuritystate().getDeclaration().iterator();
		while (declIter.hasNext()) {
			DeclType decl = declIter.next();
			
			Expression expression = new Expression(decl.getValue().getExpType());
			preview += "\t" + decl.getType() + " " + decl.getIdentifier() + " = " + expression.toString() + ";\n";
		}
 
		preview += "\n";
		
		// Generate the security rules
		Iterator<RuleType> ruleIter = spec.getRule().iterator();
		while (ruleIter.hasNext()) {
			RuleType rule = ruleIter.next();
			When when = new When (rule);
			
			preview += when.getType().toUpperCase() + " ";
			if (when.getType() == "after") {
				preview += when.getReturn().getType() + " " + when.getReturn().getIdentifier() + " = ";
			}
			
			preview += when.getIdentifier() + "(";
			
			Iterator<ParameterType> paramIter = when.getParameters().iterator();
			while (paramIter.hasNext()) {
				ParameterType param = paramIter.next();
				preview += param.getType() + " " + param.getIdentifier();
				if (paramIter.hasNext()) {
					preview += ", ";
				}
			}
			preview += ") PERFORM\n";
			
			Iterator<ReactionType> reactIter = rule.getPerform().getReaction().iterator();
			while (reactIter.hasNext()) {
				ReactionType react = reactIter.next();
				Expression guard = new Expression(react.getGuard().getExpType());
				preview += "\t" + guard + " -> {";
				
				Iterator<AssignType> assignIter = react.getUpdate().getAssign().iterator();
				if (react.getUpdate().getAssign().size() <= 1) {
					while (assignIter.hasNext()) {
						AssignType assign = assignIter.next();
						Expression value = new Expression(assign.getValue().getExpType());
						preview += " " + assign.getIdentifier() + " = " + value + ";";
					}
					preview += " }\n";
				}
				else {
					preview += "\n";
					while (assignIter.hasNext()) {
						AssignType assign = assignIter.next();
						Expression value = new Expression(assign.getValue().getExpType());
						preview += "\t\t" + assign.getIdentifier() + " = " + value + ";\n";
					}
					preview += "\t}\n";
				}
			}
			
			if ((rule.getPerform().getElse().getAssign() != null) && (rule.getPerform().getElse().getAssign().size() > 0)) {
				preview += "ELSE\n";
				Iterator<AssignType> ruleElseIter = rule.getPerform().getElse().getAssign().iterator();
				while (ruleElseIter.hasNext()) {
					AssignType ruleElse = ruleElseIter.next();
					Expression value = new Expression(ruleElse.getValue().getExpType());
					preview += "\t" + ruleElse.getIdentifier() + " = " + value + ";\n";
				}
			}
			
			preview += "\n";
		}
		
		Font font = new Font(text.getDisplay(), "Consolas", 10, java.awt.Font.PLAIN);
		text.setFont(font);

		text.setText(preview);
	}
	
	/**
	 * Closes all project files on project close.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(final IResourceChangeEvent event){
		// TODO: Check whether the resource has changed outside of the editor.
		if(event.getType() == IResourceChangeEvent.PRE_CLOSE){
			Display.getDefault().asyncExec(new Runnable(){
				public void run(){
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i<pages.length; i++){
//						if(((FileEditorInput)editor.getEditorInput()).getFile().getProject().equals(event.getResource())){
//							IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
//							pages[i].closeEditor(editorPart,true);
//						}
					}
				}            
			});
		}
	}
}
