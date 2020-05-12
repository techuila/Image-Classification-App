package com.example.ibato.adapters;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.ibato.R;
import com.example.ibato.models.Feedback;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FeedbackAdapter extends BaseAdapter {

    private List<Feedback> feedbacks;
    private LayoutInflater layoutInflater;
    private Context context;

    public FeedbackAdapter(List<Feedback> feedbacks, Context context) {
        this.feedbacks = feedbacks;
        this.context = context;
    }

    public float getTotalRating() {
        double totalRating = 0.0;

        for (int x = 0; x < feedbacks.size(); x++) {
            totalRating += feedbacks.get(x).getUserRating();
        }
        double s = totalRating / feedbacks.size();
        System.out.println(s);
        totalRating = Math.round( s * 10.0) / 10.0;

        return (float) totalRating;
    }

    @Override
    public int getCount() {
        return feedbacks.size();
    }

    @Override
    public Object getItem(int position) {
        return feedbacks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if(view == null){
            LayoutInflater layoutInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.custom_listview, null);
        }
//        layoutInflater = LayoutInflater.from(context);
//        view = layoutInflater.inflate(R.layout.custom_listview, parent, false);

        /* Declare components */
        CircleImageView mProfileImage;
        TextView mUsername, mFeedback, mDate;
        RatingBar mUserRating;

        /* Initialize components */
        mProfileImage = view.findViewById(R.id.profile_image);
        mUsername = view.findViewById(R.id.username_text);
        mFeedback = view.findViewById(R.id.feedback_text);
        mDate = view.findViewById(R.id.date_text);
        mUserRating = view.findViewById(R.id.user_rating);

        /* Populate data on layout */
        if (!feedbacks.get(position).getImage().equals("") && feedbacks.get(position).getImage() != null)
            Glide.with(context).load(feedbacks.get(position).getImage()).into(mProfileImage);
        else
            mProfileImage.setImageDrawable(context.getResources().getDrawable(R.drawable.default_photo));


        mUsername.setText(feedbacks.get(position).getUsername());
        mFeedback.setText(feedbacks.get(position).getFeedback());
        mDate.setText(feedbacks.get(position).getDate());
        mUserRating.setRating(feedbacks.get(position).getUserRating());

        return view;
    }
}