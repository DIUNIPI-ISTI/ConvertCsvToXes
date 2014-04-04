package org.processmining.plugins.keyvalue;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

/**
 * @author michael
 * 
 */
@Plugin(name = "Convert Log to Key/Value Set", parameterLabels = { "Log" }, returnLabels = { "Key/Value Set" }, returnTypes = { KeyValue.class }, userAccessible = true)
public class LogToKeyValue {
	static final String ID = "ID";
	static final String TRACE_ID = "Trace ID";
	static final String TRACE_SERIAL = "Trace Serial";
	static final String EVENT_SERIAL = "Event Serial";
	static final String TOTAL_SERIAL = "Global Serial";

	/**
	 * @param context
	 * @param log
	 * @return
	 */
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "M. Westergaard", email = "m.westergaard@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Default settings", requiredParameterLabels = { 0 })
	public static KeyValue logToKeyValue(final PluginContext context, final XLog log) {
		final KeyValue result = new KeyValueTrove();
		int traceCounter = 0;
		int totalCounter = 0;
		for (final XTrace trace : log) {
			int eventCounter = 0;
			for (final XEvent event : trace) {
				final Map<String, Object> value = new HashMap<String, Object>();
				value.put(LogToKeyValue.TRACE_ID, trace.toString());
				value.put(LogToKeyValue.ID, event.getID());
				value.put(TRACE_SERIAL, traceCounter);
				value.put(EVENT_SERIAL, eventCounter);
				value.put(TOTAL_SERIAL, totalCounter);
				for (final Entry<String, XAttribute> attribute : event.getAttributes().entrySet()) {
					if (attribute.getValue() instanceof XAttributeBoolean) {
						value.put(attribute.getKey(), ((XAttributeBoolean) attribute.getValue()).getValue());
					} else if (attribute.getValue() instanceof XAttributeContinuous) {
						value.put(attribute.getKey(), ((XAttributeContinuous) attribute.getValue()).getValue());
					} else if (attribute.getValue() instanceof XAttributeDiscrete) {
						value.put(attribute.getKey(), ((XAttributeDiscrete) attribute.getValue()).getValue());
					} else if (attribute.getValue() instanceof XAttributeLiteral) {
						value.put(attribute.getKey(), ((XAttributeLiteral) attribute.getValue()).getValue());
					} else if (attribute.getValue() instanceof XAttributeTimestamp) {
						value.put(attribute.getKey(), ((XAttributeTimestamp) attribute.getValue()).getValue());
					}
				}
				result.add(value);
				eventCounter++;
				totalCounter++;
			}
			traceCounter++;
		}
		return result;
	}

}
