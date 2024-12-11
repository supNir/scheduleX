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

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(RegisterActivity.this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(username, password);
            }
        });
    }

    private void registerUser(final String username, final String password) {
        new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL("http://10.0.2.2/schedulex/register_user.php");
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

                // Чтение ответа от сервера
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                        finish(); // Вернуться на главный экран
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }).start();
    }
}
