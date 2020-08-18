package org.noise_planet.noisemodelling.gui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class InputComponent {

    private String id;

    private String name = "";
    private String title = "";
    private String description = "";
    private Class type = String.class;
    private String extra = "";
    private String defaultValue = "";
    private boolean optional = false;
    private List<String> availableTables;

    private Component component;

    InputComponent(String id) {
        setId(id);
        availableTables = new Vector<String>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<String> getAvailableTables() {
        return availableTables;
    }

    public void setAvailableTables(List<String> availableTables) {
        this.availableTables = availableTables;
    }

    public Component getComponent() {
        if (component == null) {
            generateComponent();
        }
        return component;
    }

    public void generateComponent() {
        if (type == String.class) {
            if (extra.equals("path")) {
                JPanel panel = new JPanel();
                JTextField comp = new JTextField();
                panel.add(comp);
                JButton button = new JButton("...");
                panel.add(button);
                button.addActionListener(actionEvent -> {
                    JFileChooser chooser = new JFileChooser();
                    int choice = chooser.showOpenDialog(comp);
                    if (choice == JFileChooser.APPROVE_OPTION) {
                        comp.setText(chooser.getSelectedFile().getAbsolutePath());
                    }
                });
                if (!defaultValue.equals("")) {
                    comp.setText(defaultValue);
                }
                comp.setToolTipText(description);
                comp.setPreferredSize(new Dimension(200, 30));
                component = panel;
                return;
            }
            else if (extra.equals("table")) {
                JComboBox<String> comp = new JComboBox<String>(new Vector<String>(availableTables));
                if (availableTables.contains(defaultValue)) {
                    comp.setSelectedItem(defaultValue);
                }
                comp.setToolTipText(description);
                comp.setPreferredSize(new Dimension(200, 30));
                component = comp;
                return;
            }
            else {
                JTextField comp = new JTextField();
                comp.setText(defaultValue);
                comp.setToolTipText(description);
                comp.setPreferredSize(new Dimension(200, 30));
                component = comp;
                return;
            }
        }
        if (type == Boolean.class) {
            JCheckBox comp = new JCheckBox();
            if (!defaultValue.equals("")) {
                comp.setSelected(Boolean.parseBoolean(defaultValue));
            }
            comp.setToolTipText(description);
            component = comp;
            return;
        }
        if (type == Integer.class) {
            JSpinner comp = new JSpinner();
            SpinnerNumberModel model = new SpinnerNumberModel();
            if (extra.equals("SRID")) {
                model.setValue(2154); // Software is made in france !
                model.setMinimum(2000);
                model.setMinimum(32766);
            }
            if (!defaultValue.equals("")) {
                model.setValue(Integer.valueOf(defaultValue));
            }
            comp.setModel(model);
            comp.setToolTipText(description);
            comp.setPreferredSize(new Dimension(200, 30));
            component = comp;
        }
    }

    public Object getValue() {
        if (type == String.class) {
            if (extra.equals("path")) {
                JPanel panel = (JPanel) component;
                JTextField comp = (JTextField) panel.getComponent(0);
                return comp.getText();
            }
            else if (extra.equals("table")) {
                JComboBox<String> comp = (JComboBox<String>) component;
                return (String) comp.getSelectedItem();
            }
            else {
                JTextField comp = (JTextField) component;
                return comp.getText();
            }
        }
        if (type == Boolean.class) {
            JCheckBox comp = (JCheckBox) component;
            return comp.isSelected();
        }
        if (type == Integer.class) {
            JSpinner comp = (JSpinner) component;
            return comp.getValue();
        }
        return new Object();
    }
}
