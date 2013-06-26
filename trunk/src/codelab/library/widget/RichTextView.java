package codelab.library.widget;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannedString;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

/**
 * 	
 * Rich Text View <br/>
 * <br/>
 * <br/>	textView1 = (RichTextView)findViewById(R.id.title1);
 * <br/>		textView1.setSingleLine();
 * <br/>	textView1.setEllipsize(TruncateAt.END);
 * <br/>	textView1.setText(TEMP_TEXT);
 * <br/>
 * <br/>	// color
 * <br/>	textView2 = (RichTextView)findViewById(R.id.title2);
 * <br/>	SpannableStringBuilder ssb = new SpannableStringBuilder(TEMP_TEXT);
 * <br/>	ssb.setSpan(new RichTextView.ForegroundAndSelectedColorSpan(Color.RED, Color.BLUE), 0, 6,
 * <br/>		Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 * <br/>	textView2.setText(ssb);
 * <br/>
 * <br/>	// selected color and word wrap
 * <br/>	textView3 = (RichTextView)findViewById(R.id.title3);
 * <br/>	textView3.setSelected(true);
 * <br/>	SpannableStringBuilder ssb2 = new SpannableStringBuilder(TEMP_TEXT);
 * <br/>	ssb2.setSpan(new RichTextView.ForegroundAndSelectedColorSpan(Color.RED, Color.BLUE), 0, 6,
 * <br/>		Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 * <br/>	textView3.setWordWrap(true);
 * <br/>	textView3.setText(ssb2);
 * <br/>
 * <br/>	// divider
 * <br/>	textView4 = (RichTextView)findViewById(R.id.title4);
 * <br/>	SpannableStringBuilder ssb3 = new SpannableStringBuilder(" ");
 * <br/>	ssb3.setSpan(new RichTextView.CellDividerSpan(Color.RED, Color.BLUE, 10, 10, 10, 10), 0, 1,
 * <br/>		Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 * <br/>	ssb3.append(TEMP_TEXT);
 * <br/>	textView4.setText(ssb3);
 * <br/>
 * <br/>	// text in drawble
 * <br/>	textView5 = (RichTextView)findViewById(R.id.title5);
 * <br/>	SpannableStringBuilder ssb4 = new SpannableStringBuilder(" ");
 * <br/>	String text = String.valueOf("1");
 * <br/>	Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.list_circle_number_bg).copy(
 * <br/>		Bitmap.Config.ARGB_8888, true);
 * <br/>	BitmapDrawable bmpDrawable = new BitmapDrawable(bm);
 * <br/>	bmpDrawable.setBounds(0, 0, bm.getWidth(), bm.getHeight());
 * <br/>	ssb4.setSpan(new RichTextView.TextInDrawableSpan(bmpDrawable, text, 10f, Color.RED, Color.BLUE), 0, 1,
 * <br/>		Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 * <br/>	ssb4.append(" ");
 * <br/>	ssb4.append(TEMP_TEXT);
 * <br/>	textView4.setText(ssb4);
 * <br/>
 * 
 * @author Hovan Yoo
 */
public class RichTextView extends TextView {

	private final float mScale = getContext().getResources().getDisplayMetrics().density;

	private RootView mRootView;

	private boolean mDoRelayout = false;

	private Paint mPaint;

	private float mLineSpacingBefore;
	private boolean mLineSpacingBeforeExceptFirst;

	private float mLineSpacingAfter;
	private boolean mLineSpacingAfterExceptLast;

	private int mMaxLines;
	private boolean mSingleLine;

	private boolean mIsWordWrap;

	private TruncateAt mEllipsize;

	public RichTextView(Context context) {
		super(context);
		init();
	}

	public RichTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * 라인 전 sapcing 을 설정한다.
	 * 
	 * @param dp
	 */
	public void setLineSpacingBefore(float dp) {
		setLineSpacingBefore(dp, true);
	}

	/**
	 * 라인 전 spacing 과 첫번째 라인 전에 spacing을 줄껀지 여부
	 * 
	 * @param dp
	 * @param lineSpacingBeforeExceptFirst
	 */
	public void setLineSpacingBefore(float dp, boolean lineSpacingBeforeExceptFirst) {
		mLineSpacingBefore = getPixel(dp);
		mLineSpacingBeforeExceptFirst = lineSpacingBeforeExceptFirst;
	}

	/**
	 * 라인 후 sapcing 을 설정한다.
	 * 
	 * @param dp
	 */
	public void setLineSpacingAfter(float dp) {
		setLineSpacingAfter(dp, true);
	}

	/**
	 * 라인 후 spacing 과 마지막 라인에 spacing을 줄껀지 여부
	 * 
	 * @param dp
	 * @param lineSpacingAfterExceptLast
	 */
	public void setLineSpacingAfter(float dp, boolean lineSpacingAfterExceptLast) {
		mLineSpacingAfter = getPixel(dp);
		mLineSpacingAfterExceptLast = lineSpacingAfterExceptLast;
	}

