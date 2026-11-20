# 微光 · Android 安装包

把仓库根目录的 `index.html` 打成一个极小的原生 WebView 应用。没有 Capacitor、没有 AndroidX，也没有任何第三方运行时依赖，APK 只有几百 KB，完全离线。

## 构建

需要 JDK 17 和 Android SDK（`ANDROID_HOME` 指向 SDK，或放一个 `local.properties`）。

```powershell
cd android
.\gradlew.bat assembleRelease
```

产物：`android/app/build/outputs/apk/release/app-release.apk`

也可以只跑 `.\build-apk.ps1`（会构建并把安装包复制到 `android/dist/`，文件名带版本号）。

装到手机上：把 APK 传到手机，点开，允许「安装未知应用」。之后它就是一个独立的小应用，图标、启动画面和全屏房间都与网页版一致。

## 实现要点

- 网页资源在构建时从仓库根目录同步进 `assets/`，所以网页始终只有一份源文件；改完 `index.html` 重新打包即可。
- 页面通过 `https://appassets.androidplatform.net/` 这个虚拟源提供（拦截后从 assets 读取），这样 `IndexedDB`、`getUserMedia`、`MediaRecorder` 仍然是安全上下文，录音、录像、本地存储全部照常。
- `sw.js` 故意不打包，避免多一层缓存导致更新后仍是旧页面。
- 相机 / 麦克风权限在第一次使用时才申请，并转交给 WebView。
- 备份导出：WebView 不能下载 `blob:`，所以 `shell.js` 把导出的 JSON 分片交给原生，再用系统文件选择器保存；导入走标准文件选择器。
- 数据仍然只在本机。APK 没有申请存储权限，也没有任何上传代码。

## 签名

`keystore/weiguang.jks` 是故意提交进仓库的：Android 只允许相同签名的包覆盖安装，**如果换了签名，用户必须先卸载，本地记录也会一起消失**。请保留这个文件。密码和别名都是 `weiguang`。

## 图标

图标由根目录的 `icon-512.png` 生成：

```powershell
python tools/make_icons.py
```

## 版本

在 `app/build.gradle.kts` 里改 `versionCode` / `versionName`。每次发新版 `versionCode` 必须加一。
