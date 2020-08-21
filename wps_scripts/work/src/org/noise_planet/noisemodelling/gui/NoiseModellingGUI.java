package org.noise_planet.noisemodelling.gui;


import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class NoiseModellingGUI {
    private JPanel mainPanel;
    private JTree wpsTree;
    private JScrollPane wpsTreePane;
    private JPanel wpsPanel;
    private JButton wpsStartBtn;
    private JTable dbTable;
    private JList dbList;
    private JPanel mapHolder;

    private DataBaseManager dbMgr;

    private GroovyScriptEngine engine;
    private Connection connection;

    private WpsNode currentScript;

    public NoiseModellingGUI() {
        wpsStartBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    currentScript.run();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, IOException, SQLException, ResourceException, ScriptException {
        for (UIManager.LookAndFeelInfo info: UIManager.getInstalledLookAndFeels()) {
            System.out.println(info.getName());
            if (info.getName().equals("GTK+")) {
                UIManager.setLookAndFeel(info.getClassName());
            }
        }

        JFrame frame = new JFrame("NoiseModellingGUI");
        NoiseModellingGUI gui = new NoiseModellingGUI();

        String dbName = "h2gisdb";
        gui.dbMgr = new DataBaseManager(dbName);

        gui.dbList.setModel(gui.dbMgr.getTableListModel());

        gui.dbList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                try {
                    String tableName = (String) gui.dbList.getSelectedValue();
                    gui.dbTable.setModel(gui.dbMgr.getTableModel(tableName));
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        File fileRoot = new File("/home/valoo/Projects/IFSTTAR/NoiseModelling/wps_scripts/src/main/groovy/org/noise_planet/noisemodelling/wps/");
        gui.engine = new GroovyScriptEngine(fileRoot.getAbsolutePath());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new WpsNode(fileRoot, gui.dbMgr, "SCRIPTS"));
        DefaultTreeModel model = new DefaultTreeModel(root);

        File[] categories = fileRoot.listFiles();
        if (categories != null) {
            for (File category : categories) {
                if (category.isDirectory()) {
                    DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(new WpsNode(category, gui.dbMgr));
                    File[] wpsFiles = category.listFiles();
                    if (wpsFiles != null) {
                        for (File wpsFile : wpsFiles) {
                            WpsNode wpsNode = new WpsNode(wpsFile, gui.dbMgr);
                            categoryNode.add(new DefaultMutableTreeNode(wpsNode));
                            gui.wpsPanel.add(wpsNode.getPanel(), wpsNode.toString());
                        }
                    }
                    root.add(categoryNode);
                } else {
                    WpsNode wpsNode = new WpsNode(category, gui.dbMgr);
                    root.add(new DefaultMutableTreeNode(wpsNode));
                    gui.wpsPanel.add(wpsNode.getPanel(), wpsNode.toString());
                }
            }
        }

        gui.wpsTree.setModel(model);
        for (int i = 0; i < gui.wpsTree.getRowCount(); i++) {
            gui.wpsTree.expandRow(i);
        }

        gui.wpsTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        gui.wpsTree.getLastSelectedPathComponent();

                if (node == null) return;
                if (!node.isLeaf()) return;

                WpsNode wpsNode = (WpsNode) node.getUserObject();
                gui.currentScript = wpsNode;
                CardLayout layout = (CardLayout) gui.wpsPanel.getLayout();
                layout.show(gui.wpsPanel, wpsNode.toString());
            }
        });

        frame.setContentPane(gui.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
