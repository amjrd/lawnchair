package com.android.launcher3.allapps;

import static android.view.View.GONE;
import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_BOTTOM_LEFT;
import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_BOTTOM_RIGHT;
import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_NOTHING;
import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_TOP_LEFT;
import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_TOP_RIGHT;
import static com.android.launcher3.allapps.UserProfileManager.STATE_DISABLED;
import static com.android.launcher3.allapps.UserProfileManager.STATE_ENABLED;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.allapps.search.SearchAdapterProvider;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.views.ActivityContext;

/**
 * Adapter for all the apps.
 *
 * @param <T> Type of context inflating all apps.
 */
public abstract class BaseAllAppsAdapter<T extends Context & ActivityContext> extends
        RecyclerView.Adapter<BaseAllAppsAdapter.ViewHolder> {

    public static final String TAG = "BaseAllAppsAdapter";

    public static final int VIEW_TYPE_ICON = 1 << 1;
    public static final int VIEW_TYPE_MASK_ICON = VIEW_TYPE_ICON;

    public static final int VIEW_TYPE_EMPTY_SEARCH = 1 << 2;
    public static final int VIEW_TYPE_ALL_APPS_DIVIDER = 1 << 3;

    public static final int VIEW_TYPE_WORK_EDU_CARD = 1 << 4;
    public static final int VIEW_TYPE_WORK_DISABLED_CARD = 1 << 5;
    public static final int VIEW_TYPE_PRIVATE_SPACE_HEADER = 1 << 6;
    public static final int VIEW_TYPE_PRIVATE_SPACE_SYS_APPS_DIVIDER = 1 << 7;
    public static final int VIEW_TYPE_MASK_PRIVATE_SPACE_HEADER = VIEW_TYPE_PRIVATE_SPACE_HEADER;

    public static final int VIEW_TYPE_MASK_DIVIDER = VIEW_TYPE_ALL_APPS_DIVIDER
            | VIEW_TYPE_PRIVATE_SPACE_SYS_APPS_DIVIDER;

    public static final int NEXT_ID = 8;

    protected final AlphabeticalAppsList<T> mApps;
    protected final LayoutInflater mLayoutInflater;
    protected final OnClickListener mOnIconClickListener;
    protected OnLongClickListener mOnIconLongClickListener = null;
    protected OnFocusChangeListener mIconFocusListener = null;

    protected final SearchAdapterProvider<?> mAdapterProvider;
    protected final T mActivityContext;

    public BaseAllAppsAdapter(T activityContext, LayoutInflater inflater,
            AlphabeticalAppsList<T> apps, SearchAdapterProvider<?> adapterProvider) {
        mActivityContext = activityContext;
        mApps = apps;
        mLayoutInflater = inflater;
        mOnIconClickListener = mActivityContext.getItemOnClickListener();
        mAdapterProvider = adapterProvider;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View v) {
            super(v);
        }
    }

    public void setIconFocusListener(OnFocusChangeListener focusListener) {
        mIconFocusListener = focusListener;
    }

    public void setOnIconLongClickListener(OnLongClickListener onIconLongClickListener) {
        mOnIconLongClickListener = onIconLongClickListener;
    }

    public abstract RecyclerView.LayoutManager getLayoutManager();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_ICON:
                int layout = mActivityContext.getDeviceProfile().inv.enableTwoLinesInAllApps
                        ? R.layout.all_apps_icon_twoline : R.layout.all_apps_icon;
                BubbleTextView icon = (BubbleTextView) mLayoutInflater.inflate(
                        layout, parent, false);
                icon.setLongPressTimeoutFactor(1f);
                icon.setOnFocusChangeListener(mIconFocusListener);
                icon.setOnClickListener(mOnIconClickListener);
                icon.setOnLongClickListener(mOnIconLongClickListener);
                icon.getLayoutParams().height =
                        mActivityContext.getDeviceProfile().getAllAppsProfile().getCellHeightPx();
                setupAppCardBackground(icon);
                return new ViewHolder(icon);
            case VIEW_TYPE_EMPTY_SEARCH:
                return new ViewHolder(mLayoutInflater.inflate(R.layout.all_apps_empty_search,
                        parent, false));
            case VIEW_TYPE_ALL_APPS_DIVIDER, VIEW_TYPE_PRIVATE_SPACE_SYS_APPS_DIVIDER:
                return new ViewHolder(mLayoutInflater.inflate(
                        R.layout.private_space_divider, parent, false));
            case VIEW_TYPE_WORK_EDU_CARD:
                return new ViewHolder(mLayoutInflater.inflate(
                        R.layout.work_apps_edu, parent, false));
            case VIEW_TYPE_WORK_DISABLED_CARD:
                return new ViewHolder(mLayoutInflater.inflate(
                        R.layout.work_apps_paused, parent, false));
            case VIEW_TYPE_PRIVATE_SPACE_HEADER:
                return new ViewHolder(mLayoutInflater.inflate(
                        R.layout.private_space_header, parent, false));
            case R.layout.all_apps_folder_icon:
                FolderIcon folderIcon = (FolderIcon) mLayoutInflater.inflate(
                        R.layout.all_apps_folder_icon, parent, false);
                folderIcon.setTextVisibility(true);
                folderIcon.getLayoutParams().height =
                        mActivityContext.getDeviceProfile().getAllAppsProfile().getCellHeightPx();
                setupAppCardBackground(folderIcon);
                return new ViewHolder(folderIcon);
            case R.layout.all_apps_card:
                FrameLayout fl = new FrameLayout(parent.getContext());
                fl.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        mActivityContext.getDeviceProfile().getAllAppsProfile().getCellHeightPx()));
                return new ViewHolder(fl);
            default:
                if (mAdapterProvider.isViewSupported(viewType)) {
                    return mAdapterProvider.onCreateViewHolder(mLayoutInflater, parent, viewType);
                }
                throw new RuntimeException("Unexpected view type" + viewType);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.itemView.setVisibility(View.VISIBLE);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_ICON: {
                AdapterItem adapterItem = mApps.getAdapterItems().get(position);
                BubbleTextView icon = (BubbleTextView) holder.itemView;
                icon.reset();
                PrivateProfileManager privateProfileManager = mApps.getPrivateProfileManager();
                boolean skipUserBadge = !mApps.hasSearchResults()
                        && (mApps.isWorkAppsList()
                                || (privateProfileManager != null
                                        && privateProfileManager.isPrivateSpaceItem(adapterItem)));
                icon.setSkipUserBadge(skipUserBadge);
                icon.applyFromApplicationInfo(adapterItem.itemInfo);
                icon.setOnFocusChangeListener(mIconFocusListener);
                if (privateProfileManager != null) {
                    boolean isPrivateSpaceItem =
                            privateProfileManager.isPrivateSpaceItem(adapterItem);
                    if (icon.getAlpha() == 0 || icon.getAlpha() == 1) {
                        icon.setAlpha(isPrivateSpaceItem
                                && privateProfileManager.isStateTransitioning()
                                && (privateProfileManager.isScrolling() ||
                                    privateProfileManager.getReadyToAnimate())
                                && privateProfileManager.getCurrentState() == STATE_ENABLED
                                ? 0 : 1);
                    }
                }
                setupAppCardBackground(icon);
                break;
            }
            case VIEW_TYPE_EMPTY_SEARCH: {
                TextView emptyView = (TextView) holder.itemView;
                emptyView.setText(mApps.getEmptySearchText());
                break;
            }
            case VIEW_TYPE_ALL_APPS_DIVIDER:
                break;
            case VIEW_TYPE_WORK_DISABLED_CARD:
                ((WorkPausedCard) holder.itemView).getWorkDisabledCardController()
                        .setWorkAppsPaused(mApps.getWorkProfileManager().isWorkModePaused());
                break;
            case VIEW_TYPE_PRIVATE_SPACE_HEADER:
                PrivateProfileManager privateProfileManager = mApps.getPrivateProfileManager();
                if (privateProfileManager != null) {
                    privateProfileManager.bindPrivateSpaceHeaderView(holder.itemView);
                }
                break;
            case VIEW_TYPE_PRIVATE_SPACE_SYS_APPS_DIVIDER:
                PrivateProfileManager ppm = mApps.getPrivateProfileManager();
                if (ppm != null) {
                    ppm.bindPrivateSpaceSysAppsDivider(holder.itemView);
                }
                break;
            case R.layout.all_apps_folder_icon: {
                AdapterItem adapterItem = mApps.getAdapterItems().get(position);
                FolderIcon folderIcon = (FolderIcon) holder.itemView;
                folderIcon.reset();
                folderIcon.setTextVisibility(true);
                folderIcon.applyFromFolderInfo((FolderInfo) adapterItem.itemInfo);
                folderIcon.setOnFocusChangeListener(mIconFocusListener);
                folderIcon.getLayoutParams().height =
                        mActivityContext.getDeviceProfile().getAllAppsProfile().getCellHeightPx();
                setupAppCardBackground(folderIcon);
                break;
            }
            case R.layout.all_apps_card: {
                FrameLayout fl = (FrameLayout) holder.itemView;
                fl.removeAllViews();
                AdapterItem adapterItem = mApps.getAdapterItems().get(position);
                if (adapterItem.composeView != null) {
                    fl.addView(adapterItem.composeView);
                }
                break;
            }
            default:
                if (mAdapterProvider.isViewSupported(holder.getItemViewType())) {
                    mAdapterProvider.onBindView(holder, position);
                }
        }
    }

    @Override
    public boolean onFailedToRecycleView(ViewHolder holder) {
        return true;
    }

    @Override
    public int getItemCount() {
        return mApps.getAdapterItems().size();
    }

    @Override
    public int getItemViewType(int position) {
        AdapterItem item = mApps.getAdapterItems().get(position);
        return item.viewType;
    }

    protected static boolean isViewType(int viewType, int viewTypeMask) {
        return (viewType & viewTypeMask) != 0;
    }

    private void setupAppCardBackground(View view) {
        float density = view.getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setCornerRadius(18 * density);

        boolean isDark = (view.getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int bgColor = isDark 
                ? android.graphics.Color.argb(35, 255, 255, 255) 
                : android.graphics.Color.argb(20, 0, 0, 0);
        cardBg.setColor(bgColor);

        int strokeColor = isDark 
                ? android.graphics.Color.argb(25, 255, 255, 255) 
                : android.graphics.Color.argb(15, 0, 0, 0);
        cardBg.setStroke(Math.max(1, (int) (1 * density)), strokeColor);

        int padH = (int) (4 * density);
        int padV = (int) (4 * density);
        android.graphics.drawable.InsetDrawable insetCard = new android.graphics.drawable.InsetDrawable(
                cardBg, padH, padV, padH, padV);
        view.setBackground(insetCard);
    }
}
