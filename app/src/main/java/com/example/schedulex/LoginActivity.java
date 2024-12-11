package com.example.schedulex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etUsername = findViewById(R.id.etLoginUsername);
        EditText etPassword = findViewById(R.id.etLoginPassword);
        Button btnLogin = findViewById(R.id.btnLoginSubmit);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(username, password);
            }
        });
    }

    private void loginUser(final String username, final String password) {
        new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL("http://192.168.31.234/schedulex/login_user.php");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                // Формирование JSON-объекта
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", username);
                jsonParam.put("password", password);

                // Отправка данных
                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(jsonParam.toString());
                writer.flush();
                writer.close();
                os.close();

                // Чтение ответа
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Авторизация успешна", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Ошибка авторизации", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }).start();
    }
}
