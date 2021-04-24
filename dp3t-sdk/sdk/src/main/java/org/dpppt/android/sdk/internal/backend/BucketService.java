/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.backend;

import okhttp3.ResponseBody;
import org.dpppt.android.sdk.internal.backend.proto.Exposed;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

interface BucketService {

	@Headers("Accept: application/zip")
	@GET("v2UMA/gaen/exposed")
	Call<ResponseBody> getExposees(/*@Path("batchReleaseTime") long batchReleaseTime*/);

}
