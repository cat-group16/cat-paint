package com.example.catpaint;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.catpaint.databinding.FragmentFirstBinding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private static final String PREFS_NAME = "CatPaintPrefs";
    private static final String KEY_CAT_LAYOUT = "cat_layout_enabled";
    
    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (binding != null) {
                binding.drawingView.clearCanvas();
                updateWidgetCache();
            }
        }
    };

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && getContext() != null) {
                    try {
                        InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                        if (inputStream != null) {
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.drawingView.drawImage(bitmap);
                            inputStream.close();
                            updateWidgetCache();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Errore nel caricamento immagine", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<String[]> pickPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null && getContext() != null) {
                    try {
                        ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(uri, "r");
                        if (pfd != null) {
                            PdfRenderer renderer = new PdfRenderer(pfd);
                            if (renderer.getPageCount() > 0) {
                                PdfRenderer.Page page = renderer.openPage(0);
                                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                                binding.drawingView.drawImage(bitmap);
                                page.close();
                                updateWidgetCache();
                            }
                            renderer.close();
                            pfd.close();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), R.string.error_load_pdf, Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateCatDecorations();

        // Colors
        binding.colorBlack.setOnClickListener(v -> binding.drawingView.setColor(Color.BLACK));
        binding.colorRed.setOnClickListener(v -> binding.drawingView.setColor(Color.parseColor("#FF5252")));
        binding.colorBlue.setOnClickListener(v -> binding.drawingView.setColor(Color.parseColor("#448AFF")));
        binding.colorGreen.setOnClickListener(v -> binding.drawingView.setColor(Color.parseColor("#69F0AE")));
        binding.colorYellow.setOnClickListener(v -> binding.drawingView.setColor(Color.YELLOW));
        binding.colorPink.setOnClickListener(v -> binding.drawingView.setColor(Color.parseColor("#FF4081")));

        // Actions
        binding.btnClear.setOnClickListener(v -> {
            binding.drawingView.clearCanvas();
            updateWidgetCache();
        });
        
        binding.btnSave.setOnClickListener(v -> showExportMenu(v));
        
        binding.btnEraser.setOnClickListener(v -> binding.drawingView.setEraser(true));
        binding.btnSettings.setOnClickListener(v -> 
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SettingsFragment));
        
        binding.btnAddImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        
        binding.btnLoadPdf.setOnClickListener(v -> pickPdfLauncher.launch(new String[]{"application/pdf"}));

        binding.btnAddText.setOnClickListener(v -> showTextDialog());

        binding.drawingView.setOnDrawListener(this::updateWidgetCache);

        // Listen for remote clear commands from widget
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter("com.example.catpaint.REFRESH");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(refreshReceiver, filter);
            }
        }

        // Handle direct widget actions from activity
        getParentFragmentManager().setFragmentResultListener("widget_request", getViewLifecycleOwner(), (requestKey, result) -> {
            String action = result.getString("WIDGET_ACTION");
            if ("CLEAR".equals(action)) {
                binding.drawingView.clearCanvas();
                updateWidgetCache();
            } else if ("SAVE".equals(action)) {
                saveDrawing();
            }
        });
    }

    private void showTextDialog() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.dialog_add_text_title);

        final EditText input = new EditText(getContext());
        input.setHint(R.string.dialog_add_text_hint);
        builder.setView(input);

        builder.setPositiveButton(R.string.dialog_add, (dialog, which) -> {
            String text = input.getText().toString();
            binding.drawingView.drawText(text);
            updateWidgetCache();
        });
        builder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateWidgetCache() {
        if (getContext() == null || binding == null) return;
        try {
            Bitmap bitmap = binding.drawingView.getBitmap();
            File cacheFile = new File(getContext().getCacheDir(), "current_drawing.png");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // Notify widget to update preview
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), NewAppWidget.class));
            if (ids.length > 0) {
                Intent intent = new Intent(getContext(), NewAppWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                getContext().sendBroadcast(intent);
            }
        } catch (Exception ignored) {}
    }

    private void showExportMenu(View view) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenu().add("Salva come PNG");
        popup.getMenu().add("Salva come JPG");
        popup.getMenu().add("Salva come PDF");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Salva come PNG")) {
                saveDrawing("image/png", Bitmap.CompressFormat.PNG, ".png");
            } else if (title.equals("Salva come JPG")) {
                saveDrawing("image/jpeg", Bitmap.CompressFormat.JPEG, ".jpg");
            } else if (title.equals("Salva come PDF")) {
                saveAsPdf();
            }
            return true;
        });
        popup.show();
    }

    private void saveDrawing(String mimeType, Bitmap.CompressFormat format, String extension) {
        if (getContext() == null || binding == null) return;
        Bitmap bitmap = binding.drawingView.getBitmap();
        String fileName = "CatPaint_" + System.currentTimeMillis() + extension;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CatPaint");

        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try {
                OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    bitmap.compress(format, 100, outputStream);
                    outputStream.close();
                }
                Toast.makeText(getContext(), "Disegno salvato come " + extension.toUpperCase().substring(1) + "! 🐾", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Errore nel salvataggio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveAsPdf() {
        if (getContext() == null || binding == null) return;
        Bitmap bitmap = binding.drawingView.getBitmap();
        
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        canvas.drawBitmap(bitmap, 0, 0, paint);
        document.finishPage(page);

        String fileName = "CatPaint_" + System.currentTimeMillis() + ".pdf";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/CatPaint");

        Uri uri = getContext().getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        if (uri != null) {
            try {
                OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    document.writeTo(outputStream);
                    outputStream.close();
                }
                Toast.makeText(getContext(), "Disegno salvato come PDF! 📄🐾", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Errore nel salvataggio PDF", Toast.LENGTH_SHORT).show();
            }
        }
        document.close();
    }

    private void saveDrawing() {
        saveDrawing("image/png", Bitmap.CompressFormat.PNG, ".png");
    }

    private void updateCatDecorations() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean catLayoutEnabled = prefs.getBoolean(KEY_CAT_LAYOUT, true);
        int visibility = catLayoutEnabled ? View.VISIBLE : View.GONE;
        binding.catDecoration1.setVisibility(visibility);
        binding.catDecoration2.setVisibility(visibility);
        binding.catDecoration3.setVisibility(visibility);
        binding.catDecoration4.setVisibility(visibility);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCatDecorations();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getContext() != null) {
            try {
                getContext().unregisterReceiver(refreshReceiver);
            } catch (Exception ignored) {}
        }
        binding = null;
    }
}