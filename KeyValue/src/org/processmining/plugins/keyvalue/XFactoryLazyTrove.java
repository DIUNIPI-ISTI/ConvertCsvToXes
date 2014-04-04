package org.processmining.plugins.keyvalue;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.impl.XAttributeMapLazyImpl;

/**
 * @author michael
 * 
 */
public class XFactoryLazyTrove extends XFactoryTrove {
	/**
	 * @see org.deckfour.xes.factory.XFactoryNaiveImpl#createAttributeMap()
	 */
	@Override
	public XAttributeMap createAttributeMap() {
		return new XAttributeMapLazyImpl<XAttributeTroveMap>(XAttributeTroveMap.class);
	}

}
