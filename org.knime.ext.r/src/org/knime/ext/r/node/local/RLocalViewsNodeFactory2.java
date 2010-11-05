/*
 * ------------------------------------------------------------------
 * Copyright, 2003 - 2010
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * This file is part of the R integration plugin for KNIME.
 *
 * The R integration plugin is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St., Fifth Floor, Boston, MA 02110-1301, USA.
 * Or contact us: contact@knime.org.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.ext.r.node.local;

import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory of the <code>RLocalViewsNodeModel</code>.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class RLocalViewsNodeFactory2 extends
        NodeFactory<RLocalViewsNodeModel2> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RLocalViewsNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RLocalViewsNodeModel2 createNodeModel() {
        return new RLocalViewsNodeModel2();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<RLocalViewsNodeModel2> createNodeView(
            final int viewIndex, final RLocalViewsNodeModel2 nodeModel) {
        if (viewIndex == 0) {
            return new RLocalViewsNodeView2(nodeModel);
        } else if (viewIndex == 1) {
            return new ExtToolStderrNodeView<RLocalViewsNodeModel2>(nodeModel);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}