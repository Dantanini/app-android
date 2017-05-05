package im.where.whereim.dialogs;

import android.content.Context;
import android.content.Intent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import im.where.whereim.Config;
import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogShareLocation {
    public DialogShareLocation(Context context, String title, double lat, double lng){
        try {
            String t = URLEncoder.encode(title!=null && !title.trim().isEmpty() ? title.trim() : "", "utf-8");
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.action_share));
            i.putExtra(Intent.EXTRA_TEXT, title+"\n"+String.format(Config.WHERE_IM_URL, "here/"+lat+"/"+lng+(t.isEmpty()?"":"/"+t)));
            context.startActivity(Intent.createChooser(i, context.getString(R.string.action_share)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
