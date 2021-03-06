package de.unipaderborn.visuflow.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.google.common.base.Optional;

import de.unipaderborn.visuflow.model.DataModel;
import de.unipaderborn.visuflow.model.VFNode;
import de.unipaderborn.visuflow.model.VFUnit;
import de.unipaderborn.visuflow.ui.Attribute;
import de.unipaderborn.visuflow.ui.view.filter.ResultViewFilter;
import de.unipaderborn.visuflow.util.ServiceUtil;

/**
 * This class creates a view which shows the results of an analysis.
 * @author Shashank B S
 *
 */
public class ResultView extends ViewPart implements EventHandler {

	/**
	 * table which contains the results of the analysis
	 */
	private TableViewer viewer;
	/**
	 * instance of {@link de.unipaderborn.visuflow.ui.view.filter.ResultViewFilter} that performs filtering of the analysis results
	 */
	private ResultViewFilter filter;
	private List<VFUnit> units;
	private Button highlightNodes;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);
		Label searchLabel = new Label(parent, SWT.NONE);
		searchLabel.setText("Search: ");

		highlightNodes = new Button(parent, SWT.CHECK);
		highlightNodes.setText("Highlight selected nodes on graph");

		highlightNodes.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				highlightNodesOnGraph(highlightNodes.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// noOp
			}
		});

		final Text searchText = new Text(parent, SWT.BORDER | SWT.SEARCH);
		searchText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		this.units = ServiceUtil.getService(DataModel.class).getSelectedMethodUnits();
		createViewer(parent);

		searchText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent ke) {
				filter.setSearchText(searchText.getText());
				viewer.refresh();
			}

		});
		filter = new ResultViewFilter();
		viewer.addFilter(filter);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private void highlightNodesOnGraph(boolean selection) {
		TableItem[] selectedNodes = viewer.getTable().getItems();
		List<VFNode> nodesToFilter = new ArrayList<>();
		for (TableItem tableItem : selectedNodes) {
			if (tableItem.getChecked())
				nodesToFilter.add(new VFNode((VFUnit) tableItem.getData(), 0));
		}
		try {
			ServiceUtil.getService(DataModel.class).filterGraph(nodesToFilter, selection, true, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createViewer(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK | SWT.READ_ONLY | SWT.PUSH);
		createColumns(parent, viewer);
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.getTable().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				if (highlightNodes.getSelection())
					highlightNodesOnGraph(highlightNodes.getSelection());
			}
		});

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setInput(this.units);
		getSite().setSelectionProvider(viewer);

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);

		Hashtable<String, Object> properties = new Hashtable<>();
		String[] topics = new String[] { DataModel.EA_TOPIC_DATA_SELECTION, DataModel.EA_TOPIC_DATA_UNIT_CHANGED, DataModel.EA_TOPIC_DATA_VIEW_REFRESH };
		properties.put(EventConstants.EVENT_TOPIC, topics);
		ServiceUtil.registerService(EventHandler.class, this, properties);

	}

	private void createColumns(final Composite parent, final TableViewer viewer) {
		String[] titles = { "Selection", "Unit", "Unit Type", "In-Set", "Out-Set", "Customized Attr." };
		int[] bounds = { 100, 100, 100, 100, 100, 150 };

		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return "";
			}
		});
		// Unit
		col = createTableViewerColumn(titles[1], bounds[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VFUnit unit = (VFUnit) element;
				return unit.getUnit().toString();
			}
		});
		// Unit Type
		col = createTableViewerColumn(titles[2], bounds[2], 2);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VFUnit unit = (VFUnit) element;
				return unit.getUnit().getClass().getSimpleName();
			}
		});
		// In-Set
		col = createTableViewerColumn(titles[3], bounds[3], 3);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VFUnit unit = (VFUnit) element;
				return Optional.fromNullable(unit.getInSet()).or("n/a").toString();
			}
		});
		// Out-Set
		col = createTableViewerColumn(titles[4], bounds[4], 4);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VFUnit unit = (VFUnit) element;
				return Optional.fromNullable(unit.getOutSet()).or("n/a").toString();
			}
		});

		// Add custom attributes
		col = createTableViewerColumn(titles[5], bounds[5], 5);

		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VFUnit vfUnit = (VFUnit) element;

				if (!(vfUnit.getHmCustAttr().isEmpty())) {
					String attrs = costumizedAttrs(vfUnit);
					return attrs;

				} else {
					return "";
				}
			}
		});

		Menu menu = new Menu(parent);
		viewer.getControl().setMenu(menu);

		MenuItem menuItemCustAttr = new MenuItem(menu, SWT.None);
		menuItemCustAttr.setText("Custom attribute");
		menuItemCustAttr.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				StructuredSelection selection = (StructuredSelection) viewer.getSelection();
				@SuppressWarnings("unchecked")
				List<VFUnit> l = selection.toList();
				ArrayList<VFNode> listAttrNodes = new ArrayList<>();

				// Open the dialog
				Attribute p = new Attribute(e.display.getActiveShell());
				p.open();
				if (p.getAnalysis().length() > 0 && p.getAttribute().length() > 0) {
					for (VFUnit vu : l) {
						vu.setHmCustAttr(setCustAttr(vu, p));
						VFNode vfn = new VFNode(vu, 0);
						listAttrNodes.add(vfn);
					}

					try {
						ServiceUtil.getService(DataModel.class).filterGraph(listAttrNodes, true, true, "customAttribute");
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	@Override
	public void handleEvent(Event event) {
		if (event.getTopic().equals(DataModel.EA_TOPIC_DATA_SELECTION)) {
			if (viewer != null && !viewer.getControl().isDisposed()) {
				viewer.getTable().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						@SuppressWarnings("unchecked")
						List<VFUnit> units = (List<VFUnit>) event.getProperty("selectedMethodUnits");
						viewer.setInput(units);
					}
				});
			}
		} else if (event.getTopic().equals(DataModel.EA_TOPIC_DATA_UNIT_CHANGED)) {
			if (viewer != null && !viewer.getControl().isDisposed()) {
				viewer.getTable().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						viewer.refresh();
					}
				});
			}
		} else if (event.getTopic().equals(DataModel.EA_TOPIC_DATA_VIEW_REFRESH)) {
			if (viewer != null && !viewer.getControl().isDisposed()) {
				viewer.getTable().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						viewer.refresh();
					}
				});
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private String costumizedAttrs(VFUnit vfUnit) {
		String attrs = "";
		Set set = vfUnit.getHmCustAttr().entrySet();

		// Get an iterator
		Iterator i = set.iterator();

		// Display elements
		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			attrs = attrs + me.getKey() + " = " + me.getValue() + "\n";

		}
		if (attrs.equals("")) {
			return "";
		}

		return attrs;
	}

	@SuppressWarnings("rawtypes")
	public static Map<String, String> setCustAttr(VFUnit vfSelected, Attribute p) {
		Map<String, String> hmCustAttr = new HashMap<>();

		// Get actual customized attributes
		Set set = vfSelected.getHmCustAttr().entrySet();
		Iterator i = set.iterator();

		// Display elements
		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			hmCustAttr.put((String) me.getKey(), (String) me.getValue());
		}

		hmCustAttr.put(p.getAnalysis(), p.getAttribute());

		// TODO ask how to get the nod ID from VFUnit
		// colorCostumizedNode((Node)vfSelected);

		return hmCustAttr;
	}

}
