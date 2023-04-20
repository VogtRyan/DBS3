/*
 * Copyright (c) 2010-2023 Ryan Vogt <rvogt@ualberta.ca>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION
 * OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ca.ualberta.dbs3.gui;

import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * The <code>MenuItemGroup</code> class represents a group of menu items that
 * can be checked off. However, exactly one and only one element in a
 * <code>MenuItemGroup</code> can be checked off at one time. This class is an
 * alternative to using a <code>ButtonGroup</code> with a set of
 * <code>JRadioButtonMenuItem</code>s, and is especially useful because Java's
 * OS X look and feel does not properly implement the
 * <code>JRadioButtonMenuItem</code> class. Note that this class was written
 * back when Java on OS X still used a system-specific look and feel.
 */
public class MenuItemGroup {
    /**
     * The elements of the menu item group, one of which can be checked at a
     * time.
     */
    private ArrayList<JCheckBoxMenuItem> elements;

    /**
     * The listeners that should be informed when one of the elements is
     * selected.
     */
    private ArrayList<MenuItemGroupListener> listeners;

    /**
     * Creates a new <code>MenuItemGroup</code> consisting of the given
     * elements that can be checked.
     *
     * @param elements the text of the elements to add to the new group.
     */
    public MenuItemGroup(String[] elements) {
        this.elements = new ArrayList<JCheckBoxMenuItem>();
        this.listeners = new ArrayList<MenuItemGroupListener>();
        this.add(elements);
    }

    /**
     * Appends this item group to the given menu, with the element at the given
     * index selected.
     *
     * @param menu the menu to which to add this group.
     * @param selectedIndex the index of the element to be selected initially.
     * @throws ArrayIndexOutOfBoundsException if the given index is less than
     *         zero, or greater than or equal to the number of elements in this
     *         group.
     */
    public void addTo(JMenu menu, int selectedIndex) {
        int size = this.elements.size();
        if (selectedIndex < 0 || selectedIndex >= size)
            throw new ArrayIndexOutOfBoundsException("Invalid index: "
                    + selectedIndex + " not in [0, " + (size - 1) + "]");
        for (int i = 0; i < size; i++) {
            JCheckBoxMenuItem item = this.elements.get(i);
            item.setSelected(i == selectedIndex);
            menu.add(item);
        }
    }

    /**
     * Returns the text displayed on the selected item.
     *
     * @return the text of the selected item, or <code>null</code> if the group
     *         has not yet been added to a menu.
     */
    public String getSelectedText() {
        int numElements = this.elements.size();
        for (int i = 0; i < numElements; i++) {
            JCheckBoxMenuItem item = this.elements.get(i);
            if (item.isSelected())
                return item.getText();
        }
        return null;
    }

    /**
     * Changes the currently selected index by the given amount (positive or
     * negative) and informs the group listeners. If the given increment or
     * decrement would move us beyond the bounds of the menu item group, make
     * the last or first item the selected item.
     *
     * @param increment the amount by which to increment (or decrement) the
     *        selected index.
     */
    public void incrementSelectedIndex(int increment) {
        int numElements = this.elements.size();
        int selected = -1;
        for (int i = 0; i < numElements; i++) {
            if (this.elements.get(i).isSelected()) {
                selected = i;
                break;
            }
        }
        if (selected == -1)
            return; /* Items have not been added to a menu */

        int newSelected = selected + increment;
        if (newSelected < 0)
            newSelected = 0;
        else if (newSelected >= numElements)
            newSelected = numElements - 1;
        if (selected == newSelected)
            return;

        this.elements.get(selected).setSelected(false);
        JCheckBoxMenuItem item = this.elements.get(newSelected);
        item.setSelected(true);
        String text = item.getText();
        for (MenuItemGroupListener listener : this.listeners)
            listener.groupItemSelected(text, newSelected);
    }

    /**
     * Adds the given group listener to the set of listeners that will be
     * informed when an element of this group is selected.
     *
     * @param listener the listener to add. If <code>null</code>, this method
     *        will do nothing.
     */
    public void addGroupListener(MenuItemGroupListener listener) {
        if (listener != null)
            this.listeners.add(listener);
    }

    /**
     * Appends a new element with the given text into the group.
     *
     * @param element the text of the element to add.
     */
    private void add(String element) {
        int index = this.elements.size();
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(element);
        item.addActionListener(new SingleCheck(index));
        this.elements.add(item);
    }

    /**
     * Appends new elements with the given text into the group.
     *
     * @param elements the text of the elements to add.
     */
    private void add(String[] elements) {
        for (int i = 0; i < elements.length; i++)
            this.add(elements[i]);
    }

    /**
     * The <code>SingleCheck</code> class is an <code>ActionListener</code>
     * responsible for ensuring that only a single menu item is checked off at
     * a time, and also for notifying the group listeners when an element is
     * chosen.
     */
    private class SingleCheck implements ActionListener {
        /**
         * The index of the element to which this object is listening.
         */
        private int index;

        /**
         * Creates a new <code>SingleCheck</code> that will listen to the
         * element at the given index.
         * 
         * @param index the index of the element to listen to.
         */
        public SingleCheck(int index) {
            this.index = index;
        }

        /**
         * Uncheck all the other elements when this element is selected, then
         * inform all the group listeners.
         * 
         * @param e the action that triggered this call, which is ignored.
         */
        public void actionPerformed(ActionEvent e) {
            int size = elements.size();
            String text = null;
            for (int i = 0; i < size; i++) {
                JCheckBoxMenuItem item = elements.get(i);
                if (i == this.index) {
                    item.setSelected(true);
                    text = item.getText();
                } else
                    item.setSelected(false);
            }
            for (MenuItemGroupListener listener : listeners)
                listener.groupItemSelected(text, this.index);
        }
    }
}
