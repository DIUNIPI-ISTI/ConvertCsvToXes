package org.processmining.plugins.keyvalue;

import java.util.Map;

/**
 * @author michael
 * 
 */
public interface KeyValue extends Iterable<Map<String, Object>> {
	/**
	 * @param item
	 */
	void add(Map<String, Object> item);

	/**
	 * @param i
	 * @return
	 */
	Map<String, Object> get(int i);

	/**
	 * @return
	 */
	int size();
}
