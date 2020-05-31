package com.example.ibato.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.ibato.home.DetailActivity;
import com.example.ibato.home.HomeActivity;
import com.example.ibato.R;
import com.example.ibato.interfaces.IHomeActivity;
import com.example.ibato.models.Model;

import java.util.List;

import static com.example.ibato.Utils.Utils.loadImage;

public class Adapter extends BaseAdapter {

    private List<Model> models;
    private LayoutInflater layoutInflater;
    private Context context;
    private HomeActivity fragment;

    public Adapter(List<Model> models, Context context, HomeActivity fragment) {
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
        TextView mTitle, mTitleSub, mDescr, mDate;
        ImageView mStatus, mImage;
        ProgressBar progressBar;

        /* Initialize components */
        mTitle = view.findViewById(R.id.title_text);
        mTitleSub = view.findViewById(R.id.title_text_sub);
        mDescr = view.findViewById(R.id.descr_text);
        mDate = view.findViewById(R.id.date_text);
        mStatus = view.findViewById(R.id.status_img);
        mImage = view.findViewById(R.id.card_background);
        progressBar = view.findViewById(R.id.loading);

        /* Populate data on layout */
        mTitle.setText(models.get(position).getTitle());
        mDescr.setText(models.get(position).getDesc());
        mDate.setText(models.get(position).getDate());

        String subText;
        int statusIcon;
        if (models.get(position).getIsEdible().equals("Can")) {
            subText = "(Can Consume)";
            statusIcon = R.drawable.ic_check_black_24dp;
        } else if (models.get(position).getIsEdible().equals("Cannot")) {
            if (models.get(position).getTitle().equals("Unknown")) {
                subText = "";
                statusIcon = R.drawable.ic_help_outline_black_24dp;
            }
            else {
                subText = "(Can't Consume)";
                statusIcon = R.drawable.ic_close_black_24dp;
            }
        } else {
            subText = "(Can't Consume)";
            statusIcon = R.drawable.ic_warning_black_24dp;
        }

        mTitleSub.setText(subText);


        //Loading image using Picasso
        mStatus.setImageResource(statusIcon);
        loadImage(context, models.get(position).getImage(), progressBar, mImage);


        /* Click to expand event */
        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("title", models.get(position).getTitle());
            intent.putExtra("title_sub", subText);
            intent.putExtra("descr", models.get(position).getDesc());
            intent.putExtra("date", models.get(position).getDate());
            intent.putExtra("image", models.get(position).getImage());
            intent.putExtra("key", models.get(position).getKey());
            intent.putExtra("isEdible", models.get(position).getIsEdible());
            intent.putExtra("icon", statusIcon);
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