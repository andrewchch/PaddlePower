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
import java.util.ArrayList;
import java.util.List;

/*
For managing chart operations
 */
public class ChartController {

    private static final String TAG = "ChartController";
    int MAX_SERIES = 5;

    // ChartController stuff
    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeriesRenderer mCurrentRenderer;
    private Context parentContext;

    public ChartController () {
    }

    private XYMultipleSeriesRenderer buildRenderer(int[] colors) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        setRenderer(renderer, colors);
        return renderer;
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
        mChart = ChartFactory.getTimeChartView(context, mDataset, mRenderer,
                "hh:mm");

        Log.i(TAG, "initChart");

        // Create MAX_SERIES renderers
        int[] colors = new int[] {
                Color.argb(0, 0xff, 0xff, 0xff),
                Color.argb(0, 0xcc, 0xcc, 0xcc),
                Color.argb(0, 0xaa, 0xaa, 0xaa),
                Color.argb(0, 0x88, 0x88, 0x88),
                Color.argb(0, 0x66, 0x66,0x66)
        };

        XYMultipleSeriesRenderer renderer = buildRenderer(colors);
        int length = renderer.getSeriesRendererCount();
        for (int i = 0; i < length; i++) {
            ((XYSeriesRenderer) renderer.getSeriesRendererAt(i))
                    .setFillPoints(true);
        }

        if (mCurrentRenderer == null) {
            mCurrentRenderer = new XYSeriesRenderer();
            mCurrentRenderer.setLineWidth(4);

            mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
            mCurrentRenderer.setFillPoints(true);
            mCurrentRenderer.setColor(Color.GREEN);
            Log.i(TAG, "initChart mCurrentRenderer == null");

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

            renderer.addSeriesRenderer(mCurrentRenderer);
        }

        return mChart;
    }

    public void repaint() {
        mChart.repaint();
    }

    public void addSeries(List<StrokePoint> points) {
        // Add a new series of points to the dataset, and update the display of the previous series

        if (mDataset.getSeriesCount() >= MAX_SERIES) {
            mDataset.removeSeries(0);
        }
        XYSeries series = new XYSeries("");

        for (StrokePoint point: points) {
            series.add((double)point.time, (double)point.force);
        }
        mDataset.addSeries(series);

        // Get bounding box for all series
        double minX = Double.MIN_VALUE, minY = Double.MIN_VALUE, maxX = Double.MAX_VALUE, maxY = Double.MAX_VALUE;
        for (XYSeries _series: mDataset.getSeries()) {
            minX = Math.min(_series.getMinX(), minX);
            minY = Math.min(_series.getMinY(), minY);
            maxX = Math.max(_series.getMaxX(), maxX);
            maxY = Math.max(_series.getMaxY(), maxY);
        }

        if (((DeviceControlActivity)parentContext).currentlyVisible) {
            mRenderer.setYAxisMin(0);
            mRenderer.setYAxisMax(maxY + 20);

            if ((maxX - minX) < 5 * 60 * 1000) {
                mRenderer.setXAxisMin(minX);
                mRenderer.setXAxisMax(minX + (5 * 60 * 1000));
            } else {
                mRenderer.setXAxisMin(maxX - (5 * 60 * 1000));
                mRenderer.setXAxisMax(maxX);
            }

            mChart.repaint();
            mChart.zoomReset();
        }
    }

}
