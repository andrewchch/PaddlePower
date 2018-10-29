package com.paddlesense.paddlepower;

import android.os.Bundle;
import android.app.Activity;
import android.widget.LinearLayout;

import org.achartengine.GraphicalView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChartTestActivity extends Activity implements IVisible {

    private ChartController mChartController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_test);

        // Add the chart
        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        mChartController = new ChartController();
        GraphicalView mChart = mChartController.initChart(this);
        layout.addView(mChart);

        // Create some test data
        List<StrokePoint> points1 = new ArrayList<>();
        long now = new Date().getTime();

        points1.add(new StrokePoint(1.3f, now ));
        points1.add(new StrokePoint(1.5f, now + 400));
        points1.add(new StrokePoint(2.8f, now + 600));
        points1.add(new StrokePoint(1.5f, now + 800));
        points1.add(new StrokePoint(0.6f, now + 1000));

        List<StrokePoint> points2 = new ArrayList<>(),
                points3 = new ArrayList<>(),
                points4 = new ArrayList<>(),
                points5 = new ArrayList<>(),
                points6 = new ArrayList<>();

        for (StrokePoint p: points1) {
            points2.add(new StrokePoint(p.force + 0.1f, p.time));
            points3.add(new StrokePoint(p.force + 0.2f, p.time));
            points4.add(new StrokePoint(p.force + 0.3f, p.time));
            points5.add(new StrokePoint(p.force + 0.4f, p.time));
            points6.add(new StrokePoint(p.force + 0.5f, p.time));
        }

        mChartController.addSeries(points1);
        mChartController.addSeries(points2);
        mChartController.addSeries(points3);
        mChartController.addSeries(points4);
        mChartController.addSeries(points5);

        // Add another series, should trigger reuse of the first series
        mChartController.addSeries(points6);

        mChartController.repaint();
    }

    @Override
    public boolean isCurrentlyVisible() {
        return true;
    }
}
