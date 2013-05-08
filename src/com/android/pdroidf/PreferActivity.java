/**
 * Setting and preferences are shown and selected  from this class
 */
package com.android.pdroidf;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * @author sjagannathan
 *
 */
public class PreferActivity extends PreferenceActivity {
	private static final String TAG = "PreferActivity";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"oncreate");
        addPreferencesFromResource(R.xml.preferences);
    }
    
}

