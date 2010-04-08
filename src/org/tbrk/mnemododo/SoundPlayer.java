package org.tbrk.mnemododo;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import android.media.MediaPlayer;

public class SoundPlayer
{
    private String base_path = "";
    private Queue<String> to_play = new LinkedList<String>();
    private MediaPlayer mp = null;
    
    public void setBasePath(String path)
    {
        base_path = path;
    }
    
    public void queue(String[] sounds)
    {
        for (String sound : sounds) {
            File f = new File(base_path, sound);
            to_play.add(f.getAbsolutePath());
        }
        
        startPlaying();
    }
    
    public void clear()
    {
        stopPlaying();
        to_play.clear();
    }
    
    public void release()
    {
        clear();
        if (mp != null) {
            mp.release();
            mp = null;
        }
    }
    
    private void startPlaying()
    {
        if (to_play.isEmpty()) {
            return;
        }
        
        if (mp == null) {
            mp = new MediaPlayer();
            
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp)
                {
                    SoundPlayer.this.startPlaying();
                }
            });
            
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra)
                {
                    return true;
                }
            });
            
        } else {
            if (mp.isPlaying()) {
                return;
            }

            mp.reset();
        }
        
        try {
            mp.setDataSource(to_play.remove());
            mp.prepare();
            mp.start();

        } catch (IllegalArgumentException e) {
            startPlaying();
            return;
        } catch (IllegalStateException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    private void stopPlaying()
    {
        if (mp == null) {
            return;
        }
        
        try {
            mp.stop();
        } catch (IllegalStateException e) {
            return;
        }
    }
}
