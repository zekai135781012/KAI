# KAI Hook 模块

[![Android](https://img.shields.io/badge/Android-5.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7%2B-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个基于 Xposed 框架的 Android Hook 管理工具，采用 Jetpack Compose 构建现代化 UI，支持 Material You 动态取色与共享元素过渡动画。

## ✨ 功能特性

### 核心功能
- 🔧 **应用 Hook 管理** - 为每个应用独立配置 Hook 功能开关
- 🎨 **Material You 设计** - 完全支持 Android 12+ 动态取色（Monet）
- 🚀 **共享元素动画** - 应用列表到详情页的流畅过渡效果

### UI 亮点
| 组件 | 描述 |
|-----|------|
| `HookStatusBadge` | 状态指示器徽章，显示 Hook 运行状态 |
| `HookStatusDot` | 简洁的状态圆点指示器 |
| 级联入场动画 | 应用列表项依次渐入效果 |
| 共享元素过渡 | 应用图标/名称无缝过渡到详情页 |

## 🏗️ 项目架构

