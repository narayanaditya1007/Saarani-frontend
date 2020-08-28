package com.example.calendarapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.ramotion.foldingcell.FoldingCell;
import com.ramotion.foldingcell.views.FoldingCellView;
import com.sackcentury.shinebuttonlib.ShineButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.example.calendarapp.R.drawable.abc_btn_check_to_on_mtrl_000;
import static com.example.calendarapp.R.drawable.star_img;
import static com.example.calendarapp.R.drawable.star_yellow;
import static com.example.calendarapp.R.drawable.tick;
import static com.example.calendarapp.R.drawable.tick_yellow;

interface ClickListener {

    void onPositionClicked(int position);

    void onLongClicked(int position);
}

public class AdaptorActivity extends RecyclerView.Adapter<AdaptorActivity.ViewHolder> {
    private List<ListItems> listItems,listUpdated;
    private final ClickListener listener;
    private Context context;
    private Activity activity;
    private String eventId,marker,backFragment;
    private HashMap<Integer, String> map= new HashMap<Integer, String>();
    private HashMap<Integer, String> mapMarker= new HashMap<Integer, String>();
    DatabaseHandler databaseHandler;
    SharedPreferences prefs ;


    AdaptorActivity(List<ListItems> listItems, ClickListener listener, Context context,String backFragment,Activity activity) {
        this.listItems = listItems;
        this.listener = listener;
        this.context = context;
        this.backFragment=backFragment;
        this.activity=activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.foldable_single_event, parent, false);
        prefs=context.getSharedPreferences("user", MODE_PRIVATE);
        databaseHandler=new DatabaseHandler(context);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ListItems listItem = listItems.get(position);
        String name=listItem.getName();
        if(listItem.getName().length()>16){
            name=name.substring(0,16)+"...";
        }
        holder.name.setText(name);
        holder.desc.setText(listItem.getDesc());
        holder.byName.setText(listItem.getByName());
        holder.venue.setText(listItem.getVenue());
        holder.date.setText(listItem.getDate());
        holder.time.setText(listItem.getTime());
        eventId=listItem.getEventId();
        holder.int_count.setText(String.valueOf(listItem.getInterested()));
        holder.going_count.setText(String.valueOf(listItem.getGoing()));
        if(listItem.getState().equals("completed")){
            holder.set.setText("COMPLETED");
            holder.set.setBackgroundResource(R.drawable.completed_background);
        }
        else if(listItem.getState().equals("cancelled")){
            holder.set.setText("CANCELLED");
            holder.set.setBackgroundResource(R.drawable.cancelled_background);
        }
        map.put(position,eventId);
        marker=listItem.getMarker();
        mapMarker.put(position,listItem.getMarker());
        Log.d("mark",listItem.getMarker());
        if(listItem.getMarker().equals("interested")){
            holder.star.setChecked(true);
            holder.star.setTag(star_yellow);
        }
        else if(listItem.getMarker().equals("going")){
            holder.going.setChecked(true);
            holder.going.setTag(tick_yellow);
            Log.d("set","true");
        }
        else{
            holder.star.setTag(star_img);
            holder.going.setTag(tick);
        }
    }
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }
    public void addMarker(int position, final String mark) throws JSONException {
        FirebaseAuth mAuth= FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        databaseHandler.updateMarker(listItems.get(position),mark);
        Log.d("updated",mark);
        Log.d("up",listItems.get(position).getMarker());
        marker=mark;
        mapMarker.put(position,mark);
        listItems.get(position).setMarker(mark);
        //listItems.get(position).set
        final String url="https://socupdate.herokuapp.com/events/"+listItems.get(position).getEventId()+"/mark";
        if(user!=null){
            user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                @Override
                public void onComplete(@NonNull Task<GetTokenResult> task) {
                    if(task.getResult().getToken()!=null){
                        HashMap<String,String> mapToken=new HashMap<String, String>();
                        mapToken.put("token",task.getResult().getToken());
                        mapToken.put("mark",mark);
                        Log.d("deleteToken", String.valueOf(new JSONObject(mapToken)));
                        final RequestQueue requstQueue = Volley.newRequestQueue(context);
                        JsonObjectRequest jsonobj = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(mapToken),
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        Log.d("respone",String.valueOf(response));
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d("error",error.toString());
                                    }
                                }
                        );
                        requstQueue.add(jsonobj);
                    }
                }
            });
        }
    }

    public void startIntent(int position){
        //Toast.makeText(context,listItems.get(position).getInterested()+" " +listItems.get(position).getGoing(),Toast.LENGTH_LONG).show();
        Intent intent =new Intent(context,EventActivity.class);
        ListItems items = listItems.get(position);
        intent.putExtra("name",items.getName());
        intent.putExtra("byName",items.getByName());
        intent.putExtra("desc",items.getDesc());
        intent.putExtra("time",items.getTime());
        intent.putExtra("venue",items.getVenue());
        intent.putExtra("date",items.getDate());
        intent.putExtra("marker",items.getMarker());
        intent.putExtra("eventId",items.getEventId());
        intent.putExtra("type","event");
        intent.putExtra("screen",backFragment);
        intent.putStringArrayListExtra("attachments", items.getArrayList());
        intent.putStringArrayListExtra("attachments_name",items.getNameList());
        context.startActivity(intent);
        //activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
    public void deleteRequest(int position) throws JSONException {
        FirebaseAuth mAuth= FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        databaseHandler.updateMarker(listItems.get(position),"none");
        marker="none";
        listItems.get(position).setMarker("none");
        mapMarker.put(position,"none");
        final String url="https://socupdate.herokuapp.com/events/"+listItems.get(position).getEventId()+"/mark/delete";
        if(user!=null){
            user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                @Override
                public void onComplete(@NonNull Task<GetTokenResult> task) {
                    if(task.getResult().getToken()!=null){
                        HashMap<String,String> mapToken=new HashMap<String, String>();
                        mapToken.put("token",task.getResult().getToken());
                        Log.d("deleteToken", String.valueOf(new JSONObject(mapToken)));
                        final RequestQueue requstQueue = Volley.newRequestQueue(context);
                        JsonObjectRequest jsonobj = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(mapToken),
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        Log.d("respone",String.valueOf(response));
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d("error",error.toString());
                                    }
                                }
                        );
                        requstQueue.add(jsonobj);
                    }
                }
            });
        }
    }
    @Override
    public int getItemCount() {
        return listItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        TextView name;
        TextView desc;
        TextView byName;
        TextView venue;
        TextView date;
        TextView time;
        ShineButton star;
        ShineButton going;
        TextView interested;
        TextView markAsGoing;
        TextView set;
        TextView int_count;
        TextView going_count;
        FoldingCell foldingCell;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.eventName);
            desc = itemView.findViewById(R.id.desc);
            byName=itemView.findViewById(R.id.eventBy);
            date = itemView.findViewById(R.id.date);
            time = itemView.findViewById(R.id.time);
            venue = itemView.findViewById(R.id.venue);
            star=itemView.findViewById(R.id.mark);
            set=itemView.findViewById(R.id.set);
            going=itemView.findViewById(R.id.going);
            interested=itemView.findViewById(R.id.text_interested);
            markAsGoing=itemView.findViewById(R.id.text_going);
            foldingCell=itemView.findViewById(R.id.folding_cell);
            int_count=itemView.findViewById(R.id.interested_count);
            going_count=itemView.findViewById(R.id.going_count);
                interested.setOnClickListener(this);
                markAsGoing.setOnClickListener(this);
                star.setOnClickListener(this);
                going.setOnClickListener(this);
            itemView.setClickable(true);
            itemView.setOnClickListener(this);
        }
