package ca.uwaterloo.magic.goodhikes;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class LoginActivity extends AppCompatActivity {

    EditText user_email;
    EditText user_password;
    Button btn_login;
    TextView link_register;
    TextView link_map;
    ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
        }

        /** define variables **/
        user_email = (EditText)findViewById(R.id.user_email);
        user_password = (EditText)findViewById(R.id.user_password);
        btn_login = (Button) findViewById(R.id.btn_login);
        link_register = (TextView) findViewById(R.id.link_register);
        link_map = (TextView) findViewById(R.id.link_map);
        pDialog = new ProgressDialog(this);


        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = user_email.getText().toString();
                String password = user_password.getText().toString();
                if (!email.isEmpty() && !password.isEmpty()) {
                    try {
                        String input = "email=";
                        input += URLEncoder.encode(email, "UTF-8");
                        input += "&password=";
                        input += URLEncoder.encode(password, "UTF-8");
                        Log.d("Input for AsyncTask", input);
                        new checkLogin().execute(input);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Please fill in all credentials!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        link_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                finish();
            }
        });

        link_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                finish();
            }
        });

    }

    /** Verify user login details **/
    private class checkLogin extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... params) {
            Boolean r = false;
            try {
                // Open Connection
                URL url = new URL("http://chelseahu.comlu.com/goodhikes_php/Login.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // Send request
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(params[0]);
                writer.flush();
                writer.close();
                os.close();

                conn.connect();
                // Receive response as inputStream
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                // Convert inputstream to JSON
                String response = reader.readLine();
                Log.d("JSON Response", response);
                JSONObject json = new JSONObject(response);
                String error = json.getString("error");


                if (error == "false") {
                     r = true;
                } else { r = false; }
            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }
            return r;
        }

        protected void onPostExecute(Boolean valid) {
            if (valid == true) {
                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Incorrect email or password", Toast.LENGTH_SHORT).show();
            }
        }


    };

}