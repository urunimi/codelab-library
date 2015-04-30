package com.hovans.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.GridView;

/**
 * WrapGridView.java
 *
 * Created by Hovan on 6/26/14.
 */
public class WrapGridView extends GridView {

	boolean expanded = true;

	public WrapGridView(Context context) {
		super(context);
	}

	public WrapGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public WrapGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public boolean isExpanded() {
		return expanded;
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// HACK! TAKE THAT ANDROID!
		if (isExpanded()) {
			// Calculate entire height by providing a very large height hint.
			// View.MEASURED_SIZE_MASK represents the largest height possible.
			int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK,
				MeasureSpec.AT_MOST);
			super.onMeasure(widthMeasureSpec, expandSpec);

			ViewGroup.LayoutParams params = getLayoutParams();
			params.height = getMeasuredHeight();
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_MOVE){
			return true;
		}

		return super.onTouchEvent(event);
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}
}