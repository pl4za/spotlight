package com.pl4za.spotlight;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.github.mrengineer13.snackbar.SnackBar;
import com.pl4za.interfaces.ServiceOptions;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.PlayConfig;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import java.util.List;

/**
 * Created by Admin on 16/02/2015.
 */
public class PlayService extends Service implements PlayerNotificationCallback, ConnectionStateCallback, ServiceOptions {

    private static final String TAG = "PlayService";
    private static final int MAX_ITEMS = 100;
    private static NotificationManager mNotificationManager;
    private static Player mPlayer;
    private static boolean SKIP_NEXT = true;
    private static boolean TRACK_END = true;
    public static boolean PLAYING = false;
    private static boolean SHUFFLE = false;
    private static boolean REPEAT = false;
    private static boolean INITIALIZING = false;

    private final IBinder mBinder = new LocalBinder();
    private RemoteViews contentView;
    private Notification notification;
    private SwitchButtonListener switchButtonListener;

    // interfaces
    private final QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private final ViewCtrl viewCtrl = ViewCtrl.getInstance();
    private final SettingsManager settings = SettingsManager.getInstance();

    @Override
    public void addToQueue(String trackUri) {
        mPlayer.queue(trackUri);
        if (mNotificationManager != null && contentView != null) {
            updateNotificationButtons();
        }
    }

    @Override
    public void play(String trackUri) {
        mPlayer.play(trackUri);
        if (mNotificationManager != null && contentView != null) {
            updateNotificationButtons();
        }
    }

    @Override
    public void addToQueue(List<String> queue, int listStart) {
        clearQueue();
        if (queue.size() == 1) {
            mPlayer.play(queue);
        } else {
            int max = MAX_ITEMS;
            int start = 0;
            if (queueCtrl.getTrackList().size() < max) {
                max = queueCtrl.getTrackList().size();
            } else {
                if (listStart - max >= 0) {
                    start = listStart - max / 2;
                    max = listStart + max / 2;
                    if (max > queueCtrl.getTrackList().size()) {
                        max = queueCtrl.getTrackList().size();
                    }
                    listStart = (MAX_ITEMS / 2);
                }
            }
            // Log.i(TAG, "queue size: "+queue.size()+" start: "+start+" max: "+max+" click: "+listStart);
            // start: 76 max: 98 click: 49
            List<String> subList = queue.subList(start, max);
            // Log.i(TAG, "subList size: "+subList.size()+" max: "+subList.size());
            mPlayer.play(PlayConfig.createFor(subList).withTrackIndex(listStart));
        }
        PLAYING = false;
        TRACK_END = false;
        SKIP_NEXT = false;
        if (mNotificationManager != null && contentView != null) {
            updateNotificationButtons();
        }
    }

    @Override
    public boolean isActive() {
        return mPlayer != null && mPlayer.isLoggedIn();
    }

