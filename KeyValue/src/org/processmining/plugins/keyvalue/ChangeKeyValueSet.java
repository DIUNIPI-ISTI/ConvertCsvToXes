package org.processmining.plugins.keyvalue;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.providedobjects.ProvidedObjectDeletedException;
import org.processmining.framework.providedobjects.ProvidedObjectID;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTable;

/**
 * @author michael
 * 
 */
@Plugin(name = "Transform Key/Value Set", parameterLabels = { "Key/Value Set", "Transformation" }, returnLabels = { "Key/Value Set" }, returnTypes = { KeyValue.class }, userAccessible = true)
public class ChangeKeyValueSet {
	/**
	 * @author michael
	 * 
	 */
	public static class AddN extends Operation {
		int n = 0;
		/**
    	 * 
    	 */
		public static AddN INSTANCE = new AddN(0);

		private AddN(final int value) {
			n = value;
		}

		/**
		 * @see org.processmining.plugins.keyvalue.ChangeKeyValueSet.Operation#configure()
		 */
		@Override
		public AddN configure() {
			String string = null;
			boolean done = true;
			do {
				done = true;
				while (string == null) {
					string = JOptionPane.showInputDialog(null, "How much should be added?)");
				}
				try {
					Integer.parseInt(string);
				} catch (final Exception _) {
					done = false;
				}
			} while (!done);
			return new AddN(Integer.parseInt(string));

		}

		@Override
		public String toString() {
			if (n == 0)
				return "Add a constant to each value";
			return "Add " + n + " to each value";
		}

		@Override
		Integer apply(final Object value) {
			if (value == null)
				return null;
			Integer parameter = null;
			if (value instanceof Integer) {
				parameter = (Integer) value;
			}
			if (value instanceof Number) {
				parameter = ((Number) value).intValue();
			}
			if (value instanceof String) {
				try {
					parameter = Integer.valueOf(value.toString());
				} catch (final Exception _) {
					//Ignore
				}
			}
			if (parameter == null)
				return null;
			return parameter + n;
		}
	}

	/**
	 * @author michael
	 * 
	 */
	public static class None extends Operation {
		public static None INSTANCE = new None();

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "<ignore>";
		}

		@Override
		Object apply(final Object value) {
			return value;
		}

