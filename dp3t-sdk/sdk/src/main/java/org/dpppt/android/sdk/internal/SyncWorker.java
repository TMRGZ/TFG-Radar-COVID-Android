/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import androidx.annotation.NonNull;
import androidx.work.*;

import java.io.*;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.protobuf.InvalidProtocolBufferException;

import okhttp3.ResponseBody;
import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.backend.models.ApplicationInfo;
import org.dpppt.android.sdk.internal.backend.BackendBucketRepository;
import org.dpppt.android.sdk.internal.backend.ServerTimeOffsetException;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.backend.proto.Exposed;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.util.DayDate;
import retrofit2.Response;
import org.apache.commons.lang3.SerializationUtils;
import com.duprasville.guava.probably.CuckooFilter;

import static org.dpppt.android.sdk.internal.backend.BackendBucketRepository.BATCH_LENGTH;

public class SyncWorker extends Worker {

	private static final String TAG = "SyncWorker";
	private static final String WORK_TAG = "org.dpppt.android.sdk.internal.SyncWorker";
	private static final String KEYFILE_PREFIX = "keyfile_";

	private static PublicKey bucketSignaturePublicKey;

	public static void startSyncWorker(Context context) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
				.setConstraints(constraints)
				.build();

		WorkManager workManager = WorkManager.getInstance(context);
		workManager.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);
	}

	public static void stopSyncWorker(Context context) {
		WorkManager workManager = WorkManager.getInstance(context);
		workManager.cancelAllWorkByTag(WORK_TAG);
	}

	public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	public static void setBucketSignaturePublicKey(PublicKey publicKey) {
		bucketSignaturePublicKey = publicKey;
	}

	@NonNull
	@Override
	public Result doWork() {
		Logger.d(TAG, "start SyncWorker");
		Context context = getApplicationContext();

		long scanInterval = AppConfigManager.getInstance(getApplicationContext()).getScanInterval();
		TracingService.scheduleNextClientRestart(context, scanInterval);
		TracingService.scheduleNextServerRestart(context);

		try {
			doSync(context);
		} catch (IOException | StatusCodeException | ServerTimeOffsetException | SignatureException | SQLiteException e) {
			Logger.d(TAG, "SyncWorker finished with exception " + e.getMessage());
			return Result.retry();
		}
		Logger.d(TAG, "SyncWorker finished with success");
		return Result.success();
	}

	public static void doSync(Context context)
			throws IOException, StatusCodeException, ServerTimeOffsetException, SQLiteException, SignatureException {
		try {
			doSyncInternal(context);
			Logger.i(TAG, "synced");
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(true);
			SyncErrorState.getInstance().setSyncError(null);
			BroadcastHelper.sendErrorUpdateBroadcast(context);
		} catch (IOException | StatusCodeException | ServerTimeOffsetException | SignatureException | SQLiteException e) {
			Logger.e(TAG, e);
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(false);
			ErrorState syncError;
			if (e instanceof ServerTimeOffsetException) {
				syncError = ErrorState.SYNC_ERROR_TIMING;
			} else if (e instanceof SignatureException) {
				syncError = ErrorState.SYNC_ERROR_SIGNATURE;
			} else if (e instanceof StatusCodeException || e instanceof InvalidProtocolBufferException) {
				syncError = ErrorState.SYNC_ERROR_SERVER;
			} else if (e instanceof SQLiteException) {
				syncError = ErrorState.SYNC_ERROR_DATABASE;
			} else {
				syncError = ErrorState.SYNC_ERROR_NETWORK;
			}
			SyncErrorState.getInstance().setSyncError(syncError);
			BroadcastHelper.sendErrorUpdateBroadcast(context);
			throw e;
		}
	}

	private static void doSyncInternal(Context context) throws IOException, StatusCodeException, ServerTimeOffsetException {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.updateFromDiscoverySynchronous();
		ApplicationInfo appConfig = appConfigManager.getAppConfig();

		Database database = new Database(context);
		database.generateContactsFromHandshakes(context);

		long lastLoadedBatchReleaseTime = appConfigManager.getLastLoadedBatchReleaseTime();
		long nextBatchReleaseTime;
		if (lastLoadedBatchReleaseTime <= 0 || lastLoadedBatchReleaseTime % BATCH_LENGTH != 0) {
			long now = System.currentTimeMillis();
			nextBatchReleaseTime = now - (now % BATCH_LENGTH);
		} else {
			nextBatchReleaseTime = lastLoadedBatchReleaseTime + BATCH_LENGTH;
		}

		BackendBucketRepository backendBucketRepository =
				new BackendBucketRepository(context, appConfig.getBucketBaseUrl(), bucketSignaturePublicKey);

		DayDate lastDateToCheck = new DayDate();
		DayDate dateToLoad = lastDateToCheck.subtractDays(9);

		ArrayList<File> fileList = null;
		for (long batchReleaseTime = nextBatchReleaseTime;
			 batchReleaseTime < System.currentTimeMillis();
			 batchReleaseTime += BATCH_LENGTH) {

			Response<ResponseBody> result = backendBucketRepository.getExposees(batchReleaseTime);


			if (result.code() != 204) {
				File file = new File(context.getCacheDir(),
						KEYFILE_PREFIX + dateToLoad.formatAsString() + "_" + lastLoadedBatchReleaseTime + ".zip");
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				byte[] bytesIn = new byte[1024];
				int read = 0;
				InputStream bodyStream = result.body().byteStream();
				while ((read = bodyStream.read(bytesIn)) != -1) {
					bos.write(bytesIn, 0, read);
				}
				bos.close();

				fileList = new ArrayList<>();
				fileList.add(file);
				String token = dateToLoad.formatAsString();
				//googleExposureClient.provideDiagnosisKeys(fileList, token);
				//lastExposureClientCalls.put(dateToLoad, System.currentTimeMillis());
			}

			/*long batchReleaseServerTime = result.getBatchReleaseTime();
			for (Exposed.ProtoExposeeOrBuilder exposee : result.getExposedOrBuilderList()) {
				database.addKnownCase(
						context,
						exposee.getKey().toByteArray(),
						exposee.getKeyDate(),
						batchReleaseServerTime
				);
			}*/
			appConfigManager.setLastLoadedBatchReleaseTime(batchReleaseTime);
		}
		getFilterFromZip(fileList);

		database.removeOldData();

		appConfigManager.setLastSyncDate(System.currentTimeMillis());
	}

	private static void	getFilterFromZip(ArrayList<File> fileList) throws IOException {
		ZipInputStream keyZipInputstream = new ZipInputStream(new FileInputStream(fileList.get(0)));

		ZipEntry entry = keyZipInputstream.getNextEntry();
		boolean foundData = false;
		boolean foundSignature = false;

		byte[] signatureProto = null;
		byte[] exportBin = null;
		byte[] keyProto = null;

		while (entry != null) {
			if (entry.getName().equals("export.bin")) {
				foundData = true;
				keyZipInputstream.read(exportBin, 0, Integer.MAX_VALUE);
				keyProto = new byte[exportBin.length - 16];
				System.arraycopy(exportBin, 16, keyProto, 0, keyProto.length);
			}
			if (entry.getName().equals("export.sig")) {
				foundSignature = true;
				keyZipInputstream.read(signatureProto, 0, Integer.MAX_VALUE);
			}
			entry = keyZipInputstream.getNextEntry();
		}

		//assertTrue(foundData, "export.bin not found in zip");
		//assertTrue(foundSignature, "export.sig not found in zip");

		//TEKSignatureList list = TEKSignatureList.parseFrom(signatureProto);
		//TemporaryExposureKeyExport export = TemporaryExposureKeyExport.parseFrom(keyProto);
		CuckooFilter<byte[]> export = SerializationUtils.deserialize(keyProto);
		System.out.println(export.size());

		//var sig = list.getSignatures(0);
		//java.security.Signature signatureVerifier =
		//		java.security.Signature.getInstance(sig.getSignatureInfo().getSignatureAlgorithm().trim());
		//signatureVerifier.initVerify(signer.getPublicKey());
//
		//signatureVerifier.update(exportBin);
		//assertTrue(
		//		signatureVerifier.verify(sig.getSignature().toByteArray()),
		//		"Could not verify signature in zip file");
		//assertEquals(expectKeyCount, export.size());
	}

}
