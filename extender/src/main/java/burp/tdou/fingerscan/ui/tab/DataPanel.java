package burp.tdou.fingerscan.ui.tab;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.tdou.fingerscan.core.ScanResult;
import burp.tdou.fingerscan.core.rule.MatchResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static burp.api.montoya.ui.editor.EditorOptions.READ_ONLY;

/**
 * 扫描记录面板 — 展示所有请求历史 + 请求/响应查看
 */
public class DataPanel extends JPanel {

    private JTable dataTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JLabel statusLabel;
    private JTextField filterField;
    private int counter = 0;

    private final List<ScanResult> records = new ArrayList<>();

    // Burp 消息编辑器（延迟初始化）
    private HttpRequestEditor requestEditor;
    private HttpResponseEditor responseEditor;
    private JComponent requestPlaceholder;
    private JComponent responsePlaceholder;
    private JSplitPane editorSplitPane;

    private static final String[] COLUMNS = {
            "#", "来源", "方法", "主机", "路径", "状态码", "长度", "指纹", "时间"
    };

    public DataPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        add(createControlPanel(), BorderLayout.NORTH);

        // 上下分割：上面表格，下面请求/响应
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setTopComponent(createTablePanel());
        mainSplit.setBottomComponent(createEditorPanel());
        mainSplit.setDividerLocation(300);
        mainSplit.setResizeWeight(0.5);
        add(mainSplit, BorderLayout.CENTER);

        add(createStatusPanel(), BorderLayout.SOUTH);
    }

    /**
     * 用 Montoya API 初始化请求/响应查看器
     * 由 BurpExtender 在 initView 之后调用
     */
    public void initEditors(MontoyaApi api) {
        if (api == null) return;
        requestEditor = api.userInterface().createHttpRequestEditor(READ_ONLY);
        responseEditor = api.userInterface().createHttpResponseEditor(READ_ONLY);
        // 替换占位组件
        editorSplitPane.setLeftComponent(requestEditor.uiComponent());
        editorSplitPane.setRightComponent(responseEditor.uiComponent());
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("过滤:"));
        filterField = new JTextField(20);
        filterField.addActionListener(e -> applyFilter());
        panel.add(filterField);
        JButton clearBtn = new JButton("清空");
        clearBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            records.clear();
            counter = 0;
            clearEditors();
            updateStatus();
        });
        panel.add(clearBtn);
        return panel;
    }

    private JScrollPane createTablePanel() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0 || col == 5 || col == 6) return Integer.class;
                return String.class;
            }
        };
        dataTable = new JTable(tableModel);
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        dataTable.getTableHeader().setReorderingAllowed(false);

        int[] widths = {50, 70, 60, 200, 250, 60, 70, 150, 80};
        for (int i = 0; i < widths.length && i < dataTable.getColumnCount(); i++) {
            dataTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        tableSorter = new TableRowSorter<>(tableModel);
        dataTable.setRowSorter(tableSorter);

        // 选中行时加载请求/响应
        dataTable.getSelectionModel().addListSelectionListener(this::onRowSelected);

        dataTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            @Override
            public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
        });

        return new JScrollPane(dataTable);
    }

    private JComponent createEditorPanel() {
        requestPlaceholder = new JTextArea("请求");
        responsePlaceholder = new JTextArea("响应");
        editorSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(requestPlaceholder),
                new JScrollPane(responsePlaceholder));
        editorSplitPane.setDividerLocation(400);
        editorSplitPane.setResizeWeight(0.5);
        return editorSplitPane;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("共 0 条记录");
        panel.add(statusLabel);
        return panel;
    }

    private void onRowSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int viewRow = dataTable.getSelectedRow();
        if (viewRow < 0) {
            clearEditors();
            return;
        }
        int modelRow = dataTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= records.size()) {
            clearEditors();
            return;
        }
        ScanResult result = records.get(modelRow);
        loadEditors(result);
    }

    private void loadEditors(ScanResult result) {
        HttpRequestResponse reqResp = result.getReqResp();
        if (reqResp == null) {
            clearEditors();
            return;
        }
        if (requestEditor != null) {
            HttpRequest req = reqResp.request();
            if (req != null) {
                requestEditor.setRequest(req);
            } else {
                requestEditor.setRequest(HttpRequest.httpRequest(""));
            }
        }
        if (responseEditor != null) {
            HttpResponse resp = reqResp.response();
            if (resp != null) {
                responseEditor.setResponse(resp);
            } else {
                responseEditor.setResponse(HttpResponse.httpResponse(""));
            }
        }
    }

    private void clearEditors() {
        if (requestEditor != null) {
            requestEditor.setRequest(HttpRequest.httpRequest(""));
        }
        if (responseEditor != null) {
            responseEditor.setResponse(HttpResponse.httpResponse(""));
        }
    }

    /**
     * 添加扫描记录
     */
    public void addRecord(ScanResult result) {
        if (result == null) return;
        SwingUtilities.invokeLater(() -> {
            counter++;
            records.add(result);
            String fingerprint = "";
            if (result.hasMatch()) {
                StringBuilder sb = new StringBuilder();
                for (MatchResult mr : result.getMatchResults()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(mr.getRuleName());
                }
                fingerprint = sb.toString();
            }
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date(result.getTimestamp()));
            tableModel.addRow(new Object[]{
                    counter,
                    result.getFrom() != null ? result.getFrom() : "",
                    result.getMethod() != null ? result.getMethod() : "",
                    result.getHost() != null ? result.getHost() : "",
                    result.getUrl() != null ? result.getUrl() : "",
                    result.getStatus(),
                    result.getLength(),
                    fingerprint,
                    time
            });
            updateStatus();
        });
    }

    private void applyFilter() {
        String text = filterField.getText().trim();
        if (text.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            tableSorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(text)));
        }
        updateStatus();
    }

    private void updateStatus() {
        int total = tableModel.getRowCount();
        int visible = dataTable.getRowCount();
        statusLabel.setText(total == visible
                ? "共 " + total + " 条记录"
                : "显示 " + visible + " / " + total + " 条记录");
    }

    private void showMenu(MouseEvent e) {
        int[] rows = dataTable.getSelectedRows();
        if (rows.length == 0) return;
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copyUrl = new JMenuItem("复制URL");
        copyUrl.addActionListener(ev -> {
            StringBuilder sb = new StringBuilder();
            for (int row : rows) {
                int mr = dataTable.convertRowIndexToModel(row);
                String host = (String) tableModel.getValueAt(mr, 3);
                String path = (String) tableModel.getValueAt(mr, 4);
                if (sb.length() > 0) sb.append("\n");
                sb.append(host).append(path);
            }
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(sb.toString()), null);
        });
        menu.add(copyUrl);
        JMenuItem del = new JMenuItem("删除选中");
        del.addActionListener(ev -> {
            for (int i = rows.length - 1; i >= 0; i--) {
                int mr = dataTable.convertRowIndexToModel(rows[i]);
                tableModel.removeRow(mr);
                records.remove(mr);
            }
            updateStatus();
        });
        menu.add(del);
        menu.show(dataTable, e.getX(), e.getY());
    }
}