		@Override
		Operation configure() {
			return this;
		}
	}

	/**
	 * @author michael
	 * 
	 */
	public static abstract class Operation {
		abstract Object apply(Object value);

		abstract Operation configure();
	}

	private static class OperationsEditor extends AbstractCellEditor implements TableCellEditor {
		/**
		 * 
		 */
		private static final long serialVersionUID = 0;
		private final ProMComboBox comboBox;
		private Operation configured = null;

		@SuppressWarnings("unused")
		public OperationsEditor() {
			comboBox = new ProMComboBox(Arrays.asList(ChangeKeyValueSet.operations));
			comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent arg0) {
					final Operation o = (Operation) comboBox.getSelectedItem();
					configured = o.configure();
				}
			});
		}

		@Override
		public Object getCellEditorValue() {
			return configured;
		}

		@Override
		public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
				final int row, final int column) {
			comboBox.setSelectedItem(value);
			configured = None.INSTANCE;
			return comboBox;
		}

	}

	private static class OperationTable extends ProMTable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public OperationTable(final TableModel model, final TableColumnModel columnModel) {
			super(model, columnModel);
		}

		@SuppressWarnings("unused")
		public boolean isCellEditable(final int rowIndex, final int colIndex) {
			if (colIndex == 0)
				return false;
			return true;
		}
	}

	private static final Operation[] operations = new Operation[] { None.INSTANCE, AddN.INSTANCE };

	/**
	 * @param context
	 * @param set
	 * @param traceID
	 * @param mapping
	 * @return
	 */
	@PluginVariant(variantLabel = "Default settings", requiredParameterLabels = { 0, 1 })
	public static KeyValue transform(final PluginContext context, final KeyValue set,
			final Map<String, Operation> transformation) {
		context.log("Transforming");
		context.log("Found " + set.size() + " data item" + (set.size() != 1 ? "s" : ""));
		context.getProgress().setIndeterminate(false);
		context.getProgress().setMaximum(set.size());
		
		StringBuilder name = new StringBuilder("Transformed { ");
		for (Entry<String, Operation> entry : transformation.entrySet()) {
			if (entry.getValue() != None.INSTANCE) {
				name.append(entry.getKey());
				name.append(": ");
				name.append(entry.getValue());
			}
		}
		name.append(" }");
		for (ProvidedObjectID poid : context.getProvidedObjectManager().getProvidedObjects()) {
			try {
				if (context.getProvidedObjectManager().getProvidedObjectObject(poid, false) == set) {
					name.append(" of ");
					name.append(context.getProvidedObjectManager().getProvidedObjectLabel(poid));
					break;
				}
			} catch (ProvidedObjectDeletedException e) {
				// Ignore, it's just to get a name
			}
		}
		context.getFutureResult(0).setLabel(name.toString());

		final KeyValue result = new KeyValueTrove();
		int i = 0;

		for (final Map<String, Object> item : set) {
			context.getProgress().setValue(i++);
			final Map<String, Object> newItem = new HashMap<String, Object>();
			for (final Entry<String, Object> entry : item.entrySet()) {
				if (transformation.containsKey(entry.getKey())) {
					newItem.put(entry.getKey(), transformation.get(entry.getKey()).apply(entry.getValue()));
				} else {
					newItem.put(entry.getKey(), entry.getValue());
				}
			}
			result.add(newItem);
		}

		return result;
	}

	/**
	 * @param context
	 * @param set
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "M. Westergaard", email = "m.westergaard@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Default settings", requiredParameterLabels = { 0 })
	public static KeyValue transform(final UIPluginContext context, final KeyValue set) {
		context.getProgress().setIndeterminate(true);
		context.log("Collecting keys");
		final Set<String> values = Helper.gatherKeys(set);

		final ProMPropertiesPanel properties = new ProMPropertiesPanel(null);
		final DefaultTableModel model = Helper.buildSampleTable(set, values);

		final DefaultTableModel contentsModel = new DefaultTableModel();
		contentsModel.addColumn("Key");
		contentsModel.addColumn("Operation");
		for (final String value : values) {
			contentsModel.addRow(new Object[] { value, None.INSTANCE });
		}
		final DefaultTableColumnModel contentsColumns = new DefaultTableColumnModel();
		final TableColumn attributeColumn = new TableColumn(0, 100, new DefaultTableCellRenderer(), null);
		attributeColumn.setHeaderValue("Key");
		contentsColumns.addColumn(attributeColumn);
		final TableColumn keyColumn = new TableColumn(1, 100, new DefaultTableCellRenderer(), new OperationsEditor());
		keyColumn.setHeaderValue("Operation");
		contentsColumns.addColumn(keyColumn);
		properties.addProperty("Mapping", new OperationTable(contentsModel, contentsColumns));

		final ProMTable table = new ProMTable(model);
		table.setRowSorter(new TableRowSorter<TableModel>(model));
		properties.add(table, 0);

		final InteractionResult result = context.showConfiguration("Setup Transformation", properties);
		if (result == InteractionResult.CONTINUE) {
			final Map<String, Operation> mapping = new HashMap<String, Operation>();
			for (final Object o : contentsModel.getDataVector()) {
				assert o instanceof Vector;
				final Vector<?> v = (Vector<?>) o;
				assert v.size() >= 2;
				assert v.get(1) instanceof Operation;
				if (v.get(0) instanceof String) {
					mapping.put((String) v.get(0), (Operation) v.get(1));
				}
			}
			return ChangeKeyValueSet.transform(context, set, mapping);
		}
		context.getFutureResult(0).cancel(true);
		return null;
	}
}
