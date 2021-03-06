/*
 * Copyright (C) 2010 Timothy Bourke
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.tbrk.mnemododo;

import java.util.Vector;

import mnemogogo.mobile.hexcsv.FindCardDirAndroid;
import mnemogogo.mobile.hexcsv.HexCsvAndroid;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.KeyEvent;
import android.widget.TextView;
import android.util.Log;
import java.io.File;

public class Settings
    extends PreferenceActivity
{
    protected int key_assign_mode = 0;
    protected Dialog key_assign_dialog = null;
    protected boolean is_demo = false;

    PlatformInfo platform_info = new PlatformInfo(Settings.this);

    protected static final int key_text_ids[] = {
        R.id.key_show, R.id.key_grade0, R.id.key_grade1,
        R.id.key_grade2, R.id.key_grade3, R.id.key_grade4,
        R.id.key_grade5, R.id.key_replay_sounds
    };
    protected static final String key_pref_names[] = {
        "key_show_answer", "key_grade0", "key_grade1",
        "key_grade2", "key_grade3", "key_grade4",
        "key_grade5", "key_replay_sounds"
    };

    protected int[] keys_assigned = new int[key_text_ids.length];
    
    protected static final int DIALOG_KEY_ASSIGN = 0;

    private class PassState
    {
        CharSequence[] entries = null;
        FindCardDirsTask find_task = null;
        int key_assign_mode = 0;
        int[] keys_assigned = null;

        PassState(CharSequence[] entries, FindCardDirsTask find_task,
                  int key_assign_mode, int[] keys_assigned)
        {
            this.entries = entries;
            this.find_task = find_task;
            this.key_assign_mode = key_assign_mode;
            this.keys_assigned = keys_assigned;
        }
    }

    private FindCardDirsTask find_task = null;
    
    private class FindCardDirsTask
        extends ProgressTask<Boolean, Vector<String>>
    {
        private String restrict_paths[] = null;

        FindCardDirsTask(TaskListener<Vector<String>> callback)
        {
            super(callback, R.string.searching_for_card_dirs);
        }
        
        public void onPreExecute()
        {
            if (!is_demo) {
                SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(Settings.this);

                File extdir = null;
                if (platform_info.hasExternalFilesDir()) {
                    extdir = platform_info.getExternalFilesDir(Settings.this);
                }
    
                String path = prefs.getString("restrict_search", "").trim();
                if (!path.equals("")) {
                    if (extdir == null) {
                        restrict_paths = new String[1];
                        restrict_paths[0] = path;
                    } else {
                        restrict_paths = new String[2];
                        restrict_paths[0] = path;
                        restrict_paths[1] = extdir.toString();
                    }
                }
            }
        }

        public Vector<String> doInBackground(Boolean... ignore)
        {
            Vector<String> result = null;
            File roots[] = new File[1];

            try {
                startOperation(0, "");

                if (restrict_paths != null) {
                    result = FindCardDirAndroid.list(restrict_paths);

                } else if (is_demo) {
                    result = FindCardDirAndroid.list(!is_demo);

                } else {
                    result = null;

                    if (platform_info.hasExternalFilesDir()) {
                        roots[0] = platform_info.getExternalFilesDir(Settings.this);
                        if (roots[0] != null) {
                            result = FindCardDirAndroid.list(roots);
                        }
                    }


                    if (result == null || result.isEmpty()) {
                        roots[0] = Environment.getExternalStorageDirectory();
                        result = FindCardDirAndroid.list(roots);
                    }
                }

            } catch (Exception e) { }

            stopOperation();

            if (result == null) {
                result = new Vector<String>();
            }

            return result;
        }
    }

    TaskListener<Vector<String>> makeFindCardDirsListener(
            final ListPreference list_pref)
    {
        return new TaskListener<Vector<String>> () {
            public Context getContext ()
            {
                return Settings.this;
            }

            public String getString(int resid)
            {
                return Settings.this.getString(resid);
            }

            public void onFinished(Vector<String> result)
            {
                find_task = null;

                if (list_pref == null) {
                    return;
                }

                if (result.isEmpty()) {
                    list_pref.setSummary(R.string.cannot_find_card_dirs);

                } else {
                    String[] dirs = result.toArray(new String[result.size()]);
                    list_pref.setEntries(getCardDirValues(dirs));
                    list_pref.setEntryValues(dirs);

                    list_pref.setEnabled(true);
                }
            }
        };
    }

    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        is_demo = intent.getBooleanExtra("is_demo", false);

        addPreferencesFromResource(R.xml.settings);

        Preference prefKeys = (Preference) findPreference("prefKeys");
        prefKeys.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startAssignKeys();
                return false;
            }
        });
        
        Preference restrict = (Preference) findPreference("restrict_search");
        restrict.setEnabled(!is_demo);
        
        PassState lastnonconfig = (PassState) getLastNonConfigurationInstance();
        if (lastnonconfig == null) {
            setCardDirEntries(null);
        } else {
            find_task = lastnonconfig.find_task;
            setCardDirEntries(lastnonconfig.entries);
            key_assign_mode = lastnonconfig.key_assign_mode;
            keys_assigned = lastnonconfig.keys_assigned;
        }

        setResult(RESULT_OK);
    }

    public CharSequence[] getCardDirValues(CharSequence[] entries)
    {        
        String extdir = null;
        String cardpath = Environment.getExternalStorageDirectory().toString();

        CharSequence[] values = new CharSequence[entries.length];

        if (platform_info.hasExternalFilesDir()) {
            File dir = platform_info.getExternalFilesDir(Settings.this);
            if (dir != null) {
                extdir = dir.toString();
            }
        }

        for (int i=0; i < entries.length; ++i) {
            String e = entries[i].toString();
            if (e.startsWith(HexCsvAndroid.demo_prefix)) {
                values[i] = "demo: "
                    + e.substring(HexCsvAndroid.demo_prefix.length());
            } else if (extdir != null && e.startsWith(extdir)) {
                if (e.length() == extdir.length()) {
                    values[i] = "[files]";
                } else {
                    values[i] = e.substring(extdir.length() + 1);
                }
            } else if (e.startsWith(cardpath)) {
                values[i] = e.substring(cardpath.length());
            } else {
                values[i] = entries[i];
            }
        }
        
        return values;
    }
    
    public void setCardDirEntries(CharSequence[] entries)
    {
        ListPreference pref_card_dir = (ListPreference) getPreferenceScreen()
            .findPreference("cards_path");

        if (entries == null) {
            entries = pref_card_dir.getEntries();
        }
        
        if ((entries == null) || (entries.length == 0)) {
            pref_card_dir.setEnabled(false);

            if (find_task == null) {
                find_task = new FindCardDirsTask(
                    makeFindCardDirsListener(pref_card_dir));
                find_task.execute();
            } else {
                find_task.updateCallback(
                    makeFindCardDirsListener(pref_card_dir));
            }

        } else {
            pref_card_dir.setEntries(getCardDirValues(entries));
            pref_card_dir.setEntryValues(entries);
            pref_card_dir.setEnabled(true);
        }
    }

    public Object onRetainNonConfigurationInstance()
    {
        ListPreference pref_card_dir = (ListPreference) getPreferenceScreen()
            .findPreference("cards_path");
        return new PassState(pref_card_dir.getEntries(), find_task,
                             key_assign_mode, keys_assigned);
    }

    public void onPause()
    {
        super.onPause();
        if (find_task != null) {
            find_task.pause();
        }
    }

    public void onDestroy()
    {
        super.onDestroy();
        if (find_task != null) {
            find_task.destroy();
        }
    }

    public void onContentChanged()
    {
        super.onContentChanged();
    }

    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;

        // Context mContext = getApplicationContext();
        Context mContext = Settings.this;

        switch (id) {
        case DIALOG_KEY_ASSIGN:
            dialog = new Dialog(mContext);
            dialog.setContentView(R.layout.mapkeys);
            dialog.setTitle(getString(R.string.setting_keys));
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                {
                    if (keyCode == KeyEvent.KEYCODE_MENU
                            || keyCode == KeyEvent.KEYCODE_HOME
                            || keyCode == KeyEvent.KEYCODE_BACK)
                    {
                        key_assign_mode = 0;
                        return false;
                    }
                    
                    for (int i = 0; i < key_assign_mode; ++i) {
                        if (keys_assigned[i] == keyCode) {
                            return false;
                        }
                    }

                    keys_assigned[key_assign_mode++] = keyCode;
                    if (key_assign_mode < keys_assigned.length) {
                        highlightCurrentKey();
                    } else {
                        finishAssignKeys();
                        dialog.dismiss();
                    }

                    return true;
                }
            });
            key_assign_dialog = dialog;
            highlightCurrentKey();
            break;
        }

        return dialog;
    }

    public void highlightCurrentKey()
    {
        if (key_assign_dialog == null) {
            return;
        }

        for (int i = 0; i < key_text_ids.length; ++i) {
            TextView t = (TextView) key_assign_dialog.findViewById(key_text_ids[i]);
            if (t != null) {
                if (i == key_assign_mode) {
                    t.setEnabled(true);
                    t.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                } else {
                    t.setEnabled(false);
                    t.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                }
            }
        }
    }
    
    public void startAssignKeys()
    {
        key_assign_mode = 0;
        showDialog(DIALOG_KEY_ASSIGN);
        highlightCurrentKey();
    }

    public void finishAssignKeys()
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        Editor prefsEdit = prefs.edit();
        
        for (int i = 0; i < key_pref_names.length; ++i) {
            prefsEdit.putInt(key_pref_names[i], keys_assigned[i]);
        }
        prefsEdit.commit();
    }

}
