package org.processmining.plugins.keyvalue;

import gnu.trove.map.hash.THashMap;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;

/**
 * @author michael
 * 
 */
public class XAttributeTroveMap extends THashMap<String, XAttribute> implements XAttributeMap {
	/**
	 * @see java.lang.Object#clone()
	 */

	public XAttributeTroveMap() {
		super(4);
	}

	@Override
	public XAttributeTroveMap clone() {
		final XAttributeTroveMap map = new XAttributeTroveMap();
		map.putAll(this);
		return map;
	}
}
