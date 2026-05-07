/**
 * 指纹规则编辑对话框
 * 提供图形化界面用于添加和编辑指纹规则
 * 支持RouteVulScanPro1的YAML格式规则编辑
 * 
 * @author OneScan Team
 * @version 2.0
 */
package burp.tdou.fingerscan.ui.widget;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class FingerprintRuleDialog extends JDialog implements ActionListener {
    
    private Map<String, Object> originalRule;
    private Map<String, Object> resultRule;
    
    // UI组件
    private JTextField idField;
    private JTextField nameField;
    private JCheckBox enabledCheckBox;
    private JComboBox<String> methodComboBox;
    private JTextField urlField;
    private JTextArea regexArea;
    private JComboBox<String> typeComboBox;
    private JComboBox<String> stateComboBox;
    private JTextArea infoArea;
    
    // 预定义选项
    private static final String[] HTTP_METHODS = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH"};
    private static final String[] RULE_TYPES = {"web", "api", "admin", "backup", "config", "debug", "other"};
    private static final String[] RULE_STATES = {"active", "inactive", "testing", "deprecated"};
    
    /**
     * 构造函数
     * @param parent 父窗口
     * @param title 对话框标题
     * @param rule 要编辑的规则（null表示添加新规则）
     */
    public FingerprintRuleDialog(JFrame parent, String title, Map<String, Object> rule) {
        super(parent, title, true);
        this.originalRule = rule;
        this.resultRule = null;
        
        initializeUI();
        if (rule != null) {
            populateFields(rule);
        }
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * 初始化UI界面
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 创建表单面板
        mainPanel.add(createFormPanel(), BorderLayout.CENTER);
        
        // 创建按钮面板
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        
        add(mainPanel);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
    }
    
    /**
     * 创建表单面板
     */
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // ID字段
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        idField = new JTextField(10);
        idField.setEditable(false); // ID通常不可编辑
        panel.add(idField, gbc);
        
        row++;
        
        // 名称字段
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("名称:*"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        nameField = new JTextField(20);
        panel.add(nameField, gbc);
        
        row++;
        
        // 启用状态
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("启用:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        enabledCheckBox = new JCheckBox("启用此规则");
        enabledCheckBox.setSelected(true);
        panel.add(enabledCheckBox, gbc);
        
        row++;
        
        // HTTP方法
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("HTTP方法:*"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        methodComboBox = new JComboBox<>(HTTP_METHODS);
        methodComboBox.setEditable(true);
        panel.add(methodComboBox, gbc);
        
        row++;
        
        // URL路径
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("URL路径:*"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        urlField = new JTextField(30);
        urlField.setToolTipText("例如: /admin/login.php, /api/v1/status");
        panel.add(urlField, gbc);
        
        row++;
        
        // 正则表达式
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("正则表达式:*"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.3;
        regexArea = new JTextArea(4, 30);
        regexArea.setLineWrap(true);
        regexArea.setWrapStyleWord(true);
        regexArea.setToolTipText("用于匹配响应内容的正则表达式");
        JScrollPane regexScrollPane = new JScrollPane(regexArea);
        regexScrollPane.setBorder(new TitledBorder("正则表达式"));
        panel.add(regexScrollPane, gbc);
        
        row++;
        
        // 类型
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("类型:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        typeComboBox = new JComboBox<>(RULE_TYPES);
        typeComboBox.setEditable(true);
        panel.add(typeComboBox, gbc);
        
        row++;
        
        // 状态
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("状态:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        stateComboBox = new JComboBox<>(RULE_STATES);
        stateComboBox.setEditable(true);
        panel.add(stateComboBox, gbc);
        
        row++;
        
        // 描述信息
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("描述:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.3;
        infoArea = new JTextArea(3, 30);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setToolTipText("规则的详细描述信息");
        JScrollPane infoScrollPane = new JScrollPane(infoArea);
        infoScrollPane.setBorder(new TitledBorder("描述信息"));
        panel.add(infoScrollPane, gbc);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JButton testButton = new JButton("测试正则");
        testButton.setActionCommand("test-regex");
        testButton.addActionListener(this);
        panel.add(testButton);
        
        JButton okButton = new JButton("确定");
        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        panel.add(okButton);
        
        JButton cancelButton = new JButton("取消");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        panel.add(cancelButton);
        
        return panel;
    }
    
    /**
     * 填充表单字段
     */
    private void populateFields(Map<String, Object> rule) {
        idField.setText(rule.get("id") != null ? rule.get("id").toString() : "");
        nameField.setText(rule.get("name") != null ? rule.get("name").toString() : "");
        
        Object loaded = rule.get("loaded");
        enabledCheckBox.setSelected(loaded != null && (Boolean) loaded);
        
        String method = rule.get("method") != null ? rule.get("method").toString() : "GET";
        methodComboBox.setSelectedItem(method);
        
        urlField.setText(rule.get("url") != null ? rule.get("url").toString() : "");
        regexArea.setText(rule.get("re") != null ? rule.get("re").toString() : "");
        
        String type = rule.get("type") != null ? rule.get("type").toString() : "web";
        typeComboBox.setSelectedItem(type);
        
        String state = rule.get("state") != null ? rule.get("state").toString() : "active";
        stateComboBox.setSelectedItem(state);
        
        infoArea.setText(rule.get("info") != null ? rule.get("info").toString() : "");
    }
    
    /**
     * 验证表单输入
     */
    private boolean validateInput() {
        // 检查必填字段
        if (nameField.getText().trim().isEmpty()) {
            showError("请输入规则名称！");
            nameField.requestFocus();
            return false;
        }
        
        if (methodComboBox.getSelectedItem() == null || 
            methodComboBox.getSelectedItem().toString().trim().isEmpty()) {
            showError("请选择HTTP方法！");
            methodComboBox.requestFocus();
            return false;
        }
        
        if (urlField.getText().trim().isEmpty()) {
            showError("请输入URL路径！");
            urlField.requestFocus();
            return false;
        }
        
        if (regexArea.getText().trim().isEmpty()) {
            showError("请输入正则表达式！");
            regexArea.requestFocus();
            return false;
        }
        
        // 验证正则表达式
        try {
            Pattern.compile(regexArea.getText().trim());
        } catch (Exception e) {
            showError("正则表达式格式错误：" + e.getMessage());
            regexArea.requestFocus();
            return false;
        }
        
        // 验证URL路径格式
        String url = urlField.getText().trim();
        if (!url.startsWith("/")) {
            showError("URL路径必须以 '/' 开头！");
            urlField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * 显示错误消息
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "输入错误", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * 创建规则对象
     */
    private Map<String, Object> createRule() {
        Map<String, Object> rule = new HashMap<>();
        
        // 如果是编辑现有规则，保留原ID
        if (originalRule != null && originalRule.get("id") != null) {
            rule.put("id", originalRule.get("id"));
        }
        
        rule.put("name", nameField.getText().trim());
        rule.put("loaded", enabledCheckBox.isSelected());
        rule.put("method", methodComboBox.getSelectedItem().toString().trim());
        rule.put("url", urlField.getText().trim());
        rule.put("re", regexArea.getText().trim());
        rule.put("type", typeComboBox.getSelectedItem().toString().trim());
        rule.put("state", stateComboBox.getSelectedItem().toString().trim());
        rule.put("info", infoArea.getText().trim());
        
        return rule;
    }
    
    /**
     * 显示对话框并返回结果
     */
    public Map<String, Object> showDialog() {
        setVisible(true);
        return resultRule;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        
        switch (command) {
            case "test-regex":
                testRegex();
                break;
            case "ok":
                if (validateInput()) {
                    resultRule = createRule();
                    dispose();
                }
                break;
            case "cancel":
                resultRule = null;
                dispose();
                break;
        }
    }
    
    /**
     * 测试正则表达式
     */
    private void testRegex() {
        String regex = regexArea.getText().trim();
        if (regex.isEmpty()) {
            showError("请先输入正则表达式！");
            return;
        }
        
        try {
            Pattern.compile(regex);
            
            // 显示正则测试对话框
            String testText = JOptionPane.showInputDialog(this, 
                "请输入要测试的文本内容：", 
                "正则表达式测试", 
                JOptionPane.PLAIN_MESSAGE);
            
            if (testText != null) {
                boolean matches = Pattern.compile(regex).matcher(testText).find();
                String result = matches ? "匹配成功！" : "匹配失败！";
                JOptionPane.showMessageDialog(this, result, "测试结果", 
                    matches ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            }
            
        } catch (Exception ex) {
            showError("正则表达式格式错误：" + ex.getMessage());
        }
    }
}