/*
 * Copyright (C) 2012 Timothy Bourke
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

import android.os.Environment;
import android.os.Build;
import android.content.Context;
import java.io.File;

class PlatformInfo
{
    interface PlatformInfoProvider
    {
        boolean hasExternalFilesDir();
        File getExternalFilesDir(Context context);
    }

    private static class BasicPlatformInfo
        implements PlatformInfoProvider
    {
        public boolean hasExternalFilesDir()
        {
            return false;
        }

        public File getExternalFilesDir(Context context)
        {
            return null;
        }
    }

    private static class FroyoPlatformInfo
        implements PlatformInfoProvider
    {
        public boolean hasExternalFilesDir()
        {
            return true;
        }

        public File getExternalFilesDir(Context context)
        {
            return context.getExternalFilesDir((String)null);
        }
    }

    private PlatformInfoProvider info = null;

    PlatformInfo(Context context)
    {
        final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
        if (sdkVersion < Build.VERSION_CODES.FROYO) { /* SDK 8 */
            info = new BasicPlatformInfo();
        } else {
            info = new FroyoPlatformInfo();
        }
    }

    public boolean hasExternalFilesDir()
    {
        return info.hasExternalFilesDir();
    }

    public File getExternalFilesDir(Context context)
    {
        return info.getExternalFilesDir(context);
    }
}

