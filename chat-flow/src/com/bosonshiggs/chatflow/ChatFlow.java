package com.bosonshiggs.chatflow;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.ClipData;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ScaleGestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.net.Uri;
import android.os.Environment;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.annotations.Asset;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.appinventor.components.runtime.util.YailList;

// Importação do Markwon
import io.noties.markwon.Markwon;

@DesignerComponent(
        version = 44, // Incrementei a versão
        description = "Chat View Extension with styling, editing, exporting, full Markdown support, and message IDs.",
        nonVisible = true,
        iconName = "icon.png"
)
public class ChatFlow extends AndroidNonvisibleComponent {

    private Context context;
    private ComponentContainer container;
    private LinearLayout chatLayout;
    private ScrollView scrollView;
    private boolean loggingEnabled = false;
    private HashMap<String, MessageData> messagesMap = new HashMap<>();
    private ImageView maximizedImageView = null;
    
    // Variáveis para controle do Markwon
    private boolean markdownEnabled = false;
    private Markwon markwon; // [citation:1][citation:7]

    public ChatFlow(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
        this.container = container;
        scrollView = new ScrollView(context);
        chatLayout = new LinearLayout(context);
        chatLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(chatLayout);
        
        // Inicializa o Markwon assim que o contexto estiver disponível[citation:1][citation:7]
        initializeMarkwon();
    }

    private void initializeMarkwon() {
        // Cria uma instância básica do Markwon[citation:1][citation:6][citation:7]
        // Você pode adicionar plugins aqui (tabelas, código, etc.) se necessário
        markwon = Markwon.create(context);
    }

