/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.deviceinfo.storage;

import static com.android.settings.TestUtils.KILOBYTE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.SubSettings;
import com.android.settings.TestConfig;
import com.android.settings.applications.ManageApplications;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class StorageItemPreferenceControllerTest {
    private Context mContext;
    private VolumeInfo mVolume;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Fragment mFragment;
    @Mock
    private StorageVolumeProvider mSvp;
    private StorageItemPreferenceController mController;
    private StorageItemPreference mPreference;
    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        FakeFeatureFactory.setupForTest(mContext);
        mFakeFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        mVolume = new VolumeInfo("id", 0, null, "id");
        // Note: null is passed as the Lifecycle because we are handling it outside of the normal
        //       Settings fragment lifecycle for test purposes.
        mController = new StorageItemPreferenceController(mContext, mFragment, mVolume, mSvp);
        mPreference = new StorageItemPreference(mContext);

        // Inflate the preference and the widget.
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(
                mPreference.getLayoutResource(), new LinearLayout(mContext), false);
    }

    @Test
    public void testUpdateStateWithInitialState() {
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.memory_calculating_size));
    }

    @Test
    public void testClickPhotos() {
        mPreference.setKey("pref_photos_videos");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                any(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getType()).isEqualTo("image/*");
        assertThat(intent.getAction()).isEqualTo(android.content.Intent.ACTION_VIEW);
    }

    @Test
    public void testClickAudio() {
        mPreference.setKey("pref_music_audio");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                any(UserHandle.class));
        Intent intent = argumentCaptor.getValue();

        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ManageApplications.class.getName());
        assertThat(intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS).getInt(
                ManageApplications.EXTRA_STORAGE_TYPE, 0)).isEqualTo(
                ManageApplications.STORAGE_TYPE_MUSIC);
    }

    @Test
    public void testClickApps() {
        mPreference.setKey("pref_other_apps");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                any(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ManageApplications.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.apps_storage);
    }

    @Test
    public void testClickFiles() {
        when(mSvp.findEmulatedForPrivate(any(VolumeInfo.class))).thenReturn(mVolume);
        mPreference.setKey("pref_files");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                any(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        Intent browseIntent = mVolume.buildBrowseIntent();
        assertThat(intent.getAction()).isEqualTo(browseIntent.getAction());
        assertThat(intent.getData()).isEqualTo(browseIntent.getData());
        verify(mMetricsFeatureProvider, times(1)).action(
                any(Context.class), eq(MetricsEvent.STORAGE_FILES));
    }

    @Test
    public void testClickGames() {
        mPreference.setKey("pref_games");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                any(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ManageApplications.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.game_storage_settings);
    }

    @Test
    public void testMeasurementCompletedUpdatesPreferences() {
        StorageItemPreference audio = new StorageItemPreference(mContext);
        StorageItemPreference image = new StorageItemPreference(mContext);
        StorageItemPreference games = new StorageItemPreference(mContext);
        StorageItemPreference apps = new StorageItemPreference(mContext);
        StorageItemPreference system = new StorageItemPreference(mContext);
        StorageItemPreference files = new StorageItemPreference(mContext);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.AUDIO_KEY))).thenReturn(audio);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.PHOTO_KEY))).thenReturn(image);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.GAME_KEY))).thenReturn(games);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.OTHER_APPS_KEY))).thenReturn(apps);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.SYSTEM_KEY))).thenReturn(system);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.FILES_KEY))).thenReturn(files);
        mController.displayPreference(screen);

        mController.setSystemSize(KILOBYTE * 6);
        StorageAsyncLoader.AppsStorageResult result = new StorageAsyncLoader.AppsStorageResult();
        result.gamesSize = KILOBYTE * 8;
        result.musicAppsSize = KILOBYTE * 4;
        result.otherAppsSize = KILOBYTE * 9;
        result.systemSize = KILOBYTE * 10;
        result.externalStats = new StorageStatsSource.ExternalStorageStats(
                KILOBYTE * 50, // total
                KILOBYTE * 10, // audio
                KILOBYTE * 15, // video
                KILOBYTE * 20); // image

        result.gamesSize = KILOBYTE * 8;
        result.otherAppsSize = KILOBYTE * 9;
        mController.onLoadFinished(result);

        assertThat(audio.getSummary().toString()).isEqualTo("14.00KB"); // 4KB apps + 10KB files
        assertThat(image.getSummary().toString()).isEqualTo("35.00KB"); // 15KB video + 20KB images
        assertThat(games.getSummary().toString()).isEqualTo("8.00KB");
        assertThat(apps.getSummary().toString()).isEqualTo("9.00KB");
        assertThat(system.getSummary().toString()).isEqualTo("16.00KB");
        assertThat(files.getSummary().toString()).isEqualTo("5.00KB");
    }
}