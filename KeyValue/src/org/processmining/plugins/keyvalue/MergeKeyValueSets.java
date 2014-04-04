package org.processmining.plugins.keyvalue;

import java.util.Map;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.providedobjects.ProvidedObjectDeletedException;
import org.processmining.framework.providedobjects.ProvidedObjectID;

/**
 * @author michael
 * 
 */
@Plugin(name = "Merge Key/Value Sets", parameterLabels = { "Key/Value Set" }, returnLabels = { "Key/Value Set" }, returnTypes = { KeyValue.class }, userAccessible = true)
public class MergeKeyValueSets {

	/**
	 * @param context
	 * @param set
	 * @param traceID
	 * @param mapping
	 * @return
	 */
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "M. Westergaard", email = "m.westergaard@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Default settings", requiredParameterLabels = { 0 })
	public static KeyValue merge(final PluginContext context, final KeyValue[] sets) {
		context.log("Merging " + sets.length + " sets.");
		context.getProgress().setIndeterminate(false);
		context.getProgress().setMaximum(sets.length);

		StringBuilder name = new StringBuilder("Merge of ");
		boolean first = true;

		final KeyValue result = new KeyValueTrove();
		int i = 0;
		for (final KeyValue set : sets) {
			context.getProgress().setValue(i++);
			
			for (ProvidedObjectID poid : context.getProvidedObjectManager().getProvidedObjects()) {
				try {
					if (context.getProvidedObjectManager().getProvidedObjectObject(poid, false) == set) {
						if (!first)
						name.append(", ");
						first = false;
						name.append(context.getProvidedObjectManager().getProvidedObjectLabel(poid));
						break;
					}
				} catch (ProvidedObjectDeletedException e) {
					// Ignore, it's just to get a name
				}
			}
			
			for (final Map<String, Object> item : set) {
				result.add(item);
			}
		}
		
		context.getFutureResult(0).setLabel(name.toString());

		return result;
	}

}
