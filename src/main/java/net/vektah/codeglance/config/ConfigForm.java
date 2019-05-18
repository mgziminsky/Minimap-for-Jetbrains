/*
 * Copyright © 2013, Adam Scarr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.vektah.codeglance.config;

import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelListener;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class ConfigForm {
    private JCheckBox disabled;
    private JCheckBox locked;
    private JComboBox alignment;
    private JComboBox jumpToPosition;
    private JComboBox pixelsPerLine;
    private JComboBox renderStyle;
    private JPanel rootPanel;
    private JSpinner minLinesCount;
    private JSpinner minWindowWidth;
    private JSpinner width;
    private JTextField viewportColor;

    public ConfigForm() {
        pixelsPerLine.setModel(new DefaultComboBoxModel(new Integer[]{1, 2, 3, 4}));
        viewportColor.setInputVerifier(new InputVerifier() {
            private final Pattern pattern = Pattern.compile("[a-fA-F0-9]{6}");
            private final Border defaultBorder = viewportColor.getBorder();
            private final Border invalidBorder = BorderFactory.createLineBorder(JBColor.RED);

            @Override
            public boolean verify(JComponent input) {
                boolean valid = pattern.matcher(viewportColor.getText()).matches();
                if (!valid)
                    viewportColor.setBorder(invalidBorder);
                else
                    viewportColor.setBorder(defaultBorder);
                return valid;
            }

            @Override
            public boolean shouldYieldFocus(JComponent input) {
                verify(input);
                return true;
            }
        });

        width.setModel(new SpinnerNumberModel(110, 50, 250, 5));
        minLinesCount.setModel(new SpinnerNumberModel(1, 0, 100, 10));
        minWindowWidth.setModel(new SpinnerNumberModel(0, 0, Short.MAX_VALUE, 10));

        // Spinner scroll support
        MouseWheelListener scrollListener = e -> {
            JSpinner spinner = (JSpinner) e.getSource();
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();

            int step = model.getStepSize().intValue();
            switch (e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) {
                case InputEvent.CTRL_DOWN_MASK:
                    step *= 2;
                    break;
                case InputEvent.SHIFT_DOWN_MASK:
                    step /= 2;
                    break;
                case InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK:
                    step = 1;
                    break;
            }

            int newValue = (int) spinner.getValue() + (step * -e.getWheelRotation());
            newValue = Math.min(Math.max(newValue, (Integer) model.getMinimum()), (Integer) model.getMaximum());
            spinner.setValue(newValue);
        };
        width.addMouseWheelListener(scrollListener);
        minLinesCount.addMouseWheelListener(scrollListener);
        minWindowWidth.addMouseWheelListener(scrollListener);

        // ComboBox scroll support
        scrollListener = e -> {
            JComboBox comboBox = (JComboBox) e.getSource();
            // Bounded Scroll
            comboBox.setSelectedIndex(Math.max(0, Math.min(comboBox.getSelectedIndex() + e.getWheelRotation(), comboBox.getItemCount() - 1)));
            // Cyclic Scroll
            // comboBox.setSelectedIndex(Math.abs((comboBox.getSelectedIndex() + e.getWheelRotation()) % comboBox.getItemCount()));
        };
        alignment.addMouseWheelListener(scrollListener);
        jumpToPosition.addMouseWheelListener(scrollListener);
        pixelsPerLine.addMouseWheelListener(scrollListener);
        renderStyle.addMouseWheelListener(scrollListener);
    }

    public JPanel getRoot() {
        return rootPanel;
    }

    public int getPixelsPerLine() {
        return (int) pixelsPerLine.getSelectedItem();
    }

    public void setPixelsPerLine(int pixelsPerLine) {
        this.pixelsPerLine.setSelectedIndex(pixelsPerLine - 1);
    }

    public boolean isDisabled() {
        return disabled.getModel().isSelected();
    }

    public void setDisabled(boolean isDisabled) {
        disabled.getModel().setSelected(isDisabled);
    }

    public boolean isLocked() {
        return locked.getModel().isSelected();
    }

    public void setLocked(boolean isLocked) {
        locked.getModel().setSelected(isLocked);
    }

    public boolean jumpOnMouseDown() {
        return jumpToPosition.getSelectedIndex() == 0;
    }

    public void setJumpOnMouseDown(boolean jump) {
        jumpToPosition.setSelectedIndex(jump ? 0 : 1);
    }

    public String getViewportColor() {
        return viewportColor.getText();
    }

    public void setViewportColor(String color) {
        viewportColor.setText(color);
    }

    public boolean getCleanStyle() {
        return renderStyle.getSelectedIndex() == 0;
    }

    public void setCleanStyle(boolean isClean) {
        renderStyle.setSelectedIndex(isClean ? 0 : 1);
    }

    public boolean isRightAligned() {
        return alignment.getSelectedIndex() == 0;
    }

    public void setRightAligned(boolean isRightAligned) {
        alignment.setSelectedIndex(isRightAligned ? 0 : 1);
    }

    public int getWidth() {
        return (int) width.getValue();
    }

    public void setWidth(int width) {
        this.width.setValue(width);
    }

    public int getMinLinesCount() {
        return (int) minLinesCount.getValue();
    }

    public void setMinLinesCount(int minLinesCount) {
        this.minLinesCount.setValue(minLinesCount);
    }

    public int getMinWindowWidth() {
        return (int) minWindowWidth.getValue();
    }

    public void setMinWindowWidth(int minWindowWidth) {
        this.minWindowWidth.setValue(minWindowWidth);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(10, 4, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Pixels Per Line:");
        label1.setDisplayedMnemonic('P');
        label1.setDisplayedMnemonicIndex(0);
        rootPanel.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pixelsPerLine = new JComboBox();
        rootPanel.add(pixelsPerLine, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, 24), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Jump to position on:");
        rootPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jumpToPosition = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Mouse Down");
        defaultComboBoxModel1.addElement("Mouse Up");
        jumpToPosition.setModel(defaultComboBoxModel1);
        rootPanel.add(jumpToPosition, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Disabled:");
        rootPanel.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        disabled = new JCheckBox();
        disabled.setText("");
        rootPanel.add(disabled, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        width = new JSpinner();
        rootPanel.add(width, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Width:");
        rootPanel.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Viewport Color");
        rootPanel.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        viewportColor = new JTextField();
        rootPanel.add(viewportColor, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Minimum lines count:");
        label6.setToolTipText("Minimum number of lines to show minimap.");
        rootPanel.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        minLinesCount = new JSpinner();
        rootPanel.add(minLinesCount, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Render Style");
        rootPanel.add(label7, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        renderStyle = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("Clean");
        defaultComboBoxModel2.addElement("Accurate");
        renderStyle.setModel(defaultComboBoxModel2);
        rootPanel.add(renderStyle, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Alignment (restart)");
        rootPanel.add(label8, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        alignment = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("Right");
        defaultComboBoxModel3.addElement("Left");
        alignment.setModel(defaultComboBoxModel3);
        rootPanel.add(alignment, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Minimum Window Width:");
        rootPanel.add(label9, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minWindowWidth = new JSpinner();
        rootPanel.add(minWindowWidth, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        locked = new JCheckBox();
        locked.setText("lock");
        rootPanel.add(locked, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        rootPanel.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        label1.setLabelFor(pixelsPerLine);
        label2.setLabelFor(jumpToPosition);
        label4.setLabelFor(width);
        label5.setLabelFor(viewportColor);
        label6.setLabelFor(minLinesCount);
        label7.setLabelFor(renderStyle);
        label8.setLabelFor(alignment);
        label9.setLabelFor(minWindowWidth);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
