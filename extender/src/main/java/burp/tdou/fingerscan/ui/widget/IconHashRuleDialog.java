package burp.tdou.fingerscan.ui.widget;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class IconHashRuleDialog extends JDialog implements ActionListener {

    private Map<String, Object> resultRule;

    private JTextField nameField;
    private JTextField murmurHashField;
    private JTextField md5Field;
    private JComboBox<String> typeComboBox;
    private JTextField infoField;

    private static final String[] RULE_TYPES = {"Application", "Framework", "Server", "CMS", "Other"};

    public IconHashRuleDialog(JFrame parent, String title, Map<String, Object> rule) {
        super(parent, title, true);
        this.resultRule = null;
        initializeUI();
        if (rule != null) {
            populateFields(rule);
        }
        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("名称:*"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        nameField = new JTextField(20);
        mainPanel.add(nameField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        mainPanel.add(new JLabel("MurmurHash3:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        murmurHashField = new JTextField(20);
        murmurHashField.setToolTipText("Shodan/FOFA 兼容的 MurmurHash3-32 值，如 -305179312");
        mainPanel.add(murmurHashField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        mainPanel.add(new JLabel("MD5:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        md5Field = new JTextField(32);
        md5Field.setToolTipText("favicon 文件的 MD5 值（兼容旧指纹库）");
        mainPanel.add(md5Field, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        mainPanel.add(new JLabel("类型:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        typeComboBox = new JComboBox<>(RULE_TYPES);
        typeComboBox.setEditable(true);
        mainPanel.add(typeComboBox, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        mainPanel.add(new JLabel("描述:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        infoField = new JTextField(30);
        mainPanel.add(infoField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        JButton okButton = new JButton("确定");
        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        buttonPanel.add(okButton);
        JButton cancelButton = new JButton("取消");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        buttonPanel.add(cancelButton);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void populateFields(Map<String, Object> rule) {
        nameField.setText(getStr(rule, "name"));
        murmurHashField.setText(getStr(rule, "murmur_hash"));
        md5Field.setText(getStr(rule, "md5"));
        String type = getStr(rule, "type");
        if (!type.isEmpty()) typeComboBox.setSelectedItem(type);
        infoField.setText(getStr(rule, "info"));
    }

    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入规则名称", "输入错误", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return false;
        }
        if (murmurHashField.getText().trim().isEmpty() && md5Field.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "MurmurHash3 和 MD5 至少填一个", "输入错误", JOptionPane.ERROR_MESSAGE);
            murmurHashField.requestFocus();
            return false;
        }
        return true;
    }

    public Map<String, Object> showDialog() {
        setVisible(true);
        return resultRule;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("ok".equals(e.getActionCommand())) {
            if (validateInput()) {
                resultRule = new HashMap<>();
                resultRule.put("name", nameField.getText().trim());
                resultRule.put("murmur_hash", murmurHashField.getText().trim());
                resultRule.put("md5", md5Field.getText().trim());
                resultRule.put("type", typeComboBox.getSelectedItem().toString().trim());
                resultRule.put("info", infoField.getText().trim());
                dispose();
            }
        } else {
            resultRule = null;
            dispose();
        }
    }

    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
