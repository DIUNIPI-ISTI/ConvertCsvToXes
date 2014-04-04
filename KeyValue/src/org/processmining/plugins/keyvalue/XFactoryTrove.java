package org.processmining.plugins.keyvalue;

import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

/**
 * @author michael
 * 
 */
public class XFactoryTrove extends XFactoryNaiveImpl {
	/**
	 * @see org.deckfour.xes.factory.XFactoryNaiveImpl#createAttributeMap()
	 */
	@Override
	public XAttributeMap createAttributeMap() {
		return new XAttributeTroveMap();
	}

	/**
	 * @see org.deckfour.xes.factory.XFactoryNaiveImpl#createEvent()
	 */
	@Override
	public XEvent createEvent() {
		return super.createEvent(createAttributeMap());
	}

	/**
	 * @see org.deckfour.xes.factory.XFactoryNaiveImpl#createLog()
	 */
	@Override
	public XLog createLog() {
		return super.createLog(createAttributeMap());
	}

	/**
	 * @see org.deckfour.xes.factory.XFactoryNaiveImpl#createTrace()
	 */
	@Override
	public XTrace createTrace() {
		return super.createTrace(createAttributeMap());
	}

	/**
	 * @see org.deckfour.xes.factory.XFactoryNaiveImpl#getAuthor()
	 */
	@Override
	public String getAuthor() {
		return "M. Westergaard";
	}

	/**
	 * @see org.deckfour.xes.factory.XFactoryNaiveImpl#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Memory-based implementation using more efficient attribute maps";
	}
}
