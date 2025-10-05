# 媒体浏览器 (MediaExplorer)

一个功能强大的 Android 应用，用于扫描和浏览手机中的所有照片和视频文件，包括第三方应用缓存的媒体文件。

## 功能特性

- ✅ **全面扫描**：扫描设备上所有的照片和视频文件
- ✅ **第三方应用支持**：可以访问第三方应用缓存的图片和视频
- ✅ **智能分类**：支持按照片、视频或全部进行过滤
- ✅ **网格展示**：以美观的网格形式展示媒体文件
- ✅ **详细信息**：查看文件的创建时间、大小、分辨率等详细信息
- ✅ **图片预览**：全屏沉浸式查看图片
- ✅ **视频播放**：内置视频播放器，支持播放/暂停、进度控制
- ✅ **视频缩略图**：自动提取视频帧作为缩略图
- ✅ **按需信息显示**：点击信息按钮展开详细信息，默认全屏显示媒体
- ✅ **现代化 UI**：使用 Jetpack Compose 构建的现代化界面
- ✅ **权限管理**：智能处理 Android 各版本的存储权限

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **架构**: MVVM (Model-View-ViewModel)
- **图片加载**: Coil
- **视频播放**: Media3 ExoPlayer
- **权限处理**: Accompanist Permissions
- **最低支持版本**: Android 7.0 (API 24)
- **目标版本**: Android 14 (API 34)

## 项目结构

```
MediaExplorer/
├── app/
│   ├── build.gradle                    # 应用级 Gradle 配置
│   ├── proguard-rules.pro             # ProGuard 混淆规则
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml     # 应用清单文件
│           ├── java/com/mediaexplorer/
│           │   ├── MainActivity.kt            # 主 Activity
│           │   ├── MediaItem.kt               # 媒体项数据模型
│           │   ├── MediaFilter.kt             # 过滤器枚举
│           │   ├── MediaScanner.kt            # 媒体扫描器
│           │   ├── MediaViewModel.kt          # ViewModel
│           │   ├── MediaExplorerApp.kt        # 主界面 UI
│           │   ├── MediaDetailDialog.kt       # 详情对话框
│           │   ├── VideoPlayer.kt             # 视频播放器组件
│           │   └── PermissionScreen.kt        # 权限请求界面
│           └── res/
│               ├── drawable/
│               │   └── ic_launcher.xml        # 应用图标
│               ├── values/
│               │   ├── strings.xml            # 字符串资源
│               │   └── themes.xml             # 主题配置
│               └── xml/
│                   ├── backup_rules.xml       # 备份规则
│                   └── data_extraction_rules.xml
├── build.gradle                        # 项目级 Gradle 配置
├── settings.gradle                     # Gradle 设置
├── gradle.properties                   # Gradle 属性
├── .gitignore                         # Git 忽略文件
└── README.md                          # 本文档
```

## 核心组件说明

### 1. MediaItem.kt
定义了媒体文件的数据模型，包含：
- 文件 ID、URI、路径
- 文件名、MIME 类型、大小
- 创建和修改时间
- 分辨率（宽度和高度）
- 视频时长
- 格式化的显示信息

### 2. MediaScanner.kt
负责扫描设备上的媒体文件：
- 使用 `MediaStore` API 查询照片和视频
- 支持异步扫描，不阻塞 UI 线程
- 自动按修改时间排序
- 获取完整的文件元数据

### 3. MediaViewModel.kt
管理应用的状态和数据：
- 加载和刷新媒体列表
- 处理过滤器切换（全部/照片/视频）
- 使用 Kotlin Flow 管理响应式数据流
- 协程处理异步操作

### 4. UI 组件
- **MainActivity**: 处理权限请求和应用入口
- **MediaExplorerApp**: 主界面，包含顶部栏、过滤标签和媒体网格
- **MediaDetailDialog**: 全屏详情对话框，显示媒体预览和详细信息
- **PermissionScreen**: 权限请求引导界面

## 安装说明

### 前置要求

1. **Android Studio**: 需要 Android Studio Giraffe (2023.1.1) 或更高版本
2. **JDK**: JDK 17 或更高版本
3. **Android SDK**: 
   - 编译 SDK: API 34 (Android 14)
   - 最低 SDK: API 24 (Android 7.0)
4. **Gradle**: 8.0 或更高版本（通常由 Android Studio 自动管理）

### 构建步骤

1. **克隆或下载项目**
   ```bash
   cd /Users/yuchenzhang/cs/playground/MediaExplorer
   ```

2. **打开项目**
   - 启动 Android Studio
   - 选择 "Open an Existing Project"
   - 导航到 `MediaExplorer` 文件夹并打开

3. **同步 Gradle**
   - Android Studio 会自动提示同步 Gradle
   - 点击 "Sync Now" 等待依赖下载完成
   - 或手动点击菜单栏：File → Sync Project with Gradle Files

4. **配置签名（可选）**
   - 如果需要发布版本，在 `app/build.gradle` 中配置签名信息

5. **运行应用**
   
   **方式一：使用真机**
   - 将 Android 手机连接到电脑
   - 在手机上启用"开发者选项"和"USB 调试"
   - 在 Android Studio 中选择设备
   - 点击运行按钮 (▶) 或按 Shift+F10
   
   **方式二：使用模拟器**
   - 在 Android Studio 中打开 AVD Manager
   - 创建一个新的虚拟设备（推荐 Pixel 5, API 34）
   - 启动模拟器
   - 点击运行按钮 (▶)

### 构建 APK

**Debug 版本：**
```bash
./gradlew assembleDebug
```
输出位置: `app/build/outputs/apk/debug/app-debug.apk`

