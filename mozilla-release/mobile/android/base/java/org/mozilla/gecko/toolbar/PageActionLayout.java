/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.toolbar;

import org.mozilla.gecko.AddonUICache;
import org.mozilla.gecko.EventDispatcher;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.R;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.Tabs;
import org.mozilla.gecko.preferences.GeckoPreferences;
import org.mozilla.gecko.preferences.PreferenceManager;
import org.mozilla.gecko.pwa.PwaUtils;
import org.mozilla.gecko.util.ResourceDrawableUtils;
import org.mozilla.gecko.util.GeckoBundle;
import org.mozilla.gecko.util.ShortcutUtils;
import org.mozilla.gecko.util.ThreadUtils;
import org.mozilla.gecko.widget.GeckoPopupMenu;
import org.mozilla.gecko.widget.themed.ThemedImageButton;
import org.mozilla.gecko.widget.themed.ThemedLinearLayout;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cliqz.Telemetry;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import static org.mozilla.gecko.toolbar.PageActionLayout.PageAction.UUID_PAGE_ACTION_PWA;

public class PageActionLayout extends ThemedLinearLayout
        implements View.OnClickListener, View.OnLongClickListener {
    private static final String MENU_BUTTON_KEY = "MENU_BUTTON_KEY";
    private static final int DEFAULT_PAGE_ACTIONS_SHOWN = 2;
    public static final String PREF_PWA_ONBOARDING = GeckoPreferences.NON_PREF_PREFIX + "pref_pwa_onboarding";

    /* Cliqz Start */
    private static final String READER_MODE_ICON_URI ="drawable://ic_readermode";
    /* Cliqz End */
    public interface PageActionLayoutDelegate {
        void addPageAction(GeckoBundle message);
        void removePageAction(GeckoBundle message);
        void setCachedPageActions(List<PageAction> cachedPageActions);
    }

    private final Context mContext;
    private final LinearLayout mLayout;
    private List<PageAction> mPageActionList;

    private GeckoPopupMenu mPageActionsMenu;

    // By default it's two, can be changed by calling setNumberShown(int)
    private int mMaxVisiblePageActions;
    private PwaConfirm mPwaConfirm;

    /* Cliqz Start */
    private PageActionClickListener mPageActionListener;

    public interface PageActionClickListener {
        void onClick();
    }
    /* Cliqz End */

    public PageActionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mLayout = this;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Calling this will cause the AddonUICache to synchronously call addPageAction for all
        // PageAction messages it received while no PageActionLayout was available.
        // Processing those messages entails converting a data: URI into a drawable, which can take
        // a few ms per message and therefore in theory cause some jank.
        // In practice however, PageAction messages are commonly only generated when tabs (from the
        // BrowserApp UI) are actually loading, so the AddonUICache's message queueing mechanism
        // only becomes relevant if the BrowserApp UI has then been backgrounded *and* subsequently
        // destroyed by the OS while some tabs are still loading. Merely starting up Gecko through a
        // GeckoView-based activity on the other hand is not enough to queue up PageAction messages.
        // Therefore, this case should happen rarely enough that we can tolerate it without having
        // to think about moving the image processing into some asynchronous code path.
        AddonUICache.getInstance().setPageActionLayoutDelegate(new PageActionLayoutDelegate() {
            @Override
            public void addPageAction(final GeckoBundle message) {
                onAddPageAction(message);
            }

            @Override
            public void removePageAction(final GeckoBundle message) {
                onRemovePageAction(message);
            }

            @Override
            public void setCachedPageActions(final List<PageAction> cachedPageActions) {
                if (cachedPageActions != null) {
                    mPageActionList = cachedPageActions;
                } else {
                    mPageActionList = new ArrayList<>();
                }

                setNumberShown(DEFAULT_PAGE_ACTIONS_SHOWN);
                refreshPageActionIcons();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        AddonUICache.getInstance().removePageActionLayoutDelegate(mPageActionList);

        super.onDetachedFromWindow();
    }

    @Override
    public void setPrivateMode(boolean isPrivate) {
        super.setPrivateMode(isPrivate);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof ThemedImageButton) {
                ((ThemedImageButton) child).setPrivateMode(isPrivate);
            }
        }
    }

    private void setNumberShown(int count) {
        ThreadUtils.assertOnUiThread();

        mMaxVisiblePageActions = count;

        for (int index = 0; index < count; index++) {
            if ((getChildCount() - 1) < index) {
                mLayout.addView(createImageButton());
            }
        }
    }

    private void onAddPageAction(final GeckoBundle message) {
        ThreadUtils.assertOnUiThread();

        hidePreviousConfirmPrompt();

        /* Cliqz start */
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        /* Cliqz end */

        final String id = message.getString("id");

        if (isPageActionAlreadyAdded(id)) {
            return;
        }

        /* Cliqz start o/ maybeShowPwaOnboarding(id); /o Cliqz end */

        final String title = message.getString("title");
        final String imageURL = message.getString("icon");
        final boolean important = message.getBoolean("important");
        final boolean useTint = message.getBoolean("useTint");

        addPageAction(id, title, imageURL, useTint, new OnPageActionClickListeners() {
            @Override
            public void onClick(final String id) {
                if (UUID_PAGE_ACTION_PWA.equals(id)) {
                    mPwaConfirm = PwaConfirm.show(getContext());
                    return;
                }
                /* Cliqz Start */
                // Cannot use id to compare cause it is generated at runtime in javascript.
                if (READER_MODE_ICON_URI.equals(imageURL)) {
                    Telemetry.sendReadModeClickTelemetry();
                }
                /* Cliqz End */
                final GeckoBundle data = new GeckoBundle(1);
                data.putString("id", id);
                EventDispatcher.getInstance().dispatch("PageActions:Clicked", data);
                /* Cliqz Start */
                mPageActionListener.onClick();
                /* Cliqz End */
            }

            @Override
            public boolean onLongClick(String id) {
                final GeckoBundle data = new GeckoBundle(1);
                data.putString("id", id);
                EventDispatcher.getInstance().dispatch("PageActions:LongClicked", data);
                return true;
            }
        }, important);
    }

    private void onRemovePageAction(final GeckoBundle message) {
        ThreadUtils.assertOnUiThread();

        hidePreviousConfirmPrompt();
        removePageAction(message.getString("id"));
        /* Cliqz start */
        if(mPageActionList.size() == 0 && getVisibility() == VISIBLE) {
            setVisibility(GONE);
        }
        /* Cliqz end */
    }

    /* Cliqz start */
    // show OnBoarding after AddToHome screen appears on the url bar
    public void maybeShowPwaOnboarding() {
        // only show pwa at normal mode
        /*final Tab selectedTab = Tabs.getInstance().getSelectedTab();
        if (!PwaUtils.shouldAddPwaShortcut(selectedTab) || mPageActionList == null) {
            return;
        }
        for(PageAction action : mPageActionList) {
            if (UUID_PAGE_ACTION_PWA.equals(action.getID())) {
                final SharedPreferences prefs = GeckoSharedPrefs.forApp(getContext());
                final boolean show = prefs.getBoolean(PREF_PWA_ONBOARDING, true);
                if (show && ShortcutUtils.isPinShortcutSupported()) {
                    PwaOnboarding.show(getContext());
                    prefs.edit().putBoolean(PREF_PWA_ONBOARDING, false).apply();
                    break;
                }
            }
        }*/
    }
    /* Cliqz end */

    private boolean isPageActionAlreadyAdded(String id) {
        for (PageAction pageAction : mPageActionList) {
            if (pageAction.getID() != null && pageAction.getID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private void addPageAction(final String id, final String title, final String imageData, final boolean useTint,
                               final OnPageActionClickListeners onPageActionClickListeners, boolean important) {
        ThreadUtils.assertOnUiThread();

        /*Cliqz Start*/
        // add useTint parameter
        final PageAction pageAction = new PageAction(id, title, null, onPageActionClickListeners, important,useTint);
        /*Cliqz End*/

        int insertAt = mPageActionList.size();
        while (insertAt > 0 && mPageActionList.get(insertAt - 1).isImportant()) {
            insertAt--;
        }
        mPageActionList.add(insertAt, pageAction);

        ResourceDrawableUtils.getDrawable(mContext, imageData, new ResourceDrawableUtils.BitmapLoader() {
            @Override
            public void onBitmapFound(final Drawable d) {
                if (mPageActionList.contains(pageAction)) {
                    /* Cliqz start */
                    // final Drawable icon;
                    // if (useTint) {
                    //     final ColorStateList colorStateList = ContextCompat.getColorStateList(
                    //             getContext(), R.color.page_action_fg);
                    //     icon = DrawableUtil.tintDrawableWithStateList(d, colorStateList);
                    // } else {
                    //     icon = d;
                    // }
                    final Drawable icon = applyTint(d,useTint);
                    /* Cliqz end */

                    pageAction.setDrawable(icon);
                    refreshPageActionIcons();
                }
            }
        });
    }

    /* Cliqz Start */
    private Drawable applyTint(Drawable drawable, boolean useTint) {
        final Drawable icon = DrawableCompat.wrap(drawable.mutate());
        final int colorId;
        if (useTint) {
            if (PreferenceManager.getInstance().isLightThemeEnabled()) {
                colorId = android.R.color.white;
            } else {
                colorId = R.color.general_blue_color;
            }
        } else {
            if (PreferenceManager.getInstance().isLightThemeEnabled()) {
                colorId = R.color.url_bar_dark_blue_color;
            } else {
                colorId = R.color.inactive_icon_color;
            }
        }
        DrawableCompat.setTint(icon, ContextCompat.getColor(getContext(), colorId));
        return drawable;
    }
    /* Cliqz End */

    private void removePageAction(String id) {
        ThreadUtils.assertOnUiThread();

        if (PageAction.removeFromList(mPageActionList, id)) {
            refreshPageActionIcons();
        }
    }

    private ThemedImageButton createImageButton() {
        ThreadUtils.assertOnUiThread();

        final ToolbarRoundButton imageButton = new ToolbarRoundButton(mContext, null, R.style.UrlBar_ImageButton);
        final int width = mContext.getResources().getDimensionPixelSize(R.dimen.page_action_button_width);
        final int height = mContext.getResources().getDimensionPixelSize(R.dimen
                .browser_toolbar_height);

        LayoutParams params = new LayoutParams(width, height);
        params.gravity = Gravity.CENTER_VERTICAL;
        imageButton.setLayoutParams(params);
        imageButton.setAdjustViewBounds(true);
        imageButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageButton.setOnClickListener(this);
        imageButton.setOnLongClickListener(this);
        return imageButton;
    }

    @Override
    public void onClick(View v) {
        String buttonClickedId = (String) v.getTag();
        if (buttonClickedId != null) {
            if (buttonClickedId.equals(MENU_BUTTON_KEY)) {
                showMenu(v, mPageActionList.size() - mMaxVisiblePageActions + 1);
            } else {
                getPageActionWithId(buttonClickedId).onClick();
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        String buttonClickedId = (String) v.getTag();
        if (buttonClickedId.equals(MENU_BUTTON_KEY)) {
            showMenu(v, mPageActionList.size() - mMaxVisiblePageActions + 1);
            return true;
        } else {
            return getPageActionWithId(buttonClickedId).onLongClick();
        }
    }

    private void setActionForView(final ImageButton view, final PageAction pageAction) {
        ThreadUtils.assertOnUiThread();

        if (pageAction == null) {
            view.setTag(null);
            view.setImageDrawable(null);
            view.setVisibility(View.GONE);
            view.setContentDescription(null);
            return;
        }

        view.setTag(pageAction.getID());
        view.setImageDrawable(pageAction.getDrawable());
        view.setVisibility(View.VISIBLE);
        view.setContentDescription(pageAction.getTitle());
    }

    private void refreshPageActionIcons() {
        ThreadUtils.assertOnUiThread();

        final Resources resources = mContext.getResources();
        for (int i = 0; i < this.getChildCount(); i++) {
            final ImageButton v = (ImageButton) this.getChildAt(i);
            final PageAction pageAction = getPageActionForViewAt(i);

            // If there are more page actions than buttons, set the menu icon.
            // Otherwise, set the page action's icon if there is a page action.
            if ((i == this.getChildCount() - 1) && (mPageActionList.size() > mMaxVisiblePageActions)) {
                v.setTag(MENU_BUTTON_KEY);
                v.setImageDrawable(resources.getDrawable(R.drawable.icon_pageaction));
                v.setVisibility((pageAction != null) ? View.VISIBLE : View.GONE);
                v.setContentDescription(resources.getString(R.string.page_action_dropmarker_description));
            } else {
                setActionForView(v, pageAction);
            }

            if (v instanceof ThemedImageButton) {
                ((ThemedImageButton) v).setPrivateMode(isPrivateMode());
            }
        }
        /* Cliqz start */
        // updateTitleBarWidth based on how many pageActions appear
        ViewParent parent = getParent();
        while(parent != null && !(parent instanceof ToolbarDisplayLayout)) {
            parent = parent.getParent();
        }
        if (parent != null) {
            ((ToolbarDisplayLayout) parent).updateTitleBarWidth();
        }
        /* Cliqz end */
    }

    private PageAction getPageActionForViewAt(int index) {
        ThreadUtils.assertOnUiThread();

        /**
         * We show the user the most recent pageaction added since this keeps the user aware of any new page actions being added
         * Also, the order of the pageAction is important i.e. if a page action is added, instead of shifting the pagactions to the
         * left to make space for the new one, it would be more visually appealing to have the pageaction appear in the blank space.
         *
         * buttonIndex is needed for this reason because every new View added to PageActionLayout gets added to the right of its neighbouring View.
         * Hence the button on the very leftmost has the index 0. We want our pageactions to start from the rightmost
         * and hence we maintain the insertion order of the child Views which is essentially the reverse of their index
         */

        final int buttonIndex = (this.getChildCount() - 1) - index;

        if (mPageActionList.size() > buttonIndex) {
            // Return the pageactions starting from the end of the list for the number of visible pageactions.
            final int buttonCount = Math.min(mPageActionList.size(), getChildCount());
            return mPageActionList.get((mPageActionList.size() - buttonCount) + buttonIndex);
        }
        return null;
    }

    private PageAction getPageActionWithId(String id) {
        ThreadUtils.assertOnUiThread();

        for (PageAction pageAction : mPageActionList) {
            if (pageAction.getID().equals(id)) {
                return pageAction;
            }
        }
        return null;
    }

    private void showMenu(View pageActionButton, int toShow) {
        ThreadUtils.assertOnUiThread();

        if (mPageActionsMenu == null) {
            mPageActionsMenu = new GeckoPopupMenu(pageActionButton.getContext(), pageActionButton);
            mPageActionsMenu.inflate(0);
            mPageActionsMenu.setOnMenuItemClickListener(new GeckoPopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int id = item.getItemId();
                    for (int i = 0; i < mPageActionList.size(); i++) {
                        PageAction pageAction = mPageActionList.get(i);
                        if (pageAction.key() == id) {
                            pageAction.onClick();
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        Menu menu = mPageActionsMenu.getMenu();
        menu.clear();

        for (int i = 0; i < mPageActionList.size() && i < toShow; i++) {
            PageAction pageAction = mPageActionList.get(i);
            MenuItem item = menu.add(Menu.NONE, pageAction.key(), Menu.NONE, pageAction.getTitle());
            item.setIcon(pageAction.getDrawable());
        }
        mPageActionsMenu.show();
    }

    private static interface OnPageActionClickListeners {
        public void onClick(String id);

        public boolean onLongClick(String id);
    }

    private void hidePreviousConfirmPrompt() {
        if (mPwaConfirm != null) {
            if (ViewCompat.isAttachedToWindow(mPwaConfirm) || mPwaConfirm.getParent() != null) {
                mPwaConfirm.disappear();
            }
            mPwaConfirm = null;
        }
    }

    public static class PageAction {
        public static final String UUID_PAGE_ACTION_PWA = "279c269d-6397-4f86-a6d2-452e26456d4a";

        private final OnPageActionClickListeners mOnPageActionClickListeners;
        private Drawable mDrawable;
        private final String mTitle;
        private final String mId;
        private final int key;
        private final boolean mImportant;
        /*Cliqz Start */
        private boolean mUseTint;

        public PageAction(String id,
                          String title,
                          Drawable image,
                          OnPageActionClickListeners onPageActionClickListeners,
                          boolean important,
                          boolean useTint) {
            mId = id;
            mTitle = title;
            mDrawable = image;
            mOnPageActionClickListeners = onPageActionClickListeners;
            mImportant = important;

            key = UUID.fromString(mId.subSequence(1, mId.length() - 2).toString()).hashCode();
            mUseTint = useTint;
            /*Cliqz End */
        }

        public Drawable getDrawable() {
            return mDrawable;
        }

        public void setDrawable(Drawable d) {
            mDrawable = d;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getID() {
            return mId;
        }

        public int key() {
            return key;
        }

        public boolean isImportant() {
            return mImportant;
        }

        public void onClick() {
            if (mOnPageActionClickListeners != null) {
                mOnPageActionClickListeners.onClick(mId);
            }
        }

        public boolean onLongClick() {
            if (mOnPageActionClickListeners != null) {
                return mOnPageActionClickListeners.onLongClick(mId);
            }
            return false;
        }

        /**
         * @return True if any PageAction was actually removed, false otherwise.
         */
        public static boolean removeFromList(Collection<PageAction> pageActionCollection, String id) {
            final Iterator<PageAction> iter = pageActionCollection.iterator();
            while (iter.hasNext()) {
                final PageAction action = iter.next();
                if (action.getID().equals(id)) {
                    iter.remove();
                    return true;
                }
            }
            return false;
        }
    }

    /* Cliqz start */
    // accessed in @ToolbarDisplayLayout.java based on Actions list size we modify the urlBar width
    public int getPageActionListSize() {
        return mPageActionList != null ? mPageActionList.size() : 0;
    }

    public void setOnPageActionClickedListener(PageActionClickListener listener) {
        mPageActionListener = listener;
    }

    public void setLightTheme(boolean isLightTheme) {
        super.setLightTheme(isLightTheme);
        if (mPageActionList != null) {
            for (PageAction pageAction : mPageActionList) {
                applyTint(pageAction.mDrawable, pageAction.mUseTint);
            }
        }
    }
    /* Cliqz end */
}
