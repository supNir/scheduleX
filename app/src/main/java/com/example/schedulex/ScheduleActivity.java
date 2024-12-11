package com.example.schedulex;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.ViewGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ScheduleActivity extends AppCompatActivity {

    private LinearLayout mondayContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // Связываем элементы
        mondayContent = findViewById(R.id.mondayContent);

        // Добавляем обработчик на заголовок понедельника
        findViewById(R.id.mondayHeader).setOnClickListener(v -> {
            LinearLayout contentLayout = findViewById(R.id.mondayContent);

            // Используем родительский контейнер для анимации
            ViewGroup parent = (ViewGroup) contentLayout.getParent();

            // Применяем анимацию только к контейнеру, который содержит контент для понедельника
            TransitionManager.beginDelayedTransition(parent, new AutoTransition());

            if (contentLayout.getVisibility() == View.GONE) {
                loadDaySchedule("monday", contentLayout);  // Загрузите данные, если это нужно
                contentLayout.setVisibility(View.VISIBLE);
            } else {
                contentLayout.setVisibility(View.GONE);
            }
        });
//        findViewById(R.id.mondayHeader).setOnClickListener(v -> {
//            LinearLayout contentLayout = findViewById(R.id.mondayContent);
//
//            if (contentLayout.getVisibility() == View.GONE) {
//                // Загружаем расписание и показываем контент
//                loadDaySchedule("monday", contentLayout);
//                contentLayout.setVisibility(View.VISIBLE);
//
//                // Делаем анимацию расширения
//                animateContentExpansion(contentLayout);
//            } else {
//                // Делаем анимацию сворачивания
//                animateContentCollapse(contentLayout);
//            }
//        });
        // Аналогично добавьте обработчики для остальных дней недели
    }
//    private void animateContentExpansion(LinearLayout contentLayout) {
//        // Слушаем изменения размера контента, чтобы правильно анимировать
//        contentLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                contentLayout.getViewTreeObserver().removeOnPreDrawListener(this);
//
//                // После того как контент полностью загружен и размещен, получаем высоту
//                int targetHeight = contentLayout.getHeight();
//
//                contentLayout.getLayoutParams().height = 0;  // Изначальная высота
//                contentLayout.requestLayout();
//
//                ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
//                animator.setDuration(300);  // Длительность анимации
//                animator.addUpdateListener(valueAnimator -> {
//                    contentLayout.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
//                    contentLayout.requestLayout();
//                });
//                animator.start();
//
//                return true;
//            }
//        });
//    }

    // Метод для анимации сворачивания
//    private void animateContentCollapse(LinearLayout contentLayout) {
//        int initialHeight = contentLayout.getHeight();
//        int targetHeight = 0;
//
//        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
//        animator.setDuration(300);  // Установите продолжительность анимации
//        animator.addUpdateListener(valueAnimator -> {
//            contentLayout.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
//            contentLayout.requestLayout();
//        });
//        animator.start();
//
//        // После завершения анимации скрываем содержимое
//        animator.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                contentLayout.setVisibility(View.GONE);
//            }
//        });
//    }



    // Метод для загрузки расписания из базы данных
    @SuppressLint("SetTextI18n")
    private void loadDaySchedule(String dayOfWeek, LinearLayout contentLayout) {
        new Thread(() -> {
            try {
                URL url = new URL(Constants.URL_GET_SCHEDULE + "?day_of_week=" + dayOfWeek);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    connection.disconnect();

                    // Обработка ответа
                    String decodedResponse = new String(response.toString().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    JSONObject jsonResponse = new JSONObject(decodedResponse);

                    // Проверяем успешность ответа
                    if (jsonResponse.getInt("success") == 1) {
                        JSONArray scheduleArray = jsonResponse.getJSONArray("schedule");

                        // Обновляем UI
                        runOnUiThread(() -> {
                            contentLayout.removeAllViews(); // Удаляем старые элементы

                            for (int i = 0; i < scheduleArray.length(); i++) {
                                try {
                                    JSONObject schedule = scheduleArray.getJSONObject(i);
                                    // Создаём горизонтальный LinearLayout для одной строки расписания
                                    LinearLayout rowLayout = new LinearLayout(this);
                                    rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                                    rowLayout.setPadding(8, 8, 8, 8);

                                    // Устанавливаем вес для равномерного распределения ширины
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
                                    );

                                    TextView startTime = new TextView(this);
                                    startTime.setLayoutParams(params);
                                    startTime.setText(schedule.getString("start_time").substring(0, 5) + "-" + schedule.getString("end_time").substring(0, 5));
                                    startTime.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                                    // Предмет
                                    TextView subject = new TextView(this);
                                    subject.setLayoutParams(params);
                                    subject.setText(schedule.getString("subject"));
                                    subject.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                                    // Аудитория
                                    TextView classroom = new TextView(this);
                                    classroom.setLayoutParams(params);
                                    classroom.setText(schedule.getString("room"));
                                    classroom.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                                    // Добавляем TextView в строку
                                    rowLayout.addView(startTime);
                                    rowLayout.addView(subject);
                                    rowLayout.addView(classroom);

                                    // Добавляем строку в контент
                                    contentLayout.addView(rowLayout);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    showErrorToast("Ошибка в данных расписания");
                                }
                            }
                        });
                    } else {
                        showErrorToast("Ошибка: " + jsonResponse.getString("message"));
                    }
                } else {
                    showErrorToast("Ошибка загрузки расписания");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorToast("Ошибка подключения");
            }
        }).start();
    }

    // Метод для отображения ошибки
    private void showErrorToast(String message) {
        runOnUiThread(() -> Toast.makeText(ScheduleActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}