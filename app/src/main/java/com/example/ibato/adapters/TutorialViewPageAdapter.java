package com.example.ibato.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.example.ibato.R;
import com.example.ibato.tutorial.ScreenItem;

import java.util.List;

public class TutorialViewPageAdapter extends PagerAdapter {

    Context context;
    List<ScreenItem> mListScreen;

    public TutorialViewPageAdapter(Context context, List<ScreenItem> mListScreen) {
        this.context = context;
        this.mListScreen = mListScreen;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.tutorial_screen, null);

        ImageView imgSlide = view.findViewById(R.id.intro_img);
        TextView mtitle = view.findViewById(R.id.intro_title);
        TextView mDesc = view.findViewById(R.id.intro_desc);

        mtitle.setText(mListScreen.get(position).getTitle());
        mDesc.setText(mListScreen.get(position).getDescription());
        imgSlide.setImageResource(mListScreen.get(position).getScreenImg());

        container.addView(view);

        return view;
    }

    @Override
    public int getCount() {
        return mListScreen.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}
