# Awesome ToDo

Awesome ToDo 是一款完全离线的安卓待办与专注计时应用。它以“创建待办 → 专注计时 → 查看统计”为核心闭环，使用原创的 Material 3 界面，不包含第三方产品的品牌或素材。

## 功能

- 待办创建、编辑、完成、恢复与删除
- 1–180 分钟自定义时长及 6 套渐变卡片主题
- 支持暂停、继续、提前结束和放弃的前后台专注计时
- 累计、当日、日/周/月/自定义区间及年度统计
- 完整专注记录和明确的计入状态
- 版本化 JSON 备份与整体恢复
- 简体中文、系统亮色/暗色主题、Android 10+

## 专注统计规则

- 自然完成始终计入次数和完整计划分钟。
- 提前结束且实际专注不超过 10 分钟：不计次数和时长，但保留记录。
- 提前结束且实际专注超过 10 分钟：计 1 次，时长向下取完整分钟。
- 放弃不计次数和时长，但保留记录。
- 活跃日均值 = 有效专注总分钟 ÷ 有有效记录的自然日数量。

## 构建

要求 JDK 17+ 和 Android SDK 35：

```powershell
./gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 架构

- 单 Activity + Jetpack Compose + Material 3
- Room 保存待办、专注记录和当前计时状态
- DataStore 保存应用设置
- 前台 Service 保证后台/锁屏计时，按时间锚点计算而非每秒写库
- Compose Canvas 绘制统计图表
- Storage Access Framework 负责备份文件的导出与导入

当前运行计时作为单例持久化；暂停、继续和结束会事务化更新。待办删除不会级联删除历史记录，专注标题使用快照保存。

## 备份格式

备份为 `schemaVersion = 1` 的 JSON 文件，包含待办、专注记录与设置。导入前会完成结构、枚举、日期、时区和范围校验；验证失败不会修改数据库。计时进行中禁止导入或导出。

## 测试

- JVM 单元测试覆盖 10 分钟边界、完整分钟取整、暂停和统计聚合。
- Android 仪器测试覆盖 Room 数据保留和待办创建流程。
- CI 对提交执行单元测试、lint 和 Debug APK 构建；里程碑标签与手动运行会上传 APK。

## 许可

[MIT License](LICENSE)