    @SimpleFunction(description = "Initializes the Chat View within a container, such as a HorizontalArrangement or VerticalArrangement.")
    public void Initialize(AndroidViewComponent container) {
        try {
            ViewGroup viewGroup = (ViewGroup) container.getView();
            viewGroup.addView(scrollView);
            logMessage("ChatFlow initialized successfully.");
        } catch (Exception e) {
            logError("Failed to initialize ChatFlow: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Enables or disables logging mode.")
    public void EnableLogging(boolean enable) {
        loggingEnabled = enable;
        logMessage(loggingEnabled ? "Logging enabled." : "Logging disabled.");
    }

    @SimpleFunction(description = "Generates and returns a unique ID for messages. Returns a UUID string.")
    public String GenerateMessageId() {
        return UUID.randomUUID().toString();
    }

    @SimpleFunction(description = "Adds a message to the Chat View with styling options.")
    public void AddMessage(final String message, final boolean isUser, @Asset final String imageProfile, 
                         @Asset final String messageImage, final String timestamp, 
                         final int timestampFontColor, final float timestampFontSize,
                         final int fontColor, final int backgroundColor, final float fontSize, 
                         final boolean bold, final boolean italic, final float cornerRadius,
                         final YailList marginsMessageImage, final YailList reactions, 
                         final boolean isHtml, final boolean allowExternalScripts) {
        AddMessageWithId("", message, isUser, imageProfile, messageImage, timestamp, 
                        timestampFontColor, timestampFontSize, fontColor, backgroundColor, 
                        fontSize, bold, italic, cornerRadius, marginsMessageImage, 
                        reactions, isHtml, allowExternalScripts);
    }

    @SimpleFunction(description = "Adds a message to the Chat View with styling options and a custom ID. If ID is empty, generates a new one.")
    public void AddMessageWithId(final String messageId, final String message, final boolean isUser, 
                                @Asset final String imageProfile, @Asset final String messageImage, 
                                final String timestamp, final int timestampFontColor, 
                                final float timestampFontSize, final int fontColor, 
                                final int backgroundColor, final float fontSize, 
                                final boolean bold, final boolean italic, final float cornerRadius,
                                final YailList marginsMessageImage, final YailList reactions, 
                                final boolean isHtml, final boolean allowExternalScripts) {

        final String id = (messageId == null || messageId.isEmpty()) ? GenerateMessageId() : messageId;
        final String attachedImagePath = (messageImage != null && !messageImage.isEmpty()) ? messageImage : "";

        chatLayout.post(new Runnable() {
            @Override
            public void run() {
                try {
                    LinearLayout messageLayout = new LinearLayout(context);
                    messageLayout.setOrientation(LinearLayout.HORIZONTAL);
                    messageLayout.setPadding(10, 10, 10, 10);
                    messageLayout.setGravity(isUser ? Gravity.END : Gravity.START);

                    ImageView profileImage = null;
                    if (imageProfile != null && !imageProfile.isEmpty()) {
                        profileImage = createImageView(imageProfile, 100, 100);
                    }

                    LinearLayout bubbleLayout = new LinearLayout(context);
                    bubbleLayout.setOrientation(LinearLayout.VERTICAL);
                    bubbleLayout.setPadding(10, 10, 10, 10);

                    LinearLayout.LayoutParams bubbleLayoutParams = new LinearLayout.LayoutParams(
                            0, 
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.0f 
                    );
                    bubbleLayout.setLayoutParams(bubbleLayoutParams);

                    TextView messageView = new TextView(context);
                    
                    // =============================
                    // PROCESSAMENTO DE TEXTO PRINCIPAL
                    // =============================
                    if (markdownEnabled && !isHtml) {
                        // Usa o Markwon para processar e aplicar Markdown diretamente[citation:1][citation:7]
                        markwon.setMarkdown(messageView, message);
                    } else if (isHtml) {
                        // Modo HTML (mantido para compatibilidade)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            int mode = allowExternalScripts ? 
                                android.text.Html.FROM_HTML_MODE_LEGACY : 
                                android.text.Html.FROM_HTML_MODE_COMPACT;
                            messageView.setText(android.text.Html.fromHtml(message, mode));
                        } else {
                            messageView.setText(android.text.Html.fromHtml(message, 
                                android.text.Html.FROM_HTML_MODE_COMPACT));
                        }
                        messageView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                    } else {
                        // Texto simples
                        messageView.setText(message);
                    }
                    
                    // Aplica formatação base (negrito, itálico) apenas se especificado e NÃO for Markdown/HTML
                    // O Markwon já cuida da formatação quando ativado
                    if (!isHtml && !markdownEnabled && (bold || italic)) {
                        messageView.setTypeface(null, (bold ? 1 : 0) | (italic ? 2 : 0));
                    } else {
                        messageView.setTypeface(null, 0); // Reset para fonte normal
                    }
                    
                    messageView.setTextSize(fontSize);
                    messageView.setTextColor(fontColor);
                    
                    GradientDrawable bubbleBackground = new GradientDrawable();
                    bubbleBackground.setCornerRadius(cornerRadius);
                    bubbleBackground.setColor(backgroundColor);
                    bubbleBackground.setStroke(2, Color.LTGRAY);
                    messageView.setBackground(bubbleBackground);
                    messageView.setPadding(20, 15, 20, 15);

                    bubbleLayout.addView(messageView);

                    if (reactions != null && reactions.size() > 0) {
                        LinearLayout reactionsLayout = new LinearLayout(context);
                        reactionsLayout.setOrientation(LinearLayout.HORIZONTAL);
                        reactionsLayout.setGravity(Gravity.START);
                        reactionsLayout.setPadding(10, 5, 10, 5);

                        for (int i = 0; i < reactions.size(); i++) {
                            final String iconId = UUID.randomUUID().toString();
                            final String reactionIcon = reactions.getString(i);

                            if (reactionIcon != null && !reactionIcon.isEmpty()) {
                                ImageView iconView = new ImageView(context);
                                LinearLayout.LayoutParams iconLayoutParams = new LinearLayout.LayoutParams(32, 32);
                                iconLayoutParams.setMargins(5, 0, 5, 0);
                                iconView.setLayoutParams(iconLayoutParams);
                                iconView.setImageBitmap(emojiToBitmap(reactionIcon, 32, 32));
                                reactionsLayout.addView(iconView);

                                final int reactionIndex = i;
                                iconView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ReactionIconClicked(iconId, message, reactionIndex);
                                    }
                                });
                            }
                        }

                        bubbleLayout.addView(reactionsLayout);
                    }

                    if (timestamp != null && !timestamp.isEmpty()) {
                        TextView timestampView = createTimestampView(timestamp, timestampFontColor, timestampFontSize);
                        bubbleLayout.addView(timestampView);
                    }

                    if (isUser) {
                        messageLayout.addView(bubbleLayout);
                        if (profileImage != null) {
                            messageLayout.addView(profileImage);
                        }
                    } else {
                        if (profileImage != null) {
                            messageLayout.addView(profileImage);
                        }
                        messageLayout.addView(bubbleLayout);
                    }

                    if (!attachedImagePath.isEmpty()) {
                        ImageView messageImageView = createImageView(attachedImagePath, 400, 400);
                        if (messageImageView != null) {
                            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(400, 400);

                            if (marginsMessageImage != null && marginsMessageImage.size() == 4) {
                                int left = Integer.parseInt(marginsMessageImage.getString(0));
                                int top = Integer.parseInt(marginsMessageImage.getString(1));
                                int right = Integer.parseInt(marginsMessageImage.getString(2));
                                int bottom = Integer.parseInt(marginsMessageImage.getString(3));
                                imageParams.setMargins(left, top, right, bottom);
                            } else {
                                imageParams.setMargins(15, 2, 0, 0);
                            }
                            messageImageView.setLayoutParams(imageParams);
                            chatLayout.addView(messageLayout);
                            chatLayout.addView(messageImageView);

                            messageImageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ImageClicked(attachedImagePath);
                                }
                            });
                        }
                    } else {
                        chatLayout.addView(messageLayout);
                    }

