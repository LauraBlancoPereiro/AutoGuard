package lpro.autoguard;

import java.util.HashMap;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

public class NotificationSound {

    private static final HashMap<String, Integer> gravitySound = new HashMap<String, Integer>(){{
        put("ALTA", R.raw.sound3);
        put("MEDIA", R.raw.sound7);
        put("LEVE", R.raw.sound6);
    }};

    private MediaPlayer mp;
    private final String gravity;
    private final String message;
    private final Uri uri;

    public NotificationSound(String gravity, String message, Context context){

        this.gravity = gravity;
        this.message = message;
        uri= Uri.parse("android.resource://" + "lpro.autoguard" + "/" + gravitySound.get(gravity));
        mp = MediaPlayer.create(context, uri);

        if(gravity != "LEVE"){
            mp.setLooping(true);
        }

    }

    public Integer getSound(){
        return gravitySound.get(gravity);
    }

    public Uri getUri(){
        return uri;
    }

    public String getGravity(){
        return gravity;
    }

    public String getMessage(){
        return message;
    }

    public void startNotification(){
        mp.start();
    }

    public void stopNotification(){
        mp.stop();
        mp.prepareAsync();
    }

}