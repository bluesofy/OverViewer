package cn.byk.pandora.overviewersample;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import cn.byk.pandora.overviewer.OverViewer;

/**
 * @author Created by Byk on 2019/1/8
 */
public class ScrollingActivity extends AppCompatActivity {

    private static final String TAG = ScrollingActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getWrapper());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }
        });
    }

    private View getWrapper() {
        return OverViewer.with(this, TAG)
                         .enable(true)
                         .drawIds(true)
                         .drawViews(true)
                         .borderColor(getColorValue(R.color.colorPrimary), Color.RED)
                         .textColor(getColorValue(R.color.colorAccent))
                         .wrap(getContentView());
    }

    private View getContentView() {
        return LayoutInflater.from(this)
                             .inflate(R.layout.activity_scrolling, null);
    }

    private int getColorValue(int id) {
        return ResourcesCompat.getColor(getResources(), id, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