	/**
	 * maxLines
	 * 
	 * XML을 처음 파싱할때 TextView에서 com.android.internal.R.styleable.TextView_maxLines에 대해 setMaxLines()가 호출됨
	 * 
	 *  @param maxlines
	 */
	@Override
	public void setMaxLines(int maxlines) {
		mMaxLines = maxlines;
		super.setMaxLines(maxlines);
	}

	/**
	 * singleLines
	 * 
	 * XML을 처음 파싱할때 TextView에서 com.android.internal.R.styleable.TextView_singleLine에 대해 결국에는 setSingleLine()이 호출됨
	 */
	public void setSingleLine() {
		mSingleLine = true;
		super.setSingleLine();
	}

	/**
	 *  line count
	 */
	@Override
	public int getLineCount() {
		if (mRootView != null) {
			return mRootView.getViewCount();
		} else {
			return 0;
		}
	}

	/**
	 *  word wrap을 설정 한다. default는 false
	 * 
	 * @param wordWrap
	 */
	public void setWordWrap(boolean wordWrap) {
		mIsWordWrap = wordWrap;
	}

	/**
	 * word wrap 관련 API : 이것을 오버라이드 하면 각 textView별로 word wrap 규칙을 만들 수 있다.
	 * -> protected로 선언하여 이를 상속받는 다른 View에서는 word wrap 규칙을 따로 만들도록 한다. 
	 * 
	 * @param ch
	 */
	protected boolean isWordSeparatorDefined(char ch) {
		return isBaseWordSeparatorDefined(ch);
	}

	/**
	 * Ellipsize String을 각자 정의해서 쓸수 있도록 protected로
	 * 
	 */
	protected String getEllipsizeString() {
		return "...";
	}

	private float getPixel(float dp) {
		return (dp * mScale + 0.5f);
	}

	private void init() {
		resetPaint();
	}

	private void resetPaint() {
		if (mPaint == null) {
			mPaint = new Paint();
		}

		mPaint.setAntiAlias(true);
		mPaint.setTextSize(getTextSize());
		mPaint.setFakeBoldText(false);
		mPaint.setTextSkewX(0);
		mPaint.setColor(getTextColors().getDefaultColor());

		if (getPaint() != null) {
			int orgLineSapcing = getLineHeight() - getPaint().getFontMetricsInt(null);
			if (orgLineSapcing > 0) {
				mLineSpacingAfter = orgLineSapcing;
				mLineSpacingAfterExceptLast = true;
			}
		}
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
		super.onTextChanged(text, start, lengthBefore, lengthAfter);
		boolean needReformatting = mRootView != null;
		if (needReformatting) {
			mDoRelayout = true;
			requestLayout();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int width = widthSize - getCompoundPaddingLeft() - getCompoundPaddingRight();
		boolean needReformatting = mRootView == null || mRootView.getWidth() != width || mDoRelayout;
		if (needReformatting) {
			mDoRelayout = false;
			relayout(width);
			if (getLayoutParams().width == LayoutParams.WRAP_CONTENT) {
				widthSize = Math.round(mRootView.getExactWidth());
			}
		}

		setMeasuredDimension(widthSize,
				Math.round(mRootView.getHeight()) + getCompoundPaddingTop() + getCompoundPaddingBottom());
	}

	private void relayout(int width) {
		if (mRootView == null) {
			mRootView = new RootView(null);
		}
		if (width <= 0) {
			return;
		}
		mRootView.clearChildren();
		mRootView.loadChildren(0, width);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mRootView != null) {
			mRootView.onDraw(canvas, getCompoundPaddingLeft(), getCompoundPaddingTop());
		} else {
			super.onDraw(canvas);
		}
	}

	/**
	 * 
	 * @author Hovan Yoo
	 *
	 */
	enum FormattingType {
		LayoutFull,
		LayoutFinished,
		AllLayoutFinished
	}

	/**
	 * abstract view
	 * 
	 * @author Hovan Yoo
	 */
	private static abstract class AbstractView {

		protected float mWrappingWidth;
		protected float mWrappingHeight;
		protected float mX;
		protected float mY;
		protected int mStartOffset;
		protected int mEndOffset;
		protected AbstractView mParent;

		public AbstractView(AbstractView parent) {
			mParent = parent;
		}

		protected abstract FormattingType loadChildren(int modelOffset, float wrappingWidth);

		protected abstract void onDraw(Canvas canvas, float posX, float posY);

		protected int getStartOffset() {
			return mStartOffset;
		}

		protected int getEndOffset() {
			return mEndOffset;
		}

		protected float getWidth() {
			return mWrappingWidth;
		}

		protected float getExactWidth() {
			return mWrappingWidth;
		}

		protected float getHeight() {
			return mWrappingHeight;
		}

	}

	/**
	 * abstract view
	 * 
	 * @author Hovan Yoo
	 */
	private static abstract class AbstractCompositeView extends AbstractView {

		protected ArrayList<AbstractView> mChildren;

		public AbstractCompositeView(AbstractView parent) {
			super(parent);
			mChildren = new ArrayList<RichTextView.AbstractView>();
		}

		public void clearChildren() {
			mChildren.clear();
		}

		public void add(AbstractView view) {
			mChildren.add(view);
		}

