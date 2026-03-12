# IntelliJ 全局搜索插件

一个类似 VS Code 搜索视图的 IntelliJ IDEA 插件，提供强大的全文搜索和替换功能。

## 功能特性

### 搜索功能
- ✅ 全工程文本搜索
- ✅ 支持正则表达式
- ✅ 区分大小写选项
- ✅ 全词匹配选项
- ✅ 自定义搜索范围（工程/模块/目录）
- ✅ Include/Exclude 文件模式过滤
- ✅ 渐进式结果显示
- ✅ 按文件分组展示结果
- ✅ 双击跳转到匹配位置
- ✅ 高亮显示匹配文本

### 替换功能
- ✅ 全部替换功能
- ✅ 支持正则表达式替换
- ✅ 批量文件替换
- ✅ 替换前确认对话框

## 使用方法

### 打开搜索工具窗口
1. 使用快捷键：`Ctrl+Alt+Shift+F` (Windows/Linux) 或 `Cmd+Alt+Shift+F` (macOS)
2. 或者通过菜单：Main Menu → Open Project Search

### 搜索操作
1. 在搜索框中输入关键字
2. 选择搜索选项：
   - Match Case (Aa)：区分大小写
   - Match Whole Word (Ab)：全词匹配
   - Use Regex (.*)：使用正则表达式
3. 选择搜索范围：Project / Module / Directory
4. 可选：设置 Include/Exclude 文件模式
5. 按回车或点击 Search 按钮开始搜索

### 替换操作
1. 点击工具栏的 "Toggle Replace Mode" 按钮显示替换输入框
2. 在 Replace 框中输入替换文本
3. 执行搜索后，点击 "Replace All" 按钮
4. 确认替换操作

### 查看结果
- 结果按文件分组显示
- 点击文件名展开/折叠该文件的匹配结果
- 双击任意匹配行跳转到编辑器对应位置
- 匹配的文本会以粗体高亮显示

## 项目结构

```
src/main/kotlin/com/github/y1j2x34/intellijsearchplugin/
├── model/
│   └── SearchModels.kt          # 数据模型（SearchOptions, SearchResult, etc.）
├── services/
│   ├── ProjectSearchService.kt  # 搜索服��
│   └── ReplaceService.kt        # 替换服务
├── ui/
│   ├── SearchToolWindowFactory.kt    # 工具窗口工厂
│   ├── SearchToolWindowPanel.kt      # 主面板
│   ├── SearchFormPanel.kt            # 搜索表单
│   └── SearchResultsPanel.kt         # 结果展示
├── actions/
│   └── OpenSearchToolWindowAction.kt # 打开窗口的 Action
└── icons/
    └── PluginIcons.kt           # 图标资源
```

## 技术实现

### 核心技术
- **搜索引擎**：基于 IntelliJ Platform 的 `FilenameIndex` 和 `FileBasedIndex` API
- **异步处理**：使用 `ProgressManager` 和 `Task.Backgroundable` 实现后台搜索
- **文件操作**：使用 `WriteCommandAction` 确保文件修改的事务性
- **UI 组件**��使用 JetBrains UI 组件库（JBTextField, Tree, etc.）

### 性能优化
- 渐进式结果加载，避免 UI 阻塞
- 基于索引的文件搜索，而非全盘遍历
- 支持搜索过程中取消操作
- 文件类型白名单过滤

## 构建和运行

### 开发环境要求
- JDK 11+
- Gradle 7.3+
- IntelliJ IDEA 2020.3+

### 运行插件
```bash
./gradlew runIde
```

### 构建插件
```bash
./gradlew buildPlugin
```

生成的插件文件位于：`build/distributions/`

## 配置说明

### 默认排除模式
插件默认排除以下目录：
- `**/node_modules/**`
- `**/dist/**`
- `**/build/**`
- `**/.git/**`

### 支持的文件类型
默认搜索以下文本文件类型：
- 编程语言：java, kt, js, ts, tsx, jsx, py, go, rs, c, cpp, cs, php, rb
- 标记语言：html, xml, json, yaml, yml, md
- 配置文件：properties, gradle, sh, bash
- 样式文件：css, scss, sass
- 其他：txt, sql, vue

## 扩展点

### 未来可扩展功能
- [ ] 支持 Search Everywhere 集成
- [ ] 支持当前选中区域搜索
- [ ] 支持搜索历史记录
- [ ] 支持保存搜索结果快照
- [ ] 支持结构化搜索（基于 PSI）
- [ ] 支持单个匹配项替换
- [ ] 支持搜索结果导出

## 快捷键

| 操作 | Windows/Linux | macOS |
|------|---------------|-------|
| 打开搜索窗口 | `Ctrl+Alt+Shift+F` | `Cmd+Alt+Shift+F` |
| 执行搜索 | `Enter` | `Enter` |

## 许可证

本项目采用开源许可证。

## 贡献

欢迎提交 Issue 和 Pull Request！
