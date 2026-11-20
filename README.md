# 微光

> 那些亮了一下的瞬间，都收在这里。
>
> *A quiet place for the moments that lit up, just for a second.*

一个人也能用的快乐记录仪。不评分，不分类，不打卡——只是把「刚才有点开心」这件事，轻轻放进去。

*A small, unjudging journal for happy moments. No scores, no categories, no streaks. You just place "that felt good, just now" into it, gently.*

---

## 怎么开始 · How to begin

打开这个页面就可以用，手机和电脑都行：

**https://fluffytighnari.github.io/SlightLight_HappyMomentRecoder/**

*Open the link above on your phone or on a computer, and you are already inside.*

- 手机上想更顺手：iOS 用「分享 → 添加到主屏幕」，Android 用菜单里的「安装应用」。之后它就像一个很小的小应用，点开就是那间房间。
  *On a phone, add it to your home screen — Share → Add to Home Screen on iOS, or Install app from the menu on Android. It then opens like a tiny app, straight into the room.*
- 如果 Android 浏览器里找不到「安装应用」，或者加到了主屏却打不开，也可以直接装下面那个很小的 APK。
  *If the Android browser has no "Install app" entry, or the shortcut refuses to open, use the tiny APK described below instead.*
- 不用注册，不用登录。打开就有光。
  *No account, no sign-in. The light is already there.*

---

## 三种手势 · Three gestures

| 手势 · Gesture | 你会得到 · What you get |
|---|---|
| 轻点「此刻」<br>*Tap「此刻」· This moment* | 一个时间戳，和当时的光色：晨光、昼白、暮色、夜灯、深宵。<br>*A timestamp, and the colour of the light right then — morning, daylight, dusk, lamplight, late night.* |
| 按住「声」<br>*Hold「声」· Sound* | 一段现场的声音，松手即存，最长三十秒；按住的时候，能看见声音的高低起伏。<br>*The sound of where you are, kept the moment you let go, up to thirty seconds. While you hold it, you can watch the sound rise and fall.* |
| 轻点「影」，再轻点快门<br>*Tap「影」· Image, then the shutter* | 一张照片；按住快门，是一段五秒的短片。<br>*A photograph. Hold the shutter a little longer and it becomes a five-second film.* |

---

## 房间里的光 · The lights in the room

每记下一次，房间就多一粒光。房间里每一粒光，都对应一条记录——点它，就是回到那一刻。

*Every time you keep something, another small light appears in the room. Each light is one memory; touch it, and you are back inside that moment.*

- 房间只留最近三十粒。更早的光会慢慢沉进右下角的「光瓶」。
  *The room keeps the last thirty. Older ones drift down into the jar in the bottom right corner.*
- 瓶子里可以按时间翻看：光粒沿着玻璃管往下沉，日期是刻在玻璃上的刻度，每一粒都由一根细线引出它的时间和描述。可以从「全部」切到「瓶底」，也可以直接搜一句话、一个日期，或一个礼拜。
  *Inside the jar you can go back through time: the lights sink along a deep glass tube, dates are the marks on the glass, and a thin thread draws each light's time and its words out beside it. You can move from "all" down to "the bottom of the jar", or simply search a sentence, a date, a week.*
- 也可以「一粒一粒翻看」：一屏只有一刻，上下滑动翻页；照片、视频、语音和文字各自展开。记录不到三十粒时同样可以用。
  *Or open "one by one": one moment per screen, paging with a swipe; photos, videos, sounds and words each unfold. It is there from the very first light.*
- 一条光里最多可以叠四层：先记下这一刻，再点「+ 再加一点」，补一句话、一段声，或一张影。叠了多层的时刻，在房间里会多出一圈很淡的环。
  *One light can hold up to four layers: keep the moment first, then「+ 再加一点」· "one more thing" to add a sentence, a sound, or an image. Moments with layers carry a very faint ring around them in the room.*
- 记录之后可以补一句话，也可以永远不补。
  *You can add words afterwards, or never.*
- 不做编辑，只做删除。
  *Nothing is edited. Only letting go.*
- 每次打开，会有一粒旧光自己亮起来。
  *Each time you open it, one older light comes up on its own.*
- 设置里有「放映这段日子的光」：把最近十二粒光按时间慢慢放一遍，轻点即可跳过。
  *In settings there is「放映这段日子的光」· "play the light of these days": the last twelve lights, in order, slowly. Tap to skip.*

---

## 只属于你 · Yours alone

所有记录只留在这台设备上，不会上传到任何地方。也可以把备份带走，在另一台设备上继续；重复的光会自己跳过，不会叠两次。

*Everything stays on this device and is never uploaded anywhere. You can also carry a backup to another device and keep going there; lights you already have are recognised and quietly skipped.*

---

## Android 安装包 · Android package

浏览器不支持「添加到主屏幕」，或者加到了主屏却打不开？可以直接装一个很小的安装包。它把同一个页面整个打包进应用里，完全离线，约 0.4 MB。

*If adding to the home screen is unavailable, install the tiny APK instead. It bundles the very same page, works fully offline, and is about 0.4 MB.*

- 下载：仓库的 Releases 页面；或者 Actions → Android APK → 最近一次运行，下载 `weiguang-apk`。
  *Download it from the repository's Releases, or from the latest Android APK workflow run.*
- 自己构建：`cd android` → `.\gradlew.bat assembleRelease`，或运行 `android\build-apk.ps1`；产物在 `android/dist/`。
  *Build it yourself with the Gradle wrapper; the script leaves the APK in `android/dist/`.*
- 安装包的存储和浏览器是分开的。第一次使用时，先在网页版「导出备份」，再在安装包里「导入备份」，光就都回来了。
  *The APK keeps its data separately from the browser. Export a backup from the web page once, then import it in the app.*
- 如果提示「禁止安装未知应用」，在系统设置里给文件管理器（或浏览器）打开这个开关即可。
  *When Android asks, allow your file manager or browser to install unknown apps.*

---

> 光不需要被整理，它只需要被留下。
>
> *Light doesn't need to be sorted. It only needs somewhere to stay.*
