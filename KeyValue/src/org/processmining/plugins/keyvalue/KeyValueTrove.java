package org.processmining.plugins.keyvalue;

import gnu.trove.map.hash.THashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * @author michael
 * 
 */
public class KeyValueTrove implements KeyValue {
	private final ArrayList<Map<String, Object>> values;

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public KeyValueTrove() {
		this(Collections.EMPTY_LIST);
	}

	/**
	 * @param initial
	 */
	public KeyValueTrove(final Collection<Map<String, Object>> initial) {
		values = new ArrayList<Map<String, Object>>();
		values.addAll(initial);
	}

	/**
	 * @param value
	 */
	@Override
	public void add(final Map<String, Object> value) {
		values.add(new THashMap<String, Object>(value));
	}

	@Override
	public Map<String, Object> get(final int i) {
		return values.get(i);
	}

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Map<String, Object>> iterator() {
		return values.iterator();
	}

	@Override
	public int size() {
		return values.size();
	}
}