		@Override
		public float getHeight() {
			float h = 0;
			AbstractView view = null;
			int size = getViewCount();
			for (int i = 0; i < size; i++) {
				view = getView(i);
				if (view != null) {
					h += view.getHeight();
				}
			}
			return h;
		}

		public float getExactWidth() {
			float w = 0;
			AbstractView view = null;
			int size = getViewCount();
			for (int i = 0; i < size; i++) {
				view = getView(i);
				if (view != null) {
					w += view.getExactWidth();
				}
			}
			return w;
		}

		@Override
		public int getStartOffset() {
			AbstractView view = getView(0);
			if (view != null) {
				return view.getStartOffset();
			} else {
				return 0;
			}
		}

		@Override
		public int getEndOffset() {
			AbstractView view = getView(getViewCount() - 1);
			if (view != null) {
				return view.getEndOffset();
			} else {
				return 0;
			}
		}

		public int getViewCount() {
			return mChildren.size();
		}

		public AbstractView getView(int idx) {
			if (idx < 0 || mChildren.size() <= idx) {
				return null;
			}
			return mChildren.get(idx);
		}

		@Override
		protected void onDraw(Canvas canvas, float posX, float posY) {
			AbstractView view = null;
			int size = getViewCount();
			for (int i = 0; i < size; i++) {
				view = getView(i);
				if (view != null) {
					float tempX = posX + view.mX;
					float tempY = posY + view.mY;
					view.onDraw(canvas, tempX, tempY);
				}
			}
		}

	}

	/**
	 * ParagraphView
	 * 
	 * @author Hovan Yoo
	 */
	private class RootView extends AbstractCompositeView {

		public RootView(AbstractView parent) {
			super(parent);
		}

		@Override
		protected FormattingType loadChildren(int modelOffset, float wrappingWidth) {
			mWrappingWidth = wrappingWidth;
			mStartOffset = 0;
			mEndOffset = getText().length();
			mEllipsize = getEllipsize();

			FormattingType type = FormattingType.LayoutFinished;
			float x = 0;
			float y = 0;

			boolean mIsFirstLine = true;
			while (true) {
				LineView lineView = new LineView(this);
				lineView.mX = x;
				lineView.mY = y;
				lineView.mSpacingBefore = mLineSpacingBefore;
				lineView.mSpacingAfter = mLineSpacingAfter;

				type = lineView.loadChildren(modelOffset, wrappingWidth);

				if (lineView.getViewCount() == 0 && type == FormattingType.LayoutFull) {
					// layout width가 너무 작아서 한글자도 못들어 간경우
					return FormattingType.LayoutFinished;
				}

				add(lineView);
				int endOffset = lineView.getEndOffset();

				//line spacing before 처리
				if (mIsFirstLine) {
					if (mLineSpacingBeforeExceptFirst) {
						lineView.mSpacingBefore = 0;
					}
					mIsFirstLine = false;
				}

				if (type == FormattingType.AllLayoutFinished || endOffset >= mEndOffset) {

					//line spacing after 처리
					if (mLineSpacingAfterExceptLast) {
						lineView.mSpacingAfter = 0;
					}
					lineView.mHeight = Math.max(lineView.mHeight, lineView.mHeight + lineView.mSpacingBefore
							+ lineView.mSpacingAfter);
					break;
				}

				lineView.mHeight = Math.max(lineView.mHeight, lineView.mHeight + lineView.mSpacingBefore
					+ lineView.mSpacingAfter);

				y += lineView.getHeight();
				modelOffset = endOffset;

				//singline 처리
				if (mSingleLine) {
					break;
				}

				//max line 처리
				if (mMaxLines > 0 && getViewCount() >= mMaxLines) {
					break;
				}
			}
			return type;

		}
	}

	/**
	 * RunView
	 * 
	 * @author Hovan Yoo
	 */
	private class LineView extends AbstractCompositeView {

		protected float mAcsent;
		protected float mDescent;
		protected float mSpacingBefore;
		protected float mSpacingAfter;
		protected float mHeight;

		@Override
		public float getHeight() {
			return mHeight;
		}

		public float getBaseLine() {
			return mHeight - (mDescent + mSpacingAfter);
		}

		public LineView(AbstractView parent) {
			super(parent);
		}

		@Override
		protected void onDraw(Canvas canvas, float posX, float posY) {
			AbstractView view = null;
			int size = getViewCount();
			for (int i = 0; i < size; i++) {
				view = getView(i);
				if (view != null) {
					view.onDraw(canvas, posX, posY);
					posX += view.getWidth();
				}
			}
		}

