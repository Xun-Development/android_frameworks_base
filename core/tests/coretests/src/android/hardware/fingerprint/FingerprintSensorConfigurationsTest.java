/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.hardware.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.biometrics.fingerprint.SensorProps;
import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;

import androidx.test.filters.SmallTest;

import com.android.internal.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Presubmit
@SmallTest
public class FingerprintSensorConfigurationsTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;
    @Mock
    private TypedArray mSfpsSensorProps;

    private final String[] mAidlInstances = new String[]{"default", "virtual"};
    private String[] mHidlConfigStrings = new String[]{"0:2:15", "0:8:15"};
    private FingerprintSensorConfigurations mFingerprintSensorConfigurations;

    @Before
    public void setUp() throws RemoteException {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.obtainTypedArray(R.array.config_sfps_sensor_props))
                .thenReturn(mSfpsSensorProps);
        when(mSfpsSensorProps.length()).thenReturn(0);
        when(mResources.getIntArray(R.array.config_udfps_sensor_props)).thenReturn(new int[0]);
        when(mResources.getBoolean(R.bool.config_is_powerbutton_fps)).thenReturn(false);
        when(mResources.getInteger(R.integer.config_fingerprintMaxTemplatesPerUser)).thenReturn(1);
    }

    @Test
    public void testAidlInstanceSensorProps() {
        mFingerprintSensorConfigurations = new FingerprintSensorConfigurations(
                true /* resetLockoutRequiresHardwareAuthToken */);
        mFingerprintSensorConfigurations.addAidlSensors(mAidlInstances);

        assertThat(mFingerprintSensorConfigurations.hasSensorConfigurations()).isTrue();
        assertThat(!mFingerprintSensorConfigurations.isSingleSensorConfigurationPresent())
                .isTrue();
        assertThat(mFingerprintSensorConfigurations.getResetLockoutRequiresHardwareAuthToken())
                .isTrue();
    }

    @Test
    public void testHidlConfigStrings() {
        mFingerprintSensorConfigurations = new FingerprintSensorConfigurations(
                false /* resetLockoutRequiresHardwareAuthToken */);
        mFingerprintSensorConfigurations.addHidlSensors(mHidlConfigStrings, mContext);

        final SensorProps[] properties =
                mFingerprintSensorConfigurations.getSensorPropForInstance("defaultHIDL");
        assertThat(properties).hasLength(1);
        assertThat(properties[0].commonProps.sensorId).isEqualTo(0);
        assertThat(properties[0].commonProps.maxEnrollmentsPerUser).isEqualTo(1);
        assertThat(mFingerprintSensorConfigurations.isSingleSensorConfigurationPresent()).isTrue();
        assertThat(mFingerprintSensorConfigurations.getResetLockoutRequiresHardwareAuthToken())
                .isFalse();
    }

    @Test
    public void testHidlConfigStrings_incorrectFormat() {
        mHidlConfigStrings = new String[]{"0:8:15", "0:2", "0:fingerprint:15"};
        mFingerprintSensorConfigurations = new FingerprintSensorConfigurations(
                false /* resetLockoutRequiresHardwareAuthToken */);
        mFingerprintSensorConfigurations.addHidlSensors(mHidlConfigStrings, mContext);

        assertThat(mFingerprintSensorConfigurations.hasSensorConfigurations()).isFalse();
        assertThat(mFingerprintSensorConfigurations.doesInstanceExist("defaultHIDL")).isFalse();
        assertThat(mFingerprintSensorConfigurations.getResetLockoutRequiresHardwareAuthToken())
                .isFalse();
    }

    @Test
    public void testHidlConfigStrings_faceOnly_doesNotAddFingerprintInstance() {
        mFingerprintSensorConfigurations = new FingerprintSensorConfigurations(
                true /* resetLockoutRequiresHardwareAuthToken */);
        mFingerprintSensorConfigurations.addHidlSensors(new String[]{"1:8:255"}, mContext);

        assertThat(mFingerprintSensorConfigurations.hasSensorConfigurations()).isFalse();
        assertThat(mFingerprintSensorConfigurations.doesInstanceExist("defaultHIDL")).isFalse();
    }

    @Test
    public void testHidlConfigStrings_empty_doesNotAddFingerprintInstance() {
        mFingerprintSensorConfigurations = new FingerprintSensorConfigurations(
                true /* resetLockoutRequiresHardwareAuthToken */);
        mFingerprintSensorConfigurations.addHidlSensors(new String[0], mContext);

        assertThat(mFingerprintSensorConfigurations.hasSensorConfigurations()).isFalse();
    }

    @Test
    public void testHidlConfigStrings_withoutFingerprint_preservesAidlInstance() {
        mFingerprintSensorConfigurations = new FingerprintSensorConfigurations(
                true /* resetLockoutRequiresHardwareAuthToken */);
        mFingerprintSensorConfigurations.addAidlSensors(new String[]{"default"});
        mFingerprintSensorConfigurations.addHidlSensors(
                new String[]{"1:8:255", "0:2", "0:fingerprint:15"}, mContext);

        assertThat(mFingerprintSensorConfigurations.isSingleSensorConfigurationPresent()).isTrue();
        assertThat(mFingerprintSensorConfigurations.getSensorInstance()).isEqualTo("default");
        assertThat(mFingerprintSensorConfigurations.doesInstanceExist("defaultHIDL")).isFalse();
        assertThat(mFingerprintSensorConfigurations.getResetLockoutRequiresHardwareAuthToken())
                .isTrue();
    }
}
