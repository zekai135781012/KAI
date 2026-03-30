readme_content = """# KAI - Xposed Hook 模块

一个基于 Xposed 框架的 Android Hook 模块，提供应用 VIP 解锁、弹窗屏蔽、文本替换等功能。

## 功能特性

- **VIP 解锁**: 支持多邻国、快对作业、Fake Location、小X分身等 12+ 应用
- **弹窗屏蔽**: 智能拦截指定关键词弹窗
- **文本替换**: 动态替换应用内文本内容
- **规则管理**: 可视化配置，独立生效不干扰

## 支持应用

| 应用 | 功能 |
|------|------|
| 多邻国 | MAX会员、去广告、无限红心 |
| 快对作业 | VIP解锁、图片导出、去限制 |
| Fake Location | 专业版解锁 |
| 小X分身 | 本地会员 |
| 安卓清理君 | 免登录会员 |
| 一木记账 | 永久会员 |
| 小白录屏 | 本地会员 |
| 葫芦侠 | 主题解锁、评论恢复 |
| 倒数日 | 会员解锁 |
| Soul | 去聊天限制 |
| 绿茶VPN | 钻石VIP |

## 技术栈

- **框架**: Xposed / LSPosed
- **UI**: Jetpack Compose + Material3
- **语言**: Kotlin + Java
- **最低版本**: Android 8.0 (API 26)

## 项目结构

```
com.example.kai/
├── HookEntry.java          # Xposed 入口
├── MainActivity.kt         # Compose 主界面
├── ConfigManager.kt        # 规则配置管理
├── ComponentBlocker.kt     # 弹窗/文本拦截核心
├── ui/                     # Compose 界面组件
│   ├── HomeScreen.kt       # 首页
│   ├── AppsListScreen.kt   # 适配应用列表
│   └── ExpandScreen.kt     # 应用管理/规则配置
└── hooks/                  # 各应用 Hook 实现
    ├── Duolingo.java
    ├── KuaiDuiZuoYe.java
    └── ...
```

## 使用说明

1. 安装 LSPosed 框架并启用模块
2. 在模块作用域中勾选目标应用
3. 重启目标应用即可生效
4. 部分应用支持设置界面（如快对作业在设置页长按）

## 免责声明

仅供学习研究，请勿用于商业用途。使用造成的后果作者不承担责任。

## 鸣谢

- MT论坛、挽梦遗酒、dusheling、OctoberSama、JiGuro

---

> 作者: 探长鸽鸽 | QQ群: 709496957
"""

print(readme_content)