		private Attr getRunAttr(int startOffset, int endOffset, CharSequence text) {
			Attr attr = new Attr();
			attr.setFontSize(getTextSize());
			int defaultColor = getTextColors().getDefaultColor();
			attr.setColor(defaultColor);
			attr.setSelectedColor(getTextColors().getColorForState(SELECTED_STATE_SET, defaultColor));

			if (text instanceof SpannedString) {

				Object[] runAttrs = ((SpannedString)text).getSpans(startOffset, endOffset, Object.class);

				if (runAttrs.length != 0) {
					for (Object runAttr : runAttrs) {
						if (runAttr instanceof ImageSpan) {
							ImageSpan imageSpan = (ImageSpan)runAttr;
							int verticalAlignment = imageSpan.getVerticalAlignment();
							Drawable drawable = imageSpan.getDrawable();
							attr.setDrawable(drawable, verticalAlignment);
						} else if (runAttr instanceof StyleSpan) {
							StyleSpan styleSpan = (StyleSpan)runAttr;
							int style = styleSpan.getStyle();
							if (style == android.graphics.Typeface.BOLD) {
								attr.setBold(true);
							} else if (style == android.graphics.Typeface.ITALIC) {
								attr.setItalic(true);
							}
						} else if (runAttr instanceof ForegroundColorSpan) {
							ForegroundColorSpan colorSpan = (ForegroundColorSpan)runAttr;
							int color = colorSpan.getForegroundColor();
							attr.setColor(color);
						} else if (runAttr instanceof RichTextView.ForegroundAndSelectedColorSpan) {
							RichTextView.ForegroundAndSelectedColorSpan colorSpan = (RichTextView.ForegroundAndSelectedColorSpan)runAttr;
							int color = colorSpan.getForgroundColor();
							attr.setColor(color);
							int selectedColor = colorSpan.getSelectedColor();
							attr.setSelectedColor(selectedColor);
						} else if (runAttr instanceof AbsoluteSizeSpan) {
							AbsoluteSizeSpan sizeSpan = (AbsoluteSizeSpan)runAttr;
							attr.setFontSize(getPixel(sizeSpan.getSize()));
						} else if (runAttr instanceof RichTextView.CellDividerSpan) {
							RichTextView.CellDividerSpan divider = (RichTextView.CellDividerSpan)runAttr;
							attr.setDivider(divider);
							attr.setColor(divider.getForgroundColor());
							attr.setSelectedColor(divider.getSelectedColor());
						} else if (runAttr instanceof RichTextView.TextInDrawableSpan) {
							RichTextView.TextInDrawableSpan textInDrawableSpan = (RichTextView.TextInDrawableSpan)runAttr;
							attr.setFontSize(getPixel(textInDrawableSpan.getFontSize()));
							attr.setColor(textInDrawableSpan.getForgroundColor());
							attr.setSelectedColor(textInDrawableSpan.getSelectedColor());
							attr.setTextInDrawable(textInDrawableSpan.getText());
							attr.setDrawable(textInDrawableSpan.getDrawable());
						}
					}
				}
			}
			return attr;
		}

