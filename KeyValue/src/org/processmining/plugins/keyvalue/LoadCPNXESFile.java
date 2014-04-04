package org.processmining.plugins.keyvalue;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.XExtensionManager;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

/**
 * @author michael
 * 
 */
@Plugin(name = "Load CPN XES File", parameterLabels = { "Filename" }, returnLabels = { "Log" }, returnTypes = { XLog.class })
@UIImportPlugin(description = "CPN XES File", extensions = { "cpnxes" })
public class LoadCPNXESFile extends AbstractImportPlugin {
	@Override
	protected XLog importFromStream(final PluginContext context, final InputStream input, final String filename,
			final long fileSizeInBytes) throws Exception {
		String modelName = null;
		if (filename != null) {
			modelName = filename.replaceAll("[.]cpnxes$", "").replaceAll(".*[/\\\\]", "");
		}
		context.log("Parsing Log");
		final List<XLog> parse = new XesXmlParser().parse(input);
		if (parse != null && parse.size() >= 1) {
			context.log("Converting to Key/Value Set");
			final KeyValue keyValue = LogToKeyValue.logToKeyValue(context, parse.get(0));
			context.log("Building Mapping");
			final Map<XAttribute, List<String>> mapping = new HashMap<XAttribute, List<String>>();
			XExtension extension;
			int i = 0;
			while ((extension = XExtensionManager.instance().getByIndex(i++)) != null) { // Can't find a nicer way to iteratoe over registered extensions :-(
				for (final XAttribute attribute : extension.getEventAttributes()) {
					mapping.put(attribute, Collections.singletonList(attribute.getKey()));
				}
			}
			context.log("Converting Back to Log");
			final Object[] result = KeyValueToLog.keyValueToLog(context, new Mapping(
					Collections.singletonList("cpntools:case"), mapping, Collections.singletonList(LogToKeyValue.TOTAL_SERIAL)), keyValue);
			if (result.length >= 1 && result[0] instanceof XLog) { 
			context.getFutureResult(0).setLabel("Log (" + modelName + ")");
			return (XLog) result[0];
			} else {
				context.getFutureResult(0).cancel(true);
				return null;
			}
		}
		context.getFutureResult(0).cancel(true);
		return null;
	}
}
