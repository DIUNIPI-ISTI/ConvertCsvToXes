package org.processmining.plugins.keyvalue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

/**
 * @author michael
 * 
 */
@Plugin(name = "Load CPN Tools Log File", parameterLabels = { "Filename" }, returnLabels = { "CPN Tools Log Key/Value Set" }, returnTypes = { KeyValue.class })
@UIImportPlugin(description = "CPN Tools Simulation Log", extensions = { "txt" })
public class LoadCPNToolsLog extends AbstractImportPlugin {
	public static final String PAGE = "Page";
	public static final String INSTANCE = "Instance";
	public static final String TRANSITION = "Transition";
	public static final String TIME = "Time";
	public static final String STEP = "Step";
	public static final String SERIAL = "Serial";

	private static final Pattern transition = Pattern
			.compile("^([\\d]+)[\\s]+([\\d.]+)[\\s]+(.*) @ \\(([\\d]+):(.+) *\\)$");
	private static final Pattern binding = Pattern.compile("^ - ([^ =]+) = (\"?(.+?)\"?)$");

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		System.out.println("/arhge /erga /raeg /hello.cpn".trim().replaceAll(".*[/\\\\]", "").replaceAll("[.]cpn$", "")
				.trim());
		System.out.println("/arhge /erga /raeg \\hello.cpn".trim().replaceAll(".*[/\\\\]", "")
				.replaceAll("[.]cpn$", "").trim());
		LoadCPNToolsLog.printMatch(LoadCPNToolsLog.transition.matcher("1	0	Init @ (1:Fines)"));
		LoadCPNToolsLog.printMatch(LoadCPNToolsLog.binding.matcher(" - role0 = \"system\""));
		LoadCPNToolsLog.printMatch(LoadCPNToolsLog.binding.matcher(" - id = 230"));
	}

	private static void printMatch(final Matcher m) {
		System.out.println(m.matches());
		for (int i = 0; i <= m.groupCount(); i++) {
			System.out.println(" - " + i + ": " + m.group(i));
		}
	}

	@Override
	protected KeyValue importFromStream(final PluginContext context, final InputStream input, final String filename,
			final long fileSizeInBytes) throws Exception {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		String line = reader.readLine();
		int serial = 0;
		if (line != null && line.startsWith("CPN Tools simulation report")) {
			line = reader.readLine();
			if (line != null) {
				final String modelName = line.trim().replaceAll(".*[/\\\\]", "").replaceAll("[.]cpn$", "").trim();

				line = reader.readLine();
				if (line != null && line.startsWith("Report generated")) {
					while (line != null && !line.trim().equals("")) {
						line = reader.readLine();
					}
					while (line != null && line.trim().equals("")) {
						line = reader.readLine();
					}
					if (line != null) {
						final KeyValue result = new KeyValueTrove();
						Map<String, Object> item = null;
						while (line != null) {
							Matcher m = LoadCPNToolsLog.transition.matcher(line);
							if (m.matches()) {
								if (item != null) {
									result.add(item);
								}
								item = new HashMap<String, Object>();
								try {
									item.put(LoadCPNToolsLog.STEP, new BigInteger(m.group(1)));
								} catch (final NumberFormatException _) {
									item.put(LoadCPNToolsLog.STEP, m.group(1));
								}
								try {
									if (m.group(2).indexOf('.') < 0) {
										item.put(LoadCPNToolsLog.TIME, new BigInteger(m.group(2)));
									} else {
										item.put(LoadCPNToolsLog.TIME, Double.parseDouble(m.group(2)));
									}
								} catch (final NumberFormatException _) {
									item.put(LoadCPNToolsLog.TIME, m.group(2));
								}
								item.put(LoadCPNToolsLog.TRANSITION, m.group(3));
								item.put(LoadCPNToolsLog.SERIAL, serial++);
								try {
									item.put(LoadCPNToolsLog.INSTANCE, Integer.parseInt(m.group(4)));
								} catch (final NumberFormatException _) {
									item.put(LoadCPNToolsLog.INSTANCE, m.group(4));
								}
								item.put(LoadCPNToolsLog.PAGE, m.group(5));
							} else {
								m = LoadCPNToolsLog.binding.matcher(line);
								if (m.matches()) {
									item.put("cpnvariable:" + m.group(1), m.group(3));
								}
							}
							line = reader.readLine();
						}
						if (item != null) {
							result.add(item);
						}
						context.getFutureResult(0).setLabel("CPN Tools Log (" + modelName + ")");
						return result;
					}
				}
			}
		}

		context.getFutureResult(0).cancel(true);
		return null;
	}
}
