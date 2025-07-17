package org.macro.cwrmacro.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.macro.cwrmacro.config.FarmHandConfig;
import org.macro.cwrmacro.module.AutoSellModule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FarmHandConfigScreen extends Screen {
    private final Screen parent;
    private final FarmHandConfig config;
    private final FarmHandConfig tempConfig;

    // UI Elements
    private ButtonWidget masterToggleButton;
    private ButtonWidget autoSellToggleButton;
    private ButtonWidget triggerBotToggleButton;
    private TextFieldWidget autoSellItemField;
    private TextFieldWidget triggerBotEntityField;
    private TextFieldWidget autoSellDelayField;
    private TextFieldWidget inventoryThresholdField;
    private SliderWidget triggerBotSpeedSlider;
    private ButtonWidget doneButton;
    private ButtonWidget cancelButton;
    private ButtonWidget resetButton;
    private ButtonWidget statusButton;

    // Responsive Layout Variables
    private int contentWidth;
    private int contentHeight;
    private int startX;
    private int startY;
    private int buttonWidth;
    private int buttonHeight;
    private int fieldWidth;
    private int fieldHeight;
    private int spacing;
    private int sectionSpacing;

    // Colors
    private static final int BACKGROUND_COLOR = 0xD0000000;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int SECTION_COLOR = 0xFFFFD700;
    private static final int LABEL_COLOR = 0xFFCCCCCC;
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int VALID_FIELD_COLOR = 0x4455FF55;

    // Validation
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_]+:[a-z0-9_/]+$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");

    // State
    private final List<ValidationError> validationErrors = new ArrayList<>();
    private boolean hasUnsavedChanges = false;

    public FarmHandConfigScreen(Screen parent) {
        super(Text.literal("FarmHand Configuration"));
        this.parent = parent;
        this.config = FarmHandConfig.getInstance();
        this.tempConfig = config.copy();
    }

    @Override
    protected void init() {
        calculateLayout();
        this.validationErrors.clear();
        
        int y = startY;
        
        // Title space
        y += 25;
        
        // === GENERAL SETTINGS ===
        y += sectionSpacing;
        
        // Master Toggle
        this.masterToggleButton = createToggleButton(
            getToggleText("Master", tempConfig.enabled),
            startX, y, buttonWidth, buttonHeight,
            button -> {
                tempConfig.enabled = !tempConfig.enabled;
                button.setMessage(getToggleText("Master", tempConfig.enabled));
                markAsChanged();
            },
            "Enable/disable the entire mod"
        );
        this.addDrawableChild(masterToggleButton);
        y += buttonHeight + spacing;
        
        // === AUTO-SELL SETTINGS ===
        y += sectionSpacing;
        
        // Auto-Sell Toggle
        this.autoSellToggleButton = createToggleButton(
            getToggleText("Auto-Sell", tempConfig.autoSellEnabled),
            startX, y, buttonWidth, buttonHeight,
            button -> {
                tempConfig.autoSellEnabled = !tempConfig.autoSellEnabled;
                button.setMessage(getToggleText("Auto-Sell", tempConfig.autoSellEnabled));
                markAsChanged();
            },
            "Auto sell items with /sell hand"
        );
        this.addDrawableChild(autoSellToggleButton);
        y += buttonHeight + spacing;
        
        // Item ID Field
        y += 15; // Space for label
        this.autoSellItemField = createTextField(
            tempConfig.autoSellItemId, "Item ID", startX, y, fieldWidth, fieldHeight
        );
        this.autoSellItemField.setChangedListener(text -> {
            tempConfig.autoSellItemId = text;
            validateField("autoSellItem", text, "Item ID");
            markAsChanged();
        });
        this.addDrawableChild(autoSellItemField);
        y += fieldHeight + spacing;
        
        // Delay and Threshold Fields (responsive side by side)
        y += 15; // Space for label
        int smallFieldWidth = (fieldWidth - 10) / 2;
        
        this.autoSellDelayField = createTextField(
            String.valueOf(tempConfig.autoSellDelay), "Delay (ms)", 
            startX, y, smallFieldWidth, fieldHeight
        );
        this.autoSellDelayField.setChangedListener(text -> {
            try {
                tempConfig.autoSellDelay = Integer.parseInt(text);
                validateField("autoSellDelay", text, "Delay");
                markAsChanged();
            } catch (NumberFormatException e) {
                validateField("autoSellDelay", text, "Delay");
            }
        });
        this.addDrawableChild(autoSellDelayField);
        
        this.inventoryThresholdField = createTextField(
            String.valueOf(tempConfig.inventoryThreshold), "Threshold", 
            startX + smallFieldWidth + 10, y, smallFieldWidth, fieldHeight
        );
        this.inventoryThresholdField.setChangedListener(text -> {
            try {
                tempConfig.inventoryThreshold = Integer.parseInt(text);
                validateField("inventoryThreshold", text, "Threshold");
                markAsChanged();
            } catch (NumberFormatException e) {
                validateField("inventoryThreshold", text, "Threshold");
            }
        });
        this.addDrawableChild(inventoryThresholdField);
        y += fieldHeight + spacing;
        
        // === TRIGGERBOT SETTINGS ===
        y += sectionSpacing;
        
        // TriggerBot Toggle
        this.triggerBotToggleButton = createToggleButton(
            getToggleText("TriggerBot", tempConfig.triggerBotEnabled),
            startX, y, buttonWidth, buttonHeight,
            button -> {
                tempConfig.triggerBotEnabled = !tempConfig.triggerBotEnabled;
                button.setMessage(getToggleText("TriggerBot", tempConfig.triggerBotEnabled));
                markAsChanged();
            },
            "Auto attack entities"
        );
        this.addDrawableChild(triggerBotToggleButton);
        y += buttonHeight + spacing;
        
        // Entity ID Field
        y += 15; // Space for label
        this.triggerBotEntityField = createTextField(
            tempConfig.triggerBotEntityId, "Entity ID", startX, y, fieldWidth, fieldHeight
        );
        this.triggerBotEntityField.setChangedListener(text -> {
            tempConfig.triggerBotEntityId = text;
            validateField("triggerBotEntity", text, "Entity ID");
            markAsChanged();
        });
        this.addDrawableChild(triggerBotEntityField);
        y += fieldHeight + spacing;
        
        // Speed Slider
        y += 15; // Space for label
        this.triggerBotSpeedSlider = new TriggerBotSpeedSlider(
            startX, y, fieldWidth, buttonHeight, tempConfig.triggerBotSpeed / 1000.0
        );
        this.addDrawableChild(triggerBotSpeedSlider);
        y += buttonHeight + spacing;
        
        // === ACTION BUTTONS ===
        y += sectionSpacing;
        
        // Status Button
        this.statusButton = createButton(
            Text.literal("Show Status"), startX, y, buttonWidth, buttonHeight,
            button -> showStatus(),
            "Show current status"
        );
        this.addDrawableChild(statusButton);
        y += buttonHeight + spacing;
        
        // Bottom Action Buttons
        int actionButtonWidth = (buttonWidth - 20) / 3;
        int actionButtonSpacing = 10;
        
        this.cancelButton = createButton(
            Text.literal("Cancel"), startX, y, actionButtonWidth, buttonHeight,
            button -> this.close(), null
        );
        this.addDrawableChild(cancelButton);
        
        this.resetButton = createButton(
            Text.literal("Reset"), startX + actionButtonWidth + actionButtonSpacing, y, 
            actionButtonWidth, buttonHeight,
            button -> this.resetToDefaults(), null
        );
        this.addDrawableChild(resetButton);
        
        this.doneButton = createButton(
            Text.literal("Save"), startX + (actionButtonWidth + actionButtonSpacing) * 2, y, 
            actionButtonWidth, buttonHeight,
            button -> this.saveAndClose(), null
        );
        this.addDrawableChild(doneButton);
        
        // Initial validation
        validateAll();
    }

    private void calculateLayout() {
        // Calculate responsive dimensions based on screen size
        int screenWidth = this.width;
        int screenHeight = this.height;
        
        // Content area (80% of screen width, max 400px)
        contentWidth = Math.min(400, (int)(screenWidth * 0.8));
        contentHeight = (int)(screenHeight * 0.85);
        
        // Center the content
        startX = (screenWidth - contentWidth) / 2;
        startY = (screenHeight - contentHeight) / 2;
        
        // Element dimensions (responsive)
        buttonWidth = contentWidth - 20;
        buttonHeight = Math.max(18, screenHeight / 40);
        fieldWidth = contentWidth - 20;
        fieldHeight = Math.max(16, screenHeight / 45);
        
        // Spacing (responsive)
        spacing = Math.max(4, screenHeight / 100);
        sectionSpacing = Math.max(8, screenHeight / 80);
    }

    private ButtonWidget createToggleButton(Text message, int x, int y, int width, int height, 
                                          ButtonWidget.PressAction onPress, String tooltip) {
        ButtonWidget.Builder builder = ButtonWidget.builder(message, onPress)
            .dimensions(x, y, width, height);
        
        if (tooltip != null) {
            builder.tooltip(Tooltip.of(Text.literal(tooltip)));
        }
        
        return builder.build();
    }

    private ButtonWidget createButton(Text message, int x, int y, int width, int height, 
                                    ButtonWidget.PressAction onPress, String tooltip) {
        ButtonWidget.Builder builder = ButtonWidget.builder(message, onPress)
            .dimensions(x, y, width, height);
        
        if (tooltip != null) {
            builder.tooltip(Tooltip.of(Text.literal(tooltip)));
        }
        
        return builder.build();
    }

    private TextFieldWidget createTextField(String text, String placeholder, int x, int y, int width, int height) {
        TextFieldWidget field = new TextFieldWidget(
            this.textRenderer, x, y, width, height, Text.literal(placeholder)
        );
        field.setMaxLength(100);
        field.setText(text);
        return field;
    }

    // Custom slider class
    private class TriggerBotSpeedSlider extends SliderWidget {
        public TriggerBotSpeedSlider(int x, int y, int width, int height, double value) {
            super(x, y, width, height, Text.literal("Speed: " + getSpeedText((int)(value * 1000))), value);
        }

        @Override
        protected void updateMessage() {
            int speed = (int) (this.value * 1000);
            this.setMessage(Text.literal("Speed: " + getSpeedText(speed)));
        }

        @Override
        protected void applyValue() {
            tempConfig.triggerBotSpeed = (int) (this.value * 1000);
            markAsChanged();
        }
    }

    private String getSpeedText(int speed) {
        if (speed == 0) return "Instant";
        return speed + "ms";
    }

    private Text getToggleText(String label, boolean enabled) {
        return Text.literal(label + ": ")
            .append(Text.literal(enabled ? "ON" : "OFF")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED));
    }

    private void validateField(String fieldId, String value, String fieldName) {
        validationErrors.removeIf(e -> e.fieldId.equals(fieldId));

        if (fieldId.equals("autoSellItem") || fieldId.equals("triggerBotEntity")) {
            if (value.trim().isEmpty()) {
                validationErrors.add(new ValidationError(fieldId, fieldName + " cannot be empty"));
            } else if (!ID_PATTERN.matcher(value.toLowerCase()).matches()) {
                validationErrors.add(new ValidationError(fieldId, "Invalid " + fieldName + " format"));
            }
        } else if (fieldId.equals("autoSellDelay")) {
            if (value.trim().isEmpty()) {
                validationErrors.add(new ValidationError(fieldId, "Delay cannot be empty"));
            } else if (!NUMBER_PATTERN.matcher(value).matches()) {
                validationErrors.add(new ValidationError(fieldId, "Delay must be a number"));
            } else {
                int delay = Integer.parseInt(value);
                if (delay < 1000) {
                    validationErrors.add(new ValidationError(fieldId, "Delay must be ≥ 1000ms"));
                }
            }
        } else if (fieldId.equals("inventoryThreshold")) {
            if (value.trim().isEmpty()) {
                validationErrors.add(new ValidationError(fieldId, "Threshold cannot be empty"));
            } else if (!NUMBER_PATTERN.matcher(value).matches()) {
                validationErrors.add(new ValidationError(fieldId, "Threshold must be a number"));
            } else {
                int threshold = Integer.parseInt(value);
                if (threshold < 1 || threshold > 36) {
                    validationErrors.add(new ValidationError(fieldId, "Threshold must be 1-36"));
                }
            }
        }

        updateButtonStates();
    }

    private void validateAll() {
        validateField("autoSellItem", tempConfig.autoSellItemId, "Item ID");
        validateField("triggerBotEntity", tempConfig.triggerBotEntityId, "Entity ID");
        validateField("autoSellDelay", String.valueOf(tempConfig.autoSellDelay), "Delay");
        validateField("inventoryThreshold", String.valueOf(tempConfig.inventoryThreshold), "Threshold");
    }

    private void updateButtonStates() {
        if (doneButton != null) {
            doneButton.active = validationErrors.isEmpty();
        }
    }

    private void markAsChanged() {
        hasUnsavedChanges = !tempConfig.equals(config);
    }

    private void showStatus() {
        if (client != null && client.player != null) {
            String status = AutoSellModule.getStatusSummary();
            client.player.sendMessage(Text.literal(status).formatted(Formatting.YELLOW), false);
        }
    }

    private void resetToDefaults() {
        tempConfig.resetToDefaults();
        refreshFields();
        validateAll();
        markAsChanged();
    }

    private void refreshFields() {
        autoSellItemField.setText(tempConfig.autoSellItemId);
        triggerBotEntityField.setText(tempConfig.triggerBotEntityId);
        autoSellDelayField.setText(String.valueOf(tempConfig.autoSellDelay));
        inventoryThresholdField.setText(String.valueOf(tempConfig.inventoryThreshold));
        
        // Recreate slider with new value
        this.remove(triggerBotSpeedSlider);
        this.triggerBotSpeedSlider = new TriggerBotSpeedSlider(
            startX, triggerBotSpeedSlider.getY(), fieldWidth, buttonHeight,
            tempConfig.triggerBotSpeed / 1000.0
        );
        this.addDrawableChild(triggerBotSpeedSlider);

        masterToggleButton.setMessage(getToggleText("Master", tempConfig.enabled));
        autoSellToggleButton.setMessage(getToggleText("Auto-Sell", tempConfig.autoSellEnabled));
        triggerBotToggleButton.setMessage(getToggleText("TriggerBot", tempConfig.triggerBotEnabled));
    }

    private void saveAndClose() {
        if (!validationErrors.isEmpty()) {
            return;
        }

        config.enabled = tempConfig.enabled;
        config.autoSellEnabled = tempConfig.autoSellEnabled;
        config.triggerBotEnabled = tempConfig.triggerBotEnabled;
        config.autoSellItemId = tempConfig.autoSellItemId;
        config.triggerBotEntityId = tempConfig.triggerBotEntityId;
        config.autoSellDelay = tempConfig.autoSellDelay;
        config.inventoryThreshold = tempConfig.inventoryThreshold;
        config.triggerBotSpeed = tempConfig.triggerBotSpeed;
        config.save();

        hasUnsavedChanges = false;
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        context.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
        
        // Content background
        context.fill(startX - 10, startY - 10, startX + contentWidth + 10, startY + contentHeight + 10, 0x88000000);
        
        // Title
        context.drawCenteredTextWithShadow(
            this.textRenderer, this.title, this.width / 2, startY + 10, TITLE_COLOR
        );
        
        // Section headers
        int y = startY + 35 + sectionSpacing;
        drawSectionHeader(context, "General Settings", y - 15);
        
        y += buttonHeight + spacing + sectionSpacing;
        drawSectionHeader(context, "Auto-Sell Module", y - 15);
        
        y += buttonHeight + spacing + 15 + fieldHeight + spacing + 15 + fieldHeight + spacing + sectionSpacing;
        drawSectionHeader(context, "TriggerBot Module", y - 15);
        
        // Field labels
        y = startY + 35 + sectionSpacing + buttonHeight + spacing + sectionSpacing + buttonHeight + spacing;
        drawFieldLabel(context, "Item ID (hold when selling):", y, hasError("autoSellItem"));
        
        y += 15 + fieldHeight + spacing;
        drawFieldLabel(context, "Delay (ms) / Threshold (slots):", y, 
            hasError("autoSellDelay") || hasError("inventoryThreshold"));
        
        y += 15 + fieldHeight + spacing + sectionSpacing + buttonHeight + spacing;
        drawFieldLabel(context, "Entity ID:", y, hasError("triggerBotEntity"));
        
        y += 15 + fieldHeight + spacing;
        drawFieldLabel(context, "Attack Speed (0=Instant):", y, false);
        
        // Render all widgets
        super.render(context, mouseX, mouseY, delta);
        
        // Validation errors
        if (!validationErrors.isEmpty()) {
            int errorY = startY + contentHeight - 30;
            for (ValidationError error : validationErrors) {
                context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("✗ " + error.message).formatted(Formatting.RED),
                    this.width / 2, errorY, ERROR_COLOR
                );
                errorY -= 15;
            }
        }
        
        // Status
        if (hasUnsavedChanges) {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Unsaved changes").formatted(Formatting.YELLOW, Formatting.ITALIC),
                this.width / 2, startY + contentHeight - 5, 0xFFFFFF55
            );
        }
    }

    private void drawSectionHeader(DrawContext context, String title, int y) {
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal(title).formatted(Formatting.BOLD, Formatting.GOLD),
            this.width / 2, y, SECTION_COLOR
        );
    }

    private void drawFieldLabel(DrawContext context, String label, int y, boolean hasError) {
        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal(label).formatted(hasError ? Formatting.RED : Formatting.WHITE),
            startX, y, hasError ? ERROR_COLOR : LABEL_COLOR
        );
    }

    private boolean hasError(String fieldId) {
        return validationErrors.stream().anyMatch(e -> e.fieldId.equals(fieldId));
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Helper classes
    private static class ValidationError {
        final String fieldId;
        final String message;

        ValidationError(String fieldId, String message) {
            this.fieldId = fieldId;
            this.message = message;
        }
    }
}