		@Override
		protected FormattingType loadChildren(int modelOffset, float wrappingWidth) {
			mWrappingWidth = wrappingWidth;

			FormattingType type = FormattingType.LayoutFinished;

			float width = 0;
			float sumWidth = 0;
			float netWidth = wrappingWidth;
			float maxAscent = 0;
			float maxDescent = 0;
			float xPosition = 0;

			CharSequence text = getText();
			ArrayList<Integer> seperateList = new ArrayList<Integer>();

			//1. 먼저 span을 분석하여 run 이될 모델의 offset 들을 뽑아낸다 
			seperateList.add(modelOffset);
			seperateList.add(text.length());

			boolean isSpannedString = text instanceof SpannedString;
			if (isSpannedString) {
				SpannedString spannedStr = (SpannedString)text;

				Object[] spans = spannedStr.getSpans(modelOffset, spannedStr.length(), Object.class);

				if (spans.length != 0) {
					for (Object span : spans) {
						int startOffset = spannedStr.getSpanStart(span);
						int endOffset = spannedStr.getSpanEnd(span);
						if (modelOffset <= startOffset && !seperateList.contains(startOffset)) {
							seperateList.add(startOffset);
						}
						if (modelOffset <= endOffset && !seperateList.contains(endOffset)) {
							seperateList.add(endOffset);
						}
					}
				}
			}

			Integer[] position = new Integer[seperateList.size()];
			seperateList.toArray(position);
			Arrays.sort(position);

			int wordWrapIndex = modelOffset;

			//ellipsize 관련 처리
			String ellipsizeStr = getEllipsizeString();
			boolean needEllipsize = mEllipsize != null && ellipsizeStr != null
				&& (mSingleLine || mMaxLines > 0 && mMaxLines - 1 == getLineCount());
			float measureEllipsizeText = 0;
			if (needEllipsize) {
				measureEllipsizeText = mPaint.measureText(ellipsizeStr);
				netWidth -= measureEllipsizeText;
			}

			for (int i = 0; i < position.length - 1; i++) {
				int startOffset = position[i];
				int endOffset = position[i + 1];

				Attr attr = getRunAttr(startOffset, endOffset, text);

				float objectHeight = 0;
				Drawable drawable = null;
				RichTextView.CellDividerSpan divder = null;

				for (int j = startOffset; j < endOffset; j++) {
					char ch = text.charAt(j);

					if (isLineEndChar(ch)) {
						//break run 이 들어오면 전에까지 run views 를 만들고
						if (startOffset < j) {
							RunView runView = new RunView(this);
							runView.mStartOffset = startOffset;
							runView.mEndOffset = j;
							runView.mWrappingWidth = sumWidth - width - xPosition;
							runView.mX = xPosition;
							xPosition += runView.mWrappingWidth;
							runView.attr = attr;
							add(runView);
							mAcsent = Math.max(maxAscent, mAcsent);
							mDescent = Math.max(maxDescent, mDescent);
							objectHeight = Math.max(objectHeight, mAcsent + mDescent);
							mHeight = Math.max(mHeight, objectHeight);
						}

						//break run 을 만든다
						BreakRunView runView = new BreakRunView(this);
						runView.mStartOffset = j;
						runView.mEndOffset = j + 1;
						add(runView);
						return FormattingType.LayoutFinished;
					}

					//먼저 font의 ascent 와 descent를 계산
					Paint paint = attr.getPaint();
					FontMetrics fontMtrics = paint.getFontMetrics();
					float acent = -fontMtrics.ascent;
					maxAscent = Math.max(acent, maxAscent);
					float descent = fontMtrics.descent + fontMtrics.leading;
					maxDescent = Math.max(descent, maxDescent);

					drawable = attr.getDrawable();
					divder = attr.getDivider();

					if (drawable != null) {
						//Bitmap 일 경우에는 span 으로 조절된 width / height 를 쓰지 않고, bitmap으로 계산한다.
						if (drawable instanceof BitmapDrawable) {
							Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
							if (bitmap != null) {
								width = bitmap.getWidth();
								objectHeight = bitmap.getHeight();
							}
						} else {
							Rect rect = drawable.getBounds();
							width = rect.right - rect.left;
							objectHeight = rect.bottom - rect.top;
						}
					} else if (divder != null) {
						width = getPixel(divder.getWidth() + divder.getLeftMargin() + divder.getRightMargin());
					} else {
						width = attr.getStringWidth(String.valueOf(ch));
						resetPaint();
					}

					sumWidth += width;

					//한줄이 다 찼다면..
					if (netWidth < sumWidth) {
						if (mIsWordWrap && wordWrapIndex > modelOffset) { //1. word wrap을 처리한다. 
							RunView runView = new RunView(this);
							runView.mStartOffset = startOffset;
							runView.mEndOffset = Math.min(wordWrapIndex + 1, j);
							runView.mWrappingWidth = sumWidth - width - xPosition;
							runView.mX = xPosition;
							xPosition += runView.mWrappingWidth;
							runView.attr = attr;
							add(runView);
							mAcsent = Math.max(maxAscent, mAcsent);
							mDescent = Math.max(maxDescent, mDescent);
							objectHeight = Math.max(objectHeight, mAcsent + mDescent);
							mHeight = Math.max(mHeight, objectHeight);

							//word Wrap 전에 추가된 view들을 삭제해 준다
							for (int removeIndex = mChildren.size() - 1; removeIndex >= 0; removeIndex--) {
								AbstractView view = mChildren.get(removeIndex);
								if (runView.mEndOffset <= view.mStartOffset) {
									mChildren.remove(removeIndex);
								} else {
									break;
								}
							}

						} else if (drawable == null && divder == null && startOffset < j) { //2. word wrap이 아닐때 character단위를 처리한다.
							RunView runView = new RunView(this);
							runView.mStartOffset = startOffset;
							runView.mEndOffset = j;
							runView.mWrappingWidth = sumWidth - width - xPosition;
							runView.mX = xPosition;
							xPosition += runView.mWrappingWidth;
							runView.attr = attr;
							add(runView);
							mAcsent = Math.max(maxAscent, mAcsent);
							mDescent = Math.max(maxDescent, mDescent);
							objectHeight = Math.max(objectHeight, mAcsent + mDescent);
							mHeight = Math.max(mHeight, objectHeight);
						}

						// ellipsize start, end 처리
						if (needEllipsize) {
							if (mEllipsize == TruncateAt.START) {
								TrunCatView trunCatView = new TrunCatView(this);
								trunCatView.mWrappingWidth = measureEllipsizeText;
								trunCatView.mX = 0;
								//나머지 x위치 조정
								for (AbstractView view : mChildren) {
									view.mX += measureEllipsizeText;
								}
								mChildren.add(0, trunCatView);
							}
							if (mEllipsize == TruncateAt.END) {
								TrunCatView trunCatView = new TrunCatView(this);
								trunCatView.mWrappingWidth = measureEllipsizeText;
								trunCatView.mX = xPosition;
								add(trunCatView);
							}
						}

						return FormattingType.LayoutFull;
					}

					if (isWordSeparatorDefined(ch)) {
						wordWrapIndex = j;
					}

					if (drawable != null || divder != null) {
						break;
					}
				}

				if (drawable != null) {
					boolean isTextInDrawable = attr.getTextInDrawable() != null;
					if (isTextInDrawable) {
						TextInDrawbleView textInDrawbleView = new TextInDrawbleView(this);
						textInDrawbleView.mStartOffset = startOffset;
						textInDrawbleView.mEndOffset = endOffset;
						textInDrawbleView.mWrappingWidth = width;
						textInDrawbleView.mX = xPosition;
						xPosition += textInDrawbleView.mWrappingWidth;
						textInDrawbleView.attr = attr;
						add(textInDrawbleView);
					} else {
						ImageView imageView = new ImageView(this);
						imageView.mStartOffset = startOffset;
						imageView.mEndOffset = endOffset;
						imageView.mWrappingWidth = width;
						imageView.mX = xPosition;
						xPosition += imageView.mWrappingWidth;
						imageView.attr = attr;
						add(imageView);
					}
				} else if (divder != null) {
					DividerView dividerView = new DividerView(this);
					dividerView.mStartOffset = startOffset;
					dividerView.mEndOffset = endOffset;
					dividerView.mWrappingWidth = width;
					dividerView.mX = xPosition;
					xPosition += dividerView.mWrappingWidth;
					dividerView.attr = attr;
					add(dividerView);
				} else {
					RunView runView = new RunView(this);
					runView.mStartOffset = startOffset;
					runView.mEndOffset = endOffset;
					runView.mWrappingWidth = sumWidth - xPosition;
					runView.mX = xPosition;
					xPosition += runView.mWrappingWidth;
					runView.attr = attr;
					add(runView);
				}
				mAcsent = Math.max(maxAscent, mAcsent);
				mDescent = Math.max(maxDescent, mDescent);
				objectHeight = Math.max(objectHeight, mAcsent + mDescent);
				mHeight = Math.max(mHeight, objectHeight);
			}

			return type;
		}

