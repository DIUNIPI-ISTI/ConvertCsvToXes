package org.processmining.plugins.keyvalue;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractCellEditor;
import javax.swing.JPanel;
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
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.XExtensionManager;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeBooleanImpl;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.widgets.ProMCheckComboBox;
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTable;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.plugins.utils.HammingDistance;
import org.processmining.plugins.utils.ProvidedObjectHelper;

import com.fluxicon.slickerbox.components.SlickerButton;

/**
 * @author michael
 * 
 */
@Plugin(name = "Convert Key/Value Set to Log", parameterLabels = { "Key/Value Set", "Mapping" }, returnLabels = {
		"Log", "Mapping" }, returnTypes = { XLog.class, Mapping.class }, userAccessible = true)
public class KeyValueToLog {
	private static class KeysEditor extends AbstractCellEditor implements TableCellEditor {
		/**
		 * 
		 */
		private static final long serialVersionUID = 0;
		private final ProMCheckComboBox comboBox;

		public KeysEditor(final Set<String> values) {
			comboBox = new ProMCheckComboBox(values);
		}

		@Override
		public Object getCellEditorValue() {
			final Collection<?> selectedItems = comboBox.getSelectedItems();
			if (selectedItems != null)
				return new ArrayList<Object>(selectedItems);
			return Collections.emptyList();
		}

		@Override
		public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
				final int row, final int column) {
			comboBox.clearSelection();
			assert value instanceof Collection;
			comboBox.addSelectedItems((List<?>) value);
			return comboBox;
		}

