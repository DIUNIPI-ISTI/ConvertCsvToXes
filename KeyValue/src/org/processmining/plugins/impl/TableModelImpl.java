package org.processmining.plugins.impl;

import javax.swing.table.DefaultTableModel;

public class TableModelImpl extends DefaultTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4870856738970795258L;
	private String classTableName = null;

	public TableModelImpl(String classTableName) {
		super();

		this.classTableName = classTableName;
	}

	@Override
	public boolean isCellEditable(int row, int cols) {
		boolean cellEditable = true;

		if ("AttributeTable".equalsIgnoreCase(classTableName)) {
			if (cols != 1) {
				cellEditable = false;
			}
		} else if ("CSVTable".equalsIgnoreCase(classTableName)) {
			cellEditable = false;
		}

		return cellEditable;
	}
}
