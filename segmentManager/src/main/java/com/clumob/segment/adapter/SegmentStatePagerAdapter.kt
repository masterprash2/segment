package com.clumob.segment.adapter

import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.PagerAdapter
import com.clumob.segment.controller.common.ItemControllerWrapper
import com.clumob.segment.controller.list.ItemControllerSource
import com.clumob.segment.controller.list.SourceUpdateEvent
import com.clumob.segment.view.SegmentViewProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import java.util.*

open class SegmentStatePagerAdapter(
    val dataSource: ItemControllerSource,
    val provider: SegmentViewProvider,
    lifecycleOwner: LifecycleOwner
) : SegmentPagerAdapter(lifecycleOwner) {

    private val attachedSegments: MutableSet<Page> = HashSet()
    private val mHandler = Handler(Looper.getMainLooper())
    private val disposable : DisposableObserver<*>

    init {
        dataSource.viewInteractor = (createViewInteractor())
        disposable = object : DisposableObserver<SourceUpdateEvent?>() {
            override fun onNext(sourceUpdateEvent: SourceUpdateEvent) {
                if (sourceUpdateEvent.type == SourceUpdateEvent.Type.UPDATE_ENDS) notifyDataSetChanged()
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
            }

            override fun onComplete() {}
        }
        dataSource.observeAdapterUpdates().subscribe(disposable)
    }

    init {
        dataSource.onAttachToView()
    }

    private fun createViewInteractor(): ItemControllerSource.ViewInteractor {
        return object : ItemControllerSource.ViewInteractor {
            var deque: Deque<Runnable> = LinkedList()
            private var processingInProgress = false
            override fun processWhenSafe(runnable: Runnable) {
                deque.add(runnable)
                if (!processingInProgress) {
                    processingInProgress = true
                    this.processWhenQueueIdle()
                }
            }

            private fun processWhenQueueIdle() {
                mHandler.post(object : Runnable {
                    override fun run() {
                        if (isComputingLayout()) {
                            mHandler.post(this);
                        } else if (deque.peekFirst() != null) {
                            val runnable = deque.pollFirst();
                            runnable.run();
                            mHandler.post(this);
                        } else {
                            processingInProgress = false;
                        }
                    }
                });
            }

            override fun cancelOldProcess(runnable: Runnable) {
                mHandler.removeCallbacks(runnable)
            }
        };
    }

    override fun destroy() {
        disposable.dispose()
        dataSource.onDetachFromView()
        super.destroy()
    }


    private fun isComputingLayout(): Boolean {
        return false
    }

    override fun instantiateItemInternal(parent: ViewGroup, index: Int): Any {
        val item = getItem(index)
        val segment = provider.create(parent, item.type())
        val page = Page(item, segment, this)
        attachedSegments.add(page)
        return page
    }

    override fun getCount(): Int {
        return dataSource.itemCount
    }

    fun getAttachedSegments(): Set<Page?>? {
        return attachedSegments
    }

    override fun retrieveSegmentFromObject(item: Any): Page {
        return (item as Page)
    }

    // ToDO: The lookup algorightm is slow;
    override fun computeItemPosition(inputItem: Any): Int {
        val oldItem = (inputItem as Page).controller
        val itemCount = dataSource.itemCount
        for (i in 0 until itemCount) {
            val item = getItem(i)
            if (oldItem == item) {
                return i
            }
        }
        return POSITION_NONE
    }


    override fun destroyItem(item: Any) {
        super.destroyItem(item)
        val pair = item as Page
        pair.controller.performStop(pair.controller)
        attachedSegments.remove(pair)
    }


    override fun onConfigurationChanged(configuration: Configuration?) {
        for (segment in attachedSegments) {
            segment.viewHolder.onConfigurationChanged(configuration)
        }
    }

    fun getItem(position: Int): ItemControllerWrapper {
        return dataSource.getItem(position)
    }

}
