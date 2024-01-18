package live.hms.roomkit.ui.polls.display

import android.view.MotionEvent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder


class QuizDisableSwipingConditionally(
    val isItemPositionAnswered : (Int) -> Boolean,
    var isMoving : Boolean = false
) :
        RecyclerView.SimpleOnItemTouchListener() {

        override fun onInterceptTouchEvent(
            rv: RecyclerView,
            e: MotionEvent
        ): Boolean {

            // It's a scroll event
            if(rv.scrollState == RecyclerView.SCROLL_STATE_DRAGGING && !isMoving) {
                val isRightSwipe = e.historySize > 0 && e.x < e.getHistoricalX(0)
                if(isRightSwipe){
                    val answered = isItemPositionAnswered((rv.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
                    if(answered)
                        isMoving = true
                    return !answered
                } else return false
            }
            return false
        }
    }