		public boolean isLineEndChar(char ch) {
			return ch == 0xa/*line feed(LF), new line(NL), end of line(EOL)*/|| ch == 0xd/*carriage return(CR)*/
				|| ch == 0xb/*vertical tabulation(VT)*/;
		}

	}

	/**
	 * 
	 * @author Hovan Yoo
	 *
	 */
	private class RunView extends AbstractView {

		public RunView(AbstractView parent) {
			super(parent);
		}

		Attr attr;

		@Override
		protected FormattingType loadChildren(int modelOffset, float wrappingWidth) {
			return FormattingType.LayoutFinished;
		}

		@Override
		protected void onDraw(Canvas canvas, float posX, float posY) {
			CharSequence text = getText();
			if (text != null) {
				canvas.save();
				posY = posY + ((LineView)mParent).getBaseLine();
				if (attr != null) {
					canvas.drawText(text, mStartOffset, mEndOffset, posX, posY, attr.getPaint());
					resetPaint();
				} else {
					canvas.drawText(text, mStartOffset, mEndOffset, posX, posY, mPaint);
				}
				canvas.restore();
			}
		}
	}

	/**
	 * 
	 * @author Hovan Yoo
	 *
	 */
	private static class BreakRunView extends AbstractView {

		public BreakRunView(AbstractView parent) {
			super(parent);
		}

		@Override
		protected FormattingType loadChildren(int modelOffset, float wrappingWidth) {
			return FormattingType.LayoutFinished;
		}

		@Override
		protected void onDraw(Canvas canvas, float posX, float posY) {
			//현재 break type 을 그리라는 요구는 없기 때문에
		}
	}

	/**
	 * 
	 * @author Hovan Yoo
	 *
	 */
	private class TrunCatView extends AbstractView {

		public TrunCatView(AbstractView parent) {
			super(parent);
		}

		@Override
		protected FormattingType loadChildren(int modelOffset, float wrappingWidth) {
			return FormattingType.LayoutFinished;
		}

		@Override
		protected void onDraw(Canvas canvas, float posX, float posY) {
			String text = getEllipsizeString();
			if (text != null) {
				canvas.save();
				posY = posY + ((LineView)mParent).getBaseLine();
				canvas.drawText(text, posX, posY, mPaint);
				canvas.restore();
			}
		}
	}

	/**
	 * 
	 * @author Hovan Yoo
	 *
	 */
	private class ImageView extends RunView {

		public ImageView(AbstractView parent) {
			super(parent);
		}

		Attr attr;

		@Override
		protected void onDraw(Canvas canvas, float posX, float posY) {
			if (attr != null) {
				Drawable drawable = attr.getDrawable();
				if (drawable != null) {
					canvas.save();
					LineView lineView = (LineView)mParent;
					if (drawable instanceof BitmapDrawable) {
						BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
						Bitmap bitmap = bitmapDrawable.getBitmap();
						posY += (lineView.mHeight - lineView.mSpacingAfter - (bitmap.getHeight())) / 2;
						canvas.drawBitmap(bitmap, posX, posY, bitmapDrawable.getPaint());
					} else {
						Rect bounds = drawable.getBounds();
						posY += (lineView.mHeight - lineView.mSpacingAfter - (bounds.bottom - bounds.top)) / 2;
						canvas.translate(posX, posY);
						drawable.draw(canvas);
					}
					canvas.restore();
				}
			}

		}
	}

	/**
	 * 
	 * @author Hovan Yoo
	 *
	 */
	private class TextInDrawbleView extends ImageView {

		public TextInDrawbleView(AbstractView parent) {
			super(parent);
		}

