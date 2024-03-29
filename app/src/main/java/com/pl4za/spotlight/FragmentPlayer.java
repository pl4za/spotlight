package com.pl4za.spotlight;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.volley.AppController;

import java.util.List;

public class FragmentPlayer extends Fragment implements View.OnClickListener, FragmentOptions {

    private static final String TAG = "FragmentTracks";
    private static ImageLoader imageLoader = AppController.getInstance().getImageLoader();
    private View view;
    private ImageView ivNext;
    private ImageView ivPrevious;
    private ImageView ivPlayPause;
    private ImageView ivShuffle;
    private ImageView ivRepeat;
    private NetworkImageView ivAlbumArt;

    // Interfaces
    private final PlayCtrl playCtrl = PlayCtrl.getInstance();
    private final ViewCtrl viewCtrl = ViewCtrl.getInstance();
    private final QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private final SettingsManager settings = SettingsManager.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        viewCtrl.addFragmentView(this);
        view = inflater.inflate(R.layout.fragment_player, container, false);
        Bundle bundle = getArguments();
        if (bundle != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.findViewById(R.id.albumArt).setTransitionName(bundle.getString("TRANS_IMAGE"));
                //view.findViewById(R.id.tvTrackTitle).setTransitionName(bundle.getString("TRANS_TRACK"));
                //view.findViewById(R.id.tvAlbum).setTransitionName(bundle.getString("TRANS_ALBUM"));
                //view.findViewById(R.id.tvArtist).setTransitionName(bundle.getString("TRANS_ARTIST"));
            }
        }
        viewCtrl.updateActionBar(2);
        setButtonsListeners();
        if (queueCtrl.hasTracks()) {
            updateView();
        } else {
            ivPlayPause.setImageAlpha(30);
            ivAlbumArt.setImageAlpha(30);
            ivNext.setImageAlpha(30);
            ivPrevious.setImageAlpha(30);
            ivShuffle.setImageAlpha(30);
            ivRepeat.setImageAlpha(30);
            ivAlbumArt.setImageUrl(null, imageLoader);
            ivAlbumArt.setDefaultImageResId(R.drawable.no_image);
            ivAlbumArt.setErrorImageResId(R.drawable.no_image);
        }
        return view;
    }

    private void setButtonsListeners() {
        ivAlbumArt = (com.android.volley.toolbox.NetworkImageView) view.findViewById(R.id.albumArt);
        ivNext = (ImageView) view.findViewById(R.id.ivNext);
        ivPrevious = (ImageView) view.findViewById(R.id.ivPrevious);
        ivPlayPause = (ImageView) view.findViewById(R.id.ivPlayPause);
        ivShuffle = (ImageView) view.findViewById(R.id.ivShuffle);
        ivRepeat = (ImageView) view.findViewById(R.id.ivRepeat);
        //ImageView ivTracks = (ImageView) view.findViewById(R.id.ivTracks);
        //ImageView ivQueue = (ImageView) view.findViewById(R.id.ivQueue);
        ivAlbumArt.setOnClickListener(this);
        ivNext.setOnClickListener(this);
        ivPrevious.setOnClickListener(this);
        ivShuffle.setOnClickListener(this);
        ivRepeat.setOnClickListener(this);
        //ivQueue.setOnClickListener(this);
        //ivTracks.setOnClickListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Fragment frag = this;
        FragmentTransaction tr = getActivity().getSupportFragmentManager().beginTransaction();
        tr.detach(frag);
        tr.attach(frag);
        tr.commit();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.albumArt) {
            if (PlayService.PLAYING) {
                ivPlayPause.setImageResource(R.drawable.play_selector);
            } else {
                ivPlayPause.setImageResource(R.drawable.pause_selector);
            }
            playCtrl.resumePause();
        } else if (v.getId() == R.id.ivNext) {
            playCtrl.nextTrack();
            ivNext.setImageAlpha(30);
            ivNext.setEnabled(false);
            ivPrevious.setImageAlpha(30);
            ivPrevious.setEnabled(false);
        } else if (v.getId() == R.id.ivPrevious) {
            playCtrl.prevTrack();
            ivNext.setImageAlpha(30);
            ivNext.setEnabled(false);
            ivPrevious.setImageAlpha(30);
            ivPrevious.setEnabled(false);
        } else if (v.getId() == R.id.ivShuffle) {
            playCtrl.shuffle();
            checkRepeatAndShuffle();
        } else if (v.getId() == R.id.ivRepeat) {
            playCtrl.repeat();
            checkRepeatAndShuffle();
        }
    }

    private void insertImage() {
        if (imageLoader == null)
            imageLoader = AppController.getInstance().getImageLoader();
        ivAlbumArt.setImageUrl(queueCtrl.getCurrentTrack().getBigAlbumArt(), imageLoader);
        imageLoader.get(queueCtrl.getCurrentTrack().getBigAlbumArt(), ImageLoader.getImageListener(ivAlbumArt, R.drawable.no_image, R.drawable.no_image));
    }

    @Override
    public void updateView() {
        if (queueCtrl.hasTracks()) {
            TextView tvTrackTitle = (TextView) view.findViewById(R.id.tvTrackTitle);
            TextView tvAlbum = (TextView) view.findViewById(R.id.tvAlbum);
            TextView tvArtist = (TextView) view.findViewById(R.id.tvArtist);
            tvTrackTitle.setText(queueCtrl.getCurrentTrack().getTrack());
            tvAlbum.setText(queueCtrl.getCurrentTrack().getAlbum());
            tvArtist.setText(queueCtrl.getCurrentTrack().getSimpleArtist());
            checkRepeatAndShuffle();
            //
            if (!queueCtrl.hasTracks()) {
                ivPlayPause.setImageResource(R.drawable.play_selector);
                ivPlayPause.setImageAlpha(30);
                ivNext.setEnabled(false);
                ivPrevious.setEnabled(false);
            } else {
                ivPlayPause.setImageAlpha(255);
                if ((!queueCtrl.hasNext()) && !PlayService.isShuffled()) {
                    ivNext.setImageAlpha(30);
                    ivNext.setEnabled(false);
                } else {
                    ivNext.setImageAlpha(255);
                    ivNext.setEnabled(true);
                }
                if (!queueCtrl.hasPrevious() && !PlayService.isShuffled()) {
                    ivPrevious.setImageAlpha(30);
                    ivPrevious.setEnabled(false);
                } else {
                    ivPrevious.setImageAlpha(255);
                    ivPrevious.setEnabled(true);
                }
            }
            if (PlayService.PLAYING) {
                ivPlayPause.setImageResource(R.drawable.pause_selector);
            } else {
                ivPlayPause.setImageResource(R.drawable.play_selector);
            }
            insertImage();
        }
    }

    @Override
    public void hideFab(boolean hide) {

    }

    @Override
    public void onStart() {
        super.onStart();
        viewCtrl.updateActionBar(2);
        settings.setPlayerOnTop(true);
        updateView();
    }

    @Override
    public void onStop() {
        super.onStop();
        viewCtrl.updateActionBar(settings.getLastPagerPosition());
        settings.setPlayerOnTop(false);
    }

    private void checkRepeatAndShuffle() {
        if (PlayService.isShuffled()) {
            ivShuffle.setImageAlpha(255);
        } else {
            ivShuffle.setImageAlpha(30);
        }
        if (PlayService.isRepeating()) {
            ivRepeat.setImageAlpha(255);
        } else {
            ivRepeat.setImageAlpha(30);
        }
    }

    @Override
    public void updateFilter(String query) {
        // Not implemented
    }

    @Override
    public void setList(List<Track> list) {
        // Not implemented
    }

    @Override
    public void onSwipe(int position) {
        // Not implemented
    }

    @Override
    public void onDoubleClick(int position, View view) {
        // Not implemented
    }

    @Override
    public void loadTracks(String url) {
        // Not implemented
    }
}