**Release 版本：**
```bash
./gradlew assembleRelease
```
输出位置: `app/build/outputs/apk/release/app-release.apk`

## 使用说明

### 首次启动

1. **授予权限**
   - 首次启动时，应用会请求存储权限
   - Android 11+ 需要授予"所有文件访问权限"
   - Android 13+ 需要授予"照片和视频"权限
   - 点击"授予权限"按钮，在系统设置中开启权限

2. **媒体扫描**
   - 权限授予后，应用会自动扫描设备上的所有媒体文件
   - 扫描过程可能需要几秒到几分钟，取决于文件数量

### 主要功能使用

#### 1. 浏览媒体
- 主界面以 3 列网格形式展示所有媒体文件
- 图片直接显示缩略图
- 视频显示缩略图 + 播放按钮图标 + 时长标签

#### 2. 过滤媒体
使用顶部的标签页进行过滤：
- **全部**: 显示所有照片和视频
- **照片**: 仅显示图片文件
- **视频**: 仅显示视频文件

#### 3. 查看详情（全新设计！）
- 点击任意媒体项打开全屏详情页
- **媒体内容**：占满整个屏幕
  - **图片**：全屏沉浸式查看
  - **视频**：内置播放器，可播放/暂停、调整进度、音量控制
- **浮动按钮**：
  - 左上角 ✕ 关闭按钮
  - 右上角 ℹ️ 信息按钮
- **详细信息**：点击信息按钮后从底部滑出
  - 文件名（大标题）
  - 关键信息卡片：类型 🎬、大小 💾、分辨率 📐、时长 ⏱️
  - 详细信息：创建时间、文件路径、MIME 类型
  - 再次点击信息按钮可收起面板

#### 4. 播放视频
- 点击视频项进入详情页
- 视频会显示在播放器中
- 使用播放器控制条可以：
  - 播放/暂停
  - 拖动进度条跳转
  - 调整音量
  - 全屏播放

#### 5. 刷新列表
- 点击右上角的刷新按钮 (🔄) 重新扫描媒体文件
- 用于在添加新文件后更新列表

### 支持的文件格式

**图片格式：**
- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- WebP (.webp)
- BMP (.bmp)
- HEIF/HEIC (.heif, .heic) - Android 9+

**视频格式：**
- MP4 (.mp4)
- 3GP (.3gp)
- WebM (.webm)
- MKV (.mkv)
- AVI (.avi) - 部分支持
- MOV (.mov) - 部分支持

## 权限说明

应用需要以下权限才能正常工作：

| 权限 | 用途 | 必需性 |
|------|------|--------|
| `READ_EXTERNAL_STORAGE` | Android 12 及以下读取存储 | 必需（Android ≤12） |
| `READ_MEDIA_IMAGES` | Android 13+ 读取图片 | 必需（Android ≥13） |
| `READ_MEDIA_VIDEO` | Android 13+ 读取视频 | 必需（Android ≥13） |
| `MANAGE_EXTERNAL_STORAGE` | 访问所有文件（含第三方应用缓存） | 可选 |

**注意**：`MANAGE_EXTERNAL_STORAGE` 权限可以让应用访问第三方应用的缓存文件，但此权限需要在系统设置中手动授予。

## 常见问题

### Q1: 应用无法显示某些图片或视频？
**A**: 确保已授予所有必要的存储权限。在 Android 11+ 上，需要在设置中授予"所有文件访问权限"。

### Q2: 找不到第三方应用的图片？
**A**: 第三方应用的缓存图片通常需要 `MANAGE_EXTERNAL_STORAGE` 权限。授予此权限后，点击刷新按钮重新扫描。

### Q3: 视频无法播放？
**A**: 应用已内置 ExoPlayer 视频播放器。点击视频项进入详情页即可播放。支持大多数常见视频格式（MP4、3GP、WebM 等）。如果某些视频无法播放，可能是编码格式不支持。

### Q4: 扫描速度很慢？
**A**: 如果设备上有大量媒体文件（数千个），首次扫描可能需要较长时间。后续启动会更快。

### Q5: 如何更新媒体列表？
**A**: 点击右上角的刷新按钮，或关闭应用重新打开。

## 性能优化建议

1. **大量文件处理**：如果设备上有数万个媒体文件，可以考虑添加分页加载
2. **缓存优化**：Coil 会自动缓存图片，但可以调整缓存大小
3. **内存优化**：使用 LazyVerticalGrid 实现列表的懒加载，避免一次性加载所有项

## 开发计划

未来版本可能添加的功能：

- [x] 视频播放功能（内置播放器）✅ 已完成
- [ ] 图片编辑功能
- [ ] 搜索功能
- [ ] 按文件夹分组
- [ ] 收藏夹功能
- [ ] 导出/分享功能
- [ ] 暗黑模式支持
- [ ] 多选删除功能
- [ ] 更多排序选项（按大小、名称等）

## 技术支持

如遇到问题或有功能建议，请通过以下方式联系：
- 在项目中提交 Issue
- 发送邮件反馈

## 许可证

本项目采用 MIT 许可证。详见 LICENSE 文件。

## 致谢

本项目使用了以下开源库：
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Google 官方 UI 框架
- [Coil](https://coil-kt.github.io/coil/) - 图片加载库
- [Media3](https://developer.android.com/media/media3) - 媒体播放框架
- [Accompanist](https://google.github.io/accompanist/) - Compose 辅助库

---

**版本**: 1.0.0  
**更新日期**: 2025-10-03  
**作者**: MediaExplorer Team


