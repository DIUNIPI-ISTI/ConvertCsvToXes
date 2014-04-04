package org.processmining.plugins.keyvalue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.model.XAttribute;

public class Mapping {
	/**
	 * @return the traceID
	 */
	public List<String> getTraceID() {
		return traceID;
	}
	/**
	 * @return the mapping
	 */
	public Map<XAttribute, List<String>> getMapping() {
		return mapping;
	}
	public Mapping(List<String> traceID, Map<XAttribute, List<String>> mapping, List<String> sorting) {
		super();
		this.traceID = traceID;
		this.mapping = mapping;
		this.sorting = sorting;
	}
	
	public Mapping(List<String> traceID, Map<XAttribute, List<String>> mapping) {
		this (traceID, mapping, Collections.<String>emptyList());
	}
	
	public List<String> getSorting() {
		return sorting;
	}

	final List<String> traceID;
	private final List<String> sorting;
	final Map<XAttribute, List<String>> mapping;
}
