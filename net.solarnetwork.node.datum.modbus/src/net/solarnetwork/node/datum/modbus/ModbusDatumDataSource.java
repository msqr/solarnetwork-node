/* ==================================================================
 * ModbusDatumDataSource.java - 20/12/2017 7:04:42 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.datum.modbus;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;

/**
 * Generic Modbus device datum data source.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusDatumDataSource extends ModbusDeviceDatumDataSourceSupport implements
		DatumDataSource<GeneralNodeDatum>, SettingSpecifierProvider, ModbusConnectionAction<ModbusData> {

	private String sourceId = "modbus";
	private long sampleCacheMs = 5000;
	private int maxReadWordCount = 64;
	private ModbusPropertyConfig[] propConfigs;

	private final ModbusData sample = new ModbusData();

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return Collections.emptyMap();
	}

	@Override
	public Class<? extends GeneralNodeDatum> getDatumType() {
		return GeneralNodeDatum.class;
	}

	@Override
	public GeneralNodeDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final ModbusData currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setCreated(new Date(currSample.getDataTimestamp()));
		d.setSourceId(sourceId);
		populateDatumProperties(currSample, d, propConfigs);
		if ( currSample.getDataTimestamp() >= start ) {
			// we read from the device
			postDatumCapturedEvent(d);
		}
		return d;
	}

	private void populateDatumProperties(ModbusData sample, GeneralNodeDatum d,
			ModbusPropertyConfig[] propConfs) {
		if ( propConfs == null ) {
			return;
		}
		for ( ModbusPropertyConfig conf : propConfs ) {
			Object propVal = null;
			switch (conf.getDataType()) {
				case Bytes:
					// can't set on datum currently
					break;

				case Float32:
					propVal = sample.getFloat32(conf.getAddress());
					break;

				case Float64:
					propVal = sample.getFloat64(conf.getAddress());
					break;

				case Int16:
					propVal = sample.getInt16(conf.getAddress());
					break;

				case Int32:
					propVal = sample.getInt32(conf.getAddress());
					break;

				case Int64:
					propVal = sample.getInt64(conf.getAddress());
					break;

				case SignedInt16:
					propVal = sample.getSignedInt16(conf.getAddress());
					break;

				case StringAscii:
					propVal = sample.getAsciiString(conf.getAddress(), conf.getWordLength(), true);
					break;

				case StringUtf8:
					propVal = sample.getUtf8String(conf.getAddress(), conf.getWordLength(), true);
					break;
			}

			if ( propVal != null ) {
				switch (conf.getDatumPropertyType()) {
					case Accumulating:
						if ( propVal instanceof Number ) {
							d.putAccumulatingSampleValue(conf.getName(), (Number) propVal);
						} else {
							log.warn(
									"Cannot set datum accumulating property {} to non-number value [{}]",
									conf.getName(), propVal);
						}
						break;

					case Instantaneous:
						if ( propVal instanceof Number ) {
							d.putInstantaneousSampleValue(conf.getName(), (Number) propVal);
						} else {
							log.warn(
									"Cannot set datum instantaneous property {} to non-number value [{}]",
									conf.getName(), propVal);
						}
						break;

					case Status:
						d.putStatusSampleValue(conf.getName(), propVal);
						break;
				}
			}
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.modbus";
	}

	@Override
	public String getDisplayName() {
		return "Generic Modbus Device";
	}

	private static IntRangeSet getRegisterAddressSet(ModbusPropertyConfig[] configs) {
		IntRangeSet set = new IntRangeSet();
		if ( configs != null ) {
			for ( ModbusPropertyConfig config : configs ) {
				int len = config.getDataType().getWordLength();
				if ( len == -1 ) {
					len = config.getWordLength();
				}
				set.addAll(config.getAddress(), config.getAddress() + len);
			}
		}
		return set;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();
		ModbusDatumDataSource defaults = new ModbusDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.sampleCacheMs)));
		results.add(new BasicTextFieldSettingSpecifier("maxReadWordCount",
				String.valueOf(defaults.maxReadWordCount)));

		ModbusPropertyConfig[] confs = getPropConfigs();
		List<ModbusPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<ModbusPropertyConfig> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingsUtil.KeyedListCallback<ModbusPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(ModbusPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								ModbusPropertyConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return results;
	}

	@Override
	public ModbusData doWithConnection(final ModbusConnection conn) throws IOException {
		sample.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				final int maxReadLen = maxReadWordCount;
				IntRangeSet addressRangeSet = getRegisterAddressSet(propConfigs);
				IntRange[] ranges = addressRangeSet.ranges();
				for ( IntRange range : ranges ) {
					for ( int start = range.first(); start < range.last(); ) {
						int len = Math.min(range.last() - start, maxReadLen);
						int[] data = conn.readInts(start, len);
						m.saveDataArray(data, start);
						start += len;
					}
				}
				return true;
			}
		});
		return sample.copy();
	}

	private ModbusData getCurrentSample() {
		ModbusData currSample;
		if ( isCachedSampleExpired() ) {
			try {
				currSample = performAction(this);
				if ( currSample != null && log.isTraceEnabled() ) {
					log.trace(currSample.dataDebugString());
				}
				log.debug("Read modbus data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from Modbus device " + modbusNetwork(), e);
			}
		} else {
			currSample = sample.copy();
		}
		return currSample;
	}

	private boolean isCachedSampleExpired() {
		final long lastReadDiff = System.currentTimeMillis() - sample.getDataTimestamp();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	/**
	 * Get the sample cache maximum age, in milliseconds.
	 * 
	 * @return the cache milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 * 
	 * @param sampleCacheSecondsMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the property configurations.
	 * 
	 * @return the property configurations
	 */
	public ModbusPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Get the property configurations to use.
	 * 
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(ModbusPropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		ModbusPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ModbusPropertyConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propIncludes} elements.
	 */
	public void setPropConfigsCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		ModbusPropertyConfig[] confs = this.propConfigs;
		int lCount = (confs == null ? 0 : confs.length);
		if ( lCount != count ) {
			ModbusPropertyConfig[] newIncs = new ModbusPropertyConfig[count];
			if ( confs != null ) {
				System.arraycopy(confs, 0, newIncs, 0, Math.min(count, confs.length));
			}
			for ( int i = 0; i < count; i++ ) {
				if ( newIncs[i] == null ) {
					newIncs[i] = new ModbusPropertyConfig();
				}
			}
			this.propConfigs = newIncs;
		}
	}

	/**
	 * Set the maximum number of Modbus registers to read in any single read
	 * operation.
	 * 
	 * <p>
	 * Some modbus devices do not handle large read ranges. This setting can be
	 * used to limit the number of registers read at one time.
	 * </p>
	 * 
	 * @param maxReadWordCount
	 *        the maximum word count; defaults to {@literal 64}
	 */
	public void setMaxReadWordCount(int maxReadWordCount) {
		if ( maxReadWordCount < 1 ) {
			return;
		}
		this.maxReadWordCount = maxReadWordCount;
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param soruceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
