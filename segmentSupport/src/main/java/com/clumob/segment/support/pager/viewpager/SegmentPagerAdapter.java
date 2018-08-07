package com.clumob.segment.support.pager.viewpager;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.clumob.segment.manager.Segment;
import com.clumob.segment.manager.SegmentLifecycle;
import com.clumob.segment.manager.SegmentViewHolder;

/**
 * Created by prashant.rathore on 02/07/18.
 */

public abstract class SegmentPagerAdapter extends PagerAdapter implements SegmentLifecycle {

    private Segment<?, ?> primaryItem;

    @NonNull
    @Override
    final public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Segment<?, ?> segment = instantiateItem(position);
        segment.attach(container.getContext(), LayoutInflater.from(container.getContext()));
        SegmentViewHolder view = segment.createView(container);
        container.addView(view.getView());
        segment.bindView(view);
        segment.onStart();
        return segment;
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.setPrimaryItem(container, position, object);
        if (primaryItem != object) {
            Segment newPrimaryItem = (Segment) object;
            if (this.primaryItem != null) {
                this.primaryItem.onPause();
            }
            newPrimaryItem.onResume();
            this.primaryItem = newPrimaryItem;
        }
    }

    @Override
    final public int getItemPosition(@NonNull Object object) {
        return computeItemPosition((Segment<?, ?>) object);
    }

    public int computeItemPosition(Segment<?, ?> segment) {
        return POSITION_UNCHANGED;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        Segment<?, ?> segment = (Segment<?, ?>) object;
        View view = segment.getBoundedView().getView();
        destroyItem(segment);
        container.removeView(view);
    }


    public Segment<?, ?> getPrimaryItem() {
        return primaryItem;
    }

    @Override
    public void onResume() {
        if (primaryItem != null) {
            primaryItem.onResume();
        }
    }

    public abstract Segment<?, ?> instantiateItem(int position);

    public void destroyItem(Segment<?, ?> segment) {
        segment.onStop();
        segment.unBindView();
    }

    public boolean handleBackPressed() {
        if (primaryItem == null) {
            return false;
        }
        return primaryItem.handleBackPressed();
    }

    public void onActivityResult(int code, int resultCode, Intent data) {
        if(primaryItem != null) {
            primaryItem.onActivityResult(code,resultCode,data);
        }
    }

    public void onRequestPermissionsResult(int code, String[] permissions, int[] grantResults) {
        if(primaryItem != null)
            primaryItem.onRequestPermissionsResult(code,permissions,grantResults);
    }
}