# 更新日志

## 2026-05-06 (v3.0.1)

### 优化

- **路径收集命中次数改为按来源主机计数**

  原先命中次数为所有请求的累计总次数（`SUM(hit_count)`），同一主机的重复请求会导致计数膨胀，不能直观反映路径的分布广度。

  **优化**：将 `PathStore.getAllPaths()` 的聚合方式从 `SUM(hit_count)` 改为 `COUNT(DISTINCT host)`，命中次数现在表示有多少个不同的来源主机访问了该路径。

  涉及文件：
  - `PathStore.java` — `getAllPaths()` 查询聚合方式变更

## 2026-04-24

### Bug 修复

- **图标转为指纹规则后运行时规则未刷新**

  在「图标数据」面板点击「转为指纹规则」后，新规则仅写入 YAML 文件，`YamlConfigLoader` 缓存和 `IconHashRuleLoader` 内存索引未被刷新，导致后续新流量的 favicon 匹配仍使用旧规则，需手动到「指纹管理」点击「重新加载」才能生效。

  **修复**：在 `IconDataPanel` 添加 `onRuleAddedCallback` 回调，由 `BurpExtender` 注册，添加规则后自动执行 `YamlConfigLoader.invalidateCache()` + `IconHashRuleLoader.invalidate()` + `loadRules()`，同时刷新「指纹管理」面板的 Icon Hash 规则表格。

  涉及文件：
  - `IconDataPanel.java` — 新增回调字段与 setter，`convertToRule()` 中触发回调
  - `BurpExtender.java` — 注册回调，串联缓存失效与规则重载
  - `FingerprintPanel.java` — `loadIconHashRules()` 可见性从 `private` 改为 `public`

- **图标转为指纹规则后「匹配结果」列未更新**

  转为规则后，图标数据表中该行的「匹配结果」列仍为空或旧值，因为 `IconHashStore.saveIcon()` 使用 `INSERT OR IGNORE`，已存在的图标行不会被更新。

  **修复**：在 `IconHashStore` 新增 `updateMatchResult()` 方法；在 `IconDataPanel.convertToRule()` 中添加规则后，将规则名称写入数据库并立即刷新表格对应行的「匹配结果」列。若已有匹配结果则追加拼接。

  涉及文件：
  - `IconHashStore.java` — 新增 `updateMatchResult(String murmurHash, String matchResult)`
  - `IconDataPanel.java` — `convertToRule()` 中更新数据库与 UI 表格
