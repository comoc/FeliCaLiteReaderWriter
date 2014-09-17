/*
 * Copyright 2012 tomorrowkey@gmail.com
 * 
 * Modified by Akihiro Komori on Sep. 15, 2014
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

package com.blogspot.comolog.felicareaderwriter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.tomorrowkey.android.felicalitewriter.R;
import jp.tomorrowkey.android.felicalitewriter.felicalite.ndef.UriNdefBuilder;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class HomeActivity extends Activity {

	public static final String LOG_TAG = HomeActivity.class.getSimpleName();

	private EditText mUrlEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		initUrlEditText();
		initOKButton();

		initNfc();
	}

	private void initUrlEditText() {
		mUrlEditText = (EditText) findViewById(R.id.url_edittext);
		mUrlEditText.setText("");
	}

	private void initOKButton() {
		Button button = (Button) findViewById(R.id.ok_button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				requestMoveToWriteActivity();
			}
		});
	}

	private void requestMoveToWriteActivity() {
		String urlString = mUrlEditText.getText().toString();
		if (TextUtils.isEmpty(urlString)) {
			Toast.makeText(getApplicationContext(), R.string.invalid_url,
					Toast.LENGTH_SHORT).show();
			return;
		}

		// TextNdefBuilder builder = new TextNdefBuilder(urlString, "ja");
		UriNdefBuilder builder = new UriNdefBuilder(urlString);
		NdefMessage ndefMessage = builder.build();
		performMoveToWriteActivity(ndefMessage);
	}

	private void performMoveToWriteActivity(NdefMessage ndefMessage) {
		Intent intent = new Intent(this, WriteActivity.class);
		intent.putExtra(WriteActivity.EXTRA_NDEF_MESSAGE, ndefMessage);
		startActivity(intent);
	}

	private void readNdef(Intent intent) {
		String action = intent.getAction();
		String str = getString(R.string.tag_found) + "\n" + "Action: " + action;
		Parcelable[] raws = intent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		if (raws != null && raws.length > 0) {
			str += "\n";
			NdefMessage[] msgs = new NdefMessage[raws.length];
			for (int i = 0; i < raws.length; i++) {
				msgs[i] = (NdefMessage) raws[i];
				for (NdefRecord record : msgs[i].getRecords()) {
					if (isUriRecord(record)) {
						str += "Uri : " + getUri(record);
					} else {
						str += "Type : " + new String(record.getType()) + "\n";
						str += "TNF : " + record.getTnf() + "\n";
						byte[] payload = record.getPayload();
						if (payload == null)
							break;
						int idx = 0;
						for (byte data : payload) {
							str += String.format(
									"Payload[%2d] : 0x%02x / %c\n", idx, data,
									data);
							idx++;
						}
					}
				}
			}
		}
		// text.setText(str);
		Toast.makeText(this, str, Toast.LENGTH_LONG).show();
	}

	private boolean isUriRecord(NdefRecord record) {
		return record.getTnf() == NdefRecord.TNF_WELL_KNOWN
				&& Arrays.equals(record.getType(), NdefRecord.RTD_URI);
	}

	private static List<String> sProtocolList;
	static {
		sProtocolList = new ArrayList<String>();
		sProtocolList.add("");
		sProtocolList.add("http://www.");
		sProtocolList.add("https://www.");
		sProtocolList.add("http://");
		sProtocolList.add("https://");
		sProtocolList.add("tel:");
		sProtocolList.add("mailto:");
		sProtocolList.add("ftp://anonymous:anonymous@");
		sProtocolList.add("ftp://ftp.");
		sProtocolList.add("ftps://");
		sProtocolList.add("sftp://");
		sProtocolList.add("smb://");
		sProtocolList.add("nfs://");
		sProtocolList.add("ftp://");
		sProtocolList.add("dav://");
		sProtocolList.add("news:");
		sProtocolList.add("telnet://");
		sProtocolList.add("imap:");
		sProtocolList.add("rtsp://");
		sProtocolList.add("urn:");
		sProtocolList.add("pop:");
		sProtocolList.add("sip:");
		sProtocolList.add("sips:");
		sProtocolList.add("tftp:");
		sProtocolList.add("btspp://");
		sProtocolList.add("btl2cap://");
		sProtocolList.add("btgoep://");
		sProtocolList.add("tcpobex://");
		sProtocolList.add("irdaobex://");
		sProtocolList.add("file://");
		sProtocolList.add("urn:epc:id:");
		sProtocolList.add("urn:epc:tag:");
		sProtocolList.add("urn:epc:pat:");
		sProtocolList.add("urn:epc:raw:");
		sProtocolList.add("urn:epc:");
		sProtocolList.add("urn:nfc:");
	}

	/**
	 * RTD URI RecordからURIを取得します
	 * 
	 * @param record
	 * @return
	 */
	private String getUri(NdefRecord record) {
		if (record == null)
			throw new IllegalArgumentException();

		byte[] payload = record.getPayload();
		int identifierCode = payload[0];
		String protocol;
		if (identifierCode < sProtocolList.size()) {
			protocol = sProtocolList.get(identifierCode);
		} else {
			protocol = "";
		}

		String uri;
		try {
			String uriField = new String(payload, 1, payload.length - 1,
					"UTF-8");
			uri = protocol + uriField;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		return uri;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) || // NDEFフォーマット済み
				NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || // 未知もしくはNDEFフォーマットされていない
																	// 初回書き込み時には必要?
				NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) { // それ以外。

			readNdef(intent);

		} else {
			super.onNewIntent(intent);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) {
			// フォアグラウンドディスパッチを有効に
			mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters,
					mTechLists);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mAdapter != null) {
			// フォアグラウンドディスパッチを無効に
			mAdapter.disableForegroundDispatch(this);
		}
	}

	private void initNfc() {
		mAdapter = NfcAdapter.getDefaultAdapter(this);
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		mFilters = new IntentFilter[] { tag, };
		mTechLists = new String[][] { new String[] { NfcF.class.getName() } };
	}

	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;

}
