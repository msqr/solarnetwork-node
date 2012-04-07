/* ===================================================================
 * DatumDaoUploadJob.java
 * 
 * Created Dec 2, 2009 8:55:47 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 * $Revision$
 * ===================================================================
 */

package net.solarnetwork.node.job;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import net.solarnetwork.node.Datum;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.dao.DatumDao;

import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

/**
 * Job to query a {@link DatumDao} for data to upload via an
 * {@link UploadService}.
 * 
 * <p>This job will call {@link DatumDao#getDatumNotUploaded(String)} and for
 * each {@link Datum} returned pass that to 
 * {@link UploadService#uploadDatum(Datum)}. If that returns a non-null 
 * tracking ID, then that will be passed to 
 * {@link DatumDao#storeDatumUpload(Datum, String, Long)}.</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>datumDao</dt>
 *   <dd>The {@link DatumDao} to use to query for {@link Datum} to upload.</dd>
 *   
 *   <dt>uploadService</dt>
 *   <dd>The {@link UploadService} implementation to use to upload the
 *   datum to.</dd>
 * </dl>
 *
 * @param <T> the Datum type for this job
 * @author matt
 * @version $Revision$ $Date$
 */
public class DatumDaoUploadJob<T extends Datum> extends AbstractJob
implements StatefulJob {
	
	private Collection<DatumDao<Datum>> daos;
	private UploadService uploadService = null;

	@Override
	protected void executeInternal(JobExecutionContext jobContext)
			throws Exception {
		for ( DatumDao<Datum> datumDao : daos ) {
			if ( log.isInfoEnabled() ) {
				log.info("Collecting [" +datumDao.getDatumType().getSimpleName() 
						+"] data to bulk upload to [" +uploadService.getKey() +']');
			}

			if ( log.isInfoEnabled() ) {
				log.info("Uploading [" +datumDao.getDatumType().getSimpleName() 
						+"] data to [" +uploadService.getKey() +']');
			}
			
			List<Datum> toUpload = datumDao.getDatumNotUploaded(uploadService.getKey());
			int count = 0;
			
			if ( log.isDebugEnabled() ) {
				log.debug("Uploading " + toUpload.size() +" [" 
						+datumDao.getDatumType().getSimpleName() 
						+"] data to [" +uploadService.getKey() +']');
			}
			
			try {
				for ( Datum datum : toUpload ) {
					Long tid = uploadService.uploadDatum(datum);
					if ( log.isTraceEnabled() ) {
						log.trace("Just uploaded [" 
							+datumDao.getDatumType().getSimpleName() 
							+"] [" +datum.getId() 
							+"] and received tid [" + tid +"]");
					}
					
					if ( tid != null ) {
						datumDao.storeDatumUpload(datum, uploadService.getKey(), tid);
						count++;
					}
				}
			} catch ( RuntimeException e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				if ( root instanceof IOException ) {
					if ( log.isWarnEnabled() ) {
						log.warn("Network problem posting data: " +root.getMessage());
					}
				} else {
					if ( log.isErrorEnabled() ) {
						log.error("Exception posting data", root);
					}
				}
			}
			if ( log.isInfoEnabled() ) {
				log.info("Uploaded " +count +" [" 
						+datumDao.getDatumType().getSimpleName() 
						+"] objects to [" +uploadService.getKey() +']');
			}
		}

	}

	/**
	 * @param daos the daos to set
	 */
	public void setDaos(Collection<DatumDao<Datum>> daos) {
		this.daos = daos;
	}

	/**
	 * @param uploadService the uploadService to set
	 */
	public void setUploadService(UploadService uploadService) {
		this.uploadService = uploadService;
	}

}