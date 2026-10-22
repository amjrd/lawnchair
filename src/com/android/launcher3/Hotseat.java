/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import static com.android.launcher3.LauncherAnimUtils.VIEW_TRANSLATE_X;
import static com.android.launcher3.util.MultiTranslateDelegate.INDEX_BUBBLE_ADJUSTMENT_ANIM;
import static com.android.launcher3.util.MultiTranslateDelegate.INDEX_WIDGET_CENTERING;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.celllayout.CellPosMapper;
import com.android.launcher3.celllayout.CellPosMapper.CellPos;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.MultiPropertyFactory;
import com.android.launcher3.util.MultiPropertyFactory.MultiProperty;
import com.android.launcher3.util.MultiTranslateDelegate;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.DoubleShadowBubbleTextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import app.lawnchair.preferences.PreferenceManager;
import app.lawnchair.preferences2.PreferenceCacheExtensionsKt;
import app.lawnchair.preferences2.PreferenceManager2;

/**
 * View class that represents the hotseat (aka. the dock)
 */
public class Hotseat extends FrameLayout implements Insettable {

    // Ratio of hotseat height to transfer to workspace if needed to match design specs
    public static final float WORKSPACE_TO_HOTSEAT_HEIGHT_RATIO = 0.44f;

    // Translation property for hotseat icons
    public static final int ICONS_TRANSLATION_X_EXTRA_PINS = 0;
    public static final int ICONS_TRANSLATION_X_NAV_BAR_ANIM = 1;
    public static final int ICONS_TRANSLATION_X_HOTSEAT_PINS = 2;
    public static final int ICONS_TRANSLATION_X_SPLIT = 3;
    private static final int ICONS_TRANSLATION_X_CHANNELS_COUNT = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ICONS_TRANSLATION_X_EXTRA_PINS, ICONS_TRANSLATION_X_NAV_BAR_ANIM,
            ICONS_TRANSLATION_X_HOTSEAT_PINS, ICONS_TRANSLATION_X_SPLIT})
    public @interface IconsTranslationX {}

    // Alpha channel for QSB
    public static final int QSB_ALPHA_OF_WORKSPACE_SCALE = 0;
    private static final int ALPHA_CHANNEL_CHANNELS_COUNT = 1;

    private final ActivityContext mActivity;

    private boolean mHasVerticalHotseat;
    private Workspace<?> mWorkspace;
    private boolean mSendClicksToWorkspace;

    private final View mQsb;
    private final View mIconsContainer;

    private final MultiPropertyFactory<View> mIconsTranslationXFactory;
    private @Nullable MultiTranslateDelegate mQsbTranslationX;
    private final MultiValueAlpha mQsbAlphaChannels;

    private PreferenceManager preferenceManager;
    private PreferenceManager2 preferenceManager2;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mActivity = ActivityContext.lookupContext(context);
        mHasVerticalHotseat = mActivity.getDeviceProfile().isVerticalBarLayout();

        preferenceManager = PreferenceManager.getInstance(context);
        preferenceManager2 = PreferenceManager2.getInstance(context);

        mQsb = LayoutInflater.from(context).inflate(preferenceManager2.getHotseatMode().get().getLayoutResourceId(), this, false);
        addView(mQsb);
        mIconsContainer = LayoutInflater.from(context).inflate(R.layout.hotseat_icons_container, this, false);
        addView(mIconsContainer);

        // Sets the delegate for the Qsb translation X in case it is reorderable.
        if (mQsb instanceof Reorderable qsbReorderable) {
            mQsbTranslationX = qsbReorderable.getTranslateDelegate()
                    .getTranslationX(MultiTranslateDelegate.INDEX_NAV_BAR_ANIM);
        }
        mIconsTranslationXFactory = new MultiPropertyFactory<>(mIconsContainer,
                VIEW_TRANSLATE_X, ICONS_TRANSLATION_X_CHANNELS_COUNT, Float::sum);
        mQsbAlphaChannels = new MultiValueAlpha(mQsb, ALPHA_CHANNEL_CHANNELS_COUNT);
        mQsbAlphaChannels.setUpdateVisibility(true);

        setUpBackground();
        setClipChildren(false);
        setClipToPadding(false);
    }

    private void setUpBackground() {
        var displayMetrics = getResources().getDisplayMetrics();
        float density = displayMetrics.density;

        // iOS floating dock margins (dp to px)
        int insetHorizontalLeft = preferenceManager.getHotseatBGHorizontalInsetLeft().get();
        if (insetHorizontalLeft <= 0) {
            insetHorizontalLeft = (int) (18 * density);
        }
        int insetHorizontalRight = preferenceManager.getHotseatBGHorizontalInsetRight().get();
        if (insetHorizontalRight <= 0) {
            insetHorizontalRight = (int) (18 * density);
        }
        int insetVerticalTop = preferenceManager.getHotseatBGVerticalInsetTop().get();
        if (insetVerticalTop <= 0) {
            insetVerticalTop = (int) (6 * density);
        }
        int insetVerticalBottom = preferenceManager.getHotseatBGVerticalInsetBottom().get();
        if (insetVerticalBottom <= 0) {
            insetVerticalBottom = (int) (14 * density);
        }

        // iOS rounded squircle pill corner radius (28dp)
        float cornerRadiusDp = PreferenceCacheExtensionsKt.firstCached(
                preferenceManager2.getHotseatBackgroundCornerRadius());
        if (cornerRadiusDp < 24f) {
            cornerRadiusDp = 28f;
        }
        float cornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                cornerRadiusDp,
                displayMetrics);

        // iOS Frosted Glass effect: semi-transparent with adaptive light/dark mode
        boolean isDark = (getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int transparency = preferenceManager.getHotseatBGAlpha().get();
        if (transparency >= 100 || transparency <= 0) {
            transparency = isDark ? 40 : 45;
        }
        int alphaValue = (transparency * 255) / 100;

        int finalColor = isDark 
                ? Color.argb(alphaValue, 45, 45, 48) 
                : Color.argb(alphaValue, 245, 245, 250);

        GradientDrawable background = new GradientDrawable();
        background.setColor(finalColor);
        background.setCornerRadius(cornerRadius);

        // Subtle iOS frosted glass outline border
        int strokeWidth = Math.max(1, (int) (1 * density));
        int strokeColor = isDark 
                ? Color.argb(40, 255, 255, 255) 
                : Color.argb(80, 255, 255, 255);
        background.setStroke(strokeWidth, strokeColor);

        InsetDrawable bg = new InsetDrawable(background,
                insetHorizontalLeft, insetVerticalTop, insetHorizontalRight, insetVerticalBottom);
        setBackground(bg);
    }

    /** Provides translation X for hotseat icons for the channel. */
    public MultiProperty getIconsTranslationX(@IconsTranslationX int channelId) {
        return mIconsTranslationXFactory.get(channelId);
    }

    /** Provides translation X for hotseat Qsb. */
    @Nullable
    public MultiProperty getQsbTranslationX() {
        return mQsbTranslationX;
    }

    /** Provides alpha property for hotseat Qsb. */
    public MultiValueAlpha.AlphaProperty getQsbAlpha() {
        return mQsbAlphaChannels.getProperty(QSB_ALPHA_OF_WORKSPACE_SCALE);
    }

    /**
     * Sets whether the workspace is in multi-window mode.
     */
    public void setMultiWindowMode(boolean isMultiWindow) {
        mIconsTranslationXFactory.get(INDEX_BUBBLE_ADJUSTMENT_ANIM).setValue(0);
        mIconsTranslationXFactory.get(INDEX_WIDGET_CENTERING).setValue(0);
    }

    /**
     * Returns whether the hotseat is vertical.
     */
    public boolean isVertical() {
        return mHasVerticalHotseat;
    }

    /**
     * Sets the workspace for the hotseat.
     */
    public void setWorkspace(Workspace<?> workspace) {
        mWorkspace = workspace;
    }

    /**
     * Sets whether the hotseat should send clicks to the workspace.
     */
    public void setSendClicksToWorkspace(boolean sendClicksToWorkspace) {
        mSendClicksToWorkspace = sendClicksToWorkspace;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We don't want any clicks to go through to the workspace unless specified.
        if (!mSendClicksToWorkspace) {
            return false;
        }
        // Send the clicks to the workspace.
        return mWorkspace != null && mWorkspace.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // We don't want any clicks to go through to the workspace unless specified.
        if (!mSendClicksToWorkspace) {
            return false;
        }
        // Send the clicks to the workspace.
        return mWorkspace != null && mWorkspace.onTouchEvent(event);
    }

    /**
     * Sets the accessibility delegate for the hotseat icons.
     */
    public void setDragAndDropAccessibilityDelegate(
            DragAndDropAccessibilityDelegate accessibilityDelegate) {
        // Implementation can delegate to child views if needed
    }

    /**
     * Sets the insets for the hotseat.
     */
    @Override
    public void setInsets(Rect insets) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        DeviceProfile dp = mActivity.getDeviceProfile();

        if (dp.isVerticalBarLayout()) {
            mHasVerticalHotseat = true;
            lp.gravity = dp.isSeascape() ? Gravity.LEFT : Gravity.RIGHT;
            lp.width = dp.hotseatBarSizePx + insets.left + insets.right;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            setPadding(insets.left, insets.top, insets.right, insets.bottom);
        } else {
            mHasVerticalHotseat = false;
            lp.gravity = Gravity.BOTTOM;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = dp.hotseatBarSizePx + insets.bottom;
            setPadding(insets.left, 0, insets.right, insets.bottom);
        }
        setLayoutParams(lp);

        InsettableFrameLayout.dispatchInsets(this, insets);
    }

    /**
     * Returns the QSB view.
     */
    public View getQsb() {
        return mQsb;
    }

    /**
     * Returns the icons container view.
     */
    public View getIconsContainer() {
        return mIconsContainer;
    }

    /**
     * Resets the layout.
     */
    public void resetLayout(boolean isHotseatHorizontal) {
        // Reset subviews if needed
    }
}
