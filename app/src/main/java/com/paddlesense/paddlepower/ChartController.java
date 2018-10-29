package com.paddlesense.paddlepower;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYSeriesRenderer;


import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import java.util.List;

/*
For managing chart operations
 */
public class ChartController {

    private static final String TAG = "ChartController";
    int MAX_SERIES = 5;
    float MAX_STROKE_DURATION_MSECS = 2000f;
    int GRID_INTERVAL_MSECS = 500;

    // ChartController stuff
    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private Context parentContext;

    public ChartController () {
    }

    private XYMultipleSeriesRenderer buildRenderer(int[] colors) {
        setRenderer(mRenderer, colors);
        return mRenderer;
    }

    private void setRenderer(XYMultipleSeriesRenderer renderer, int[] colors) {

        renderer.setAxisTitleTextSize(70);
        renderer.setPointSize(5);
        renderer.setYTitle("Time");
        renderer.setYTitle("Paddle Power");
        renderer.setPanEnabled(true);
        renderer.setLabelsTextSize(50);
        renderer.setLegendTextSize(50);

        renderer.setYAxisMin(0);
        renderer.setYAxisMax(120);
        renderer.setXAxisMin(0);
        renderer.setXAxisMax(100);

        renderer.setShowLegend(false);

        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(Color.BLACK);
        renderer.setMarginsColor(Color.BLACK);

        renderer.setShowGridY(true);
        renderer.setShowGridX(true);
        renderer.setGridColor(Color.WHITE);
        // renderer.setShowCustomTextGrid(true);

        renderer.setAntialiasing(true);
        renderer.setPanEnabled(true, false);
        renderer.setZoomEnabled(true, false);
        renderer.setZoomButtonsVisible(false);
        renderer.setXLabelsColor(Color.WHITE);
        renderer.setYLabelsColor(0, Color.WHITE);
        renderer.setXLabelsAlign(Paint.Align.CENTER);
        renderer.setXLabelsPadding(10);
        renderer.setXLabelsAngle(-30.0f);
        renderer.setYLabelsAlign(Paint.Align.RIGHT);
        renderer.setPointSize(3);
        renderer.setInScroll(true);
        // renderer.setShowLegend(false);
        renderer.setMargins(new int[] { 50, 150, 10, 50 });
        renderer.setXRoundedLabels(false);

        Log.d(TAG, String.format("renderer.isXRoundedLabels = %b", renderer.isXRoundedLabels()));

        // Set chart granularity to get label calculation right
        /*
        Axis leftAxis = mChart.getLeft();
        leftAxis.setLabelCount(6, true);
        leftAxis.setAxisMinValue(1.023f);
        leftAxis.setAxisMaxValue(1.027f);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setGranularity(0.001f);
        */

        int length = colors.length;
        for (int i = 0; i < length; i++) {
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(colors[i]);
            r.setLineWidth(4);
            r.setPointStyle(PointStyle.CIRCLE);
            r.setFillPoints(true);

            renderer.addSeriesRenderer(r);
        }
    }

    public GraphicalView initChart(Context context) {

        parentContext = context;
        Log.i(TAG, "initChart");

        // Create MAX_SERIES renderers
        int[] colors = new int[] {
                Color.argb(0xff, 0xff, 0xff, 0xff),
                Color.argb(0xff, 0xcc, 0xcc, 0xcc),
                Color.argb(0xff, 0xaa, 0xaa, 0xaa),
                Color.argb(0xff, 0x88, 0x88, 0x88),
                Color.argb(0xff, 0x66, 0x66,0x66)
        };

        XYMultipleSeriesRenderer renderer = buildRenderer(colors);
        int length = renderer.getSeriesRendererCount();
        for (int i = 0; i < length; i++) {
            ((XYSeriesRenderer) renderer.getSeriesRendererAt(i))
                    .setFillPoints(true);
        }

        // Need to create some dummy series to add to the dataset as series and renderer count
        // should match
        for (int i=0; i<MAX_SERIES; i++) {
            mDataset.addSeries(new XYSeries(""));
        }

        mChart = ChartFactory.getTimeChartView(context, mDataset, mRenderer,
                "s.SS");

        return mChart;
    }

    public void repaint() {
        mChart.repaint();
    }

    public void addSeries(List<StrokePoint> points) {
        // Add a new series of points to the dataset, and update the display of the previous series

        XYSeries series;
        if (mDataset.getSeriesCount() >= MAX_SERIES) {
            series = mDataset.getSeriesAt(0);
            mDataset.removeSeries(0);
            series.clear();
        }
        else {
            series = new XYSeries("");
        }

        // Convert time to delta milliseconds from the first point
        long startTime = 0, pointTime = 0;

        for (StrokePoint point: points) {
            if (startTime == 0) {
                startTime = point.time;
            }
            series.add(point.time - startTime, (double)point.force);
        }
        mDataset.addSeries(series);

        // Get bounding box for all series
        double minX = Double.MIN_VALUE, minY = Double.MIN_VALUE, maxX = Double.MAX_VALUE, maxY = Double.MAX_VALUE;
        for (XYSeries _series: mDataset.getSeries()) {
            minX = minX == Double.MIN_VALUE ? _series.getMinX() : Math.min(_series.getMinX(), minX);
            minY = minY == Double.MIN_VALUE ? _series.getMinY() : Math.min(_series.getMinY(), minY);
            maxX = maxX == Double.MAX_VALUE ? _series.getMaxX() : Math.max(_series.getMaxX(), maxX);
            maxY = maxY == Double.MAX_VALUE ? _series.getMaxY() : Math.max(_series.getMaxY(), maxY);
        }

        // Adjust the X bounds to provide an optimal number of labels such that each grid interval
        // 0.5 seconds
        int numIntervals = (int)Math.round((maxX - minX) / GRID_INTERVAL_MSECS);
        maxX = minX + numIntervals * GRID_INTERVAL_MSECS;

        Log.d(TAG, String.format("minX = %d, maxX = %d, interval = %d, numIntervals = %d", (int)minX, (int)maxX, (int)(maxX - minX), numIntervals));

        boolean isVisible = IVisible.class.isInstance(parentContext) ? ((IVisible)parentContext).isCurrentlyVisible() : true;

        if (isVisible) {
            mRenderer.setYAxisMin(0);
            mRenderer.setYAxisMax(maxY + 0.5f);
            mRenderer.setXAxisMin(minX);
            mRenderer.setXAxisMax(maxX);
            //mRenderer.setXLabels(numIntervals + 1);
            //mRenderer.setXLabels(5);

            mChart.repaint();
            mChart.zoomReset();
        }
    }
}
