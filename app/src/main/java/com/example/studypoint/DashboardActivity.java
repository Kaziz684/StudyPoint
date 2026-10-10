
package com.example.studypoint;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DashboardActivity extends AppCompatActivity {

    private TextView dashboardName, dashboardEmail;
    private Button logoutButton;
    private SharedPreferences sharedPreferences;

    private static final String BASE_URL = "http://10.0.2.2:5000"; // For emulator
    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        dashboardName = findViewById(R.id.dashboardName);
        dashboardEmail = findViewById(R.id.dashboardEmail);
        logoutButton = findViewById(R.id.logoutButton);

        sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);

        // Load user data from SharedPreferences
        String name = sharedPreferences.getString("user_name", "N/A");
        String email = sharedPreferences.getString("user_email", "N/A");

        dashboardName.setText("Name: " + name);
        dashboardEmail.setText("Email: " + email);

        // Fetch profile data from backend (optional, for demonstration of protected route)
        fetchProfileData();

        logoutButton.setOnClickListener(v -> {
            // Clear SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Redirect to LoginActivity
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            Toast.makeText(DashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchProfileData() {
        String token = sharedPreferences.getString("token", null);
        if (token == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/profile")
                .header("x-access-token", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, "Failed to fetch profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        if (response.isSuccessful()) {
                            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                            String name = jsonResponse.get("name").getAsString();
                            String email = jsonResponse.get("email").getAsString();
                            dashboardName.setText("Name: " + name);
                            dashboardEmail.setText("Email: " + email);
                        } else {
                            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                            String message = jsonResponse.get("message").getAsString();
                            Toast.makeText(DashboardActivity.this, "Profile fetch failed: " + message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(DashboardActivity.this, "Error parsing profile response", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
