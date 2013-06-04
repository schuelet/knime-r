/*
 * ------------------------------------------------------------------
 * Copyright, 2003 - 2013
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
package org.knime.r;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.r.template.AddTemplateDialog;
import org.knime.r.template.TemplateProvider;
import org.knime.r.ui.RColumnList;
import org.knime.r.ui.RConsole;
import org.knime.r.ui.RFlowVariableList;
import org.knime.r.ui.RObjectBrowser;
import org.knime.r.ui.RSnippetTextArea;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 * The dialog component for RSnippet-Nodes.
 * 
 * @author Heiko Hofer
 */
public class RSnippetNodePanel extends JPanel implements RListener {
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(RSnippetNodeDialog.class);

	private static final String SNIPPET_TAB = "R Snippet";

	private RSnippetTextArea m_snippetTextArea;
	/** Component with a list of all input columns. */
	protected RColumnList m_colList;
	/** Component with a list of all input flow variables. */
	protected RFlowVariableList m_flowVarsList;

	/** The settings. */
	protected RSnippetSettings m_settings;
	private RSnippet m_snippet;

	/** The input data table */
	private BufferedDataTable m_data;

	private RConsole m_console;

	private RObjectBrowser m_objectBrowser;

	private boolean m_isEnabled;
	private boolean m_isInteractive;

	@SuppressWarnings("rawtypes")
	/** The templates category for templates viewed or edited by this dialog. */
	protected Class m_templateMetaCategory;
	private JLabel m_templateLocation;

	/**
	 * @param templateMetaCategory
	 *            the meta category used in the templates tab or to create
	 *            templates
	 * @param isPreview
	 *            if this is a preview used for showing templates.
	 */
	@SuppressWarnings("rawtypes")
	public RSnippetNodePanel(final Class templateMetaCategory,
			final boolean isPreview) {
		super(new BorderLayout());
		m_settings = new RSnippetSettings();
		m_snippet = new RSnippet();

		JPanel panel = createPanel(isPreview);
		m_colList.install(m_snippetTextArea);
		m_flowVarsList.install(m_snippetTextArea);
		
		m_templateMetaCategory = templateMetaCategory;
		m_isEnabled = !isPreview;

		panel.setPreferredSize(new Dimension(800, 600));
	}