		@Override
		protected void onDraw(Canvas canvas, float posX, float posY) {
			super.onDraw(canvas, posX, posY);
			if (attr != null) {
				canvas.save();

				String text = attr.getTextInDrawable();
				if (text != null) {
					LineView lineView = (LineView)mParent;
					Paint paint = attr.getPaint();
					FontMetrics fontMtrics = paint.getFontMetrics();
					int alpha = 1; //보정값
					float with = paint.measureText(text) + alpha;
					float height = fontMtrics.ascent + fontMtrics.descent + fontMtrics.leading - alpha;
					canvas.drawText(text, posX + (mWrappingWidth - with) / 2, posY
						+ (lineView.mHeight - lineView.mSpacingAfter - height) / 2,
						attr.getPaint());
				}

				canvas.restore();
			}

		}
	}

	/**
	 * 
	 * @author Hovan Yoo
	 *
	 */
	private class DividerView extends RunView {

		public DividerView(AbstractView parent) {
			super(parent);
		}

		Attr attr;

		@Override
		protected void onDraw(Canvas canvas, float posX, float posY) {
			if (attr != null) {

				RichTextView.CellDividerSpan divider = attr.getDivider();
				if (divider != null) {
					canvas.save();
					float width = getPixel(divider.getWidth());
					float height = getPixel(divider.getHeight());
					posX += getPixel(divider.getLeftMargin());
					posY = (mParent.getHeight() - height) / 2;
					canvas.drawRect(posX, posY, posX + width, posY + height, attr.getPaint());
					resetPaint();
					canvas.restore();
				}

			}

		}
	}

	/**
	 * 
	 * @author Hovan Yoo
	 *
	 */
	public class Attr implements Cloneable {
		protected static final int BOLD_MASK = 0x00000001;
		protected static final int ITALIC_MASK = 0x00000002;

		protected float mFontSize = getPixel(14);

		protected int mStyle = 0;

		protected int mColor = 0xff000000;

		protected int mSelectedColor = 0xff000000;

		protected Drawable mDrawable;
		protected int mVerticalAlignment = ImageSpan.ALIGN_BOTTOM;

		protected RichTextView.CellDividerSpan mDivider;

		protected String mTextInDrawable;

		@Override
		public Attr clone() {
			try {
				return (Attr)super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError();
			}
		}

		public void setFontSize(float pixcel) {
			this.mFontSize = pixcel;
		}

		public float getFontSize() {
			return this.mFontSize;
		}

		public void setBold(boolean bold) {
			setFlags(bold ? BOLD_MASK : 0, BOLD_MASK);
		}

		public boolean isBold() {
			return isFlagsSet(BOLD_MASK, BOLD_MASK);
		}

		public void setItalic(boolean italic) {
			setFlags(italic ? ITALIC_MASK : 0, ITALIC_MASK);
		}

		public boolean isItalic() {
			return isFlagsSet(ITALIC_MASK, ITALIC_MASK);
		}

		public void setColor(int color) {
			if (this.mColor != color) {
				this.mColor = color;
			}
		}

		public int getColor() {
			return this.mColor;
		}

		public void setSelectedColor(int color) {
			if (this.mSelectedColor != color) {
				this.mSelectedColor = color;
			}
		}

		public int getSelectedColor() {
			return this.mSelectedColor;
		}

		public void setDrawable(Drawable drawble) {
			setDrawable(drawble, mVerticalAlignment);
		}

		public void setDrawable(Drawable drawble, int verticalAlignment) {
			if (this.mDrawable != drawble) {
				this.mDrawable = drawble;
				this.mVerticalAlignment = verticalAlignment;
			}
		}

		public Drawable getDrawable() {
			return this.mDrawable;
		}

		public int getVerticalAlignment() {
			return this.mVerticalAlignment;
		}

		public void setDivider(RichTextView.CellDividerSpan divider) {
			if (this.mDivider != divider) {
				this.mDivider = divider;
			}
		}

		public RichTextView.CellDividerSpan getDivider() {
			return this.mDivider;
		}

		public void setTextInDrawable(String textInDrawable) {
			if (this.mTextInDrawable != textInDrawable) {
				this.mTextInDrawable = textInDrawable;
			}
		}

		public String getTextInDrawable() {
			return this.mTextInDrawable;
		}

		private void setFlags(int flags, int mask) {
			mStyle = (mStyle & ~mask) | (flags & mask);
		}

		private boolean isFlagsSet(int flags, int mask) {
			return (mStyle & mask) == flags;
		}

		public float getStringWidth(String text) {
			checkPaint();
			return mPaint.measureText(text);
		}

		public float getStringWidth(char[] chars, int index, int count) {
			checkPaint();
			return mPaint.measureText(chars, index, count);
		}

		public Paint getPaint() {
			checkPaint();
			return mPaint;
		}

		private void checkPaint() {

			// font size
			mPaint.setTextSize(getFontSize());

			if (mStyle != 0) {
				if (isBold()) {
					mPaint.setFakeBoldText(true);
				}
				if (isItalic()) {
					mPaint.setTextSkewX(-0.25f);
				}
			}

			if (isSelected() || isPressed()) {
				mPaint.setColor(getSelectedColor());
			} else {
				mPaint.setColor(getColor());
			}
		}
	}

