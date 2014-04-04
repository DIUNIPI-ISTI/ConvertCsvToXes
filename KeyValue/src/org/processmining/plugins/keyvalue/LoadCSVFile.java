package org.processmining.plugins.keyvalue;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTable;
import org.processmining.framework.util.ui.widgets.ProMTextArea;
import org.processmining.framework.util.ui.widgets.ProMTextField;

/**
 * @author michael
 * 
 */
@Plugin(name = "Load CSV File", parameterLabels = { "Filename" }, returnLabels = { "Key/Value Set" }, returnTypes = { KeyValue.class })
@UIImportPlugin(description = "CSV File", extensions = { "csv", "txt" })
public class LoadCSVFile extends AbstractImportPlugin {

	static List<String> sanitize(final String string, final String seperator) {
		//		System.out.println(string);
		final String[] split = string.split(seperator);
		//		System.out.println(Arrays.toString(split));
		final List<String> result = new ArrayList<String>();
		int i = 0;
		while (i < split.length) {
			String prefix = split[i];
			if (prefix.length() > 0) {
				if (prefix.charAt(0) == '"') {
					prefix = prefix.substring(1);
					while (prefix.length() == 0 || prefix.charAt(prefix.length() - 1) != '"' || prefix.length() >= 2
							&& prefix.charAt(prefix.length() - 2) == '\\') {
						prefix = prefix + seperator + split[++i];
					}
					prefix = prefix.substring(0, prefix.length() - 1);
				}
				prefix = prefix.replaceAll("\\\\(.)", "$1");
			}
			i++;
			result.add(prefix);
		}
		//		System.out.println(result);
		return result;
	}

	private Map<String, Object> convert(final List<String> headers, final List<String> instance) {
		final Map<String, Object> item = new HashMap<String, Object>();
		int i = 0;
		for (final String value : readData(headers.size(), instance)) {
			if (value != null && !"".equals(value)) {
				item.put(headers.get(i), value);
			}
			i++;
		}
		return item;
	}

	private Iterable<List<String>> load(final InputStream input, final String seperator) throws IOException {
		return new Iterable<List<String>>() {
			@Override
			public Iterator<List<String>> iterator() {
				return new Iterator<List<String>>() {
					BufferedReader reader = new BufferedReader(new InputStreamReader(input));
					String next = readline();

					@Override
					public boolean hasNext() {
						return next != null;
					}

					@Override
					public List<String> next() {
						final String old = next;
						next = readline();
						return LoadCSVFile.sanitize(old, seperator);
					}

					@Override
					public void remove() {
						// Do not
					}

					private String readline() {
						try {
							return reader.readLine();
						} catch (final IOException e) {
							return null;
						}
					}
				};
			}
		};
	}

	private String[] readData(final int columnCount, final List<String> instance) {
		boolean bandiera = columnCount >= instance.size();
		if(!bandiera){
		System.out.println(instance);
		}
		assert columnCount >= instance.size();
		final String[] result = new String[columnCount];
		int i = 0;
		for (final String s : instance) {
			result[i++] = s;
		}
		return result;
	}

	private void updateTableModel(final DefaultTableModel tableModel, final boolean headerRow, final String seperator,
			final String data) {
		try {
			boolean first = true;
			for (final List<String> instance : load(new ByteArrayInputStream(data.getBytes()), seperator)) {
				if (first && headerRow) {
					for (final String header : instance) {
						tableModel.addColumn(header);
					}
					first = false;
				} else {
					tableModel.addRow(readData(tableModel.getColumnCount(), instance));
				}
			}
		} catch (final IOException e) {
		}
	}

	@Override
	protected KeyValue importFromStream(final PluginContext context, final InputStream input, final String filename,
			final long fileSizeInBytes) throws Exception {
		String modelName = null;
		if (filename != null) {
			modelName = filename.replaceAll("[.]csv$", "").replaceAll("[.]txt$", "").replaceAll(".*[/\\\\]", "");
		}
		if (modelName != null) {
			context.log("Parsing File (" + modelName + ")");
		} else {
			context.log("Parsing File");
		}
		final boolean headerRow = true;
		String seperator = ";";
		if (false && context instanceof UIPluginContext) {
			final ProMPropertiesPanel propertiesPanel = new ProMPropertiesPanel(null);
			final JCheckBox checkBox = propertiesPanel.addCheckBox("Header row", headerRow);
			final ProMTextField textField = propertiesPanel.addTextField("Seperator", seperator);
			final StringBuilder data = new StringBuilder();
			final BufferedReader dis = new BufferedReader(new InputStreamReader(input));
			try {
				for (int i = 0; i < 100; i++) {
					data.append(dis.readLine());
					data.append('\n');
				}
			} catch (final IOException _) {
				// Stop reading then
			}
			final String string = data.toString();
			propertiesPanel.addProperty("Raw data", new ProMTextArea(false)).setText(string);

			final DefaultTableModel tableModel = new DefaultTableModel();
			propertiesPanel.addProperty("Preview", new ProMTable(tableModel));
			updateTableModel(tableModel, checkBox.isSelected(), textField.getText().trim(), string);

			checkBox.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent arg0) {
					updateTableModel(tableModel, checkBox.isSelected(), textField.getText().trim(), string);
				}
			});
			textField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(final FocusEvent e) {
					// Do nothing here
				}

				@Override
				public void focusLost(final FocusEvent e) {
					updateTableModel(tableModel, checkBox.isSelected(), textField.getText().trim(), string);
				}
			});

			if (((UIPluginContext) context).showConfiguration("CSV Loading Options", propertiesPanel) != InteractionResult.CANCEL) {
				seperator = textField.getText().trim();
			}
		}

		final KeyValue result = new KeyValueTrove();
		List<String> headers = null;
		boolean first = true;
		for (final List<String> instance : load(input, seperator)) {
			if (first) {
				first = false;
				if (headerRow) {
					headers = instance;
				} else {
					headers = new ArrayList<String>();
					int i = 1;
					for (final String v : instance) {
						headers.add("Column " + i++);
					}
					result.add(convert(headers, instance));
				}
			} else {
				result.add(convert(headers, instance));
			}
		}

		if (modelName != null) {
			context.getFutureResult(0).setLabel("Key/Value Set (" + modelName + ")");
		}
		return result;
	}
}
