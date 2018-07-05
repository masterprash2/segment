package com.clumob.segment.manager.pager;

import com.clumob.segment.manager.SegmentManager;
import com.clumob.segment.controller.SegmentInfo;

/**
 * Created by prashant.rathore on 02/07/18.
 */

public interface SegmentPagerItemFactory {
    public SegmentManager<?,?, ?> create(SegmentInfo segmentInfo);
}