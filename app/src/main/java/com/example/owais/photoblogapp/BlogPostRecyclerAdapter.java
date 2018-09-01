package com.example.owais.photoblogapp;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class BlogPostRecyclerAdapter extends RecyclerView.Adapter<BlogPostRecyclerAdapter.ViewHolder> {

    public List<BlogPost> blog_list;

    // CONSTRUCTOR - recieve the list of posts from home activity
    public BlogPostRecyclerAdapter(List<BlogPost> blog_list) {
        this.blog_list = blog_list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String desc_data = blog_list.get(position).getDesc();
        holder.setDescText(desc_data);

    }

    @Override
    public int getItemCount() {
        return blog_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;

        private TextView descView;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setDescText(String descText) {
            descView = mView.findViewById(R.id.blog_desc);
            descView.setText(descText);
        }
    }

}