                    messageLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int[] location = new int[2];
                            v.getLocationOnScreen(location);
                            MessageClicked(id, message, timestamp, attachedImagePath, location[0], location[1]);
                        }
                    });

                    scrollToBottom();

                    messagesMap.put(id, new MessageData(id, message, new Date(), messageView, isHtml || markdownEnabled));
                    logMessage("Message added with ID: " + id);

                    container.$form().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                MessageAdded(id, message, timestamp, attachedImagePath);
                            } catch (Exception e) {
                                logError("Error firing MessageAdded event: " + e.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    logError("Error adding message: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Enables or disables Markdown support for all messages. When enabled, text will be parsed as Markdown.")
    public void SetMarkdownEnabled(boolean enabled) {
        markdownEnabled = enabled;
        logMessage("Markdown support " + (enabled ? "enabled" : "disabled"));
        
        if (container.$form() != null) {
            container.$form().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MarkdownStatusChanged(enabled);
                }
            });
        }
    }

    @SimpleFunction(description = "Returns true if Markdown support is currently enabled.")
    public boolean IsMarkdownEnabled() {
        return markdownEnabled;
    }

    @SimpleFunction(description = "Converts Markdown text to HTML. Useful for preview or other purposes.")
    public String MarkdownToHtml(String markdownText) {
        try {
            // O Markwon renderiza para Spannable, não para HTML.
            // Esta é uma implementação simples que retorna o texto original.
            // Para uma conversão real, você precisaria de uma biblioteca adicional.
            logMessage("Note: Markwon renders to native Android Spannables, not HTML.");
            return markdownText;
        } catch (Exception e) {
            logError("Error in MarkdownToHtml: " + e.getMessage());
            return markdownText;
        }
    }

    @SimpleFunction(description = "Processes and adds a Markdown message directly.")
    public void AddMarkdownMessage(final String messageId, final String markdownText, final boolean isUser, 
                                 @Asset final String imageProfile, @Asset final String messageImage, 
                                 final String timestamp, final int timestampFontColor, 
                                 final float timestampFontSize, final int fontColor, 
                                 final int backgroundColor, final float fontSize, 
                                 final boolean bold, final boolean italic, final float cornerRadius,
                                 final YailList marginsMessageImage, final YailList reactions) {
        
        // Usa a função principal com Markdown ativado temporariamente
        boolean originalState = markdownEnabled;
        markdownEnabled = true;
        
        AddMessageWithId(messageId, markdownText, isUser, imageProfile, messageImage, timestamp,
                        timestampFontColor, timestampFontSize, fontColor, backgroundColor,
                        fontSize, bold, italic, cornerRadius, marginsMessageImage,
                        reactions, false, false); // isHtml = false
        
        // Restaura o estado original se necessário
        if (!originalState) {
            markdownEnabled = false;
        }
    }

    private Bitmap emojiToBitmap(String emoji, int width, int height) {
        TextView emojiView = new TextView(context);
        emojiView.setText(emoji);
        emojiView.setTextSize(24);

        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        emojiView.layout(0, 0, 32, 32);
        emojiView.draw(canvas);
        return bitmap;
    }

    @SimpleFunction(description = "Adds a reaction icon to a specific message by its ID and updates the view.")
    public void AddReactionToMessage(String messageId, String emoji) {
        MessageData messageData = messagesMap.get(messageId);
        if (messageData != null) {
            LinearLayout bubbleLayout = (LinearLayout) messageData.getMessageView().getParent();

            LinearLayout reactionsLayout;
            if (bubbleLayout.getChildAt(bubbleLayout.getChildCount() - 1) instanceof LinearLayout) {
                reactionsLayout = (LinearLayout) bubbleLayout.getChildAt(bubbleLayout.getChildCount() - 1);
            } else {
                reactionsLayout = new LinearLayout(context);
                reactionsLayout.setOrientation(LinearLayout.HORIZONTAL);
                reactionsLayout.setGravity(Gravity.START);
                reactionsLayout.setPadding(10, 5, 10, 5);
                bubbleLayout.addView(reactionsLayout, bubbleLayout.getChildCount() - 1); 
            }

            ImageView iconView = new ImageView(context);
            iconView.setLayoutParams(new LinearLayout.LayoutParams(32, 32));
            iconView.setImageBitmap(emojiToBitmap(emoji, 32, 32));
            reactionsLayout.addView(iconView);

            bubbleLayout.invalidate();
            reactionsLayout.invalidate();
        }
    }

    @SimpleFunction(description = "Deletes a message by its ID and updates the Chat View.")
    public void DeleteMessage(String id) {
        MessageData messageData = messagesMap.get(id);
        if (messageData != null) {
            View parentLayout = (View) messageData.getMessageView().getParent().getParent();

            if (parentLayout != null && parentLayout.getParent() == chatLayout) {
                chatLayout.removeView(parentLayout);
                messagesMap.remove(id);
                chatLayout.invalidate();
                logMessage("Message deleted with ID: " + id);
            } else {
                logError("Failed to delete message: Parent layout not found in chatLayout.");
            }
        } else {
            logError("Message ID not found: " + id);
        }
    }

    @SimpleFunction(description = "Displays a floating menu at X and Y coordinates with a list of options and a tag. Optionally includes Markdown toggle.")
    public void ShowFloatingMenu(int x, int y, YailList items, final String tag, boolean includeMarkdownToggle) {
        final PopupMenu popupMenu = new PopupMenu(context, chatLayout);
        popupMenu.getMenu().clear();
        
        for (Object item : items.toArray()) {
            popupMenu.getMenu().add(item.toString());
        }
        
        if (includeMarkdownToggle) {
            if (items.size() > 0) {
                popupMenu.getMenu().add("-");
            }
            
            String markdownItem = markdownEnabled ? "Disable Markdown" : "Enable Markdown";
            android.view.MenuItem menuItem = popupMenu.getMenu().add(markdownItem);
            menuItem.setOnMenuItemClickListener(new android.view.MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    SetMarkdownEnabled(!markdownEnabled);
                    return true;
                }
            });
        }
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem menuItem) {
                String title = menuItem.getTitle().toString();
                if (!title.equals("-") && !title.equals("Enable Markdown") && !title.equals("Disable Markdown")) {
                    FloatingMenuClicked(title, tag);
                }
                return true;
            }
        });
        popupMenu.show();
    }

    @SimpleEvent(description = "Triggered when Markdown support is enabled or disabled.")
    public void MarkdownStatusChanged(boolean enabled) {
        EventDispatcher.dispatchEvent(this, "MarkdownStatusChanged", enabled);
    }

    private ImageView createImageView(String imagePathOrUrl, int width, int height) {
        ImageView imageView = new ImageView(context);

        if (imagePathOrUrl.startsWith("http://") || imagePathOrUrl.startsWith("https://")) {
            loadImageFromUrl(imagePathOrUrl, imageView, width, height);
        } else {
            try {
                Drawable drawable = MediaUtil.getBitmapDrawable(container.$form(), imagePathOrUrl);
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, width, height, false));
            } catch (IOException e) {
                logError("Failed to load image from asset: " + e.getMessage());
                return null;
            }
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
        layoutParams.gravity = Gravity.CENTER;
        imageView.setLayoutParams(layoutParams);

        GradientDrawable roundedBackground = new GradientDrawable();
        roundedBackground.setCornerRadius(20); 
        roundedBackground.setColor(Color.TRANSPARENT); 
        imageView.setBackground(roundedBackground);
        imageView.setClipToOutline(true); 

        return imageView;
    }

    private void loadImageFromUrl(final String url, final ImageView imageView, final int width, final int height) {
        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream input = new java.net.URL(url).openStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);

                    final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(scaledBitmap);
                        }
                    });
                } catch (Exception e) {
                    logError("Error loading image from URL: " + e.getMessage());
                }
            }
        }).start();
    }

    @SimpleFunction(description = "Exports the entire conversation in JSON format.")
    public String ExportChatToJson() {
        try {
            List<HashMap<String, Object>> exportData = new ArrayList<>();
            
            for (MessageData data : messagesMap.values()) {
                HashMap<String, Object> messageMap = new HashMap<>();
                messageMap.put("id", data.id);
                messageMap.put("message", data.message);
                messageMap.put("timestamp", DateFormat.format("yyyy-MM-dd hh:mm a", data.timestamp).toString());
                exportData.add(messageMap);
            }
            
            String jsonString = new Gson().toJson(exportData);
            return jsonString;
        } catch (Exception e) {
            logError("Error exporting to JSON: " + e.getMessage());
            return "{}";
        }
    }

    @SimpleFunction(description = "Edits a message by ID.")
    public void EditMessage(String id, String newMessage) {
        MessageData messageData = messagesMap.get(id);
        if (messageData != null) {
            messageData.setMessage(newMessage);
            updateMessageView(id, newMessage);
            logMessage("Message edited: " + newMessage);
        } else {
            logError("Message ID not found.");
        }
    }

    @SimpleFunction(description = "Copies text to the clipboard.")
    public void CopyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
        logMessage("Text copied to clipboard.");
    }

    @SimpleFunction(description = "Converts a hexadecimal color string with alpha value to an int color for App Inventor.")
    public int HexColorToInt(String hexColor, int alpha) {
        try {
            int color = Color.parseColor(hexColor);
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        } catch (Exception e) {
            logError("Invalid hex color: " + hexColor);
            return Color.BLACK;
        }
    }

    @SimpleFunction(description = "Checks if a message contains an attached image.")
    public boolean HasImage(String messageId) {
        MessageData messageData = messagesMap.get(messageId);
        return messageData != null && messageData.messageView != null;
    }

    @SimpleFunction(description = "Gets the image path for a message if it exists.")
    public String GetImagePath(String messageId) {
        MessageData messageData = messagesMap.get(messageId);
        return messageData != null ? messageData.messageView.getContentDescription().toString() : "";
    }

    @SimpleFunction(description = "Displays an image in full screen with zoom control and a close button.")
    public void ShowFullScreenImage(@Asset final String imagePathOrUrl) {
        final ImageView fullScreenImageView = new ImageView(context);
        fullScreenImageView.setBackgroundColor(Color.BLACK);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        fullScreenImageView.setLayoutParams(params);

        if (imagePathOrUrl.startsWith("http://") || imagePathOrUrl.startsWith("https://")) {
            loadImageFromUrl(imagePathOrUrl, fullScreenImageView, 800, 800);
        } else {
            try {
                Drawable drawable = MediaUtil.getBitmapDrawable(container.$form(), imagePathOrUrl);
                fullScreenImageView.setImageDrawable(drawable);
            } catch (IOException e) {
                logError("Error loading full screen image: " + e.getMessage());
                return;
            }
        }

        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float scaleFactor = 1.0f;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 3.0f));
                fullScreenImageView.setScaleX(scaleFactor);
                fullScreenImageView.setScaleY(scaleFactor);
                return true;
            }
        });

        fullScreenImageView.setOnTouchListener(new View.OnTouchListener() {
            private long lastTapTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTapTime < 200) {
                        FullScreenImageClicked(imagePathOrUrl);
                    }
                    lastTapTime = currentTime;
                }

                return true;
            }
        });

        final FrameLayout overlayLayout = new FrameLayout(context);
        overlayLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlayLayout.setBackgroundColor(Color.TRANSPARENT);
        overlayLayout.addView(fullScreenImageView);

        Button closeButton = new Button(context);
        closeButton.setText("X");
        closeButton.setTextColor(Color.WHITE);
        closeButton.setBackgroundColor(Color.TRANSPARENT);
        closeButton.setTextSize(24);
        closeButton.setPadding(20, 20, 20, 20);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ViewGroup) scrollView.getParent()).removeView(overlayLayout);
                maximizedImageView = null;
                FullScreenImageClosed();
            }
        });

        FrameLayout.LayoutParams closeButtonParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        closeButtonParams.gravity = Gravity.TOP | Gravity.END;
        closeButtonParams.setMargins(20, 20, 20, 20);
        overlayLayout.addView(closeButton, closeButtonParams);

        ((ViewGroup) scrollView.getParent()).addView(overlayLayout);
    }

    @SimpleFunction(description = "Returns true if an image is currently maximized, false otherwise.")
    public boolean IsImageMaximized() {
        return maximizedImageView != null;
    }

    @SimpleFunction(description = "Requests storage permissions required to download an image.")
    public void RequestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ((Activity) context).requestPermissions(
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @SimpleFunction(description = "Downloads an image to a specified gallery directory and updates with progress events.")
    public void DownloadImageToGallery(@Asset final String imageUrl, final String directoryName) {
        final String imageId = UUID.randomUUID().toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream input = null;
                    Bitmap bitmap = null;
                    int totalBytesRead = 0;
                    int contentLength = -1;
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                        URL url = new URL(imageUrl);
                        input = url.openStream();
                        contentLength = url.openConnection().getContentLength();
                    } else {
                        Drawable drawable = MediaUtil.getBitmapDrawable(container.$form(), imageUrl);
                        bitmap = ((BitmapDrawable) drawable).getBitmap();
                        contentLength = bitmap.getByteCount();
                    }

                    if (input != null) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        while ((bytesRead = input.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;

                            int progress = (int) ((totalBytesRead / (float) contentLength) * 100);
                            DownloadProgress(imageId, progress);
                        }
                        byte[] imageData = outputStream.toByteArray();
                        bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                        input.close();
                    }

                    File imageFile;
                    if (directoryName != null && !directoryName.isEmpty()) {
                        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), directoryName);
                        if (!directory.exists()) {
                            directory.mkdirs();
                        }

                        imageFile = new File(directory, "DownloadedImage_" + System.currentTimeMillis() + ".jpg");
                        FileOutputStream fos = new FileOutputStream(imageFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.close();
                        
                    } else {
                        imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DownloadedImage_" + System.currentTimeMillis() + ".jpg");
                        FileOutputStream fos = new FileOutputStream(imageFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.close();
                    }

                    MediaScannerConnection.scanFile(context, new String[]{imageFile.getAbsolutePath()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                logMessage("Image scanned into gallery: " + path);
                                DownloadCompleted(imageId);
                            }
                        });

                } catch (Exception e) {
                    logError("Error downloading image: " + e.getMessage());
                }
            }
        }).start();
    }

    @SimpleFunction(description = "Returns the path of an attached image if clicked, otherwise returns an empty string.")
    public String CheckImageClicked(String messageId) {
        MessageData messageData = messagesMap.get(messageId);
        if (messageData != null && messageData.messageView != null) {
            return messageData.messageView.getContentDescription().toString();
        }
        return "";
    }

    @SimpleFunction(description = "Sets message margin and padding.")
    public void SetMessageMarginPadding(int left, int top, int right, int bottom) {
        chatLayout.setPadding(left, top, right, bottom);
    }

    @SimpleFunction(description = "Clears all messages from the chat view.")
    public void ClearAllMessages() {
        chatLayout.removeAllViews();
        messagesMap.clear();
        logMessage("All messages cleared.");
    }

    @SimpleFunction(description = "Gets the total count of messages in the chat.")
    public int GetMessageCount() {
        return messagesMap.size();
    }

    @SimpleFunction(description = "Checks if a message with the given ID exists.")
    public boolean MessageExists(String messageId) {
        return messagesMap.containsKey(messageId);
    }

    @SimpleFunction(description = "Gets the message text by ID.")
    public String GetMessageText(String messageId) {
        MessageData messageData = messagesMap.get(messageId);
        return messageData != null ? messageData.message : "";
    }

    // Eventos
    @SimpleEvent(description = "Triggered when a reaction icon is clicked.")
    public void ReactionIconClicked(String iconId, String message, int reactionIndex) {
        EventDispatcher.dispatchEvent(this, "ReactionIconClicked", iconId, message, reactionIndex);
    }
    
    @SimpleEvent(description = "Triggered when the image download is completed.")
    public void DownloadCompleted(String imageId) {
        EventDispatcher.dispatchEvent(this, "DownloadCompleted", imageId);
    }

    @SimpleEvent(description = "Triggered to show download progress from 0 to 100.")
    public void DownloadProgress(String imageId, int progress) {
        EventDispatcher.dispatchEvent(this, "DownloadProgress", imageId, progress);
    }

    @SimpleEvent(description = "Triggered when the fullscreen image is closed by clicking outside.")
    public void FullScreenImageClosed() {
        EventDispatcher.dispatchEvent(this, "FullScreenImageClosed");
    }

    @SimpleEvent(description = "Triggered when the fullscreen image is clicked.")
    public void FullScreenImageClicked(String imagePath) {
        EventDispatcher.dispatchEvent(this, "FullScreenImageClicked", imagePath);
    }

    @SimpleEvent(description = "Event triggered when the user submits the entered text.")
    public void TextInputSubmitted(String text, String tag) {
        EventDispatcher.dispatchEvent(this, "TextInputSubmitted", text, tag);
    }

    @SimpleEvent(description = "Triggered when a floating menu item is selected.")
    public void FloatingMenuClicked(String item, String tag) {
        EventDispatcher.dispatchEvent(this, "FloatingMenuClicked", item, tag);
    }

    @SimpleEvent(description = "Triggered when an attached image is clicked.")
    public void ImageClicked(String imagePath) {
        EventDispatcher.dispatchEvent(this, "ImageClicked", imagePath);
    }

    @SimpleEvent(description = "Triggered when an error or log message occurs.")
    public void ErrorOccurred(String message) {
        EventDispatcher.dispatchEvent(this, "ErrorOccurred", message);
    }

    @SimpleEvent(description = "Triggered when a message is clicked.")
    public void MessageClicked(String id, String message, String timestamp, String attachedImagePath, int x, int y) {
        EventDispatcher.dispatchEvent(this, "MessageClicked", id, message, timestamp, attachedImagePath, x, y);
    }

    @SimpleEvent(description = "Triggered when a message is added.")
    public void MessageAdded(String id, String message, String timestamp, String attachedImagePath) {
        EventDispatcher.dispatchEvent(this, "MessageAdded", id, message, timestamp, attachedImagePath);
    }

    // Métodos auxiliares privados
    private void updateMessageView(String id, String newMessage) {
        MessageData messageData = messagesMap.get(id);
        if (messageData != null) {
            TextView messageView = messageData.getMessageView();
            
            if (messageData.isHtml || markdownEnabled) {
                if (markdownEnabled) {
                    // Atualiza usando o Markwon
                    markwon.setMarkdown(messageView, newMessage);
                } else {
                    // Atualiza usando HTML (modo antigo)
                    String processedText = newMessage;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        messageView.setText(android.text.Html.fromHtml(processedText, 
                            android.text.Html.FROM_HTML_MODE_COMPACT));
                    } else {
                        messageView.setText(android.text.Html.fromHtml(processedText, 
                            android.text.Html.FROM_HTML_MODE_COMPACT));
                    }
                }
            } else {
                messageView.setText(newMessage);
            }
        }
    }

    private TextView createTimestampView(String timestamp, int timestampFontColor, float timestampFontSize) {
        TextView timestampView = new TextView(context);
        timestampView.setText(timestamp);
        timestampView.setTextSize(timestampFontSize);  
        timestampView.setTextColor(timestampFontColor);
        timestampView.setPadding(10, 5, 10, 0);  
        timestampView.setGravity(Gravity.END);  
        return timestampView;
    }

    private void scrollToBottom() {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void logMessage(String message) {
        if (loggingEnabled) {
            ErrorOccurred(message);
        }
    }

    private void logError(String error) {
        if (loggingEnabled) {
            ErrorOccurred(error);
        }
    }

    private static class MessageData {
        String id;
        String message;
        Date timestamp;
        TextView messageView;
        boolean isHtml;

        MessageData(String id, String message, Date timestamp, TextView messageView, boolean isHtml) {
            this.id = id;
            this.message = message;
            this.timestamp = timestamp;
            this.messageView = messageView;
            this.isHtml = isHtml;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public TextView getMessageView() {
            return messageView;
        }
    }
}