		//	    public boolean stopCellEditing() {
		//	    	             // Commit edited value.
		//	    	 comboBox.actionPerformed(new ActionEvent(
		//	    	                      DefaultCellEditor.this, 0, ""));
		//	    	         }
		//	    	         return super.stopCellEditing();
		//	    	         };

	}

	private static class MappingTable extends ProMTable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public MappingTable(final TableModel model, final TableColumnModel columnModel) {
			super(model, columnModel);
		}

		public boolean isCellEditable(final int rowIndex, final int colIndex) {
			if (colIndex == 0)
				return false;
			return true;
		}
	}

	private static final class NamedAttribute implements Comparable<NamedAttribute> {
		private final String name;
		private final XAttribute attribute;

		public NamedAttribute(final String name, final XAttribute attribute) {
			this.name = name;
			this.attribute = attribute;
		}

		@Override
		public int compareTo(final NamedAttribute o) {
			return name.compareTo(o.name);
		}

		public XAttribute getAttribute() {
			return attribute;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	enum AttributeKind {
		LITERAL("Literal (string)", XAttributeLiteral.class), DATE("Date/time", XAttributeTimestamp.class), BOOLEAN(
				"Boolean", XAttributeBoolean.class), DISCRETE("Discrete (integer)", XAttributeDiscrete.class), CONTINUOUS(
				"Continuous (double)", XAttributeContinuous.class);

		private final Class<? extends XAttribute> clazz;
		private final String name;

		private AttributeKind(final String name, final Class<? extends XAttribute> clazz) {
			this.name = name;
			this.clazz = clazz;
		}

		public Class<? extends XAttribute> getClazz() {
			return clazz;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static final Pattern pattern = Pattern.compile("\\s*(\\d+)-(\\d+)-(\\d+)(\\s*(\\d+):(\\d+)(:(\\d+))?)?");

	private static String [] valtime = {"dd-MM-yyyy HH:mm:ss","dd/MM/yyyy HH:mm:ss.S","dd-MM-yyyy HH:mm:ss.S" ,"dd/MM/yyyy HH:mm:ss","dd/MM/yyyy HH:mm",
		"MM/dd/yyyy HH:mm:ss.S","MM/dd/yyyy HH:mm:ss","MM/dd/yyyy HH:mm","yyyy/MM/dd HH:mm:ss.S","dd/MM/yyyy HH:mm:ss:S"};

	private final static ProMComboBox time = new ProMComboBox(new HashSet<String>(Arrays.asList(valtime)));

	
	/**
	 * @param context
	 * @param set
	 * @param traceID
	 * @param mapping
	 * @return
	 */
	@PluginVariant(variantLabel = "Default settings", requiredParameterLabels = { 1, 0 })
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "M. Westergaard", email = "m.westergaard@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN)
	public static Object[] keyValueToLog(final PluginContext context, Mapping mapping, final KeyValue... set) {
		if (set == null || set.length < 1) {
			context.getFutureResult(0).cancel(true);
			return null;
		}
		XLog resultLog = null;
		context.log("Converting " + set.length + " log" + (set.length == 1 ? "" : "s") + "...");
		int n = 0;
		for (KeyValue s : set) {
			n++;
			context.log((set.length > 1 ? ("Log " + n + " / " + set.length + ": ") : "") + "Found " + s.size()
					+ " data item" + (s.size() != 1 ? "s" : ""));
			context.getProgress().setIndeterminate(false);
			context.getProgress().setMaximum(s.size() * 3);

			int i = 0;

			final Map<Object, Set<XEvent>> traces = new TreeMap<Object, Set<XEvent>>(new Comparator<Object>() {
				@Override
				@SuppressWarnings("unchecked")
				public int compare(final Object arg0, final Object arg1) {
					if (arg0 == null)
						return arg1 == null ? 0 : 1;
					if (arg1 == null)
						return -1;
					if (arg0 instanceof Comparable) {
						try {
							return ((Comparable<Object>) arg0).compareTo(arg1);
						} catch (final Exception _) {
						}
					}
					if (arg1 instanceof Comparable) {
						try {
							return -((Comparable<Object>) arg1).compareTo(arg0);
						} catch (final Exception _) {
						}
					}
					if (arg0.equals(arg1))
						return 0;
					final int result = arg0.getClass().toString().compareTo(arg1.getClass().toString());
					if (result == 0)
						return arg0.hashCode() - arg1.hashCode();
					return result;
				}
			});
			final Map<XEvent, Object> sorting = new HashMap<XEvent, Object>();
			for (final Map<String, Object> item : s) {
				Object key = KeyValueToLog.get(mapping.getTraceID(), item);
				if (key == null) {
					key = Boolean.TRUE;
				}
				if (!traces.containsKey(key)) {
					final TreeSet<XEvent> trace = new TreeSet<XEvent>(new Comparator<XEvent>() {
						@Override
						public int compare(final XEvent arg0, final XEvent arg1) {
							if (arg0 == null)
								return arg1 == null ? 0 : -1;
							if (arg1 == null)
								return 1;
							final Date date0 = XTimeExtension.instance().extractTimestamp(arg0);
							final Date date1 = XTimeExtension.instance().extractTimestamp(arg1);
							if (date0 == null)
								return date1 == null ? 1 : -1;
							if (date1 == null)
								return 1;
							int result = date0.compareTo(date1);
							if (result != 0)
								return result;
							result = compareSorting(sorting.get(arg0), sorting.get(arg1));
							if (result != 0)
								return result;
							result = localComp(XConceptExtension.instance().extractName(arg0), XConceptExtension
									.instance().extractName(arg1));
							if (result != 0)
								return result;
							result = localComp(XLifecycleExtension.instance().extractStandardTransition(arg0),
									XLifecycleExtension.instance().extractStandardTransition(arg1));
							if (result != 0)
								return result;
							result = localComp(XLifecycleExtension.instance().extractTransition(arg0),
									XLifecycleExtension.instance().extractTransition(arg1));
							if (result != 0)
								return result;
							result = localComp(XOrganizationalExtension.instance().extractResource(arg0),
									XOrganizationalExtension.instance().extractResource(arg1));
							if (result != 0)
								return result;
							return 0;
							//						return arg1.hashCode() - arg1.hashCode();
						}

						@SuppressWarnings("unchecked")
						private int compareSorting(Object object, Object object2) {
							if (object == null)
								return object2 == null ? 0 : -1;
							if (object2 == null)
								return 1;
							try {
								return ((Comparable<Object>) object).compareTo(object2);
							} catch (Exception _) {
								// Ignore, this happens if it is not comparable or not comparable to the type of object2
							}
							try {
								return -1 * (((Comparable<Object>) object2).compareTo(object));
							} catch (Exception _) {
								// Ignore, same reason as above
							}
							return 0;
						}

						public <T extends Comparable<T>> int localComp(final T a, final T b) {
							if (a == null)
								return b == null ? 0 : -1;
							if (b == null)
								return 1;
							return a.compareTo(b);
						}
					});
					traces.put(key, trace);
				}
				final Set<XEvent> trace = traces.get(key);
				final XEvent event = XFactoryRegistry.instance().currentDefault().createEvent();
				for (final Entry<XAttribute, List<String>> entry : mapping.getMapping().entrySet()) {
					final Object value = KeyValueToLog.get(entry.getValue(), item);
					if (value != null) {
						final XAttribute convertValue = KeyValueToLog.convertValue(entry.getKey(), value);
						if (convertValue != null) {
							event.getAttributes().put(convertValue.getKey(), convertValue);
						}
					}
				}
				sorting.put(event, get(mapping.getSorting(), item));
				trace.add(event);

				context.getProgress().setValue(i += 2);
			}
			sorting.clear();
			context.log("Found " + traces.size() + " trace" + (traces.size() != 1 ? "s" : ""));

			final XLog result = XFactoryRegistry.instance().currentDefault().createLog();
			for (final Entry<Object, Set<XEvent>> trace : traces.entrySet()) {
				final XTrace t = XFactoryRegistry.instance().currentDefault().createTrace();
				t.getAttributes().put(XConceptExtension.ATTR_NAME.getKey(),
						KeyValueToLog.convertValue(XConceptExtension.ATTR_NAME, trace.getKey()));
				t.addAll(trace.getValue());
				result.add(t);
				context.getProgress().setValue(i += t.size());
			}
			if (n == 1) {
				resultLog = result;
			} else {
				ProvidedObjectHelper.publish(context, "Log " + n, result, XLog.class, true);
			}
		}
		return new Object[] { resultLog };
	}

	/**
	 * @param context
	 * @param set
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "M. Westergaard", email = "m.westergaard@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Default settings", requiredParameterLabels = { 0 })
	public static Object[] keyValueToLog(final UIPluginContext context, final KeyValue... set) {
		if (set == null || set.length < 1) {
			context.getFutureResult(0).cancel(true);
			return null;
		}
		context.getProgress().setIndeterminate(true);

		context.log("Collecting XES attributes");
		final Set<NamedAttribute> attributes = new TreeSet<NamedAttribute>();
		int i = 0;
		XExtension extension;
		while ((extension = XExtensionManager.instance().getByIndex(i++)) != null) { // Can't find a nicer way to iteratoe over registered extensions :-(
			for (final XAttribute attribute : extension.getEventAttributes()) {
				attributes.add(new NamedAttribute(attribute.getKey() + " (" + extension.getName() + ")", attribute));
			}
		}

		context.log("Collecting keys");
		final Set<String> values = new HashSet<String>();
		for (KeyValue s : set) {
			values.addAll(Helper.gatherKeys(s));
		}

		final ProMPropertiesPanel properties = new ProMPropertiesPanel(null);
		properties.addProperty("Extra sorting", new ProMCheckComboBox(values));
		final DefaultTableModel model = Helper.buildSampleTable(set[0], values);
		properties.addProperty("Time format",time);
		final DefaultTableModel contentsModel = new DefaultTableModel();
		contentsModel.addColumn("Attribute");
		contentsModel.addColumn("Keys");
		contentsModel.addRow(new Object[] { "Trace identifier", Collections.emptyList() });
		for (final NamedAttribute attribute : attributes) {
			contentsModel.addRow(new Object[] { attribute, Collections.emptyList() });
			//			Collections.singletonList(HammingDistance.getBestMatch(attribute.toString(), values)) });
		}
		final DefaultTableColumnModel contentsColumns = new DefaultTableColumnModel();
		final TableColumn attributeColumn = new TableColumn(0, 100, new DefaultTableCellRenderer(), null);
		attributeColumn.setHeaderValue("Attribute");
		contentsColumns.addColumn(attributeColumn);
		final TableColumn keyColumn = new TableColumn(1, 100, new DefaultTableCellRenderer(), new KeysEditor(values));
		keyColumn.setHeaderValue("Keys");
		contentsColumns.addColumn(keyColumn);
		properties.addProperty("Mapping", new MappingTable(contentsModel, contentsColumns));

		final JPanel jPanel = new JPanel();
		jPanel.setOpaque(false);
		jPanel.setLayout(new BorderLayout());
		properties.add(jPanel);
		final ProMTextField attributeName = new ProMTextField();
		attributeName.setPreferredSize(new Dimension(200, 30));
		attributeName.setMinimumSize(new Dimension(50, 30));
		jPanel.add(attributeName, BorderLayout.CENTER);
		final ProMComboBox comboBox = new ProMComboBox(AttributeKind.values());
		comboBox.setPreferredSize(new Dimension(200, 30));
		comboBox.setMinimumSize(new Dimension(50, 30));
		comboBox.setMaximumSize(new Dimension(200, 30));
		jPanel.add(comboBox, BorderLayout.WEST);
		final SlickerButton button = new SlickerButton("Add Attribute");
		jPanel.add(button, BorderLayout.EAST);

		final ProMTable table = new ProMTable(model);
		table.setPreferredSize(new Dimension(300, 120));
		table.setRowSorter(new TableRowSorter<TableModel>(model));
		properties.add(table, 0);

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				XAttribute attribute = null;
				final String key = attributeName.getText().trim();
				if (!"".equals(key)) {
					switch ((AttributeKind) comboBox.getSelectedItem()) {
						case BOOLEAN :
							attribute = new XAttributeBooleanImpl(key, false);
							break;
						case DATE :
							attribute = new XAttributeTimestampImpl(key, 0);
							break;
						case DISCRETE :
							attribute = new XAttributeDiscreteImpl(key, 0);
							break;
						case CONTINUOUS :
							attribute = new XAttributeContinuousImpl(key, 0);
							break;
						case LITERAL :
							attribute = new XAttributeLiteralImpl(key, "dummy");
							break;
					}
					assert attribute != null;
					contentsModel.addRow(new Object[] { new NamedAttribute(key, attribute),
							Collections.singletonList(HammingDistance.getBestMatch(key, values)) });
				}
			}
		});
		final InteractionResult result = context.showConfiguration("Setup Mapping", properties);
		if (result == InteractionResult.CONTINUE) {
			final Map<XAttribute, List<String>> mapping = new HashMap<XAttribute, List<String>>();
			List<String> trace = null;
			for (final Object o : contentsModel.getDataVector()) {
				assert o instanceof Vector;
				final Vector<?> v = (Vector<?>) o;
				assert v.size() >= 2;
				assert v.get(1) instanceof List;
				if (v.get(0) instanceof NamedAttribute) {
					mapping.put(((NamedAttribute) v.get(0)).getAttribute(), (List<String>) v.get(1));
				} else {
					trace = (List<String>) v.get(1);
				}
			}
			Mapping mappingSetup = new Mapping(trace, mapping);
			return new Object[] { (KeyValueToLog.keyValueToLog(context, mappingSetup, set))[0], mappingSetup };
		}
		context.getFutureResult(0).cancel(true);
		return null;
	}

	private static XAttribute convertValue(final XAttribute key, final Object value) {
		if (value == null)
			return null;
		final XAttribute clone = (XAttribute) key.clone();
		if (key instanceof XAttributeBoolean) {
			if (value instanceof Boolean) {
				((XAttributeBoolean) clone).setValue((Boolean) value);
				return clone;
			}
		}
		if (key instanceof XAttributeTimestamp) {
			if (value instanceof Date) {
				((XAttributeTimestamp) clone).setValue((Date) value);
				return clone;
			}
			if (value instanceof Long) {
				((XAttributeTimestamp) clone).setValueMillis((Long) value);
				return clone;
			}
			if (value instanceof Integer) {
				((XAttributeTimestamp) clone).setValueMillis((Integer) value);
				return clone;
			}
			if (value instanceof BigInteger) {
				((XAttributeTimestamp) clone).setValueMillis(((BigInteger) value).longValue());
				return clone;
			}
			if (value instanceof String) {
				final Date date = KeyValueToLog.datify((String) value);
				if (date != null) {
					((XAttributeTimestamp) clone).setValue(date);
					return clone;
				}
			}
		}
		if (key instanceof XAttributeContinuous) {
			if (value instanceof Double) {
				((XAttributeContinuous) clone).setValue((Double) value);
				return clone;
			}
			if (value instanceof String) {
				try {
					((XAttributeContinuous) clone).setValue(Double.valueOf((String) value));
					return clone;
				} catch (final NumberFormatException _) {
					// Ignore
				}
			}
		}
		if (key instanceof XAttributeDiscrete) {
			if (value instanceof Long) {
				((XAttributeDiscrete) clone).setValue((Long) value);
				return clone;
			}
			if (value instanceof Integer) {
				((XAttributeDiscrete) clone).setValue((Integer) value);
				return clone;
			}
			if (value instanceof String) {
				try {
					((XAttributeDiscrete) clone).setValue(Long.valueOf((String) value));
					return clone;
				} catch (final NumberFormatException _) {
					// Ignore
				}
			}
		}
		if (key instanceof XAttributeLiteral) {
			((XAttributeLiteral) clone).setValue("" + value);
			return clone;
		}
		return null;
	}

	private static Object get(final List<String> keys, final Map<String, Object> item) {
		final List<Object> result = new ArrayList<Object>(keys.size());
		if (keys != null) {
			for (final String key : keys) {
				final Object value = item.get(key);
				if (value != null) {
					result.add(value);
				}
			}
		}
		if (result.isEmpty())
			return null;
		if (result.size() == 1)
			return result.get(0);
		return result;
	}

	/*protected synchronized static Date datify(final String string) {

		final Matcher matcher = KeyValueToLog.pattern.matcher(string);
		try {
			if (matcher.matches()) {
				final int d = Integer.parseInt(matcher.group(1));
				final int m = Integer.parseInt(matcher.group(2));
				int y = Integer.parseInt(matcher.group(3));
				if (y < 50) {
					y += 100;
				}
				if (y < 150) {
					y += 1900;
				}
				int h = 0, min = 0, s = 0;
				try {
					h = Integer.parseInt(matcher.group(5));
					min = Integer.parseInt(matcher.group(6));
					s = Integer.parseInt(matcher.group(8));
				} catch (final Exception _) {
					// Ignore
				}
				final GregorianCalendar gregorianCalendar = new GregorianCalendar(y, m - 1, d, h, min, s);
				return gregorianCalendar.getTime();
			} else {
				// assume xsDataTime
				return XAttributeTimestamp.FORMATTER.parseObject(string);
			}
		} catch (final Exception _) {
			// Mask
		}
		return null;
	}*/
	protected synchronized static Date datify(final String string) {
		//final Matcher matcher = KeyValueToLog.pattern.matcher(string);
		try {
			String format= time.getSelectedItem().toString();
			SimpleDateFormat formatter= new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");;
			if(format!=null){
				formatter = new SimpleDateFormat(format);
			}

			Date date = (Date)formatter.parse(string);
			return date;

		} catch (final Exception _) {
			// Mask
		}
		return null;
	}
}
