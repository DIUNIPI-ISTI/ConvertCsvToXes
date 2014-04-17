package org.processmining.plugins.keyvalue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.table.DefaultTableModel;

import org.processmining.plugins.impl.TableModelImpl;

/**
 * @author michael
 * 
 */
public class Helper {
	public static DefaultTableModel buildSampleTable(final KeyValue set, final Set<String> values) {
		final DefaultTableModel model = new TableModelImpl("CSVTable");
		for (final String value : values) {
			final int pos = value.lastIndexOf(':');
			String name = value;
			if (pos >= 0) {
				name = value.substring(pos + 1);
			}
			model.addColumn(name);
		}
		final Set<Integer> used = new HashSet<Integer>();
		for (int j = 0; j < Math.min(100, set.size()); j++) {
			final Object[] row = new Object[values.size()];
			int k;
			do {
				k = (int) (Math.random() * set.size());
			} while (used.contains(k));
			used.add(k);
			final Map<String, Object> item = set.get(k);

			k = 0;
			for (final String value : values) {
				row[k++] = item.get(value);
			}
			model.addRow(row);
		}
		return model;
	}

	public static Set<String> gatherKeys(final KeyValue set) {
		final Set<String> values = new TreeSet<String>();
		for (final Map<String, Object> item : set) {
			values.addAll(item.keySet());
		}
		return values;
	}

}
