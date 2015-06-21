package com.hovans.android.graphics;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Locale;

/**
 * Collection of utility functions used in this package. <br/>
 * Default Gallery의 코드를 참조하여 작성
 * 
 * @author Hovan
 * 
 */
public class GraphicsUtil {

	private static final String TAG = "Util";
	public static final int DIRECTION_LEFT = 0;
	public static final int DIRECTION_RIGHT = 1;
	public static final int DIRECTION_UP = 2;
	public static final int DIRECTION_DOWN = 3;

	public static final int UNCONSTRAINED = -1;

	private static OnClickListener sNullOnClickListener;

	private GraphicsUtil() {
	}

	/**
	 * Rotates the bitmap by the specified degree. </br>
	 * If a new bitmap is created, the original bitmap is recycled.
	 * @param bitmap bitmap for rotating
	 * @param degrees
	 * @return new bitmap
	 */
	public static Bitmap rotate(Bitmap bitmap, int degrees) {
		if (degrees != 0 && bitmap != null) {
			Matrix m = new Matrix();
			m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
			try {
				Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
				if (bitmap != b2) {
					bitmap.recycle();
					bitmap = b2;
				}
			} catch (OutOfMemoryError ex) {
				// We have no memory to rotate. Return the original bitmap.
			}
		}
		return bitmap;
	}

	/**
	 * Uri로 File정보를 읽어오거나 content uri로 orientation정보를 읽어옴
	 * @param cr
	 * @param uri
	 * @return rotation degree
	 * @see ExifInterface#ORIENTATION_ROTATE_90
	 * @see ExifInterface#ORIENTATION_ROTATE_180
	 * @see ExifInterface#ORIENTATION_ROTATE_270
	 */
	public static int getExifOrientation(ContentResolver cr, Uri uri) {
		int degree = 0;

		if (uri.getScheme().equals("content")) {
			String[] projection = { Images.ImageColumns.ORIENTATION };
			Cursor c = cr.query(uri, projection, null, null, null);
			if (c.moveToFirst()) {
				degree = c.getInt(0);
			}
		} else if (uri.getScheme().equals("file")) {
			try {
				ExifInterface exif = new ExifInterface(uri.getPath());

				int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
				if (orientation != -1) {
					// We only recognize a subset of orientation tag values.
					switch (orientation) {
					case ExifInterface.ORIENTATION_ROTATE_90:
						degree = 90;
						break;
					case ExifInterface.ORIENTATION_ROTATE_180:
						degree = 180;
						break;
					case ExifInterface.ORIENTATION_ROTATE_270:
						degree = 270;
						break;
					}

				}
			} catch (IOException e) {
				Log.e(TAG, "Error checking exif", e);
			}
		}

		return degree;
	}

