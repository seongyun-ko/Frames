/*
 * Copyright (c) 2018. Jahir Fiquitiva
 *
 * Licensed under the CreativeCommons Attribution-ShareAlike
 * 4.0 International License. You may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *    http://creativecommons.org/licenses/by-sa/4.0/legalcode
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jahirfiquitiva.libs.frames.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import ca.allanwang.kau.utils.drawable
import ca.allanwang.kau.utils.gone
import ca.allanwang.kau.utils.postDelayed
import ca.allanwang.kau.utils.visible
import ca.allanwang.kau.utils.visibleIf
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import jahirfiquitiva.libs.frames.helpers.extensions.fastScrollThumbInactiveColor
import jahirfiquitiva.libs.kauextensions.extensions.activeIconsColor
import jahirfiquitiva.libs.kauextensions.extensions.applyColorFilter
import jahirfiquitiva.libs.kauextensions.extensions.dividerColor
import jahirfiquitiva.libs.kauextensions.extensions.hasContent
import jahirfiquitiva.libs.kauextensions.extensions.secondaryTextColor
import jahirfiquitiva.libs.kauextensions.extensions.setDecodedBitmap
import java.lang.ref.WeakReference

open class EmptyViewRecyclerView : FastScrollRecyclerView {
    
    private var swipeToRefresh: WeakReference<SwipeRefreshLayout?>? = null
    
    var loadingView: View? = null
    var emptyView: View? = null
    var textView: TextView? = null
    
    var loadingText: String = ""
    var emptyText: String = ""
    
    var state: State = State.LOADING
        set(value) {
            if (value != field) {
                field = value
                updateStateViews()
            }
        }
    
    fun setLoadingText(@StringRes res: Int) {
        loadingText = context.getString(res)
    }
    
    fun setEmptyText(@StringRes res: Int) {
        emptyText = context.getString(res)
    }
    
    fun setEmptyImage(image: Bitmap?) {
        emptyView?.let {
            if (it is ImageView) {
                it.setImageBitmap(image)
            } else {
                throw UnsupportedOperationException(
                        "Cannot set a Drawable in a View that is not ImageView")
            }
        }
    }
    
    fun setEmptyImage(image: Drawable?) {
        emptyView?.let {
            if (it is ImageView) {
                it.setImageBitmap(null)
                it.setImageDrawable(image)
            } else {
                throw UnsupportedOperationException(
                        "Cannot set a Drawable in a View that is not ImageView")
            }
        }
    }
    
    fun setEmptyImage(@DrawableRes res: Int) {
        emptyView?.let {
            if (it is ImageView) {
                it.setImageBitmap(null)
                it.setImageDrawable(null)
                try {
                    it.setDecodedBitmap(res)
                } catch (e: Exception) {
                    it.setImageDrawable(context.drawable(res))
                }
            } else {
                throw UnsupportedOperationException(
                        "Cannot set a Drawable in a View that is not ImageView")
            }
        }
    }
    
    fun attachSwipeRefreshLayout(refreshLayout: SwipeRefreshLayout?) {
        refreshLayout?.let {
            this.swipeToRefresh = WeakReference(it)
        }
    }
    
    constructor(context: Context) : super(context) {
        init()
    }
    
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init()
    }
    
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int)
            : super(context, attributeSet, defStyleAttr) {
        init()
    }
    
    private fun init() {
        setTrackColor(context.dividerColor)
        setThumbInactiveColor(context.fastScrollThumbInactiveColor)
        setOnScrollbarStateChangeListener(object : OnFastScrollStateChangeListener {
            override fun onFastScrollStart() {
                swipeToRefresh?.get()?.isEnabled = false
            }
            
            override fun onFastScrollStop() {
                swipeToRefresh?.get()?.isEnabled = true
            }
        })
    }
    
    private fun setStateInternal() {
        state = if (adapter != null) {
            val items = adapter.itemCount
            if (items > 0) {
                State.NORMAL
            } else {
                State.EMPTY
            }
        } else {
            State.LOADING
        }
    }
    
    private fun updateStateViews() {
        val rightText = when (state) {
            State.LOADING -> loadingText
            State.EMPTY -> emptyText
            else -> ""
        }
        if (rightText.hasContent()) textView?.text = rightText
        textView?.setTextColor(context.secondaryTextColor)
        textView?.visibleIf(state != State.NORMAL && rightText.hasContent())
        loadingView?.visibleIf(state == State.LOADING)
        updateEmptyState()
        visibleIf(state == State.NORMAL)
    }
    
    private val observer: RecyclerView.AdapterDataObserver = object :
            RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            setStateInternal()
        }
        
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            setStateInternal()
        }
        
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            setStateInternal()
        }
        
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            setStateInternal()
        }
        
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            setStateInternal()
        }
        
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            setStateInternal()
        }
    }
    
    override fun setAdapter(adapter: Adapter<*>?) {
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(observer)
        setStateInternal()
    }
    
    private fun View.showAndAnimate() {
        (this as? ImageView)?.drawable?.applyColorFilter(context.activeIconsColor)
        visible()
        (this as? ImageView)?.let {
            postDelayed(200) { (it.drawable as? Animatable)?.start() }
        }
    }
    
    fun updateEmptyState() {
        if (state == State.EMPTY) {
            emptyView?.showAndAnimate()
        } else {
            emptyView?.gone()
        }
    }
    
    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.VISIBLE) showScrollbar()
    }
    
    enum class State {
        EMPTY, NORMAL, LOADING
    }
}