	private JPanel createPanel(final boolean isPreview) {
		JPanel p = this;
		JComponent snippet = createSnippetPanel();
		JPanel snippetPanel = new JPanel(new BorderLayout());
		JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		JButton runButton = new JButton("Run Script");
		runButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					rClearRWorkspace();
					RController.getDefault().getConsoleQueue()
							.putRScript(m_snippetTextArea.getText());
				} catch (InterruptedException e1) {
					throw new RuntimeException(e1);
				}
			}
		});
		runPanel.add(runButton);
		JButton evalSelButton = new JButton("Eval Selection");
		evalSelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					String selected = m_snippetTextArea.getSelectedText();
					if (selected != null) {
						RController.getDefault().getConsoleQueue()
								.putRScript(selected);
					}
				} catch (InterruptedException e1) {
					throw new RuntimeException(e1);
				}
			}
		});
		runPanel.add(evalSelButton);
		snippetPanel.add(snippet, BorderLayout.CENTER);
		snippetPanel.add(runPanel, BorderLayout.SOUTH);
		JComponent colsAndVars = createColsAndVarsPanel();

		JSplitPane leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		leftSplitPane.setLeftComponent(colsAndVars);
		leftSplitPane.setRightComponent(snippetPanel);
		leftSplitPane.setDividerLocation(170);

		m_objectBrowser = new RObjectBrowser();
		m_objectBrowser.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					int row = m_objectBrowser.getSelectedRow();
					if (row > -1) {
						String name = (String) m_objectBrowser.getValueAt(row,
								0);
						String cmd = "print(" + name + ")";
						try {
							RController.getDefault().getConsoleQueue()
									.putRScript(cmd);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			}
		});
		JScrollPane objectBrowserScroller = new JScrollPane(m_objectBrowser);
		objectBrowserScroller.setBorder(createEmptyTitledBorder("Workspace"));

		JSplitPane rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		rightSplitPane.setLeftComponent(leftSplitPane);
		rightSplitPane.setRightComponent(objectBrowserScroller);
		rightSplitPane.setDividerLocation(550);

		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainSplitPane.setTopComponent(rightSplitPane);

		m_console = new RConsole();
		JScrollPane consoleScroller = new JScrollPane(m_console);
		consoleScroller.setBorder(createEmptyTitledBorder("Console"));
		mainSplitPane.setBottomComponent(consoleScroller);
		mainSplitPane.setOneTouchExpandable(true);

		JPanel centerPanel = new JPanel(new GridLayout(0, 1));
		centerPanel.add(mainSplitPane);

		p.add(centerPanel, BorderLayout.CENTER);
		JPanel templateInfoPanel = createTemplateInfoPanel(isPreview);
        p.add(templateInfoPanel, BorderLayout.NORTH);		
		return p;
	}


	/**
	 * Create the panel with the snippet.
	 */
	private JComponent createSnippetPanel() {

		m_snippetTextArea = new RSnippetTextArea(m_snippet);

		// // reset style which causes a recreation of the folds
		// m_snippetTextArea.setSyntaxEditingStyle(
		// SyntaxConstants.SYNTAX_STYLE_NONE);
		// m_snippetTextArea.setSyntaxEditingStyle(
		// SyntaxConstants.SYNTAX_STYLE_JAVA);
		// // collapse all folds
		// int foldCount = m_snippetTextArea.getFoldManager().getFoldCount();
		// for (int i = 0; i < foldCount; i++) {
		// Fold fold = m_snippetTextArea.getFoldManager().getFold(i);
		// fold.setCollapsed(true);
		// }
		JScrollPane snippetScroller = new RTextScrollPane(m_snippetTextArea);
		snippetScroller.setBorder(createEmptyTitledBorder("R Script"));
		JPanel snippet = new JPanel(new BorderLayout());
		snippet.add(snippetScroller, BorderLayout.CENTER);
		ErrorStrip es = new ErrorStrip(m_snippetTextArea);
		snippet.add(es, BorderLayout.LINE_END);
		return snippet;
	}

	/**
	 * The panel at the left with the column and variables at the input.
	 * Override this method when the columns or variables should not be
	 * displayed.
	 * 
	 * @return the panel at the left with the column and variables at the input.
	 */
	protected JComponent createColsAndVarsPanel() {
		JSplitPane varSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		m_colList = new RColumnList();

		JScrollPane colListScroller = new JScrollPane(m_colList);
		colListScroller.setBorder(createEmptyTitledBorder("Column List"));
		varSplitPane.setTopComponent(colListScroller);

		// set variable panel
		m_flowVarsList = new RFlowVariableList();
		JScrollPane flowVarScroller = new JScrollPane(m_flowVarsList);
		flowVarScroller
				.setBorder(createEmptyTitledBorder("Flow Variable List"));
		varSplitPane.setBottomComponent(flowVarScroller);
		varSplitPane.setOneTouchExpandable(true);
		varSplitPane.setResizeWeight(0.9);

		return varSplitPane;
	}

	/**
	 * Create Panel with additional options to be displayed in the south.
	 * 
	 * @return options panel or null if there are no additional options.
	 */
	protected JPanel createOptionsPanel() {
		return null;
	}
	
	/**
     * The panel at the to with the "Create Template..." Button.
     */
    private JPanel createTemplateInfoPanel(final boolean isPreview) {
        final JButton addTemplateButton = new JButton("Create Template...");
        addTemplateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Frame parent = (Frame)SwingUtilities.getAncestorOfClass(
                        Frame.class, addTemplateButton);
                RSnippetTemplate newTemplate = AddTemplateDialog.openUserDialog(
                        parent, m_snippet,
                        m_templateMetaCategory);
                if (null != newTemplate) {
                    TemplateProvider.getDefault().addTemplate(newTemplate);
                    // update the template UUID of the current snippet
                    m_settings.setTemplateUUID(newTemplate.getUUID());
                    String loc = TemplateProvider.getDefault().
                        getDisplayLocation(newTemplate);
                    m_templateLocation.setText(loc);
                    validate();
                }
            }
        });
        JPanel templateInfoPanel = new JPanel(new BorderLayout());
        TemplateProvider provider = TemplateProvider.getDefault();
        String uuid = m_settings.getTemplateUUID();
        RSnippetTemplate template = null != uuid ? provider.getTemplate(
                UUID.fromString(uuid)) : null;
        String loc = null != template
                ? createTemplateLocationText(template)
                : "";
        m_templateLocation = new JLabel(loc);
        if (isPreview) {
            templateInfoPanel.add(m_templateLocation, BorderLayout.CENTER);
        } else {
            templateInfoPanel.add(addTemplateButton, BorderLayout.LINE_END);
        }
        templateInfoPanel.setBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return templateInfoPanel;
    }   	

	/**
	 * Create an empty, titled border.
	 * 
	 * @param title
	 *            Title of the border.
	 * @return Such a new border.
	 */
	protected Border createEmptyTitledBorder(final String title) {
		return BorderFactory.createTitledBorder(
				BorderFactory.createEmptyBorder(5, 0, 0, 0), title,
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP);
	}


	private void rClearRWorkspace() {
		// try {
		// RController.getDefault().clearWorkspace();
		// } catch (REngineException | REXPMismatchException e) {
		// // TODO: Add log entry
		// }
	}

	private void rPrintValue(final String name) {
		REXP rexp;
		try {
			rexp = RController.getDefault().idleEval("print(" + name + ")");
		} catch (REngineException | REXPMismatchException e) {
			// TODO: Add log entry
		}
	}

	private String[] rGetObjectNames() {
		REXP rexp;
		try {
			rexp = RController.getDefault().idleEval("ls()");
			return rexp != null ? rexp.asStrings() : null;
		} catch (REngineException | REXPMismatchException e) {
			// TODO: Add log entry
			return null;
		}
	}

	private String[] rGetObjectClasses() {
		REXP rexp;
		try {
			rexp = RController
					.getDefault()
					.idleEval(
							"sapply(ls(),function(a)class(get(a,envir=globalenv()))[1])");
			return rexp != null ? rexp.asStrings() : null;
		} catch (REngineException | REXPMismatchException e) {
			// TODO: Add log entry
			return null;
		}
	}

	@Override
	public void workspaceChanged(final REvent e) {
		final String[] objectNames = rGetObjectNames();
		if (objectNames != null && objectNames.length > 0) {
			final String[] objectClasses = rGetObjectClasses();
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					m_objectBrowser.updateData(objectNames, objectClasses);
				}
			});

		}
	}

	/**
	 * Reinitialize with the given blueprint.
	 * 
	 * @param template
	 *            the template
	 * @param flowVariables
	 *            the flow variables at the input
	 * @param spec
	 *            the input spec
	 */
	public void applyTemplate(final RSnippetTemplate template,
			final DataTableSpec spec,
			final Map<String, FlowVariable> flowVariables) {
		// save and read settings to decouple objects.
		NodeSettings settings = new NodeSettings(template.getUUID());
		template.getSnippetSettings().saveSettings(settings);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			settings.saveToXML(os);
			NodeSettingsRO settingsro = NodeSettings
					.loadFromXML(new ByteArrayInputStream(os.toString("UTF-8")
							.getBytes("UTF-8")));
			m_settings.loadSettings(settingsro);
		} catch (Exception e) {
			LOGGER.error("Cannot apply template.", e);
		}

		m_colList.setSpec(spec);
		m_flowVarsList.setFlowVariables(flowVariables.values());
		m_snippet.setSettings(m_settings);
		// update template info panel
		m_templateLocation.setText(createTemplateLocationText(template));

		m_snippetTextArea.requestFocus();		
	}

	/**
	 * Get the template's location for display.
	 * 
	 * @param template
	 *            the template
	 * @return the template's loacation for display
	 */
	private String createTemplateLocationText(final RSnippetTemplate template) {
		TemplateProvider provider = TemplateProvider.getDefault();
		return provider.getDisplayLocation(template);
	}

	/**
	 * {@inheritDoc}
	 */
	public void onOpen() {
		// FIXME: Should we keep following lines?
		rClearRWorkspace();
		m_console.setText("");
		m_objectBrowser.updateData(new String[0], new String[0]);

		m_snippetTextArea.requestFocus();
		m_snippetTextArea.requestFocusInWindow();

		RController.getDefault().getConsoleController().attachOutput(m_console);
		// start listing to the RController for updating the object browser
		RController.getDefault().addRListener(this);

	}

	public void onClose() {
		RController.getDefault().getConsoleController().detach(m_console);
		// start listing to the RController for updating the object browser
		RController.getDefault().removeRListener(this);
	}

	/**
	 * {@inheritDoc}
	 */

	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		RSnippetSettings s = m_snippet.getSettings();

		// give subclasses the chance to modify settings
		preSaveSettings(s);

		s.saveSettings(settings);
	}

	/**
	 * Called right before storing the settings object. Gives subclasses the
	 * chance to modify the settings object.
	 * 
	 * @param s
	 *            the settings
	 */
	protected void preSaveSettings(final RSnippetSettings s) {
		// just a place holder.
	}
	
	
	public void updateData(final NodeSettingsRO settings,
			final DataTableSpec spec, final Collection<FlowVariable> flowVariables) {
		m_settings.loadSettingsForDialog(settings);
		updateData(m_settings, null, spec, flowVariables);
	}


	public void updateData(final RSnippetSettings settings,
			final BufferedDataTable inputData, final Collection<FlowVariable> flowVariables) {
		updateData(settings, inputData, inputData.getDataTableSpec(), flowVariables);
		
	}

	public void updateData(final RSnippetSettings settings,
			final DataTableSpec spec, final Collection<FlowVariable> flowVariables) {
		DataTableSpec fooSpec = spec != null
    			? spec
    			: new DataTableSpec(); 		
		updateData(settings, null, fooSpec, flowVariables);
	}
	
	private void updateData(final RSnippetSettings settings,
			final BufferedDataTable inputData,
			final DataTableSpec spec, final Collection<FlowVariable> flowVariables) {
		ViewUtils.invokeAndWaitInEDT(new Runnable() {
			@Override
			public void run() {
				updateDataInternal(settings, inputData, spec, flowVariables);
			}


		});
		
	}	
	
	protected void updateDataInternal(final RSnippetSettings settings,
			final BufferedDataTable inputData, final DataTableSpec spec,
			final Collection<FlowVariable> flowVariables) {
		m_settings.loadSettings(settings);

		m_colList.setSpec(spec);
		m_flowVarsList.setFlowVariables(flowVariables);
		m_snippet.setSettings(m_settings);

		// // set caret position to the start of the custom expression
		// m_snippetTextArea.setCaretPosition(
		// m_snippet.getDocument().getGuardedSection(
		// RSnippetDocument.GUARDED_BODY_START).getEnd().getOffset()
		// + 1);
		m_snippetTextArea.requestFocusInWindow();

        // update template info panel
        TemplateProvider provider = TemplateProvider.getDefault();
        String uuid = m_settings.getTemplateUUID();
        RSnippetTemplate template = null != uuid ? provider.getTemplate(
                UUID.fromString(uuid)) : null;
        String loc = null != template ? createTemplateLocationText(template)
                : "";
        m_templateLocation.setText(loc);	
	}	
	
}