//        public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String eventId = intent.getStringExtra("eventId");
//                String markedAs = intent.getStringExtra("markedAs");
////            Toast.makeText(getContext(),ItemName +" "+qty ,Toast.LENGTH_SHORT).show();
//
//            }
//        };
//        public void sendBroadcast(String msg,int position){
//            Intent intent = new Intent("custom-message");
//            intent.putExtra("eventId",map.get(position));
//            intent.putExtra("markedAs",msg);
//            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//        }
        @Override
        public void onClick(View v) {
            if(listItems.get(getAdapterPosition()).getState().equals("upcoming") ) {
                if (v.getId() == star.getId() || v.getId() == interested.getId()) {
                    if (prefs.getString("society", "false").equals("false")) {
                        String action="none";
                        if(isOnline()) {
                            Object tag = star.getTag();
                            if (tag != null && (Integer) tag == star_yellow) {
                                action="unmarked";
                                Snackbar.make(v, "Unmarked", Snackbar.LENGTH_LONG).show();
                                star.setTag(R.drawable.star_img);
                                int_count.setText(String.valueOf(listItems.get(getAdapterPosition()).getInterested() - 1));
                                try {
                                    databaseHandler.updateCount(listItems.get(getAdapterPosition()), "-", "interested");
                                    listItems.get(getAdapterPosition()).setInterested(listItems.get(getAdapterPosition()).getInterested() - 1);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                star.setChecked(false, true);
                                try {
                                    deleteRequest(this.getAdapterPosition());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                action ="marked";
                                Snackbar.make(v, "Marked as interested", Snackbar.LENGTH_LONG).show();
                                star.setTag(star_yellow);
                                star.setChecked(true, true);
                                listItems.get(getAdapterPosition()).setInterested(listItems.get(getAdapterPosition()).getInterested() + 1);
                                int_count.setText(String.valueOf(listItems.get(getAdapterPosition()).getInterested()));
                                String x = "-";
                                if ((Integer) going.getTag() == tick_yellow) {
                                    going.setTag(tick);
                                    going.setChecked(false);
                                    listItems.get(getAdapterPosition()).setGoing(listItems.get(getAdapterPosition()).getGoing() - 1);
                                    going_count.setText(String.valueOf(listItems.get(getAdapterPosition()).getGoing()));
                                    x = "going";
                                    action="marked|going";
                                }
                                try {
                                    databaseHandler.updateCount(listItems.get(getAdapterPosition()), "interested", x);
                                    //listItems.get(getAdapterPosition()).setInterested(listItems.get(getAdapterPosition()).getInterested()+1);
                                    //listItems.get(getAdapterPosition()).setGoing(listItems.get(getAdapterPosition()).getGoing()-1);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    addMarker(this.getAdapterPosition(), "interested");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else{
                            if(star.isChecked())
                                star.setChecked(false);
                            else
                                star.setChecked(true);
                            Snackbar.make(v, "Not connected to internet! Please try again later.", Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        going.setChecked(false);
                        star.setChecked(false);
                        Snackbar.make(v, "This feature is not available for societies", Snackbar.LENGTH_LONG).show();
                    }
                } else if (v.getId() == going.getId() || v.getId() == markAsGoing.getId()) {
                    if (prefs.getString("society", "false").equals("false")) {
                        if(isOnline()) {
                            Object tag = going.getTag();
                            if (tag != null && (Integer) tag == tick_yellow) {
                                Snackbar.make(v, "Unmarked", Snackbar.LENGTH_LONG).show();
                                going.setTag(tick);
                                going.setChecked(false, true);
                                listItems.get(getAdapterPosition()).setGoing(listItems.get(getAdapterPosition()).getGoing() - 1);
                                going_count.setText(String.valueOf(listItems.get(getAdapterPosition()).getGoing()));
                                try {
                                    databaseHandler.updateCount(listItems.get(getAdapterPosition()), "-", "going");
                                    //listItems.get(getAdapterPosition()).setGoing(listItems.get(getAdapterPosition()).getGoing()-1);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    deleteRequest(this.getAdapterPosition());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Snackbar.make(v, "Marked as going.", Snackbar.LENGTH_LONG).show();
                                going.setTag(tick_yellow);
                                going.setChecked(true, true);
                                String x = "-";
                                listItems.get(getAdapterPosition()).setGoing(listItems.get(getAdapterPosition()).getGoing() + 1);
                                going_count.setText(String.valueOf(listItems.get(getAdapterPosition()).getGoing()));
                                if ((Integer) star.getTag() == star_yellow) {
                                    star.setTag(R.drawable.star_img);
                                    star.setChecked(false);
                                    x = "interested";
                                    listItems.get(getAdapterPosition()).setInterested(listItems.get(getAdapterPosition()).getInterested() - 1);
                                    int_count.setText(String.valueOf(listItems.get(getAdapterPosition()).getInterested()));
                                }
                                try {
                                    databaseHandler.updateCount(listItems.get(getAdapterPosition()), "going", x);
                                    //listItems.get(getAdapterPosition()).setInterested(listItems.get(getAdapterPosition()).getInterested()-1);
                                    //listItems.get(getAdapterPosition()).setGoing(listItems.get(getAdapterPosition()).getGoing()+1);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    addMarker(this.getAdapterPosition(), "going");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            if (going.isChecked())
                                going.setChecked(false);
                            else
                                going.setChecked(true);
                            Snackbar.make(v, "Not connected to internet! Please try again later.", Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        going.setChecked(false);
                        star.setChecked(false);
                        Snackbar.make(v, "This feature is not available for societies", Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    //startIntent(this.getAdapterPosition());
                    foldingCell.toggle(false);
                }
            }
            else if(listItems.get(getAdapterPosition()).getState().equals("completed")){
                Snackbar.make(v, "This event has finished!", Snackbar.LENGTH_LONG).show();
            }
            else{
                Snackbar.make(v, "This event has been cancelled!", Snackbar.LENGTH_LONG).show();
            }
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }
}
