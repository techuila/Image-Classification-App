package com.example.ibato.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.ibato.history.DetailActivity;
import com.example.ibato.history.HistoryActivity;
import com.example.ibato.R;
import com.example.ibato.models.Model;

import java.io.IOException;
import java.util.List;

public class Adapter extends BaseAdapter {

    private List<Model> models;
    private LayoutInflater layoutInflater;
    private Context context;
    private HistoryActivity fragment;

    public Adapter(List<Model> models, Context context, HistoryActivity fragment) {
        this.models = models;
        this.context = context;
        this.fragment = fragment;
    }

    @Override
    public int getCount() {
        return models.size();
    }

    @Override
    public Object getItem(int position) {
        return models.get(position);
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
            view = layoutInflater.inflate(R.layout.card_item, null);
        }

        /* Declare components */
        TextView mTitle, mDescr;
        ImageView mStatus, mImage;
        ProgressBar progressBar;

        /* Initialize components */
        mTitle = view.findViewById(R.id.title_text);
        mDescr = view.findViewById(R.id.descr_text);
        mStatus = view.findViewById(R.id.status_img);
        mImage = view.findViewById(R.id.card_background);
        progressBar = view.findViewById(R.id.loading);

        /* Populate data on layout */
        mTitle.setText(models.get(position).getTitle());
        mDescr.setText(models.get(position).getDesc());
        if (models.get(position).getIsEdible()) {
            mStatus.setImageResource(R.drawable.ic_check_black_24dp);
        } else {
            mStatus.setImageResource(R.drawable.ic_close_black_24dp);
        }

        //Loading image using Picasso
        if (models.get(position).getImage() != null && !models.get(position).getImage().equals(""))
            Glide.with(context)
                    .load(models.get(position).getImage())
                    .apply(
                        new RequestOptions()
                                .error(R.drawable.not_found)
                    )
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(mImage);
        else
            mImage.setImageResource(R.drawable.no_img);


        /* Click to expand event */
        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("param", models.get(position).getTitle());
            context.startActivity(intent);
            // finish();
        });


        return view;
    }

//    @Override
//    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
//        return view.equals(object);
//    }

//    @NonNull
//    @Override
//    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
//        layoutInflater = LayoutInflater.from(context);
//        View view = layoutInflater.inflate(R.layout.item, container, false);
//
//        RelativeLayout deleteBtn;
//        ImageView imageView;
//        TextView title, desc;
//
//        imageView = view.findViewById(R.id.image);
//        title = view.findViewById(R.id.title);
//        desc = view.findViewById(R.id.desc);
//        deleteBtn = view.findViewById(R.id.deleteBtn);
//
//        //Loading image using Picasso
//        Glide.with(context).load(models.get(position).getImage()).into(imageView);
//        title.setText(models.get(position).getTitle());
//        desc.setText(models.get(position).getDesc());
//
//        /* Click to expand event */
//        view.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(context, DetailActivity.class);
//                intent.putExtra("param", models.get(position).getTitle());
//                context.startActivity(intent);
//                // finish();
//            }
//        });
//
//        /* Click to delete event */
//        deleteBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final AlertDialog builder = new AlertDialog.Builder(context)
//                    .setTitle("Confirmation")
//                    .setMessage("Are you sure you want to permanently delete this data?")
//                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            fragment.deleteData(models.get(position));
//                            dialog.dismiss();
//                        }
//                    })
//                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                        }
//                    })
//                    .create();
//
//                builder.setOnShowListener(new DialogInterface.OnShowListener() {
//                    @Override
//                    public void onShow(DialogInterface dialog) {
//                        builder.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(context.getResources().getColor(R.color.transparent));
//                        builder.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(R.color.colorPrimary));
//
//                        builder.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(context.getResources().getColor(R.color.transparent));
//                        builder.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.colorPrimary));
//                    }
//                });
//
//
//
//                builder.show();
//            }
//        });
//
//        container.addView(view, 0);
//        return view;
//    }
//
//    @Override
//    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
//        container.removeView((View)object);
//    }
}