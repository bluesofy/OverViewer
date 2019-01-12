package cn.byk.pandora.overviewer;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * @author Created by Byk on 2019/1/10.
 */
public class ViewerContainer extends LinearLayout {

    private ViewerPanel mPanel;
    private Switch mSwTotal;
    private Switch mSwViews;

    private String mTitle;
    private boolean mIsEnable;
    private boolean mIsViewsEnable;

    public ViewerContainer(Context context) {
        this(context, "");
    }

    public ViewerContainer(Context context, String tag) {
        super(context);

        mTitle = tag;
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_container, this);

        mPanel = findViewById(R.id.view_panel);

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(mTitle);

        mSwTotal = findViewById(R.id.sw_enable);
        mSwTotal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPanel.setLayerInteractionEnabled(isChecked);
            }
        });

        mSwViews = findViewById(R.id.sw_enable_views);
        mSwViews.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPanel.setDrawViews(isChecked);
            }
        });

        SeekBar seeker = findViewById(R.id.seeker);
        seeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPanel.setSpacing(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public boolean isEnable() {
        return mIsEnable;
    }

    public ViewerContainer enableTotal(boolean enable) {
        mIsEnable = enable;
        if (mSwTotal != null) {
            mSwTotal.setChecked(mIsEnable);
        }
        return this;
    }

    public ViewerContainer enableViews(boolean enable) {
        mIsViewsEnable = enable;
        if (mSwViews != null) {
            mSwViews.setChecked(mIsViewsEnable);
        }
        return this;
    }

    public ViewerPanel getPanel() {
        return mPanel;
    }
}
