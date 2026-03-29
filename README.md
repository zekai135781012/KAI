# KAI - Xposed Hook 模块

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-26%2B-orange.svg)](https://developer.android.com/studio/releases/platforms)

> 一个基于 Xposed 框架的 Android Hook 模块，采用 Jetpack Compose 构建现代化 UI 界面。

## ✨ 特性

- 🎨 **Material Design 3** - 采用 Jetpack Compose 构建的现代化 UI
- 🔧 **模块化 Hook 架构** - 支持多应用独立 Hook 实现
- 📱 **应用管理界面** - 直观的适配应用列表与状态展示
- 🛡️ **弹窗屏蔽系统** - 基于规则引擎的 Dialog 拦截功能
- ⚙️ **配置持久化** - 通过 ContentProvider 实现应用与 Xposed 进程间配置共享

## 📋 适配应用列表

| 应用名称 | 包名 | Hook 功能 |
|---------|------|----------|
| 小x分身 | `com.bly.dkplat` | 解锁本地会员、去广告 |
| 安卓清理君 | `com.magicalstory.cleaner` | 解锁本地会员、免登录 |
| Fake Location | `com.lerist.fakelocation` | 解锁专业版 |
| 多邻国 | `com.duolingo` | 去广告、MAX会员、无限红心 |
| TapTap | `com.taptap` | 伪造购买、破解授权 |
| 葫芦侠 | `com.huluxia.gametools` | 解锁主题、恢复评论 |
| 一木记账 | `com.wangc.bill` | 解锁本地会员 |
| 小白录屏 | `com.xiaobai.screen.record` | 解锁本地会员 |
| 快对作业 | `com.kuaiduizuoye.scan` | 解锁VIP、去广告、图片导出 |
| 绿茶VPN | `com.abglvcha.main` | 解锁钻石VIP |
| Soul | `cn.soulapp.android` | 去聊天限制、消息撤回 |
| 倒数日 | `com.clover.daysmatter` | 解锁会员 |

## 🏗️ 项目架构

