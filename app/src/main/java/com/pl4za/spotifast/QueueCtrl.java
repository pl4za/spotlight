package com.pl4za.spotifast;

import android.util.Log;

import com.pl4za.interfaces.QueueOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class QueueCtrl implements QueueOptions {

    private Queue queue;
    private PlayCtrl playCtrl = PlayCtrl.getInstance();
    private ViewCtrl viewCtrl = ViewCtrl.getInstance();

    private static final QueueCtrl INSTANCE = new QueueCtrl();

    private QueueCtrl() {
        queue = new Queue();
    }

    public static QueueCtrl getInstance() {
        return INSTANCE;
    }

    @Override
    public void addTrack(Track track) {
        boolean queueExists = hasTracks();
        if (playCtrl.isActive()) {
            queue.addToQueue(track);
            playCtrl.addToQueue(track.getTrackURI());
            setQueueChanged(true);
            viewCtrl.showSnackBar("Queued: " + track.getTrack());
            if (!queueExists) {
                playCtrl.play(track.getTrackURI());
            }
        } else {
            viewCtrl.showSnackBar("Player not initialized");
        }
    }

    @Override
    public void addTrackList(List<Track> tracklist, int listStart) {
        if (playCtrl.isActive()) {
            queue.addToQueue(tracklist, listStart);
            //TODO: fix player queue limit.
            playCtrl.addToQueue(getTrackURIList(tracklist).subList(0, 250), listStart);
            viewCtrl.showSnackBar("Playing");
        } else {
            viewCtrl.showSnackBar("Initializing player");
            playCtrl.initializePlayer();
        }
    }

    @Override
    public Track getTrack(int position) {
        return null;
    }

    @Override
    public void removeFromList(int position) {
        viewCtrl.showSnackBar("Removed: " + queue.getQueue().get(position).getTrack());
        int oldPos = queue.getQueuePosition();
        if (position == oldPos) {
            queue.setTrackNumberUpdate(0);
            if (!queue.hasNext()) {
                playCtrl.prevTrack();
            }
            playCtrl.nextTrack();
        } else {
            queue.setTrackNumberUpdate(1);
        }
        queue.removeFromQueue(position);
        if (queue.isEmpty()) {
            playCtrl.resumePause();
        }
    }

    @Override
    public int getTrackNumberUpdate() {
        return queue.getTrackNumberUpdate();
    }

    @Override
    public void clear() {
        queue.clearQueue();
        playCtrl.clearQueue();
    }

    @Override
    public boolean hasTracks() {
        return !queue.isEmpty();
    }

    @Override
    public void setTrackList(List<Track> newTrackList) {

    }

    @Override
    public boolean hasNext() {
        return queue.hasNext();
    }

    @Override
    public boolean hasPrevious() {
        return queue.hasPrevious();
    }

    @Override
    public void updateTrackNumberAndPlayingTrack(String trackURI) {
        queue.updateTrackNumberAndPlayingTrack(trackURI);
    }

    @Override
    public List<Track> getTrackList() {
        return queue.getQueue();
    }

    @Override
    public Track getCurrentTrack() {
        return queue.getCurrentTrack();
    }

    @Override
    public int getQueuePosition() {
        return queue.getQueuePosition();
    }

    @Override
    public boolean isQueueChanged() {
        return queue.queueChanged();
    }

    @Override
    public void setQueueChanged(boolean changed) {
        queue.setQueueChanged(changed);
    }

    public List<String> getTrackURIList(List<Track> queueToList) {
        Log.i("Queue", "Receiving TRACK_LIST");
        List<String> uriQueue = new ArrayList<>();
        for (Track t : queueToList) {
            uriQueue.add(t.getTrackURI());
        }
        return uriQueue;
    }

}
