package com.example.pashkeeva_lr4;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private TextView headingText;
    private Spinner fontSizeSpinner;

    private void loadTextPreferences() {
        SharedPreferences prefs = getSharedPreferences("TextEditorPrefs", MODE_PRIVATE);

        String text = prefs.getString("text", "");
        String boldPositions = prefs.getString("boldPositions", "");
        String italicPositions = prefs.getString("italicPositions", "");

        SpannableString spannable = new SpannableString(text);


        String heading = prefs.getString("heading", "Heading"); // Загружаем заголовок, если нет - ставим "Heading"

        editText.setText(text);
        headingText.setText(heading);

        // Жирный текст
        if (!boldPositions.isEmpty()) {
            String[] boldRanges = boldPositions.split(";");
            for (String range : boldRanges) {
                if (!range.isEmpty()) {
                    String[] indices = range.split(",");
                    int start = Integer.parseInt(indices[0]);
                    int end = Integer.parseInt(indices[1]);
                    spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        // Курсивный текст
        if (!italicPositions.isEmpty()) {
            String[] italicRanges = italicPositions.split(";");
            for (String range : italicRanges) {
                if (!range.isEmpty()) {
                    String[] indices = range.split(",");
                    int start = Integer.parseInt(indices[0]);
                    int end = Integer.parseInt(indices[1]);
                    spannable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        editText.setText(spannable);
    }

    private TextView noteTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteTextView = findViewById(R.id.noteTextView);


        editText = findViewById(R.id.editText);
        headingText = findViewById(R.id.headingText);
        fontSizeSpinner = findViewById(R.id.fontSizeSpinner);
        ImageButton boldButton = findViewById(R.id.boldButton);
        ImageButton italicButton = findViewById(R.id.italicButton);
        ImageButton clearButton = findViewById(R.id.clearButton);
        loadNoteSelectionDialog();
        registerForContextMenu(editText);


        Button createNoteButton = findViewById(R.id.createNoteButton);


        createNoteButton.setOnClickListener(v -> showCreateNoteDialog());


        loadTextPreferences(); // Загружаем текст и форматирование
        clearButton.setOnClickListener(v -> {
            editText.setText("");
            headingText.setText("Heading"); // Возвращаем заголовок к исходному
        });
        // Настройка выпадающего списка для выбора размера шрифта
        String[] fontSizes = {"10", "12", "14", "16", "18", "20", "24", "28", "32"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fontSizes);
        fontSizeSpinner.setAdapter(adapter);

        // Изменение размера шрифта выделенного текста
        fontSizeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                changeFontSize(Integer.parseInt(fontSizes[position]));
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Обработчики нажатий на кнопки
        boldButton.setOnClickListener(v -> applyTextStyle(Typeface.BOLD));
        italicButton.setOnClickListener(v -> applyTextStyle(Typeface.ITALIC));
        clearButton.setOnClickListener(v -> {
            editText.setText("");
            headingText.setText("Heading");
        });
    }

    private void loadNoteSelectionDialog() {
        Cursor cursor = getContentResolver().query(NotesProvider.CONTENT_URI, null, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            // Собираем список заголовков заметок
            final String[] noteTitles = new String[cursor.getCount()];
            final int[] noteIds = new int[cursor.getCount()];
            int index = 0;

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                noteIds[index] = id;
                noteTitles[index] = title;
                index++;
            }

            // Создаем диалог выбора заметки
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Выберите заметку")
                    .setItems(noteTitles, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int selectedNoteId = noteIds[which];
                            loadSelectedNoteContent(selectedNoteId);
                        }
                    })
                    .show();

            cursor.close();
        }
    }
    private void showCreateNoteDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_note, null);
        EditText titleEditText = dialogView.findViewById(R.id.titleEditText);
        EditText contentEditText = dialogView.findViewById(R.id.contentEditText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создать новую заметку")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String title = titleEditText.getText().toString();
                    String content = contentEditText.getText().toString();
                    if (!title.isEmpty() && !content.isEmpty()) {
                        saveNewNoteToProvider(title, content);
                    } else {
                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отменить", null)
                .show();
    }

    private void saveNewNoteToProvider(String title, String content) {
        // Создаем объект ContentValues для новой заметки
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);

        // Вставляем новую заметку в ContentProvider
        getContentResolver().insert(NotesProvider.CONTENT_URI, values);
        Toast.makeText(this, "Заметка сохранена", Toast.LENGTH_SHORT).show();

        // Перезагружаем список заметок после сохранения
        loadNoteSelectionDialog();
    }
    private void loadSelectedNoteContent(int noteId) {
        Cursor cursor = getContentResolver().query(
                NotesProvider.CONTENT_URI, null, "_id=?", new String[]{String.valueOf(noteId)}, null);

        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndex("title"));
            String content = cursor.getString(cursor.getColumnIndex("content"));

            // Отображаем данные в noteTextView
            noteTextView.setText(title + "\n\n" + content);
            cursor.close();
        }
    }



    // Метод для создания контекстного меню
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        EditText editText = (EditText) v;
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start == end) {
            // Если текст НЕ выделен, не показываем контекстное меню
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    // Метод для обработки выбора в контекстном меню
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start < end) { // Проверяем, выделен ли текст
            SpannableStringBuilder spannable = new SpannableStringBuilder(editText.getText());

            switch (item.getItemId()) {
                case R.id.menu_bold:
                    spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    editText.setText(spannable);
                    editText.setSelection(start, end);
                    return true;

                case R.id.menu_italic:
                    spannable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    editText.setText(spannable);
                    editText.setSelection(start, end);
                    return true;

                case R.id.menu_clear:
                    spannable.delete(start, end);
                    editText.setText(spannable);
                    editText.setSelection(start);
                    return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    // Метод для изменения размера выделенного текста
    private void changeFontSize(int fontSize) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == end) return; // Если нет выделенного текста, ничего не делаем

        SpannableStringBuilder ssb = new SpannableStringBuilder(editText.getText());
        ssb.setSpan(new RelativeSizeSpan(fontSize / 14.0f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editText.setText(ssb);
        editText.setSelection(end);
    }

    // Метод для применения жирного или курсивного стиля к выделенному тексту
    private void applyTextStyle(int style) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == end) return;

        Spannable spannable = editText.getText();
        boolean hasStyle = false;

        // Проверка, есть ли уже стиль
        StyleSpan[] spans = spannable.getSpans(start, end, StyleSpan.class);
        for (StyleSpan span : spans) {
            if (span.getStyle() == style) {
                spannable.removeSpan(span);
                hasStyle = true;
            }
        }

        // Если стиль отсутствует, добавляем его
        if (!hasStyle) {
            spannable.setSpan(new StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        editText.setText(spannable);
        editText.setSelection(end);
    }
    private void saveTextPreferences() {
        SharedPreferences prefs = getSharedPreferences("TextEditorPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Сохраняем текст
        String text = editText.getText().toString();
        editor.putString("text", text);
        // Сохраняем текст и заголовок
        editor.putString("text", editText.getText().toString());
        editor.putString("heading", headingText.getText().toString());
        editor.putInt("fontSize", (int) editText.getTextSize());

        // Получаем стили текста
        Spannable spannable = editText.getText();
        StyleSpan[] spans = spannable.getSpans(0, spannable.length(), StyleSpan.class);

        StringBuilder boldPositions = new StringBuilder();
        StringBuilder italicPositions = new StringBuilder();

        for (StyleSpan span : spans) {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);

            if (span.getStyle() == Typeface.BOLD) {
                boldPositions.append(start).append(",").append(end).append(";");
            }
            if (span.getStyle() == Typeface.ITALIC) {
                italicPositions.append(start).append(",").append(end).append(";");
            }
        }


        // Сохраняем позиции жирного и курсива
        editor.putString("boldPositions", boldPositions.toString());
        editor.putString("italicPositions", italicPositions.toString());

        editor.apply();
    }


    @Override
    protected void onPause() {
        super.onPause();
        saveTextPreferences();
    }

}
