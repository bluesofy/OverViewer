package cn.byk.pandora.overviewer;

import android.content.Context;
import android.view.View;

/**
 * @author Created by Byk on 2019/1/11.
 */
public class OverViewer {

    private ViewerContainer mContainer;
    private ViewerPanel mPanel;

    private static class SingletonHolder {

        private static final OverViewer INSTANCE = new OverViewer();
    }

    private OverViewer() {}

    public static OverViewer getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private OverViewer init(Context context, String tag) {
        mContainer = new ViewerContainer(context, tag);
        mPanel = mContainer.getPanel();
        return this;
    }

    public static OverViewer with(Context context, String tag) {
        return getInstance().init(context, tag);
    }

    public OverViewer enable(boolean enable) {
        mContainer.enableTotal(enable);
        return this;
    }

    public OverViewer drawViews(boolean enable) {
        mContainer.enableViews(enable);
        return this;
    }

    public OverViewer drawIds(boolean enable) {
        mPanel.setDrawIds(enable);
        return this;
    }

    public OverViewer shadowColor(int color) {
        mPanel.setChromeShadowColor(color);
        return this;
    }

    public OverViewer borderColor(int safeColor, int alarmColor) {
        mPanel.setChromeColor(safeColor);
        mPanel.setChromeAlarmColor(alarmColor);
        return this;
    }

    public OverViewer textColor(int color) {
        mPanel.setTextColor(color);
        return this;
    }

    public OverViewer alarmLayer(int index) {
        mPanel.setAlarmLayer(index);
        return this;
    }

    public ViewerContainer wrap(View contentView) {
        mPanel.addView(contentView);
        return mContainer;
    }
}
