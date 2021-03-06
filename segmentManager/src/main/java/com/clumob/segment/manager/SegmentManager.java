package com.clumob.segment.manager;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;

import com.clumob.log.AppLog;
import com.clumob.segment.controller.SegmentInfo;
import com.clumob.segment.controller.util.ParcelableUtil;
import com.clumob.segment.empty.EmptySegment;

/**
 * Created by prashant.rathore on 23/02/18.
 */

public class SegmentManager implements SegmentLifecycle {

    public static final int SEGMENT_ID_EMPTY = Integer.MIN_VALUE;
    private final SegmentManager parentSegmentManager;
    private final SegmentCallbacks callbacks;
    private final Handler mHandler = new Handler();
    private final LayoutInflater layoutInflater;

    private Segment segment;
    private SegmentViewHolder screenView;

    private Context context;
    private SegmentNavigation navigation;

    public SegmentManager(int managerId, Context context, SegmentCallbacks callbacks, LayoutInflater layoutInflater) {
        this(null, managerId, context, callbacks,layoutInflater);
    }

    SegmentManager(SegmentManager parentSegmentManager, int managerId, Context context, SegmentCallbacks callbacks, LayoutInflater layoutInflater) {
        this.parentSegmentManager = parentSegmentManager;
        this.context = context;
        this.callbacks = callbacks;
        this.navigation = callbacks.createSegmentNavigation(this);
        this.layoutInflater = layoutInflater;
    }

    public SegmentCallbacks getCallbacks() {
        return callbacks;
    }

    public SegmentManager getRootSegmentManager() {
        if (this.parentSegmentManager == null) {
            return this;
        } else
            return this.parentSegmentManager.getRootSegmentManager();
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        this.segment = createDefaultSegmentController(savedInstanceState);
        segment.onCreate();
        attachSegment();
    }


    private void attachSegment() {
        screenView = segment.createView(null);
        changeView(screenView.getView(), null);
        segment.bindView(screenView);
    }

    private Segment createDefaultSegmentController(Bundle savedInstanceState) {
        Segment segment;
        SegmentInfo segmentInfo = restoreSegment(savedInstanceState);
        if (segmentInfo == null) {
            segment = createEmptySegment();
        } else {
            segment = createRestoreSegment(segmentInfo);
        }
        segment.attach( context, layoutInflater);
        return segment;
    }

    private Segment createEmptySegment() {
        return new EmptySegment(new SegmentInfo(SEGMENT_ID_EMPTY, null));
    }

    protected SegmentInfo restoreSegment(Bundle savedInstanceState) {
        byte[] segmentInfoBytes = null;
        if (savedInstanceState == null) {
//            segmentInfoBytes = getIntent().getByteArrayExtra("SEGMENT_INFO");
        } else {
            segmentInfoBytes = savedInstanceState.getByteArray("SEGMENT_INFO");
        }
        SegmentInfo segmentInfo = null;
        try {
            if (segmentInfoBytes != null) {
                segmentInfo = ParcelableUtil.unmarshall(segmentInfoBytes, SegmentInfo.CREATOR);
            }
        } catch (Exception e) {
            AppLog.printStack(e);
        }
        return segmentInfo;
    }

    private Segment createRestoreSegment(SegmentInfo segmentInfo) {
        return segmentInfo.getId() == SEGMENT_ID_EMPTY ? createEmptySegment() : callbacks.provideSegment(segmentInfo);
    }

    SegmentInfo changeSegment(SegmentInfo segmentInfo) {
        final Segment newController = createRestoreSegment(segmentInfo);
        newController.attach(context, layoutInflater);
        final Segment oldController = this.segment;
        final SegmentViewHolder newScreen = newController.createView(null);
        switch (oldController.currentState) {
            case CREATE:
                newController.onCreate();
                newController.bindView(newScreen);
                break;
            case START:
                newController.onCreate();
                newController.bindView(newScreen);
                newController.onStart();
                break;
            case RESUME:
                oldController.onPause();
                newController.onCreate();
                newController.bindView(newScreen);
                newController.onStart();
                newController.onResume();
                break;
            case PAUSE:
                newController.onCreate();
                newController.bindView(newScreen);
                newController.onStart();
                break;
            case STOP:
                newController.onCreate();
                newController.bindView(newScreen);
                break;
            case DESTROY:
                return segmentInfo;

        }
        changeView(newScreen.getView(), new Runnable() {
            public void run() {
                switch (oldController.currentState) {
                    case CREATE:
                        oldController.unBindView();
                        oldController.onDestroy();
                        break;
                    case PAUSE:
                    case RESUME:
                    case START:
                        oldController.onStop();
                        oldController.unBindView();
                        oldController.onDestroy();
                        break;
                    case STOP:
                        oldController.unBindView();
                        oldController.onDestroy();
                        break;
                    case DESTROY:
                        return;
                }
            }
        });
        this.segment = newController;
        this.screenView = newScreen;
        return oldController.getSegmentInfo();
    }

    protected void changeView(View newView, Runnable onCompleHandler) {
        callbacks.setSegmentView(newView);
        if (onCompleHandler != null) {
            mHandler.post(onCompleHandler);
        }
    }



    public void onStart() {
        segment.onStart();
    }

    public void onResume() {
        segment.onResume();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        segment.onConfigurationChanged(newConfig);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        segment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        segment.onActivityResult(requestCode, resultCode, data);
    }

    public void onPause() {
        segment.onPause();
    }

    public void onSaveInstanceState(Bundle outState) {
        try {
            SegmentInfo segmentInfo = segment.getSegmentInfo();
            byte[] marshall = ParcelableUtil.marshall(segmentInfo);
            outState.putByteArray("SEGMENT_INFO", marshall);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onStop() {
        segment.onStop();
    }

    public boolean handleBackPressed() {
        return segment != null && (segment.handleBackPressed() || navigation.popBackStack());
    }

    public void onDestroy() {
        segment.onDestroy();
        this.screenView = null;
    }

    public SegmentNavigation getNavigation() {
        return navigation;
    }


    public interface SegmentCallbacks<SI extends SegmentInfo> {

        public Segment provideSegment(SI segmentInfo);

        public void setSegmentView(View view);

        SegmentNavigation createSegmentNavigation(SegmentManager segmentManager);

    }

}
