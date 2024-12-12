package com.example.schedulex;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private boolean isEditMode = false;
    private String userRole = "user"; // Должно задаваться при авторизации
    // Локальное хранилище изменений
    private List<Map<String, String>> changes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // Устанавливаем Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        userRole = getIntent().getStringExtra("role");

        if (userRole == null) {
            userRole = "user"; // Значение по умолчанию
        }
        // Связываем элементы
        mondayContent = findViewById(R.id.mondayContent);
        mondayContent.post(() -> mondayContent.setTag(measureContentHeight(mondayContent)));

        // Добавляем обработчик на заголовок понедельника
        findViewById(R.id.mondayHeader).setOnClickListener(v -> {
            LinearLayout contentLayout = findViewById(R.id.mondayContent);
            if (contentLayout.getVisibility() == View.GONE) {
                loadDaySchedule("monday", contentLayout);
                contentLayout.setVisibility(View.VISIBLE);
                animateContentExpansion(contentLayout);
            } else {
                animateContentCollapse(contentLayout);
            }
        });
        // Аналогично добавьте обработчики для остальных дней недели
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
        heightAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

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

        // Анимация прозрачности
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
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator()); // Плавная интерполяция

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
        boolean isEditing = isEditMode;
        // Показываем кнопку "Редактировать" только для администраторов
        menu.findItem(R.id.action_edit).setVisible(isAdmin && !isEditing);
        menu.findItem(R.id.action_save).setVisible(isEditing);
        menu.findItem(R.id.action_cancel).setVisible(isEditing);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            enterEditMode();  // Включить режим редактирования
            return true;
        }
        else if (item.getItemId() == R.id.action_save) {
            confirmSaveChanges();  // Подтвердить сохранение изменений
            return true;
        }
        else if (item.getItemId() == R.id.action_cancel) {
            exitEditMode();  // Выйти из режима редактирования
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void enterEditMode() {
        isEditMode = true;
        expandAllDays();
        invalidateOptionsMenu();
        addEditControls();
    }

    private void exitEditMode() {
        isEditMode = false;
        invalidateOptionsMenu();
        removeEditControls();
        changes.clear(); // Очистить изменения при отмене
    }

    private void confirmSaveChanges() {
        new AlertDialog.Builder(this)
                .setTitle("Сохранить изменения?")
                .setMessage("Вы уверены, что хотите сохранить изменения?")
                .setPositiveButton("Да", (dialog, which) -> saveChanges())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void saveChanges() {
        new Thread(() -> {
            try {
                URL url = new URL(Constants.URL_SAVE_SCHEDULE);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);

                JSONArray changesArray = new JSONArray(changes);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(changesArray.toString().getBytes());
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show();
                        exitEditMode();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка сохранения изменений", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Ошибка подключения", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void expandAllDays() {
        // Развернуть все списки расписания (реализуйте аналогично для других дней)
        mondayContent.setVisibility(View.VISIBLE);
    }

    private void addEditControls() {
        // Добавить кнопки "+", "-", "Редактировать" для каждого дня и строки
        addAddButton(mondayContent, "monday");
    }

    private void addAddButton(LinearLayout contentLayout, String dayOfWeek) {
        TextView addButton = new TextView(this);
        addButton.setText("+");
        addButton.setTextSize(20);
        addButton.setOnClickListener(v -> showEditDialog(dayOfWeek, null));
        contentLayout.addView(addButton);
    }

    private void removeEditControls() {
        // Удалить кнопки "+", "-", "Редактировать" из интерфейса
    }

    private void showEditDialog(String dayOfWeek, Map<String, String> scheduleData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(scheduleData == null ? "Добавить запись" : "Редактировать запись");

        @SuppressLint("InflateParams")
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_schedule, null);
        builder.setView(dialogView);

        EditText startTimeField = dialogView.findViewById(R.id.edit_start_time);
        EditText endTimeField = dialogView.findViewById(R.id.edit_end_time);
        EditText subjectField = dialogView.findViewById(R.id.edit_subject);
        EditText roomField = dialogView.findViewById(R.id.edit_room);

        if (scheduleData != null) {
            startTimeField.setText(scheduleData.get("start_time"));
            endTimeField.setText(scheduleData.get("end_time"));
            subjectField.setText(scheduleData.get("subject"));
            roomField.setText(scheduleData.get("room"));
        }

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            Map<String, String> change = new HashMap<>();
            change.put("day", dayOfWeek);
            change.put("start_time", startTimeField.getText().toString());
            change.put("end_time", endTimeField.getText().toString());
            change.put("subject", subjectField.getText().toString());
            change.put("room", roomField.getText().toString());

            if (scheduleData != null) {
                change.put("id", scheduleData.get("id")); // Указать ID для редактирования
                change.put("action", "edit");
            } else {
                change.put("action", "add");
            }

            changes.add(change);
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
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

    private void toggleDayVisibility(LinearLayout contentLayout) {
        // Логика анимации для сворачивания/разворачивания дня
    }
}
