/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * ------------------------------------------------------------------------
 *
 * History
 *   19.09.2007 (thiel): created
 */
package org.knime.r.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.r.Activator;

/**
 *
 * @author Heiko Hofer
 */
public class RPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Creates a new preference page.
     */
    public RPreferencePage() {
        super(GRID);

        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("KNIME R (labs) preferences");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        DirectoryFieldEditor rPath =
                new DirectoryFieldEditor(
                        RPreferenceInitializer.PREF_R_HOME,
                        "Path to R Home", parent);
        addField(rPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench) {
        // nothing to do
    }
}
