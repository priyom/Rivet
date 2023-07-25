// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// This program comes with ABSOLUTELY NO WARRANTY

package org.e2k;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class DisplayView extends JPanel implements AdjustmentListener {
    public static final long serialVersionUID = 1;
    private final StyledDocument doc;

    private boolean adjustScrollBar = true;

    private int scrollPreviousValue = -1;
    private int scrollPreviousMaximum = -1;

    public DisplayView() {
        super(new BorderLayout());
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);

        doc = textPane.getStyledDocument();

        JPopupMenu namePopMenu = new JPopupMenu();
        JMenuItem copy = new JMenuItem("Copy");
        copy.addActionListener(new DefaultEditorKit.CopyAction());
        namePopMenu.add(copy);
        textPane.setComponentPopupMenu(namePopMenu);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(this);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    private AttributeSet composeTextAttributeSet(Color color, Font font) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setItalic(attrs, font.isItalic());
        StyleConstants.setBold(attrs, font.isBold());
        StyleConstants.setFontFamily(attrs, font.getFamily());
        StyleConstants.setFontSize(attrs, font.getSize());
        return attrs;
    }

    // Gets all the text on the screen and returns it as a string
    public String getText() {
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    // Add a line to the display //
    public void addLine(String line, Color tcol, Font tfont) {
        try {
            doc.insertString(doc.getLength(), line + "\n", composeTextAttributeSet(tcol, tfont));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    // Adds a single character to the current line
    public void addChar(String ch, Color col, Font font) {
        try {
            doc.insertString(doc.getLength(), ch, composeTextAttributeSet(col, font));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    // Newline
    public void newLine() {
        try {
            doc.insertString(doc.getLength(), "\n", new SimpleAttributeSet());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    // Clear the display screen
    public void clearScreen() {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void adjustmentValueChanged(final AdjustmentEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                checkScrollBar(e);
            }
        });
    }

    private void checkScrollBar(AdjustmentEvent e) {
        JScrollBar scrollBar = (JScrollBar) e.getSource();
        BoundedRangeModel listModel = scrollBar.getModel();
        int value = listModel.getValue();
        int extent = listModel.getExtent();
        int maximum = listModel.getMaximum();

        boolean valueChanged = scrollPreviousValue != value;
        boolean maximumChanged = scrollPreviousMaximum != maximum;

        //  Check if the user has manually repositioned the scrollbar

        if (valueChanged && !maximumChanged) {
            adjustScrollBar = value + extent >= maximum;
        }

        //  Reset the "value" so we can reposition the viewport and
        //  distinguish between a user scroll and a program scroll.
        //  (ie. valueChanged will be false on a program scroll)

        if (adjustScrollBar) {
            //  Scroll the viewport to the end.
            scrollBar.removeAdjustmentListener(this);
            value = maximum - extent;
            scrollBar.setValue(value);
            scrollBar.addAdjustmentListener(this);
        }
        scrollPreviousValue = value;
        scrollPreviousMaximum = maximum;
    }
}