	/**
	 * forground 와 selected color
	 * 
	 * @author Hovan Yoo
	 *
	 */
	public static class ForegroundAndSelectedColorSpan extends CharacterStyle {

		private final int mForgroundColor;

		private final int mSelectedColor;

		public ForegroundAndSelectedColorSpan(int forgroundColor, int selectedColor) {
			mForgroundColor = forgroundColor;
			mSelectedColor = selectedColor;
		}

		public int getForgroundColor() {
			return mForgroundColor;
		}

		public int getSelectedColor() {
			return mSelectedColor;
		}

		@Override
		public void updateDrawState(TextPaint tp) {
		}
	}

	/**
	 * list cell 에 들어가는 Divider
	 * 
	 * @author Hovan Yoo
	 *
	 */
	public static class CellDividerSpan {

		private final int mForgroundColor;

		private final int mSelectedColor;

		private final float mWidth; //dp

		private final float mHeight; //dp

		private final float mLeftMargin; //dp

		private final float mRightMargin; //dp

		public CellDividerSpan() {
			mForgroundColor = Color.parseColor("#dad8d8");
			mSelectedColor = Color.parseColor("#5da3ee");
			mWidth = 0.67f;
			mHeight = 10.67f;
			mLeftMargin = 4.67f;
			mRightMargin = 4.67f;
		}

		public CellDividerSpan(int forgroundColor, int selectedColor, float width, float height, float leftMargin,
			float rightMargin) {
			mForgroundColor = forgroundColor;
			mSelectedColor = selectedColor;
			mWidth = width;
			mHeight = height;
			mLeftMargin = leftMargin;
			mRightMargin = rightMargin;
		}

		public int getForgroundColor() {
			return mForgroundColor;
		}

		public int getSelectedColor() {
			return mSelectedColor;
		}

		public float getWidth() {
			return mWidth;
		}

		public float getHeight() {
			return mHeight;
		}

		public float getLeftMargin() {
			return mLeftMargin;
		}

		public float getRightMargin() {
			return mRightMargin;
		}

	}

	/**
	 * bitmap 과 text 를 동시에 표현하기 위한 span
	 * 
	 * @author Hovan Yoo
	 *
	 */
	public static class TextInDrawableSpan {

		private Drawable mDrawable;
		private String mText;
		private float mFontSize = 12f;
		private int mColor = Color.BLACK;
		private int mSelectedColor = Color.BLACK;

		public TextInDrawableSpan(Drawable drawable, String text, float fontSize, int color, int selectedColor) {
			mDrawable = drawable;
			mText = text;
			mFontSize = fontSize;
			mColor = color;
			mSelectedColor = selectedColor;
		}

		public Drawable getDrawable() {
			return mDrawable;
		}

		public String getText() {
			return mText;
		}

		public float getFontSize() {
			return mFontSize;
		}

		public int getForgroundColor() {
			return mColor;
		}

		public int getSelectedColor() {
			return mSelectedColor;
		}

	}

	/**
	 * 언어에서 가장 기본적인 word seperator를 연산.
	 * 
	 */
	private boolean isBaseWordSeparatorDefined(char ch) {
		if (isSpaceChar(ch)) { // 스페이스(non-breaking space 포함)
			return true;
		} else if (isWordSeparator(ch) && ("_+-*/^=&\\".indexOf(ch) < 0)) {
			// _,+,-,*,/,^,=,&,통화표시기호 를 제외한 모든 기호는 앞의단어에 포함되며 뒤의 단어와 구분시켜 주는 구분자
			return true;
		} else {
			return false;
		}
	}

	private boolean isWordSeparator(char ch) {
		return isSeparator(ch) || isPunctuation(ch) || isSymbol(ch);
	}

	private boolean isSpaceChar(char ch) {
		return Character.isWhitespace(ch) || Character.isSpaceChar(ch) || ch == 12288;
	}

	private boolean isPunctuation(char ch) {
		switch (Character.getType(ch)) {
			case Character.CONNECTOR_PUNCTUATION:
			case Character.DASH_PUNCTUATION:
			case Character.START_PUNCTUATION:
			case Character.END_PUNCTUATION:
			case Character.INITIAL_QUOTE_PUNCTUATION:
			case Character.FINAL_QUOTE_PUNCTUATION:
			case Character.OTHER_PUNCTUATION:
				return true;
			default:
				return false;
		}
	}

	private boolean isSymbol(char ch) {
		switch (Character.getType(ch)) {
			case Character.MATH_SYMBOL:
			case Character.CURRENCY_SYMBOL:
			case Character.MODIFIER_SYMBOL:
			case Character.OTHER_SYMBOL:
				return true;
			default:
				return false;
		}
	}

	private boolean isSeparator(char ch) {
		switch (Character.getType(ch)) {
			case Character.SPACE_SEPARATOR:
			case Character.LINE_SEPARATOR:
			case Character.PARAGRAPH_SEPARATOR:
				return true;
			default:
				return false;
		}
	}
}
