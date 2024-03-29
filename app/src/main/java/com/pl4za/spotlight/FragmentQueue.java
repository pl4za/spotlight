package com.pl4za.spotlight;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.melnykov.fab.FloatingActionButton;
import com.pl4za.help.TracksAdapter;
import com.pl4za.help.Params;
import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.volley.AppController;

import java.util.List;

public class FragmentQueue extends Fragment implements FragmentOptions {
    private static final String TAG = "FragmentQueue";
    private static final int SCROLL_STATE_IDLE = 0;
    private static TracksAdapter mAdapter;
    private static RecyclerView recyclerView;
    private static FloatingActionButton fabPlay;
    private static FloatingActionButton fabTracks;
    // interfaces
    private final QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private final ViewCtrl viewCtrl = ViewCtrl.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_queue, container, false);
        viewCtrl.setActivityView((ActivityOptions) getActivity());
        viewCtrl.addFragmentView(this);
        recyclerView = (RecyclerView) view.findViewById(R.id.rvTracks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        fabPlay = (FloatingActionButton) view.findViewById(R.id.fabPlay);
        fabTracks = (FloatingActionButton) view.findViewById(R.id.fabTracks);
        // Custom class listener for FAB
        FabClickListener fcl = new FabClickListener();
        fabPlay.setOnClickListener(fcl);
        fabTracks.setOnClickListener(fcl);
        recyclerView.addOnScrollListener(new ListViewScrollListener());
        mAdapter = new TracksAdapter(queueCtrl.getTrackList());
        mAdapter.setSwipeListener(this);
        mAdapter.setSwipeDirection("left");
        recyclerView.setAdapter(mAdapter);
        if (viewCtrl.isLandscape()) {
            fabTracks.setVisibility(View.INVISIBLE);
        } else {
            fabTracks.setVisibility(View.VISIBLE);
        }
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppController.getInstance().cancelPendingRequests(Params.TAG_getSelectedPlaylistTracks);
        recyclerView.removeAllViews();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppController.getInstance().cancelPendingRequests(Params.TAG_getSelectedPlaylistTracks);
        //recyclerView.removeAllViews();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            fabTracks.setVisibility(View.INVISIBLE);
        } else {
            fabTracks.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updateView() {
        mAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(queueCtrl.getQueuePosition());
    }

    @Override
    public void hideFab(boolean hide) {
        if (hide) {
            fabPlay.hide(true);
            fabTracks.hide(true);
        } else {
            fabPlay.show(true);
            fabTracks.show(true);
        }
    }

    @Override
    public void updateFilter(String query) {
    }

    @Override
    public void setList(List<Track> list) {

    }

    @Override
    public void onSwipe(int position) {
        queueCtrl.removeFromList(position);
        viewCtrl.updateActionBar(1);
        mAdapter.notifyItemRemoved(position);
        mAdapter.notifyItemRangeChanged(position, queueCtrl.getTrackList().size());
        //mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDoubleClick(int position, View view) {
        queueCtrl.addTrackList(queueCtrl.getTrackList(), position);
    }

    @Override
    public void loadTracks(String url) {
        //Not implemented
    }

    private boolean listIsAtTop() {
        return recyclerView.getChildCount() == 0 || recyclerView.getChildAt(0).getTop() == 0;
    }

    private class FabClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.fabTracks) {
                viewCtrl.setViewPagerPosition(0);
                //viewCtrl.updateActionBar(0);
            } else if (v.getId() == R.id.fabPlay) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new FragmentPlayer(), "FragmentPlayer")
                        .addToBackStack("player")
                        .commit();
                viewCtrl.updateActionBar(2);
            }
        }
    }

    private class ListViewScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == SCROLL_STATE_IDLE) {
                hideFab(false);

            } else {
                if (!listIsAtTop()) {
                    hideFab(true);
                }
            }
        }
    }

}