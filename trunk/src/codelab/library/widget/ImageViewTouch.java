package codelab.library.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * ImageView를 상속 받아서 Move, pinch to zoom in/out을 구현
 * @author Hovan
 *
 */
public class ImageViewTouch extends ImageViewTouchBase {
	public ImageViewTouch(Context context) {
		super(context);
		createGestureDetectors(context);
	}

	public ImageViewTouch(Context context, AttributeSet attrs) {
		super(context, attrs);
		createGestureDetectors(context);
	}
	
	List<GestureDetector> mListGestureDetector = new ArrayList<GestureDetector>();
	
	public void addGestureDetector(GestureDetector.SimpleOnGestureListener listener) {
		mListGestureDetector.add(new GestureDetector(getContext(), listener));
	}
	
	private void createGestureDetectors(Context context) {
		if(mGestureDetector == null) mGestureDetector = new GestureDetector(context, new MyGestureListener());
		if(mScaleDetector == null) mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
	}

	public void postTranslateCenter(float dx, float dy) {
		super.postTranslate(dx, dy);
		center(true, true);
	}

	GestureDetector mGestureDetector;
	ScaleGestureDetector mScaleDetector;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mGestureDetector.onTouchEvent(event);
		mScaleDetector.onTouchEvent(event);
		for(GestureDetector detector : mListGestureDetector) {
			detector.onTouchEvent(event);
		}
		
		// We do not use the return value of
		// mGestureDetector.onTouchEvent because we will not receive
		// the "up" event if we return false for the "down" event.
		return true;
	}

	private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return super.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if (getScale() > 1F) {
				postTranslateCenter(-distanceX, -distanceY);
			}
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// Switch between the original scale and 3x scale.
			if (getScale() >= 2f) {
				zoomTo(1f);
			} else {
				zoomToPoint(2f, e.getX(), e.getY());
			}
			return true;
		}
	}

	private float mScaleFactor = 1.f;

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();

			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(0.2f, Math.min(mScaleFactor, 5.0f));

			zoomTo(mScaleFactor);
			return true;
		}
	}
}