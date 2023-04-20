/*
 * Copyright (c) 2009-2023 Ryan Vogt <rvogt@ualberta.ca>
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
import javax.swing.*;

/**
 * The <code>MenuDecorated</code> is a convenience class, representing a menu
 * in which items can have mnemonics and accelerators.
 */
public class MenuDecorated extends JMenu {
    /**
     * Creates a new <code>MenuDecorated</code> with the given text.
     *
     * @param text the text to display for the menu.
     */
    public MenuDecorated(String text) {
        super(text);
    }

    /**
     * Creates a new <code>MenuDecorated</code> with the given text and
     * mnemonic.
     *
     * @param text the text to display for the menu.
     * @param mnemonic the mnemonic used by this menu.
     */
    public MenuDecorated(String text, int mnemonic) {
        super(text);
        this.setMnemonic(mnemonic);
    }

    /**
     * Adds a new item to the menu with the given text and
     * <code>ActionListener</code>.
     *
     * @param text the text for the menu item.
     * @param listener the listener to add to the menu item.
     * @return the constructed menu item.
     */
    public JMenuItem add(String text, ActionListener listener) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(listener);
        this.add(menuItem);
        return menuItem;
    }

    /**
     * Adds a new item to the menu with the given text, mnemonic, and
     * <code>ActionListener</code>.
     *
     * @param text the text for the menu item.
     * @param mnemonic the mnemonic used by this menu item.
     * @param listener the listener to add to the menu item.
     * @return the constructed menu item.
     */
    public JMenuItem add(String text, int mnemonic, ActionListener listener) {
        JMenuItem menuItem = new JMenuItem(text, mnemonic);
        menuItem.addActionListener(listener);
        this.add(menuItem);
        return menuItem;
    }

    /**
     * Adds a new item to the menu with the given text, mnemonic, accelerator,
     * and <code>ActionListener</code>. The accelerator will be bound to the
     * system-dependent accelerator mask, such as control (Windows) or command
     * (OS X).
     *
     * @param text the text for the menu item.
     * @param mnemonic the mnemonic used by this menu item.
     * @param accelerator the keystroke to be bound to the control or command
     *        mask that activates this menu item.
     * @param listener the listener to add to the menu item.
     * @return the constructed menu item.
     */
    public JMenuItem add(String text, int mnemonic, int accelerator,
            ActionListener listener) {
        JMenuItem menuItem = new JMenuItem(text, mnemonic);
        int mask = ApplicationGUI.getAcceleratorMask();
        KeyStroke accelKey = KeyStroke.getKeyStroke(accelerator, mask);
        menuItem.setAccelerator(accelKey);
        menuItem.addActionListener(listener);
        this.add(menuItem);
        return menuItem;
    }

    /**
     * Adds a check box menu item with the given text and mnemonic, in the
     * given state, with the given action listener.
     *
     * @param text the text for the menu item.
     * @param mnemonic the mnemonic used by this menu item.
     * @param selected whether the item is initially selected.
     * @param listener the listener to add to the menu item.
     * @return the constructed menu item.
     */
    public JCheckBoxMenuItem add(String text, int mnemonic, boolean selected,
            ActionListener listener) {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text, selected);
        menuItem.setMnemonic(mnemonic);
        menuItem.addActionListener(listener);
        this.add(menuItem);
        return menuItem;
    }

    /**
     * Adds a {@link MenuItemGroup} to this menu consisting of the given
     * elements with the given index initially selected, bound to the given
     * listener.
     *
     * @param text the text for the menu items.
     * @param indexSelected the index of the item initially selected.
     * @param listener the listener to attach to the
     *        <code>MenuItemGroup</code>.
     * @return the constructed menu item group.
     * @throws ArrayIndexOutOfBoundsException if the given index is less than
     *         zero, or greater than or equal to the number of elements in the
     *         group.
     */
    public MenuItemGroup add(String[] text, int indexSelected,
            MenuItemGroupListener listener) {
        MenuItemGroup group = new MenuItemGroup(text);
        this.add(group, indexSelected, listener);
        return group;
    }

    /**
     * Adds the given <code>MenuItemGroup</code> to this menu with the given
     * index initially selected, bound to the given listener.
     *
     * @param group the menu item group.
     * @param indexSelected the index of the item initially selected.
     * @param listener the listener to attach to the
     *        <code>MenuItemGroup</code>.
     * @throws ArrayIndexOutOfBoundsException if the given index is less than
     *         zero, or greater than or equal to the number of elements in the
     *         group.
     */
    public void add(MenuItemGroup group, int indexSelected,
            MenuItemGroupListener listener) {
        group.addGroupListener(listener);
        group.addTo(this, indexSelected);
    }
}