	/**
	 * Compute the sample size as a function of minSideLength and maxNumOfPixels. minSideLength is used to specify that
	 * minimal width or height of a bitmap. maxNumOfPixels is used to specify the maximal size in pixels that is
	 * tolerable in terms of memory usage. <br/>
	 * <br/>
	 * The function returns a sample size based on the constraints. Both size and minSideLength can be passed in as
	 * IImage.UNCONSTRAINED, which indicates no care of the corresponding constraint. The functions prefers returning a
	 * sample size that generates a smaller bitmap, unless minSideLength = IImage.UNCONSTRAINED.<br/>
	 * <br/>
	 * Also, the function rounds up the sample size to a power of 2 or multiple of 8 because BitmapFactory only honors
	 * sample size this way. For example, BitmapFactory downsamples an image by 2 even though the request is 3. So we
	 * round up the sample size to avoid OOM.<br/>
	 * @param options
	 * @param minSideLength
	 * @param maxNumOfPixels
	 * @return rounded size
	 */
	public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == UNCONSTRAINED) ? 128 : (int) Math.min(Math.floor(w / minSideLength),
				Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if ((maxNumOfPixels == UNCONSTRAINED) && (minSideLength == UNCONSTRAINED)) {
			return 1;
		} else if (minSideLength == UNCONSTRAINED) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

	/** Whether we should recycle the input (unless the output is the input). */
	public static final boolean RECYCLE_INPUT = true;
	public static final boolean NO_RECYCLE_INPUT = false;

	public static Bitmap transform(Matrix scaler, Bitmap source, int targetWidth, int targetHeight, boolean scaleUp,
			boolean recycle) {
		int deltaX = source.getWidth() - targetWidth;
		int deltaY = source.getHeight() - targetHeight;
		if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
			/*
			 * In this case the bitmap is smaller, at least in one dimension, than the target. Transform it by placing
			 * as much of the image as possible into the target and leaving the top/bottom or left/right (or both)
			 * black.
			 */
			Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(b2);

			int deltaXHalf = Math.max(0, deltaX / 2);
			int deltaYHalf = Math.max(0, deltaY / 2);
			Rect src = new Rect(deltaXHalf, deltaYHalf, deltaXHalf + Math.min(targetWidth, source.getWidth()),
					deltaYHalf + Math.min(targetHeight, source.getHeight()));
			int dstX = (targetWidth - src.width()) / 2;
			int dstY = (targetHeight - src.height()) / 2;
			Rect dst = new Rect(dstX, dstY, targetWidth - dstX, targetHeight - dstY);
			c.drawBitmap(source, src, dst, null);
			if (recycle) {
				source.recycle();
			}
			return b2;
		}
		float bitmapWidthF = source.getWidth();
		float bitmapHeightF = source.getHeight();

		float bitmapAspect = bitmapWidthF / bitmapHeightF;
		float viewAspect = (float) targetWidth / targetHeight;

		if (bitmapAspect > viewAspect) {
			float scale = targetHeight / bitmapHeightF;
			if (scale < .9F || scale > 1F) {
				scaler.setScale(scale, scale);
			} else {
				scaler = null;
			}
		} else {
			float scale = targetWidth / bitmapWidthF;
			if (scale < .9F || scale > 1F) {
				scaler.setScale(scale, scale);
			} else {
				scaler = null;
			}
		}

		Bitmap b1;
		if (scaler != null) {
			// this is used for minithumb and crop, so we want to filter here.
			b1 = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), scaler, true);
		} else {
			b1 = source;
		}

		if (recycle && b1 != source) {
			source.recycle();
		}

		int dx1 = Math.max(0, b1.getWidth() - targetWidth);
		int dy1 = Math.max(0, b1.getHeight() - targetHeight);

		Bitmap b2 = Bitmap.createBitmap(b1, dx1 / 2, dy1 / 2, targetWidth, targetHeight);

		if (b2 != b1) {
			if (recycle || b1 != source) {
				b1.recycle();
			}
		}

		return b2;
	}

	public static <T> int indexOf(T[] array, T s) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(s)) {
				return i;
			}
		}
		return -1;
	}

	public static void closeSilently(Closeable c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (Throwable t) {
			// do nothing
		}
	}

	public static void closeSilently(ParcelFileDescriptor c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (Throwable t) {
			// do nothing
		}
	}
	
	/**
	 * Make a bitmap from a given Uri. <br />
	 * See this. {@link #makeBitmap(int, int, Uri, ContentResolver, ParcelFileDescriptor, boolean)}
	 * 
	 * @param uri
	 */
	public static Bitmap makeBitmap(int maxNumOfPixels, Uri uri, ContentResolver cr) {
		return makeBitmap(UNCONSTRAINED, maxNumOfPixels, uri, cr, false);
	}

	/**
	 * Make a bitmap from a given Uri. <br />
	 * See this. {@link #makeBitmap(int, int, Uri, ContentResolver, ParcelFileDescriptor, boolean)}
	 * 
	 * @param uri
	 */
	public static Bitmap makeBitmap(int minSideLength, int maxNumOfPixels, Uri uri, ContentResolver cr,
			boolean isMutable) {
		ParcelFileDescriptor input = null;
		try {
			input = cr.openFileDescriptor(uri, "r");
			Bitmap result = makeBitmap(minSideLength, maxNumOfPixels, uri, cr, input, isMutable);

			return result;
		} catch (IOException ex) {
			return null;
		} finally {
			closeSilently(input);
		}
	}

	/**
	 * Make a bitmap from a given parcelFileDescriptor. <br />
	 * See this. {@link #makeBitmap(int, int, Uri, ContentResolver, ParcelFileDescriptor, boolean)}
	 * 
	 * @param minSideLength
	 * @param maxNumOfPixels
	 * @param pfd
	 * @param isMutable
	 * @return Result bitmap
	 */
	public static Bitmap makeBitmap(int minSideLength, int maxNumOfPixels, ParcelFileDescriptor pfd, boolean isMutable) {
		return makeBitmap(minSideLength, maxNumOfPixels, null, null, pfd, isMutable);
	}

	/**
	 * 먼저 Bitmap의 out bound를 계산한 다음 Bitmap을 만들어 준다.
	 * Rotation정보가 있을 경우 반영하여 결과를 리턴한다.<br/>
	 * <br/>
	 * <font color=#ff0000>Caution!</font>
	 * isMutable이 true일 경우 속도에 영향을 많이 주므로 조심해야 함!<br>
	 * <font color=#ff0000>Caution!</font>
	 * Need permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
	 * in AndroidManifest.xml if isMutable is true.
	 * 
	 * @param minSideLength
	 * @param maxNumOfPixels
	 * @param uri
	 * @param cr
	 * @param pfd
	 * @param isMutable
	 * @return Result bitmap
	 */
	public static Bitmap makeBitmap(int minSideLength, int maxNumOfPixels, Uri uri, ContentResolver cr,
			ParcelFileDescriptor pfd, boolean isMutable) {
		try {
			Bitmap result;
			if (pfd == null)
				pfd = makeInputStream(uri, cr);
			if (pfd == null)
				return null;
			BitmapFactory.Options options = new BitmapFactory.Options();

			FileDescriptor fd = pfd.getFileDescriptor();
			options.inJustDecodeBounds = true;
			BitmapManager.instance().decodeFileDescriptor(fd, options);
			if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
				return null;
			}
			options.inSampleSize = computeSampleSize(options, minSideLength, maxNumOfPixels);
			options.inJustDecodeBounds = false;

			options.inDither = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;

			if (!isMutable) {
				result = BitmapManager.instance().decodeFileDescriptor(fd, options);

			} else {
				result = BitmapManager.instance().decodeFileDescriptor(fd, options);
				File file = new File(Environment.getExternalStorageDirectory() + "/image.tmp");
				if (!file.exists()) file.createNewFile();

				// Open an RandomAccessFile
				/*
				 * Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
				 * into AndroidManifest.xml file
				 */
				RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

				// get the width and height of the source bitmap.
				int width = result.getWidth();
				int height = result.getHeight();

				// Copy the byte to the file
				// Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
				FileChannel channel = randomAccessFile.getChannel();
				MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, width * height * 4);
				result.copyPixelsToBuffer(map);
				// recycle the source bitmap, this will be no longer used.
				result.recycle();
				// Create a new bitmap to load the bitmap again.
				result = Bitmap.createBitmap(width, height, Config.ARGB_8888);
				map.position(0);
				// load it back from temporary
				result.copyPixelsFromBuffer(map);
				// close the temporary file and channel , then delete that also
				channel.close();
				randomAccessFile.close();
				file.delete();
			}

			int orientation = getExifOrientation(cr, uri);
			if (orientation != 0) result = rotate(result, orientation);

			return result;
		} catch (Exception ex) {
			Log.e(TAG, "Got oom exception ", ex);
			return null;
		} finally {
			closeSilently(pfd);
		}
	}

	private static ParcelFileDescriptor makeInputStream(Uri uri, ContentResolver cr) {
		try {
			return cr.openFileDescriptor(uri, "r");
		} catch (IOException ex) {
			return null;
		}
	}

	public static synchronized OnClickListener getNullOnClickListener() {
		if (sNullOnClickListener == null) {
			sNullOnClickListener = new OnClickListener() {
				public void onClick(View v) {
				}
			};
		}
		return sNullOnClickListener;
	}

	public static void Assert(boolean cond) {
		if (!cond) {
			throw new AssertionError();
		}
	}

	public static boolean equals(String a, String b) {
		// return true if both string are null or the content equals
		return a == b || a.equals(b);
	}
	
	static public void toFile(Context context, Bitmap bmp, int nQuality, String szPath) throws IOException {
		OutputStream os = new FileOutputStream(szPath);
		try {
			Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
			szPath = szPath.toLowerCase(Locale.getDefault());
			if (szPath.endsWith("png")) { //$NON-NLS-1$
				format = Bitmap.CompressFormat.PNG;
			} else {
				format = Bitmap.CompressFormat.JPEG;
			}
			bmp.compress(format, nQuality, os);
		} finally {
			os.close();
		}
	}

	public static Bitmap fastblur(Bitmap sentBitmap, int radius) {

		// Stack Blur v1.0 from
		// http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
		//
		// Java Author: Mario Klingemann <mario at quasimondo.com>
		// http://incubator.quasimondo.com
		// created Feburary 29, 2004
		// Android port : Yahel Bouaziz <yahel at kayenko.com>
		// http://www.kayenko.com
		// ported april 5th, 2012

		// This is a compromise between Gaussian Blur and Box blur
		// It creates much better looking blurs than Box Blur, but is
		// 7x faster than my Gaussian Blur implementation.
		//
		// I called it Stack Blur because this describes best how this
		// filter works internally: it creates a kind of moving stack
		// of colors whilst scanning through the image. Thereby it
		// just has to add one new block of color to the right side
		// of the stack and remove the leftmost color. The remaining
		// colors on the topmost layer of the stack are either added on
		// or reduced by one, depending on if they are on the right or
		// on the left side of the stack.
		//
		// If you are using this algorithm in your code please add
		// the following line:
		//
		// Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

		Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

		if (radius < 1) {
			return (null);
		}

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		int[] pix = new int[w * h];
		Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.getPixels(pix, 0, w, 0, 0, w, h);

		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;

		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];

		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}

		yw = yi = 0;

		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;

		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = pix[yi + Math.min(wm, Math.max(i, 0))];
				sir = stack[i + radius];
				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);
				rbs = r1 - Math.abs(i);
				rsum += sir[0] * rbs;
				gsum += sir[1] * rbs;
				bsum += sir[2] * rbs;
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
			}
			stackpointer = radius;

			for (x = 0; x < w; x++) {

				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];

				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[(stackpointer) % div];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;

				sir = stack[i + radius];

				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];

				rbs = r1 - Math.abs(i);

				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;

				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}

				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				// Preserve alpha channel: ( 0xff000000 & pix[yi] )
				pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];

				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi += w;
			}
		}

		Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.setPixels(pix, 0, w, 0, 0, w, h);

		return (bitmap);
	}
}
