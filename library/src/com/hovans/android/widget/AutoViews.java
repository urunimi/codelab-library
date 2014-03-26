package com.hovans.android.widget;

import java.lang.reflect.Field;

import android.app.Activity;
import android.view.View;

import com.hovans.android.constant.DebugConfig;
import com.hovans.android.log.LogByCodeLab;

/**
 * {@link View#findViewById(int)} 함수를 자동으로 구현하기 위한 static method 모음. <br/>
 * XML의 ID와 멤버변수의 이름이 같으면 알아서 mapping을 해준다.
 * 
 * @author Hovan Yoo
 */
public class AutoViews {

	public static void mappingViews(Activity activity, Class<? extends Activity> clazz) {
		Field[] fields = clazz.getDeclaredFields();

		for (Field field : fields) {
			if (View.class.isAssignableFrom(field.getType()) == false) {
				continue;
			}
			String identifierString = field.getName();

			int id = activity.getResources().getIdentifier(identifierString, "id", activity.getPackageName());

			if (id == 0)
				continue;

			View view = activity.findViewById(id);

			if (view == null)
				continue;
			if (field.getType().isAssignableFrom(view.getClass())) {
				try {
					field.setAccessible(true);
					field.set(activity, view);
					if(DebugConfig.isShowLogCat()) LogByCodeLab.v("Inject success : " + field.getName() + "," + id + "," + view);
				} catch (Exception e) {
					if(DebugConfig.isShowLogCat()) LogByCodeLab.e(e, "Inject failed : " + field.getName() + "," + id + "," + view);
				}
			}
		}
	}

	public static void mappingViews(Activity activity) {
		mappingViews(activity, activity.getClass());
	}

	public static void mappingViews(View viewGroup, Object target) {
		Field[] fields = target.getClass().getDeclaredFields();

		for (Field field : fields) {
			if (View.class.isAssignableFrom(field.getType()) == false) {
				continue;
			}
			String identifierString = field.getName();
			int id = viewGroup.getResources().getIdentifier(identifierString, "id",
					viewGroup.getContext().getPackageName());

			if (id == 0)
				continue;

			View view = viewGroup.findViewById(id);

			if (view == null)
				continue;
			if (field.getType().isAssignableFrom(view.getClass())) {
				try {
					field.setAccessible(true);
					field.set(target, view);
					if(DebugConfig.isShowLogCat()) LogByCodeLab.v("Inject success : " + field.getName() + "," + id + "," + view);
				} catch (Exception e) {
					if(DebugConfig.isShowLogCat()) LogByCodeLab.e(e, "Inject failed : " + field.getName() + "," + id + "," + view);
				}
			}
		}
	}
}
