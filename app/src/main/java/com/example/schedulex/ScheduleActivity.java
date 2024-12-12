package com.example.schedulex;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleActivity extends AppCompatActivity {

    private LinearLayout mondayContent;
    private LinearLayout tuesdayContent;
    private LinearLayout wednesdayContent;
    private LinearLayout thursdayContent;
    private LinearLayout fridayContent;
    private LinearLayout saturndayContent;
    private LinearLayout sundayContent;
    private boolean isEditMode = false;
    private String userRole = "user"; // Должно задаваться при авторизации
    // Локальное хранилище изменений
    private List<Map<String, String>> changes = new ArrayList<>();
    String[] countries = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        userRole = getIntent().getStringExtra("role");
        mondayContent = findViewById(R.id.mondayContent);
        tuesdayContent = findViewById(R.id.tuesdayContent);
        wednesdayContent = findViewById(R.id.wednesdayContent);
        thursdayContent = findViewById(R.id.thursdayContent);
        fridayContent = findViewById(R.id.fridayContent);
        saturndayContent = findViewById(R.id.saturndayContent);
        sundayContent = findViewById(R.id.sundayContent);
        if (userRole == null) {
            userRole = "user"; // Значение по умолчанию
        }
        setupDaySchedule(R.id.mondayHeader, R.id.mondayContent, "monday");
        setupDaySchedule(R.id.tuesdayHeader, R.id.tuesdayContent, "tuesday");
        setupDaySchedule(R.id.wednesdayHeader, R.id.wednesdayContent, "wednesday");
        setupDaySchedule(R.id.thursdayHeader, R.id.thursdayContent, "thursday");
        setupDaySchedule(R.id.fridayHeader, R.id.fridayContent, "friday");
        setupDaySchedule(R.id.saturndayHeader, R.id.saturndayContent, "saturday");
        setupDaySchedule(R.id.sundayHeader, R.id.sundayContent, "sunday");
    }
    private void setupDaySchedule(int headerId, int contentId, String dayOfWeek) {
        LinearLayout contentView = findViewById(contentId);
        contentView.post(() -> contentView.setTag(measureContentHeight(contentView)));

        // Обработчик на заголовок дня
        findViewById(headerId).setOnClickListener(v -> {
            if (contentView.getVisibility() == View.GONE) {
                loadDaySchedule(dayOfWeek, contentView);
                contentView.setVisibility(View.VISIBLE);
                animateContentExpansion(contentView);
            } else {
                animateContentCollapse(contentView);
            }
        });
    }
    private void animateContentExpansion(LinearLayout contentLayout) {
        // Отключаем мгновенный пересчёт высоты
        contentLayout.measure(
                View.MeasureSpec.makeMeasureSpec(contentLayout.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
        );

        int targetHeight = contentLayout.getMeasuredHeight(); // Заранее измеряем высоту
        contentLayout.getLayoutParams().height = 1; // Устанавливаем минимальную высоту
        contentLayout.setAlpha(0f); // Прозрачный до начала анимации
        contentLayout.setVisibility(View.VISIBLE); // Готовим к анимации
        contentLayout.requestLayout();

        // Создаём анимацию изменения высоты
        ValueAnimator heightAnimator = ValueAnimator.ofInt(1, targetHeight);
        heightAnimator.setDuration(300); // Длительность анимации
        heightAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        heightAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            contentLayout.getLayoutParams().height = animatedValue;
            contentLayout.requestLayout();
        });

        heightAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Устанавливаем высоту WRAP_CONTENT после завершения анимации
                contentLayout.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
                contentLayout.requestLayout();
            }
        });

        contentLayout.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(null);
        heightAnimator.start();
    }


    private int measureContentHeight(View contentLayout) {
        contentLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        return contentLayout.getMeasuredHeight();
    }

    private void animateContentCollapse(final LinearLayout contentLayout) {
        final int initialHeight = contentLayout.getHeight(); // Получаем текущую высоту

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0); // Анимация от текущей высоты до 0
        animator.setDuration(300); // Длительность 300 мс
        animator.setInterpolator(new AccelerateDecelerateInterpolator()); // Плавная интерполяция

        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            contentLayout.getLayoutParams().height = animatedValue;
            contentLayout.requestLayout();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                contentLayout.setVisibility(View.GONE); // Скрываем контент после завершения анимации
            }
        });

        animator.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.schedule_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean isAdmin = userRole.equals("admin");
        menu.findItem(R.id.action_add).setVisible(isAdmin);
        menu.findItem(R.id.action_remove).setVisible(isAdmin);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            showAddScheduleDialog();
            return true;
        }
        else if (item.getItemId() == R.id.action_remove) {
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    private boolean validateInputs(String dayOfWeek, String startTime, String endTime, String subject, String room) {
        if (dayOfWeek.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || subject.isEmpty() || room.isEmpty()) {
            return false;
        }
        // Проверяем формат и логику времени
        return isTimeValid(startTime, endTime);
    }

    private boolean isTimeValid(String startTime, String endTime) {
        try {
            String[] startParts = startTime.split(":");
            String[] endParts = endTime.split(":");

            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);

            if (startHour < endHour || (startHour == endHour && startMinute < endMinute)) {
                return true;
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // В случае ошибки формата возвращаем false
        }
        return false;
    }


    private void showAddScheduleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ScheduleActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_schedule, null);
        builder.setView(dialogView);
        Spinner spinner = dialogView.findViewById(R.id.spinner_day);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Получаем выбранный объект
                String item = (String)parent.getItemAtPosition(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinner.setOnItemSelectedListener(itemSelectedListener);

        Spinner daySpinner = dialogView.findViewById(R.id.spinner_day); // Спиннер для выбора дня недели
        EditText startTimeInput = dialogView.findViewById(R.id.edit_start_time); // Ввод времени начала
        EditText endTimeInput = dialogView.findViewById(R.id.edit_end_time); // Ввод времени конца
        EditText subjectInput = dialogView.findViewById(R.id.edit_subject); // Ввод предмета
        EditText roomInput = dialogView.findViewById(R.id.edit_room); // Ввод аудитории

        builder.setTitle("Добавить расписание")
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String dayOfWeek = daySpinner.getSelectedItem().toString();
                    String startTime = startTimeInput.getText().toString();
                    String endTime = endTimeInput.getText().toString();
                    String subject = subjectInput.getText().toString();
                    String room = roomInput.getText().toString();

                    if (validateInputs(dayOfWeek, startTime, endTime, subject, room)) {
                        addScheduleToServer(dayOfWeek, startTime, endTime, subject, room);
                    } else {
                        Toast.makeText(ScheduleActivity.this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void addScheduleToServer(String dayOfWeek, String startTime, String endTime, String subject, String room) {
        new Thread(() -> {
            String day = "";
            try {
                URL url = new URL(Constants.URL_ADD_SCHEDULE); // Убедитесь, что URL_ADD_SCHEDULE правильный
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                switch (dayOfWeek) {
                    case "Понедельник":
                        day = "monday";
                        break;
                    case "Вторник":
                        day = "tuesday";
                        break;
                    case "Среда":
                        day = "wednesday";
                        break;
                    case "Четверг":
                        day = "thursday";
                        break;
                    case "Пятница":
                        day = "friday";
                        break;
                    case "Суббота":
                        day = "saturnday";
                        break;
                    case "Воскресенье":
                        day = "sunday";
                        break;
                }
                String data = "day_of_week=" + day +
                        "&start_time=" + startTime +
                        "&end_time=" + endTime +
                        "&subject=" + subject +
                        "&room=" + room;

                // Отправка данных
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(data.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String responseString = new String(response.toString().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    JSONObject jsonResponse = new JSONObject(responseString);

                    String finalDay = day;
                    runOnUiThread(() -> {
                        try {
                            if (jsonResponse.getInt("success") == 1) {
                                Toast.makeText(ScheduleActivity.this, "Расписание успешно добавлено", Toast.LENGTH_SHORT).show();
                                LinearLayout contentLayout = getContentLayout(finalDay); // Получаем Layout для обновления
                                if (contentLayout != null) {
                                    loadDaySchedule(finalDay, contentLayout);
                                }
                            } else {
                                Toast.makeText(ScheduleActivity.this, "Ошибка при добавлении расписания", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ScheduleActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ScheduleActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    private LinearLayout getContentLayout(String day) {
        switch (day) {
            case "monday":
                return mondayContent;
            case "tuesday":
                return tuesdayContent;
            case "wednesday":
                return wednesdayContent;
            case "thursday":
                return thursdayContent;
            case "friday":
                return fridayContent;
            case "saturday":
                return saturndayContent;
            case "sunday":
                return sundayContent;
            default:
                return null;
        }
    }

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

                    String decodedResponse = new String(response.toString().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    JSONObject jsonResponse = new JSONObject(decodedResponse);

                    if (jsonResponse.getInt("success") == 1) {
                        JSONArray scheduleArray = jsonResponse.getJSONArray("schedule");

                        runOnUiThread(() -> {
                            contentLayout.removeAllViews();
                            for (int i = 0; i < scheduleArray.length(); i++) {
                                try {
                                    JSONObject schedule = scheduleArray.getJSONObject(i);
                                    LinearLayout rowLayout = new LinearLayout(this);
                                    rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                                    rowLayout.setPadding(8, 8, 8, 8);

                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);

                                    TextView startTime = new TextView(this);
                                    startTime.setLayoutParams(params);
                                    startTime.setText(schedule.getString("start_time").substring(0, 5) + "-" + schedule.getString("end_time").substring(0, 5));
                                    startTime.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                                    TextView subject = new TextView(this);
                                    subject.setLayoutParams(params);
                                    subject.setText(schedule.getString("subject"));
                                    subject.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                                    TextView classroom = new TextView(this);
                                    classroom.setLayoutParams(params);
                                    classroom.setText(schedule.getString("room"));
                                    classroom.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                                    rowLayout.addView(startTime);
                                    rowLayout.addView(subject);
                                    rowLayout.addView(classroom);

                                    contentLayout.addView(rowLayout);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    showErrorToast("Ошибка в данных расписания");
                                }
                            }
                            // Запускаем анимацию после загрузки данных
                            animateContentExpansion(contentLayout);
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

    private void showErrorToast(String message) {
        runOnUiThread(() -> Toast.makeText(ScheduleActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}