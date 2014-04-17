package org.processmining.plugins.impl;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class IconCellRendererImpl extends DefaultTableCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7748664943523989615L;
	JLabel lbl = new JLabel();

	public IconCellRendererImpl() {
		this.setHorizontalAlignment(this.LEFT);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		// lbl.setText((String) value);
		lbl.setHorizontalAlignment(JLabel.CENTER);
		if (value instanceof ImageIcon)
			lbl.setIcon((ImageIcon) value);
		return lbl;
	}
}