    @Override
    public void onDestroy() {
        destroyPlayer();
        Log.i(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started..");
        if (flags == START_FLAG_REDELIVERY) {
            /***
             * Can be called if service is set to startForeground and is killed by the system under heavy memory pressure
             * The service had previously returned START_REDELIVER_INTENT but had been killed before calling stopSelf(int) for that Intent.
             */
            Log.i(TAG, "START_FLAG_REDELIVERY = TRUE");
            //start();
        }
        initializePlayer();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service bind..");
        return mBinder;
    }

    @Override
    public void onConnectionMessage(String arg0) {
        Log.i(TAG, arg0);
        viewCtrl.showSnackBar(arg0, SnackBar.MED_SNACK);
    }

    @Override
    public void onLoggedIn() {
        Log.i(TAG, "Logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.i(TAG, "Logged out!");
    }

    @Override
    public void onLoginFailed(Throwable arg0) {
        Log.e(TAG, arg0.getMessage());
        Toast.makeText(getApplicationContext(), arg0.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTemporaryError() {
        Log.e(TAG, "Something ocurred!");
    }

    @Override
    public void onPlaybackEvent(EventType arg0, PlayerState arg1) {
        String msg = arg0.toString();
        Log.i(TAG, "Evento: " + arg0 + " playing: " + PLAYING);
        if (msg.equals("PLAY") || msg.equals("PAUSE")) {
            //PLAYING=true;
            if (notification != null) {
                updateNotificationButtons();
            }
        }
        if (msg.equals("TRACK_END")) {
            PLAYING = false;
            if (queueCtrl.isQueueChanged() && queueCtrl.hasNext()) {
                Log.i(TAG, "Queue changed: clearing and re-ading TRACK_LIST");
                mPlayer.clearQueue();
                addToQueue(queueCtrl.getTrackURIList(queueCtrl.getTrackList()), queueCtrl.getQueuePosition() + queueCtrl.getTrackNumberUpdate());
                queueCtrl.setQueueChanged(false);
            }
            TRACK_END = true;
            viewCtrl.updateView();
        } else if (msg.equals("TRACK_END") && TRACK_END) {
            Log.i(TAG, "SKIPPING TO NEXT AUTOMATICALLY");
            TRACK_END = true;
            SKIP_NEXT = false;
            mPlayer.skipToNext();
        } else {
            if ((msg.equals("TRACK_START") && SHUFFLE)) {
                Log.i(TAG, "SKIPPING TO NEXT SHUFFLE");
            } else if (msg.equals("SKIP_NEXT") && SKIP_NEXT) {
                Log.i(TAG, "SKIPPING TO NEXT VIA PRESS");
                TRACK_END = false;
                SKIP_NEXT = false;
            } else if (msg.equals("SKIP_PREV")) {
                Log.i(TAG, "SKIPPING TO PREVIOUS");
                TRACK_END = false;
            }
        }
        if (msg.equals("TRACK_START") && queueCtrl.hasTracks()) {
            if (mNotificationManager == null) {
                startNotification();
            }
            PLAYING = true;
            TRACK_END = true;
            SKIP_NEXT = true;
            queueCtrl.updateTrackNumberAndPlayingTrack(arg1.trackUri);
            viewCtrl.updateView();
        }
        if (mNotificationManager != null) {
            updateNotificationInfo();
            updateNotificationButtons();
        }
    }

    @Override
    public void onPlaybackError(PlayerNotificationCallback.ErrorType arg0, String arg1) {
        Log.e("Playback", arg1 + " " + arg0.toString());
        if (arg0.toString().equals("TRACK_UNAVAILABLE")) {
            Toast.makeText(getApplicationContext(), "Track unavailable", Toast.LENGTH_SHORT).show();
            //nextTrack();
        } else if (arg0.toString().equals("ERROR_PLAYBACK")) {
            Toast.makeText(getApplicationContext(), "Playback error", Toast.LENGTH_SHORT).show();
        }
    }

    public void startNotification() {
        switchButtonListener = new SwitchButtonListener();
        IntentFilter iFilter = new IntentFilter(actionPlayPause);
        iFilter.addAction(actionNext);
        iFilter.addAction(actionDismiss);
        registerReceiver(switchButtonListener, iFilter);
        Intent intentAction = new Intent(this, MainActivity.class);
        PendingIntent pendingIntentAction = PendingIntent.getActivity(this, 0, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);

        contentView = new RemoteViews(getPackageName(), R.layout.playing_notification);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_action_play_over_video)
                        .setContentIntent(pendingIntentAction)
                                //.setDeleteIntent(pendingIntentDismiss)
                        .setContent(contentView);
        notification = mBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        //Play
        Intent intentPlayPause = new Intent(actionPlayPause);
        PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(this, 0, intentPlayPause, 0);
        contentView.setOnClickPendingIntent(R.id.ivPlayPause_2, pendingIntentPlayPause);
        //Next
        Intent intentNext = new Intent(actionNext);
        PendingIntent pendingIntentNext = PendingIntent.getBroadcast(this, 0, intentNext, 0);
        contentView.setOnClickPendingIntent(R.id.ivNext, pendingIntentNext);
        //Next
        Intent intentClose = new Intent(actionDismiss);
        PendingIntent pendingIntentClose = PendingIntent.getBroadcast(this, 0, intentClose, 0);
        contentView.setOnClickPendingIntent(R.id.ivClose, pendingIntentClose);
        //
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        updateNotificationInfo();
        updateNotificationButtons();
    }

    public void initializePlayer() {
        if ((!INITIALIZING && (mPlayer == null || mPlayer.isShutdown() || !mPlayer.isLoggedIn()))) {
            Config playerConfig = new Config(this, settings.getAccessToken(), CLIENT_ID);
            if (settings.getProduct().equals("premium")) {
                INITIALIZING = true;
                viewCtrl.showSnackBar("Initializing player", SnackBar.PERMANENT_SNACK);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(PlayService.this);
                        mPlayer.addPlayerNotificationCallback(PlayService.this);
                        viewCtrl.clearSnackBar();
                        viewCtrl.showSnackBar("Player ready", SnackBar.MED_SNACK);
                        INITIALIZING = false;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "Could not initialize player: " + throwable.getMessage());
                        viewCtrl.clearSnackBar();
                        viewCtrl.showSnackBar(throwable.getMessage(), SnackBar.MED_SNACK);
                    }
                });
            } else {
                viewCtrl.showSnackBar("Spotify premium required", SnackBar.LONG_SNACK);
            }
        }
    }

    public static boolean isShuffled() {
        return SHUFFLE;
    }

    public boolean isPlaying() {
        return PLAYING || queueCtrl.hasTracks();
    }

    public static boolean isRepeating() {
        return REPEAT;
    }

    public void clearQueue() {
        if (mPlayer != null && mPlayer.isInitialized()) {
            mPlayer.pause();
            mPlayer.clearQueue();
        }
        PLAYING = false;
    }

    public void resumePause() {
        if (mPlayer != null) {
            if (PLAYING) {
                mPlayer.pause();
                PLAYING = false;
            } else {
                mPlayer.resume();
                PLAYING = true;
            }
        }
    }

    public void nextTrack() {
        if (queueCtrl.hasNext()) {
            if (SHUFFLE) {
                mPlayer.skipToNext();
            } else {
                mPlayer.clearQueue();
                addToQueue(queueCtrl.getTrackURIList(queueCtrl.getTrackList()), queueCtrl.getQueuePosition() + 1);
            }
            //TODO: mPlayer.skipToNext();
        }
    }

    public void prevTrack() {
        if (SHUFFLE) {
            mPlayer.skipToPrevious();
        } else {
            if (queueCtrl.hasPrevious()) {
                addToQueue(queueCtrl.getTrackURIList(queueCtrl.getTrackList()), queueCtrl.getQueuePosition() - 1);
                //mPlayer.skipToPrevious();
            }
        }
    }

    public void shuffle() {
        if (mPlayer != null) {
            if (SHUFFLE) {
                mPlayer.setShuffle(false);
                SHUFFLE = false;
            } else {
                mPlayer.setShuffle(true);
                SHUFFLE = true;
            }
        }
    }

    public void repeat() {
        if (mPlayer != null) {
            if (REPEAT) {
                mPlayer.setRepeat(false);
                REPEAT = false;
            } else {
                mPlayer.setRepeat(true);
                REPEAT = true;
            }
        }
    }

    public void destroyPlayer() {
        if (mPlayer != null) {
            mPlayer.pause();
            cancelNotification();
            Spotify.destroyPlayer(mPlayer);
            PLAYING = false;
            stopSelf();
        }
    }

    public void cancelNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancelAll();
            mNotificationManager = null;
            notification = null;
            try {
                unregisterReceiver(switchButtonListener);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "unregister receiver error");
            }
        }
    }

    private void updateNotificationInfo() {
        Log.i(TAG, "Updating notification");
        if (!queueCtrl.hasTracks()) {
            cancelNotification();
        } else if (contentView != null) {
            //contentView.setImageViewResource(R.id.image, R.drawable.no_image);
            contentView.setTextViewText(R.id.tvTrackTitle, queueCtrl.getCurrentTrack().getTrack());
            contentView.setTextViewText(R.id.tvArtistAndAlbum, queueCtrl.getCurrentTrack().getSimpleArtist() + " - " + queueCtrl.getCurrentTrack().getAlbum());
            mNotificationManager.notify(1, notification);
        }
    }

    private void updateNotificationButtons() {
        Log.i(TAG, "Updating notification");
        if (!queueCtrl.hasTracks()) {
            cancelNotification();
        } else if (contentView != null) {
            if (queueCtrl.hasNext()) {
                contentView.setImageViewResource(R.id.ivNext, R.drawable.next_selector);
            } else {
                contentView.setImageViewResource(R.id.ivNext, R.drawable.ic_next_pressed);
            }
            if (queueCtrl.hasTracks()) {
                if (!PLAYING) {
                    contentView.setImageViewResource(R.id.ivPlayPause_2, R.drawable.play_selector);
                } else {
                    contentView.setImageViewResource(R.id.ivPlayPause_2, R.drawable.pause_selector);
                }
            } else {
                contentView.setImageViewResource(R.id.ivNext, R.drawable.ic_play_pressed);
            }
            mNotificationManager.notify(1, notification);
        }
    }

    public class LocalBinder extends Binder {
        PlayService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayService.this;
        }
    }

    public class SwitchButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, intent.getAction());
            if (queueCtrl.hasTracks()) {
                if (intent.getAction().equals(actionPlayPause)) {
                    resumePause();
                } else if (intent.getAction().equals(actionNext)) {
                    nextTrack();
                }
            }
            if (intent.getAction().equals(actionDismiss)) {
                destroyPlayer();
            }
        }

    }
}
