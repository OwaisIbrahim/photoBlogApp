package com.example.owais.photoblogapp;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    private RecyclerView blog_list_view;         // LIST VIEW for blog-list-view
    private List<BlogPost> blog_list;           // LIST for post details in FIREBASE

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    private DocumentSnapshot lastVisible;
    private Boolean isFirstPageFirstLoad = true;

    private BlogPostRecyclerAdapter blogPostRecyclerAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        blog_list = new ArrayList<>();
        blog_list_view = view.findViewById(R.id.blog_list_view);

        firebaseAuth = FirebaseAuth.getInstance();

        blogPostRecyclerAdapter = new BlogPostRecyclerAdapter(blog_list);
        blog_list_view.setLayoutManager(new LinearLayoutManager(getActivity()));
        blog_list_view.setAdapter(blogPostRecyclerAdapter);

        if( firebaseAuth.getCurrentUser() != null ) {

            firebaseFirestore = FirebaseFirestore.getInstance();

            // loadMoreost will be fireup when it scroll reaches its limit (3+3+3+... in this case)
            blog_list_view.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    //if scroll reached to the bottom tben it will becomes true
                    Boolean reacherBottom = ! recyclerView.canScrollVertically(1);

                    if (reacherBottom) {
                        String lastDesc = lastVisible.getString("desc");
                        Toast.makeText(container.getContext(), "Reached: "+lastDesc, Toast.LENGTH_LONG).show();
                        loadMorePosts();
                    }

                }
            });

            //make a query to sort the firebase posts on timestamp in descending order with the limit of 3
            Query firstQuery = firebaseFirestore.collection("Posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(3);

            // feth the post from firebase show it to Blog Post List UI (fragment_home)
            firstQuery.addSnapshotListener(getActivity(), new EventListener<QuerySnapshot>() {
                //getActivity() added because it specifies the addSnapshotListener to run until HomeFragment activity is live
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {

                    if(queryDocumentSnapshots != null) {

                        // If the post from firebase reaches its end so we dont want this function to execute
                        if( !queryDocumentSnapshots.isEmpty() ) {

                            //to stop this function to fetch firebase data again when any post is updated 1 2 3, 4 5 6  ,,, 7 1 2, 3 4 5, 6
                            if(isFirstPageFirstLoad) {

                                // Record the last post on UI to fetch post next to it from firebase
                                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                            }
                            for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {

                                if (doc.getType() == DocumentChange.Type.ADDED) {

                                    String blogPostId = doc.getDocument().getId();
                                    BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);

                                    if(isFirstPageFirstLoad) {
                                        blog_list.add(blogPost);
                                    } else {
                                        blog_list.add(0, blogPost);
                                    }


                                    //adapter to notify that data set change
                                    blogPostRecyclerAdapter.notifyDataSetChanged();
                                }

                            }
                            isFirstPageFirstLoad = false;
                        }
                    }
                }
            });

        }




        // Inflate the layout for this fragment
        return view;
    }

    //This fuction function will fetch 3 more posts from firebase when it reaches at last of post limit in UI ()
    public void loadMorePosts() {

        Query nextQuery = firebaseFirestore.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(3);

        nextQuery.addSnapshotListener(getActivity(), new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {

                if(queryDocumentSnapshots != null) {

                    if( !queryDocumentSnapshots.isEmpty() ) {
                        lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                        for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {

                            if (doc.getType() == DocumentChange.Type.ADDED) {

                                String blogPostId = doc.getDocument().getId();
                                BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);
                                blog_list.add(blogPost);

                                //adapter to notify that data set change
                                blogPostRecyclerAdapter.notifyDataSetChanged();
                            }

                        }
                    }
                }
            }
        });
    }

}
