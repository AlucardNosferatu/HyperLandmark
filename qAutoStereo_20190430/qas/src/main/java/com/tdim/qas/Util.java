package com.tdim.qas;

import android.content.Context;             //API level 1
import android.content.pm.PackageInfo;      //API level 1
import android.content.pm.PackageManager;   //API level 1
import android.os.Build;                    //API level 1
import android.os.Environment;              //API level 1
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;                    //API level 1

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

final class Util {
    private static final String TAG = Util.class.getCanonicalName();

    static boolean hasPermission(Context context, String permission) {
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
                for(String p: pi.requestedPermissions)
                    if(p.equals(permission))
                        return true;
            } catch(Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        else
            return PackageManager.PERMISSION_GRANTED==context.checkSelfPermission(permission);
    }

    //------------------------------FILE SEARCH------------------------------
    interface FileFoundListener {
        /**
         * Called while searching the storage when matching file was found.
         * @param file next file that matched the filter
         * @return true if search should stop, false to continue
         */
        boolean onFileFound(File file);
    }

    static List<File> scanDirectory(final String extension, @Nullable File scanDir) {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {return file.getName().endsWith(extension);}
        };
        File dir = null==scanDir?Environment.getExternalStorageDirectory():scanDir;
        List<File> list = new ArrayList<>();
        File[] files = safeListFiles(dir);
        if(null==files)
            Log.e(TAG, "Can't read files for "+dir.getAbsolutePath());
        else
            for(File file: files)
                if(file.isFile()&&filter.accept(file))
                    list.add(file);
        return list;
    }
    static void scanRecursive(final String extension, @Nullable File startDir, @NonNull FileFoundListener listener) {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {return file.getName().endsWith(extension);}
        };
        File dir = null==startDir?Environment.getExternalStorageDirectory():startDir;
        if(dir.isDirectory())
            scanRecursive(dir, filter, listener);
    }
    private static boolean scanRecursive(File dir, FileFilter filter, FileFoundListener listener) {
        File[] files = safeListFiles(dir);
        if(null==files)
            Log.e(TAG, "Can't read files for "+dir.getAbsolutePath());
        else
            for(File file: files)
                if(file.isDirectory()&&scanRecursive(file, filter, listener) ||
                        filter.accept(file)&&listener.onFileFound(file))
                    return true;
        return false;
    }
    private static File[] safeListFiles(File dir) {
        try {
            return dir.listFiles();
        } catch(SecurityException e) {
            return null;
        }
    }

    //------------------------------FILE I/O------------------------------
    static byte[] loadRaw(Context context, int id) {
        return readAll(context.getResources().openRawResource(id));
    }
    static byte[] loadInternal(Context context, String filename) {
        InputStream is;
        try {
            is = context.openFileInput(filename);
        } catch(FileNotFoundException e) {
            is = null;
        }
        return readAll(is);
    }
    static byte[] loadExternal(String filepath) {return loadExternal(new File(filepath));}
    static byte[] loadExternal(File file) {
        InputStream is;
        try {
            is = new FileInputStream(file);
        } catch(FileNotFoundException e) {
            is = null;
        }
        return readAll(is);
    }
    static boolean saveInternal(Context context, String filename, byte[] data) {
        FileOutputStream fos;
        try {
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        } catch(IOException e) {
            fos = null;
        }
        return writeAll(fos, data);
    }
    static boolean saveExternal(String filepath, boolean append, byte[] data) {return saveExternal(new File(filepath), append, data);}
    static boolean saveExternal(File file, boolean append, byte[] data) {
        if(!file.exists()&&!new File(file.getParent()).mkdirs())
            Log.e(TAG, "Could not create directories for "+file.getAbsolutePath());
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file, append);
        } catch(FileNotFoundException e) {
            fos = null;
        }
        return writeAll(fos, data);
    }

    private static byte[] readAll(InputStream is) {
        byte[] data = new byte[0];
        if(null!=is)
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[256];
                int len;
                while (-1<(len=is.read(buf, 0, buf.length)))
                    bos.write(buf, 0, len);
                data = bos.toByteArray();
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                close(is);
            }
        return data;
    }
    private static boolean writeAll(FileOutputStream fos, byte[] data) {
        boolean success = false;
        if(null!=fos)
            try {
                fos.write(data);
                success = true;
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                close(fos);
            }
        return success;
    }
    private static void close(Closeable stream) {
        try {
            stream.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    //------------------------------RAW DATA CONVERSION------------------------------
    static byte[] toBytes(int val) {
        return new byte[]{
                (byte)((val>>24)&0xFF),
                (byte)((val>>16)&0xFF),
                (byte)((val>> 8)&0xFF),
                (byte)(val&0xFF)
        };
    }
    static byte[] toBytes(String val) {//TODO when more than 255 bytes
        byte[] src = val.getBytes(Charset.forName("UTF-8"));//excludes 0-terminator
        int len = Math.min(0xFF, src.length);
        byte[] dst = new byte[(len+1+0x03)&~0x03];//32-bit align pascal string
        dst[0] = (byte)len;
        System.arraycopy(src, 0, dst, 1, len);
        return dst;
    }
    static byte[] toBytes(byte[] data) {
        byte[] dst = new byte[(data.length+4+0x03)&~0x03];//32-bit align data+32-bit length
        byte[] len = toBytes(data.length);
        System.arraycopy(len, 0, dst, 0, len.length);//first 32-bit are length
        System.arraycopy(data, 0, dst, len.length, data.length);//0-padded data
        return dst;
    }

    static int nextInt(InputStream is) {return nextInt(is, -1);}
    static int nextInt(InputStream is, int fallback) {
        byte[] be32 = new byte[4];
        int val = fallback;
        try {
            if(be32.length==is.read(be32))
                val = (be32[0]<<24)|(toUInt(be32[1])<<16)|(toUInt(be32[2])<< 8)|toUInt(be32[3]);
        } catch(Exception e) {
            System.err.println("Could not read next int32");
        }
        return val;
    }
    static String nextString(InputStream is) {
        int len = -1;
        String str = "";
        try {
            len = is.read();
        } catch(IOException e) {
            e.printStackTrace();
        }
        if(0<len) {
            byte[] buf = new byte[(len+1+0x03)&~0x03];//32-bit align pascal string
            try {
                if(len<=is.read(buf, 1, buf.length-1))
                    str = new String(buf, 1, len, Charset.forName("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return str;
    }
    static byte[] nextBytes(InputStream is, int len) {
        byte[] data = new byte[0];
        if(0<len)
            try {
                byte[] buf = new byte[len];
                if(len==is.read(buf, 0, buf.length))
                    data = buf;
            } catch (IOException e) {
                e.printStackTrace();
            }
        return data;
    }
    static byte[] nextChunk(InputStream is) {
        int len = nextInt(is);
        byte[] data = new byte[0];
        if(0<len) {
            try {
                byte[] dst = new byte[(len+0x03)&~0x03];//32-bit align data+32-bit length
                if(len<=is.read(dst, 0, dst.length)) {
                    data = new byte[len];
                    System.arraycopy(dst, 0, data, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    private static int toUInt(byte b) {return 0xFF&(int)b;}
}
