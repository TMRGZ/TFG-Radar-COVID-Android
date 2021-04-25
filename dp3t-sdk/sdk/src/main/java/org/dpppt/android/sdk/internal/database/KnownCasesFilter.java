/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.database;

interface KnownCasesFilter {

	String TABLE_NAME = "known_cases_filter";

	String ID = "id";
	String ONSET = "onset";
	String BUCKET_TIME = "batchTimestamp";
	String CASES_FILTER = "key";

	String[] PROJECTION = {
			ID,
			CASES_FILTER
	};

	static String create() {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY NOT NULL, "
				+ CASES_FILTER + " BLOB NOT NULL, "
				+ "CONSTRAINT no_duplicates UNIQUE (" + CASES_FILTER + ") )";
	}

	static String drop() {
		return "DROP TABLE IF EXISTS " + TABLE_NAME;
	}

}
