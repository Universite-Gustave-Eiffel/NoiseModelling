package org.noise_planet.noisemodelling.gui;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WpsNode {

    static public int TYPE_DIR = 0;
    static public int TYPE_SCRIPT = 1;
    static public int TYPE_INVALID = 2;

    private final int type;
    private final File file;
    private DataBaseManager dbMgr = null;
    private String name;

    private Binding binding;
    private GroovyScriptEngine engine;
    private JPanel panel;
    private List<InputComponent> inputComponentList;

    public WpsNode(File file, DataBaseManager dbMgr) throws IOException, ResourceException, ScriptException, SQLException, ClassNotFoundException {
        this.file = file;
        this.dbMgr = dbMgr;
        List<String> availableTables = dbMgr.getAvailableTables();
        if (file.isDirectory()) {
            type = TYPE_DIR;
            name = file.getName();
        }
        else if (file.getName().endsWith(".groovy")) {
            type = TYPE_SCRIPT;
            name = file.getName().substring(0, file.getName().lastIndexOf('.'));
            panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            binding = new Binding();
            engine = new GroovyScriptEngine(file.getAbsolutePath());
            engine.run(file.getAbsolutePath(), binding);

            System.out.println("================ WPS : " + toString());
            LinkedHashMap<String, Object> inputs = (LinkedHashMap) binding.getVariable("inputs");
            inputComponentList = new ArrayList<InputComponent>();

            int row = 0;

            for (Map.Entry<String, Object> e : inputs.entrySet()) {
                String k = e.getKey();
                Object options = e.getValue();
                System.out.println("------------ input : " + k);

                InputComponent input = new InputComponent(k);

                LinkedHashMap<String, Object> optionList = (LinkedHashMap) options;
                for (Map.Entry<String, Object> entry : optionList.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    System.out.println(key + " : " + value);
                    if (key.equals("name")) {
                        input.setName((String) value);
                    }
                    if (key.equals("title")) {
                        input.setTitle((String) value);
                    }
                    if (key.equals("description")) {
                        input.setDescription((String) value);
                    }
                    if (key.equals("min") || key.equals("max")) {
                        input.setOptional(true);
                    }
                    if (key.equals("type")) {
                        input.setType((Class) value);
                    }
                    if (key.equals("extra")) {
                        input.setExtra((String) value);
                        if (input.getExtra().equals("table")) {
                            input.setAvailableTables(availableTables);
                        }
                    }
                    if (key.equals("default")) {
                        input.setDefaultValue((String) value);
                    }
                }
                if (dbMgr != null) {
                    dbMgr.refreshAvailableTables();
                    input.setAvailableTables(dbMgr.getAvailableTables());
                }
                System.out.println("-----------------------------");

                if (input.getComponent() != null) {
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = 0;
                    c.gridy = row;
                    JLabel label = new JLabel(input.getName(), JLabel.TRAILING);
                    panel.add(label, c);
                    c.gridx = 1;
                    c.gridy = row;
                    c.weightx = 2;
                    label.setLabelFor(input.getComponent());
                    panel.add(input.getComponent(), c);
                    row++;
                }
                inputComponentList.add(input);
            }
            System.out.println("=============================");
            System.out.println();
        }
        else {
            type = TYPE_INVALID;
        }
    }

    public WpsNode(File file, DataBaseManager dbMgr, String name) throws IOException, ResourceException, ScriptException, SQLException, ClassNotFoundException {
        this(file, dbMgr);
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public int getType() {
        return type;
    }

    public JPanel getPanel() {
        return panel;
    }

    public void run() throws SQLException, ClassNotFoundException, IOException {
        LinkedHashMap<String, Object> inputs = new LinkedHashMap<>();
        for (InputComponent input : inputComponentList) {
            inputs.put(input.getId(), input.getValue());
        }
        Connection connection = dbMgr.openConnection();
        Object[] args = {
                connection,
                inputs
        };
        new GroovyShell().parse(file).invokeMethod( "exec", args ) ;
        dbMgr.closeConnection(connection);
    }
}
