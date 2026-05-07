package burp.tdou.fingerscan.ui.tab.config;

import burp.tdou.fingerscan.common.Config;
import burp.tdou.fingerscan.common.L;
import burp.tdou.fingerscan.common.OnDataChangeListener;
import burp.tdou.fingerscan.ui.base.BaseConfigTab;
import burp.tdou.fingerscan.ui.widget.payloadlist.ProcessingItem;
import burp.tdou.fingerscan.ui.widget.payloadlist.SimpleProcessingList;

import java.util.ArrayList;

/**
 * Payload Processing 设置（已移除字典选择，仅保留处理规则）
 */
public class PayloadTab extends BaseConfigTab implements OnDataChangeListener {

    private SimpleProcessingList mProcessList;

    @Override
    protected void initView() {
        // Payload Processing 规则列表
        mProcessList = new SimpleProcessingList(Config.getPayloadProcessList());
        mProcessList.setActionCommand("payload-process-list-view");
        mProcessList.setOnDataChangeListener(this);
        addConfigItem(L.get("payload_processing"), L.get("payload_processing_sub_title"), mProcessList);
    }

    @Override
    public String getTitleName() {
        return L.get("tab_name.payload");
    }

    @Override
    public void onDataChange(String action) {
        if ("payload-process-list-view".equals(action)) {
            ArrayList<ProcessingItem> list = mProcessList.getDataList();
            Config.put(Config.KEY_PAYLOAD_PROCESS_LIST, list);
        }
    }
}
