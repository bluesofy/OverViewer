# OverViewer
[ ![Download](https://api.bintray.com/packages/blueyuki/maven/OverViewer/images/download.svg) ](https://bintray.com/blueyuki/maven/OverViewer/_latestVersion)

Layout 3D OverViewer, Based On ScalpelFrameLayout Lib From JakeWharton.

基于 JakeWharton大神 的 ScalpelFrameLayout 修改，做成一个直观的运行中的布局深度查看器。


## 效果图 Preview
![](https://github.com/bluesofy/OverViewer/blob/master/preview/pic1.png)
![](https://github.com/bluesofy/OverViewer/blob/master/preview/pic2.png)


## 使用说明 Usage
- 添加依赖
```gradle
implementation 'cn.byk.pandora:overviewer:1.0.0'
```

- 在 Activity 中调用，参考 [ScrollingActivity](https://github.com/bluesofy/OverViewer/blob/master/app/src/main/java/cn/byk/pandora/overviewersample/ScrollingActivity.java)（可以直接在 BaseActivity 里配置全局）
```java
OverViewer.with(this, TAG)
          .enable(true)
          .drawIds(true)
          .drawViews(true)
          .borderColor(getColorValue(R.color.colorPrimary), Color.RED)
          .textColor(getColorValue(R.color.colorAccent))
          .wrap(getContentView());
```

- 方法说明

| 方法名 | 参数 | 用途 |
| :---- | :---- | :---- |
| with(Context context, String tag) | context：上下文 <br> tag：标题显示  | 初始化基本参数 |
| enable(boolean enable) | enable：true 表示功能启用 | 查看器渲染开关 |
| drawViews(boolean enable) | 同上 | 是否绘制原有 View 内容 |
| drawIds(boolean enable) | 同上 | 是否绘制 ViewId |
| borderColor(int safeColor, int alarmColor) | safeColor：布局轮廓线正常颜色 <br> alarmColor：布局层次过深警告颜色 | 自定义轮廓线颜色 |
| textColor(int color) | color：文字颜色 | ViewId 文字颜色 |
| alarmLayer(int index) | index：层次深度警告层（默认值：3） | 自定义层次深度警告层值 |
| wrap(View contentView) | contentView：页面布局 View | 当前页面的 Layout 布局，设置后才能根据布局绘制轮廓线 |


### 联系方式  Support or Contact
E-Mail: bluesofy@qq.com / bluesofy@live.cn