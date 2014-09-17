/*
 * TextNdefBuilder is a derived version of UriNdefBuilder.
 * Created by Akihiro Komori
 * 
 * Copyright 2012 tomorrowkey@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.tomorrowkey.android.felicalitewriter.felicalite.ndef;

import java.io.UnsupportedEncodingException;

import org.apache.http.util.ByteArrayBuffer;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

/**
 * RTD_TEXT のNDEFメッセージを作成するクラス
 */
public class TextNdefBuilder {
	private String mTextString;
	private String mLangCode;

	/**
	 * Constructor
	 * 
	 * @param textString
	 *            Text to be written.
	 * @param langCode
	 *            ISO 639-1. en:English, ja:Japanese, etc
	 * @see http
	 *      ://ja.wikipedia.org/wiki/ISO_639-1%E3%82%B3%E3%83%BC%E3%83%89%E4%
	 *      B8%80%E8%A6%A7
	 */
	public TextNdefBuilder(String textString, String langCode) {
		if (textString == null || textString.isEmpty() || langCode == null
				|| langCode.isEmpty())
			throw new IllegalArgumentException();

		mTextString = textString;
		mLangCode = langCode;
	}

	/**
	 * NDEFメッセージを組み立てます
	 * 
	 * @return
	 */
	public NdefMessage build() {
		try {
			byte statusByte = (byte) mLangCode.length();
			byte[] rawLanguageCode = mLangCode.getBytes("US-ASCII");
			byte[] rawText = mTextString.getBytes("UTF-8");

			ByteArrayBuffer buffer = new ByteArrayBuffer(1
					+ rawLanguageCode.length + rawText.length);
			buffer.append(statusByte);
			buffer.append(rawLanguageCode, 0, rawLanguageCode.length);
			buffer.append(rawText, 0, rawText.length);

			byte[] payload = buffer.toByteArray();
			NdefMessage message = new NdefMessage(
					new NdefRecord[] { new NdefRecord(
							NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
							new byte[0], payload) });
			return message;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
