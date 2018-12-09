package com.benzino.facebookphotos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private Button btnChangePassword, btnRemoveUser,
            changePassword, remove, signOut;
    private TextView email;

    private EditText oldEmail, password, newPassword;
    private ProgressBar progressBar;
    private GridView gridView;
    private FirebaseAuth auth;

    RequestQueue requestQueue;


    private ArrayList<String> names;
    private ArrayList<String> images;

    private CallbackManager mCallbackManager;


    private MyAdapter myAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //get firebase auth instance
        auth = FirebaseAuth.getInstance();

        mCallbackManager = CallbackManager.Factory.create();

        FacebookSdk.sdkInitialize(this.getApplicationContext());

        names = new ArrayList<String>();
        images = new ArrayList<String>();


        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {


                progressBar.setVisibility(View.VISIBLE);


                requestQueue = Volley.newRequestQueue(getApplicationContext());

                String URL = "https://graph.facebook.com/"+ loginResult.getAccessToken().getUserId() +"/albums?fields=name,picture&access_token="+loginResult.getAccessToken().getToken() ;

                Log.e("URL", URL);
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        Log.e("anas_response", response.toString());

                        try {
                            JSONArray data = response.getJSONArray("data");

                            for(int i = 0; i< data.length(); i++){
                                JSONObject object = data.getJSONObject(i);
                                String name = object.getString("name");
                                String image = object.getJSONObject("picture").getJSONObject("data").getString("url");

                                names.add(name);
                                images.add(image);

                            }

                            gridView.setAdapter(new MyAdapter(getApplicationContext(), images, names));
                            progressBar.setVisibility(View.GONE);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

                requestQueue.add(jsonObjectRequest);


            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });

        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "user_photos","email","user_birthday", "user_friends"));





        //get current user
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //setDataToView(user);

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        signOut = findViewById(R.id.sign_out);
        gridView = findViewById(R.id.gridview);




        progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }



        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

    }

//    @SuppressLint("SetTextI18n")
//    private void setDataToView(FirebaseUser user) {
//
//        email.setText("User Email: " + user.getEmail());
//
//
//    }

    // this listener will be called when there is change in firebase user session
    FirebaseAuth.AuthStateListener authListener = new FirebaseAuth.AuthStateListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                // user auth state is changed - user is null
                // launch login activity
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            } else {


            }
        }


    };

    //sign out method
    public void signOut() {
        auth.signOut();


        // this listener will be called when there is change in firebase user session
        FirebaseAuth.AuthStateListener authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };
    }



    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }





    /**
     * Created on 8/12/18.
     *
     * @author Anas
     */
    public class MyAdapter extends BaseAdapter {


        //Context
        private Context context;

        //Array List that would contain the urls and the titles for the images
        private ArrayList<String> images;
        private ArrayList<String> names;

        public MyAdapter(Context context, ArrayList<String> images, ArrayList<String> names){
            //Getting all the values
            this.context = context;
            this.images = images;
            this.names = names;
        }

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public Object getItem(int position) {
            return images.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Creating a linear layout
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            convertView = LayoutInflater.from(context).inflate(R.layout.gridview_item, parent, false);



            //Creating imageView
            ImageView imageView = convertView.findViewById(R.id.gridview_image);
            TextView textView = convertView.findViewById(R.id.gridview_text);

            imageView.setImageResource(R.mipmap.ic_launcher);

            Picasso.get().load(images.get(position)).into(imageView);



            //Creating a textview to show the title
            //TextView textView = new TextView(context);
            textView.setText(names.get(position));

            //Scaling the imageview
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            //imageView.setLayoutParams(new GridView.LayoutParams(200,200));

            //Adding views to the layout
            //linearLayout.addView(textView);
            //linearLayout.addView(imageView);

            //Returnint the layout
            return convertView;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        return;
    }


}
