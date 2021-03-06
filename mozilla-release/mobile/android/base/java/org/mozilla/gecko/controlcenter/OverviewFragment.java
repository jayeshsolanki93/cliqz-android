package org.mozilla.gecko.controlcenter;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.mozilla.gecko.EventDispatcher;
import org.mozilla.gecko.R;
import org.mozilla.gecko.anolysis.ControlCenterMetrics;
import org.mozilla.gecko.util.GeckoBundle;
import org.mozilla.gecko.util.GeckoBundleUtils;
import org.mozilla.gecko.util.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mozilla.gecko.util.GeckoBundleUtils.safeGetBoolean;
import static org.mozilla.gecko.util.GeckoBundleUtils.safeGetBundle;
import static org.mozilla.gecko.util.GeckoBundleUtils.safeGetInt;
import static org.mozilla.gecko.util.GeckoBundleUtils.safeGetString;
import static org.mozilla.gecko.util.GeckoBundleUtils.safeGetStringArray;

/**
 * Copyright © Cliqz 2018
 */
public class OverviewFragment extends ControlCenterFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private PieChart mPieChart;
    private View mTrackersCountView;
    private TextView mTrackersCount;
    private TextView mTrackersLabel;
    private TextView mDomainName;
    private TextView mTrackersBlocked;
    private LinearLayout mTrustSiteButton;
    private LinearLayout mRestrictSiteButton;
    private LinearLayout mPauseGhosteryButton;
    private TextView mSmartBlockingCount;
    private TextView mCliqzAttrackCount;
    private TextView mAdBlockCount;
    private SwitchCompat mSmartBlockingSwitch;
    private SwitchCompat mAdBlockingSwitch;
    private SwitchCompat mAntiTrackingSwitch;
    private boolean mIsSiteTrusted;
    private boolean mIsSiteRestricted;
    private boolean mIsGhosteryPaused;
    private boolean mAreAdsBlockedGlobally;
    private boolean mIsSiteAdBlockWhiteListed;
    private GeckoBundle controlCenterSettingsData;
    private final List<Integer> colors = new ArrayList<>();
    private final List<Integer> disabledColors = new ArrayList<>();
    private final List<Integer> blockedColors = new ArrayList<>();
    private TextView mNotchTitle;
    private RadioGroup mAdBlockOptionsView;
    private RadioButton mAdBlockDisableDomain;
    private RadioButton mAdBlockDisableGlobal;
    private View mOverview;
    private ImageButton mBackButton;
    private BottomSheetBehavior<View> mBottomSheetBehavior;
    private Resources mResources = null;
    private final BottomSheetBehavior.BottomSheetCallback mBottomSheetCallback =
            new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        mOverview.setVisibility(View.VISIBLE);
                        mAdBlockOptionsView.setVisibility(View.INVISIBLE);
                        mNotchTitle.setText(R.string.cc_enhanced_options);
                        mBackButton.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                }
            };

    public OverviewFragment() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //trick to redraw the view
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .detach(this)
                .attach(this)
                .commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.ghostery_overview_fragment, container, false);
        mPieChart = (PieChart) view.findViewById(R.id.cc_donut);
        mTrackersCountView = view.findViewById(R.id.trackers_count_holder);
        mTrackersCount = (TextView) view.findViewById(R.id.trackers_count);
        mTrackersLabel = (TextView) view.findViewById(R.id.trackers_label);
        mDomainName = (TextView) view.findViewById(R.id.cc_domain_name);
        mTrackersBlocked = (TextView) view.findViewById(R.id.cc_trackers_blocked);
        mTrustSiteButton = (LinearLayout) view.findViewById(R.id.cc_ghostery_trust_site_button);
        mRestrictSiteButton = (LinearLayout) view.findViewById(R.id.cc_ghostery_restrict_site_button);
        mPauseGhosteryButton = (LinearLayout) view.findViewById(R.id.cc_ghostery_pause_button);
        mAdBlockingSwitch = (SwitchCompat) view.findViewById(R.id.cc_enhanced_blocking_switch);
        mAntiTrackingSwitch = (SwitchCompat) view.findViewById(R.id.cc_enhanced_tracking_switch);
        mSmartBlockingSwitch = (SwitchCompat) view.findViewById(R.id.cc_smart_blocking_switch);
        mSmartBlockingCount = (TextView) view.findViewById(R.id.cc_smart_blocking_count);
        mCliqzAttrackCount = (TextView) view.findViewById(R.id.cc_enhanced_tracking_count);
        mAdBlockCount = (TextView) view.findViewById(R.id.cc_enhanced_blocking_count);
        mTrustSiteButton.setOnClickListener(this);
        mRestrictSiteButton.setOnClickListener(this);
        mPauseGhosteryButton.setOnClickListener(this);
        mBackButton = (ImageButton) view.findViewById(R.id.cc_back_button);
        mAdBlockOptionsView = (RadioGroup) view.findViewById(R.id.cc_adblock_options_view);
        mAdBlockDisableDomain = (RadioButton) view.findViewById(R.id.cc_radio_this_domain);
        mAdBlockDisableGlobal = (RadioButton) view.findViewById(R.id.cc_radio_all_domain);
        mOverview = view.findViewById(R.id.cc_overview_view);
        mNotchTitle = (TextView) view.findViewById(R.id.cc_notch_title);

        final View mBottomSheetRootView = view.findViewById(R.id.cc_bottom_sheet_root_view);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetRootView);
        mBottomSheetBehavior.setBottomSheetCallback(mBottomSheetCallback);
        final View enhancedBlockingText = view.findViewById(R.id.cc_enhanced_blocking_text);
        enhancedBlockingText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mAreAdsBlockedGlobally || mIsSiteAdBlockWhiteListed) {
                    mAdBlockOptionsView.setVisibility(View.VISIBLE);
                    mOverview.setVisibility(View.INVISIBLE);
                    mBackButton.setVisibility(View.VISIBLE);
                }
            }
        });
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdBlockOptionsView.setVisibility(View.INVISIBLE);
                mOverview.setVisibility(View.VISIBLE);
                mBackButton.setVisibility(View.GONE);
                if (mAreAdsBlockedGlobally && !mIsSiteAdBlockWhiteListed) {
                    mAdBlockingSwitch.setChecked(true);
                }
            }
        });
        return view;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.cc_title_overview);
    }

    @Override
    public void updateUI(GeckoBundle controlCenterSettingsData) {
        if (getView() == null || mResources == null) {
            return; //return if view is not inflated yet
        }
        mNotchTitle.setText(R.string.cc_enhanced_options);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        this.controlCenterSettingsData = controlCenterSettingsData;
        final int allowedTrackers = safeGetInt(controlCenterSettingsData, "data/summary/trackerCounts/allowed");
        final int blockedTrackers = calculateBlockedTrackers();
        final int totalTrackers = calculateGhosteryAttrackCount() + calculateCliqzAttrackCount()
                + getAdBlockCount();
        final String domainName = safeGetString(controlCenterSettingsData, "data/summary/pageHost");
        final boolean isSmartBlockEnabled = safeGetBoolean(controlCenterSettingsData, "data/panel/panel/enable_smart_block");
        final boolean isAntiTrackingEnabled = safeGetBoolean(controlCenterSettingsData, "data/panel/panel/enable_anti_tracking");
        mAreAdsBlockedGlobally = safeGetBoolean(controlCenterSettingsData, "data/panel/panel/enable_ad_block");
        mIsSiteAdBlockWhiteListed = safeGetBoolean(controlCenterSettingsData, "data/cliqz/adblock/disabledForDomain");
        mSmartBlockingCount.setText(String.valueOf(getSmartBlockingCount()));
        mCliqzAttrackCount.setText(String.valueOf(calculateCliqzAttrackCount()));
        mAdBlockCount.setText(String.valueOf(getAdBlockCount()));

        mSmartBlockingSwitch.setOnCheckedChangeListener(null);
        mAntiTrackingSwitch.setOnCheckedChangeListener(null);
        mAdBlockingSwitch.setOnCheckedChangeListener(null);

        mSmartBlockingSwitch.setChecked(isSmartBlockEnabled);
        mAdBlockingSwitch.setChecked(mAreAdsBlockedGlobally && !mIsSiteAdBlockWhiteListed);
        mAdBlockOptionsView.setOnCheckedChangeListener(null);
        if (!mAreAdsBlockedGlobally || mIsSiteAdBlockWhiteListed) {
            mAdBlockDisableDomain.setChecked(mIsSiteAdBlockWhiteListed);
            mAdBlockDisableGlobal.setChecked(!mAreAdsBlockedGlobally);
        } else {
            mAdBlockOptionsView.clearCheck();
        }
        mAdBlockOptionsView.setOnCheckedChangeListener(adBlockCheckedChangeListener);
        mAntiTrackingSwitch.setChecked(isAntiTrackingEnabled);

        mSmartBlockingSwitch.setOnCheckedChangeListener(this);
        mAntiTrackingSwitch.setOnCheckedChangeListener(this);
        mAdBlockingSwitch.setOnCheckedChangeListener(this);

        final List<PieEntry> entries = new ArrayList<>();
        mIsGhosteryPaused = safeGetBoolean(controlCenterSettingsData, "data/summary/paused_blocking");
        final GeckoBundle[] categories = safeGetBundle(controlCenterSettingsData, "data/summary").getBundleArray("categories");
        colors.clear();
        disabledColors.clear();
        blockedColors.clear();
        for (GeckoBundle categoryBundle : categories != null ? categories : new GeckoBundle[0]) {
            final Categories category = Categories.safeValueOf(categoryBundle.getString("id"));
            final int trackersCount = categoryBundle.getInt("num_total");
            entries.add(new PieEntry(trackersCount));
            colors.add(ContextCompat.getColor(getContext(), category.categoryColor));
            disabledColors.add(ContextCompat.getColor(getContext(), category.categoryColorDisabled));
            blockedColors.add(ContextCompat.getColor(getContext(), category.categoryColorBlocked));
        }
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1));
            final @ColorRes int defaultColor = ContextCompat.getColor(getContext(), R.color.cc_default_category_disabled);
            colors.add(defaultColor);
            disabledColors.add(defaultColor);
            blockedColors.add(defaultColor);
        }
        final PieDataSet set = new PieDataSet(entries, "cc");
        if (mIsGhosteryPaused) {
            set.setColors(disabledColors);
        } else if (mIsSiteRestricted) {
            set.setColors(blockedColors);
        } else {
            set.setColors(colors);
        }
        final PieData pieData = new PieData(set);
        pieData.setDrawValues(false);
        mPieChart.setHighlightPerTapEnabled(false);
        mPieChart.setData(pieData);
        mPieChart.setDrawEntryLabels(false);
        mTrackersCount.setText(String.valueOf(totalTrackers));
        mTrackersLabel.setText(mResources.getQuantityString(R.plurals.cc_total_trackers_found, totalTrackers));
        mPieChart.setCenterTextColor(ContextCompat.getColor(getContext(), R.color.cc_text_color));
        mPieChart.setHoleRadius(80);
        mPieChart.setDescription(null);
        mPieChart.setDrawMarkers(false);
        mPieChart.getLegend().setEnabled(false);
        mPieChart.invalidate();

        mTrackersCountView.setVisibility(View.INVISIBLE);
        mPieChart.post(new Runnable() {
            @Override
            public void run() {
                mTrackersCountView.setVisibility(View.VISIBLE);
                final int donutRadius = mPieChart.getHeight() / 2;
                final int padding = (int) (Math.sqrt(2 * Math.pow(donutRadius, 2)) - donutRadius);
                mTrackersCountView.setPadding(padding, padding, padding, padding);
            }
        });
        final String trackersBlocked = mResources.getQuantityString(R.plurals.cc_total_trackers_blocked, blockedTrackers, blockedTrackers);
        final Spannable trackersBlockedSpan = new SpannableString(trackersBlocked);
        trackersBlockedSpan.setSpan(new ForegroundColorSpan(mResources.getColor(R.color.cc_restricted)), 0,
                Integer.toString(blockedTrackers).length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        mTrackersBlocked.setText(trackersBlockedSpan);
        if (mDomainName != null) { //landscape mode has no domain name
            mDomainName.setText(domainName);
        }
        final List<String> blackList = Arrays.asList(safeGetStringArray(controlCenterSettingsData, "data/summary/site_blacklist"));
        final List<String> whiteList = Arrays.asList(safeGetStringArray(controlCenterSettingsData, "data/summary/site_whitelist"));
        mIsSiteRestricted = blackList.contains(domainName);
        mIsSiteTrusted = whiteList.contains(domainName);
        if (!mIsGhosteryPaused) {
            styleButton(mTrustSiteButton, mIsSiteTrusted);
            styleButton(mRestrictSiteButton, mIsSiteRestricted);
            styleButton(mPauseGhosteryButton, mIsGhosteryPaused);
        } else {
            styleButton(mPauseGhosteryButton, mIsGhosteryPaused);
        }
    }

    private int calculateGhosteryAttrackCount() {
        int totalTrackers = 0;
        final GeckoBundle[] categories = GeckoBundleUtils.safeGetBundleArray(controlCenterSettingsData, "data/summary/categories");
        for (GeckoBundle category : categories) {
            final GeckoBundle[] trackers = category.getBundleArray("trackers");
            if (trackers != null) {
                totalTrackers+=trackers.length;
            }
        }
        return totalTrackers;
    }

    private int calculateBlockedTrackers() {
        int totalBlocked = 0;
        final GeckoBundle[] categories = GeckoBundleUtils.safeGetBundleArray(controlCenterSettingsData, "data/summary/categories");
        if (mIsSiteRestricted) {
            for (GeckoBundle category : categories) {
                final GeckoBundle[] trackers = category.getBundleArray("trackers");
                if (trackers != null) {
                    totalBlocked += trackers.length;
                }
            }
            return totalBlocked;
        }
        if (mIsSiteTrusted || mIsGhosteryPaused) {
            return 0;
        }
        for (GeckoBundle category : categories) {
            final GeckoBundle[] trackers  = category.getBundleArray("trackers");
            if (trackers != null) {
                for (GeckoBundle tracker : trackers) {
                    final boolean isBlocked = tracker.getBoolean("blocked");
                    final boolean isTrusted = tracker.getBoolean("ss_allowed");
                    final boolean isRestricted = tracker.getBoolean("ss_blocked");
                    if (isBlocked && !isTrusted) {
                        totalBlocked++;
                    } else if (isRestricted) {
                        totalBlocked++;
                    }
                }
            }
        }
        return totalBlocked;
    }

    private int calculateCliqzAttrackCount() {
        int count = 0;
        final GeckoBundle attrackBundle = GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/cliqz/antitracking");
        final String[] categories =  attrackBundle.keys();
        for (String category : categories) {
            final GeckoBundle categoryBundle = GeckoBundleUtils.safeGetBundle(attrackBundle, category);
            final String[] trackers = categoryBundle.keys();
            for (String tracker : trackers) {
                if (categoryBundle.getString(tracker).equals("unsafe")) {
                    count++;
                }
            }
        }
        return count;
    }

    private int getAdBlockCount() {
        final GeckoBundle adBlockBundle = GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/cliqz/adblock");
        return adBlockBundle.getInt("totalCount");
    }

    private int getSmartBlockingCount() {
        return safeGetBundle(controlCenterSettingsData,"data/panel/panel/smartBlock/blocked").keys().length +
                safeGetBundle(controlCenterSettingsData, "data/panel/panel/smartBlock/unblocked").keys().length;
    }

    @Override
    public void refreshUI() {
        if (mResources == null) {
            return;
        }
        final int blockedTrackers = calculateBlockedTrackers();
        final String trackersBlocked = mResources.getQuantityString(R.plurals.cc_total_trackers_blocked, blockedTrackers, blockedTrackers);
        final Spannable trackersBlockedSpan = new SpannableString(trackersBlocked);
        trackersBlockedSpan.setSpan(new ForegroundColorSpan(mResources.getColor(R.color.cc_restricted)), 0,
                Integer.toString(blockedTrackers).length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        mTrackersBlocked.setText(trackersBlockedSpan);
        final PieData donutData = mPieChart.getData();
        if (donutData != null) {
            final PieDataSet donutDataset = ((PieDataSet) donutData.getDataSet());
            if (mIsGhosteryPaused) {
                donutDataset.setColors(disabledColors);
            } else if (mIsSiteRestricted) {
                donutDataset.setColors(blockedColors);
            } else {
                donutDataset.setColors(colors);
            }
            mPieChart.invalidate();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mResources = context.getApplicationContext().getResources();
    }

    @Override
    public void onClick(View v) {
        if (controlCenterSettingsData == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.cc_ghostery_trust_site_button:
                handleTrustButtonClick();
                break;
            case R.id.cc_ghostery_restrict_site_button:
                handleRestrictButtonClick();
                break;
            case R.id.cc_ghostery_pause_button:
                handlePauseButtonClick();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final GeckoBundle geckoBundle = new GeckoBundle();
        switch (buttonView.getId()) {
            case R.id.cc_enhanced_tracking_switch:
                GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/panel/panel")
                        .putBoolean("enable_anti_tracking", isChecked);
                geckoBundle.putBoolean("enable_anti_tracking", isChecked);
                break;
            case R.id.cc_enhanced_blocking_switch:
                handleAdBlockSwitchClick(isChecked);
                break;
            case R.id.cc_smart_blocking_switch:
                GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/panel/panel")
                        .putBoolean("enable_smart_block", isChecked);
                geckoBundle.putBoolean("enable_smart_block", isChecked);
                break;
        }
        EventDispatcher.getInstance().dispatch("Privacy:SetInfo", geckoBundle);
    }

    private void handleAdBlockSwitchClick(boolean isChecked) {
        if (isChecked) {
            if (!mAreAdsBlockedGlobally) {
                final GeckoBundle geckoBundle = new GeckoBundle();
                geckoBundle.putBoolean("enable_ad_block", true);
                GeckoBundleUtils.safeGetBundle(controlCenterSettingsData,"data/panel/panel")
                        .putBoolean("enable_ad_block", true);
                EventDispatcher.getInstance().dispatch("Privacy:SetInfo", geckoBundle);
            } else if (mIsSiteAdBlockWhiteListed) {
                GeckoBundle geckoBundle = new GeckoBundle();
                geckoBundle.putString("url", safeGetString(controlCenterSettingsData, "data/summary/pageUrl"));
                geckoBundle.putBoolean("isDomain", true);
                GeckoBundleUtils.safeGetBundle(controlCenterSettingsData,"data/cliqz/adblock")
                        .putBoolean("disabledForDomain", false);
                EventDispatcher.getInstance().dispatch("Privacy:AdblockToggle", geckoBundle);
            }
        } else {
            mAdBlockOptionsView.setVisibility(View.VISIBLE);
            mOverview.setVisibility(View.INVISIBLE);
            mBackButton.setVisibility(View.VISIBLE);
            mAdBlockOptionsView.setOnCheckedChangeListener(null);
            if (mIsSiteAdBlockWhiteListed) {
                mAdBlockDisableDomain.setChecked(true);
                mAdBlockDisableGlobal.setChecked(false);
            } else if (!mAreAdsBlockedGlobally) {
                mAdBlockDisableDomain.setChecked(false);
                mAdBlockDisableGlobal.setChecked(true);
            } else {
                mAdBlockOptionsView.clearCheck();
            }
            mAdBlockOptionsView.setOnCheckedChangeListener(adBlockCheckedChangeListener);
        }
    }

    private void handlePauseButtonClick() {
        mIsGhosteryPaused = !mIsGhosteryPaused;
        mIsSiteRestricted = false;
        mIsSiteTrusted = false;
        styleButton(mPauseGhosteryButton, mIsGhosteryPaused);
        if (mIsGhosteryPaused) {
            styleButton(mTrustSiteButton, false);
            styleButton(mRestrictSiteButton, false);
            //domain should no longer be restricted/trusted if ghostery is paused
            final String pageHost = GeckoBundleUtils.safeGetString(controlCenterSettingsData, "data/summary/pageHost");
            final String[] blackList = GeckoBundleUtils.safeGetStringArray(controlCenterSettingsData, "data/summary/site_blacklist");
            final String[] whiteList = GeckoBundleUtils.safeGetStringArray(controlCenterSettingsData, "data/summary/site_whitelist");
            final ArrayList<String> updatedBlackList = new ArrayList<>(Arrays.asList(blackList != null ? blackList : new String[0]));
            final ArrayList<String> updatedWhiteList = new ArrayList<>(Arrays.asList(whiteList != null ? whiteList : new String[0]));
            updatedBlackList.remove(pageHost);
            updatedWhiteList.remove(pageHost);
            //update the data source so that other views can reflect changes
            GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/summary").putStringArray("site_blacklist", updatedBlackList);
            GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/summary").putStringArray("site_whitelist", updatedWhiteList);
            final GeckoBundle geckoBundle = new GeckoBundle();
            geckoBundle.putStringArray("site_whitelist", updatedWhiteList);
            geckoBundle.putStringArray("site_blacklist", updatedBlackList);
            EventDispatcher.getInstance().dispatch("Privacy:SetInfo", geckoBundle);
            ControlCenterMetrics.pause();
        } else {
            ControlCenterMetrics.resume();
        }
        final GeckoBundle geckoBundle = new GeckoBundle();
        geckoBundle.putBoolean("paused_blocking", mIsGhosteryPaused);
        EventDispatcher.getInstance().dispatch("Privacy:SetInfo", geckoBundle);
        GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/summary").putBoolean("paused_blocking", mIsGhosteryPaused);
    }

    private void handleTrustButtonClick() {
        ControlCenterMetrics.trustSite();
        final String pageHost = GeckoBundleUtils.safeGetString(controlCenterSettingsData, "data/summary/pageHost");
        //get list of blacklisted and whitelisted sites and then add/remove current site and send back the new lists
        final String[] blackList = GeckoBundleUtils.safeGetStringArray(controlCenterSettingsData, "data/summary/site_blacklist");
        final String[] whiteList = GeckoBundleUtils.safeGetStringArray(controlCenterSettingsData, "data/summary/site_whitelist");
        final ArrayList<String> updatedBlackList;
        final ArrayList<String> updatedWhiteList;
        mIsSiteTrusted = !mIsSiteTrusted;
        mIsSiteRestricted = false;
        mIsGhosteryPaused = false;
        styleButton(mTrustSiteButton, mIsSiteTrusted);
        if (mIsSiteTrusted) {
            styleButton(mRestrictSiteButton, false);
            styleButton(mPauseGhosteryButton, false);
            updatedBlackList = new ArrayList<>(Arrays.asList(blackList != null ? blackList : new String[0]));
            updatedWhiteList = new ArrayList<>(Arrays.asList(whiteList != null ? whiteList : new String[0]));
            updatedBlackList.remove(pageHost);
            updatedWhiteList.add(pageHost);
            final GeckoBundle geckoBundle = new GeckoBundle();
            geckoBundle.putBoolean("paused_blocking", false);
            EventDispatcher.getInstance().dispatch("Privacy:SetInfo", geckoBundle);
            final Toast toast = Toast.makeText(getContext(), R.string.cc_toast_overview_trust, Toast.LENGTH_SHORT);
            ViewUtil.centerToastText(toast);
            toast.show();
        } else {
            updatedBlackList = new ArrayList<>(Arrays.asList(blackList != null ? blackList : new String[0]));
            updatedWhiteList = new ArrayList<>(Arrays.asList(whiteList != null ? whiteList : new String[0]));
            updatedWhiteList.remove(pageHost);
            ControlCenterMetrics.restrictSite();
        }
        final GeckoBundle geckoBundle = new GeckoBundle();
        geckoBundle.putStringArray("site_whitelist", updatedWhiteList);
        geckoBundle.putStringArray("site_blacklist", updatedBlackList);
        EventDispatcher.getInstance().dispatch("Privacy:SetInfo", geckoBundle);
        //update the data source so that other views can reflect changes
        GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/summary").putStringArray("site_blacklist", updatedBlackList);
        GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/summary").putStringArray("site_whitelist", updatedWhiteList);
        GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/summary").putBoolean("paused_blocking", false);
    }

    private void handleRestrictButtonClick() {
        ControlCenterMetrics.restrictSite();
        final String pageHost = GeckoBundleUtils.safeGetString(controlCenterSettingsData, "data/summary/pageHost");
        //get list of blacklisted and whitelisted sites and then add/remove current site and send back the new lists
        final String[] blackList = GeckoBundleUtils.safeGetStringArray(controlCenterSettingsData, "data/summary/site_blacklist");
        final String[] whiteList = GeckoBundleUtils.safeGetStringArray(controlCenterSettingsData, "data/summary/site_whitelist");
        final ArrayList<String> updatedBlackList;
        final ArrayList<String> updatedWhiteList;
        mIsSiteRestricted = !mIsSiteRestricted;
        mIsSiteTrusted = false;
        mIsGhosteryPaused = false;
        styleButton(mRestrictSiteButton, mIsSiteRestricted);
        if (mIsSiteRestricted) {
            styleButton(mTrustSiteButton, false);
            styleButton(mPauseGhosteryButton, false);
            updatedBlackList = new ArrayList<>(Arrays.asList(blackList != null ? blackList : new String[0]));
            updatedWhiteList = new ArrayList<>(Arrays.asList(whiteList != null ? whiteList : new String[0]));
            updatedBlackList.add(pageHost);
            updatedWhiteList.remove(pageHost);
            final GeckoBundle geckoBundle = new GeckoBundle();
            geckoBundle.putBoolean("paused_blocking", false);
            EventDispatcher.getInstance().dispatch("Privacy:SetInfo", geckoBundle);
            final Toast toast = Toast.makeText(getContext(), R.string.cc_toast_overview_restrict, Toast.LENGTH_SHORT);
            ViewUtil.centerToastText(toast);
            toast.show();
        } else {
            updatedBlackList = new ArrayList<>(Arrays.asList(blackList != null ? blackList : new String[0]));
            updatedWhiteList = new ArrayList<>(Arrays.asList(whiteList != null ? whiteList : new String[0]));
            updatedBlackList.remove(pageHost);
        }
        final GeckoBundle geckoBundle = new GeckoBundle();
        geckoBundle.putStringArray("site_whitelist", updatedWhiteList);
        geckoBundle.putStringArray("site_blacklist", updatedBlackList);
        EventDispatcher.getInstance().dispatch("Privacy:SetInfo", geckoBundle);
        //update the data source so that other views can reflect changes
        GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/summary").putStringArray("site_blacklist", updatedBlackList);
        GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/summary").putStringArray("site_whitelist", updatedWhiteList);
        GeckoBundleUtils.safeGetBundle(controlCenterSettingsData, "data/summary").putBoolean("paused_blocking", false);
    }

    private void styleButton(ViewGroup button, boolean state) {
        final int textAndIconColor = ContextCompat.getColor(getContext(),
                state ? android.R.color.white : R.color.cc_text_color);
        final TextView buttonText = (TextView) button.findViewById(R.id.cc_button_text);
        final ImageView buttonIcon = (ImageView) button.findViewById(R.id.cc_button_icon);
        buttonIcon.setColorFilter(textAndIconColor, PorterDuff.Mode.SRC_ATOP);
        buttonText.setTextColor(textAndIconColor);
        //special cases for background color and text
        if (button == mPauseGhosteryButton) {
            if (state) {
                button.setBackgroundResource(R.drawable.cc_button_background_blue);
                buttonText.setText(getString(R.string.cc_resume_ghostery));
                buttonIcon.setImageResource(R.drawable.cc_ic_resume);
            } else {
                button.setBackgroundResource(R.drawable.cc_button_background_white);
                buttonText.setText(R.string.cc_pause_ghostery);
                buttonIcon.setImageResource(R.drawable.cc_ic_pause);
            }
        } else if (button == mRestrictSiteButton) {
            if (state) {
                button.setBackgroundResource(R.drawable.cc_button_background_red);
            } else {
                button.setBackgroundResource(R.drawable.cc_button_background_white);
            }
        } else if (button == mTrustSiteButton) {
            if (state) {
                button.setBackgroundResource(R.drawable.cc_button_background_green);
            } else {
                button.setBackgroundResource(R.drawable.cc_button_background_white);
            }
        }
        refreshUI();
    }

    private RadioGroup.OnCheckedChangeListener adBlockCheckedChangeListener =
            new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.cc_radio_this_domain) {
                        final GeckoBundle toggleAdBlockStateBundle = new GeckoBundle();
                        toggleAdBlockStateBundle.putString("url", safeGetString(controlCenterSettingsData, "data/summary/pageUrl"));
                        toggleAdBlockStateBundle.putBoolean("isDomain", true);
                        EventDispatcher.getInstance().dispatch("Privacy:AdblockToggle", toggleAdBlockStateBundle);
                        final GeckoBundle ghosteryAdblockPreferenceBundle = new GeckoBundle();
                        ghosteryAdblockPreferenceBundle.putBoolean("enable_ad_block", true);
                        GeckoBundleUtils.safeGetBundle(controlCenterSettingsData,"data/cliqz/adblock")
                                .putBoolean("disabledForDomain", true);
                        EventDispatcher.getInstance().dispatch("Privacy:SetInfo", ghosteryAdblockPreferenceBundle);
                        mAreAdsBlockedGlobally = true;
                        mIsSiteAdBlockWhiteListed = true;
                    } else {
                        //we need to toggle
                        if (mIsSiteAdBlockWhiteListed) {
                            final GeckoBundle toggleAdBlockStateBundle = new GeckoBundle();
                            toggleAdBlockStateBundle.putString("url", safeGetString(controlCenterSettingsData, "data/summary/pageUrl"));
                            toggleAdBlockStateBundle.putBoolean("isDomain", true);
                            EventDispatcher.getInstance().dispatch("Privacy:AdblockToggle", toggleAdBlockStateBundle);
                        }
                        final GeckoBundle ghosteryAdblockPreferenceBundle = new GeckoBundle();
                        ghosteryAdblockPreferenceBundle.putBoolean("enable_ad_block", false);
                        GeckoBundleUtils.safeGetBundle(controlCenterSettingsData,"data/panel/panel")
                                .putBoolean("enable_ad_block", false);
                        EventDispatcher.getInstance().dispatch("Privacy:SetInfo", ghosteryAdblockPreferenceBundle);
                        mAreAdsBlockedGlobally = false;
                        mIsSiteAdBlockWhiteListed = false;
                    }
                }
            };
}
