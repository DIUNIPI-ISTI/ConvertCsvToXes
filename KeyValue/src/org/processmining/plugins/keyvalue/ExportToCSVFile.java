package org.processmining.plugins.keyvalue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "Export KeyValue", parameterLabels = { "KeyValue", "File" }, 
returnLabels = {}, returnTypes = {}, userAccessible = true)
@UIExportPlugin(description = "KeyValue", extension = "csv")

public class ExportToCSVFile {

	@PluginVariant(requiredParameterLabels = { 0, 1 }, variantLabel = "Export KeyValue to CSV File")
	public static void export(PluginContext context, KeyValue table, File file) throws IOException {
				
		FileWriter fw = new FileWriter(file);
		
		if (table.size() == 0) {
			fw.close();
			return;
		}
		
		List<String> headers = new ArrayList<String>(Helper.gatherKeys(table));
		
		boolean isFirstColumn = true;
		
		String separator = ";";
		
		for(String column : headers) {
			if (!isFirstColumn) {
				fw.write(separator);
			}
			fw.write("\"" + column + "\"");
			isFirstColumn = false;
		}
		fw.write("\n");
				
		for(Map<String, Object> row : table) {
			isFirstColumn = true;
			
			for(String column : headers) {
				if (!isFirstColumn) {
					fw.write(separator);
				}
				if (row.get(column) == null) {
					fw.write("\"\"");
				} else {
					fw.write("\"" + row.get(column).toString() + "\"");
				}
				isFirstColumn = false;
			}
			fw.write("\n");
		}
		
		
		
		fw.close();
	}
}
