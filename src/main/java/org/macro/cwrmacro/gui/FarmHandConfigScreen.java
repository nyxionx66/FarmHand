package org.macro.cwrmacro.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
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

    // UI Layout Constants - RESPONSIVE DESIGN
    private static final int MIN_BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT = 18;
    private static final int MIN_FIELD_WIDTH = 150;
    private static final int FIELD_HEIGHT = 18;
    private static final int SMALL_FIELD_WIDTH = 70;
    private static final int ELEMENT_SPACING = 4;
    private static final int SECTION_SPACING = 12;
    private static final int TOP_MARGIN = 25;
    private static final int BOTTOM_MARGIN = 35;
    private static final int SIDE_MARGIN = 20;

    // Colors
    private static final int BACKGROUND_COLOR = 0xCC000000;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int SECTION_COLOR = 0xFFD700;
    private static final int LABEL_COLOR = 0xAAAAAA;
    private static final int ERROR_COLOR = 0xFF5555;
    private static final int SUCCESS_COLOR = 0x55FF55;
    private static final int VALID_FIELD_COLOR = 0x3355FF55;

    // Validation
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_]+:[a-z0-9_/]+$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");

    // State
    private final List<ValidationError> validationErrors = new ArrayList<>();
    private boolean hasUnsavedChanges = false;
    private final List<Section> sections = new ArrayList<>();
    private int currentY;

    // Responsive layout
    private int buttonWidth;
    private int fieldWidth;
    private int centerX;

    public FarmHandConfigScreen(Screen parent) {
        super(Text.literal("FarmHand Config"));
        this.parent = parent;
        this.config = FarmHandConfig.getInstance();
        this.tempConfig = config.copy();
    }

    @Override
    protected void init() {
        // Calculate responsive dimensions
        buttonWidth = Math.max(MIN_BUTTON_WIDTH, Math.min(200, this.width - SIDE_MARGIN * 4));
        fieldWidth = Math.max(MIN_FIELD_WIDTH, Math.min(180, this.width - SIDE_MARGIN * 4));
        centerX = this.width / 2;

        this.validationErrors.clear();
        this.sections.clear();
        currentY = TOP_MARGIN;

        // Ensure screen is scrollable if needed
        int maxHeight = this.height - BOTTOM_MARGIN;
        
        // Title
        currentY += 15;

        // General Settings Section
        addSection("General Settings");
        
        this.masterToggleButton = ButtonWidget.builder(
                        getToggleText("Master", tempConfig.enabled),
                        button -> {
                            tempConfig.enabled = !tempConfig.enabled;
                            button.setMessage(getToggleText("Master", tempConfig.enabled));
                            markAsChanged();
                        })
                .dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Enable/disable the entire mod")))
                .build();
        this.addDrawableChild(masterToggleButton);
        currentY += BUTTON_HEIGHT + SECTION_SPACING;

        // Auto-Sell Section
        addSection("Auto-Sell Module");
        
        this.autoSellToggleButton = ButtonWidget.builder(
                        getToggleText("Auto-Sell", tempConfig.autoSellEnabled),
                        button -> {
                            tempConfig.autoSellEnabled = !tempConfig.autoSellEnabled;
                            button.setMessage(getToggleText("Auto-Sell", tempConfig.autoSellEnabled));
                            markAsChanged();
                        })
                .dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Auto sell items with /sell hand")))
                .build();
        this.addDrawableChild(autoSellToggleButton);
        currentY += BUTTON_HEIGHT + ELEMENT_SPACING;

        // Item ID field
        addFieldLabel("Item ID:");
        this.autoSellItemField = createTextField(tempConfig.autoSellItemId, "Item ID");
        this.autoSellItemField.setChangedListener(text -> {
            tempConfig.autoSellItemId = text;
            validateField("autoSellItem", text, "Item ID");
            markAsChanged();
        });
        this.addDrawableChild(autoSellItemField);
        currentY += FIELD_HEIGHT + ELEMENT_SPACING;

        // Delay and Threshold fields (side by side)
        addFieldLabel("Delay (ms) / Threshold:");
        
        this.autoSellDelayField = new TextFieldWidget(
                this.textRenderer,
                centerX - fieldWidth / 2,
                currentY,
                SMALL_FIELD_WIDTH,
                FIELD_HEIGHT,
                Text.literal("Delay"));
        this.autoSellDelayField.setMaxLength(6);
        this.autoSellDelayField.setText(String.valueOf(tempConfig.autoSellDelay));
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

        this.inventoryThresholdField = new TextFieldWidget(
                this.textRenderer,
                centerX + 10,
                currentY,
                SMALL_FIELD_WIDTH,
                FIELD_HEIGHT,
                Text.literal("Threshold"));
        this.inventoryThresholdField.setMaxLength(2);
        this.inventoryThresholdField.setText(String.valueOf(tempConfig.inventoryThreshold));
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
        currentY += FIELD_HEIGHT + SECTION_SPACING;

        // TriggerBot Section
        addSection("TriggerBot Module");
        
        this.triggerBotToggleButton = ButtonWidget.builder(
                        getToggleText("TriggerBot", tempConfig.triggerBotEnabled),
                        button -> {
                            tempConfig.triggerBotEnabled = !tempConfig.triggerBotEnabled;
                            button.setMessage(getToggleText("TriggerBot", tempConfig.triggerBotEnabled));
                            markAsChanged();
                        })
                .dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Auto attack entities")))
                .build();
        this.addDrawableChild(triggerBotToggleButton);
        currentY += BUTTON_HEIGHT + ELEMENT_SPACING;

        // Entity ID field
        addFieldLabel("Entity ID:");
        this.triggerBotEntityField = createTextField(tempConfig.triggerBotEntityId, "Entity ID");
        this.triggerBotEntityField.setChangedListener(text -> {
            tempConfig.triggerBotEntityId = text;
            validateField("triggerBotEntity", text, "Entity ID");
            markAsChanged();
        });
        this.addDrawableChild(triggerBotEntityField);
        currentY += FIELD_HEIGHT + ELEMENT_SPACING;

        // Speed Slider - FIXED: Use proper constructor
        addFieldLabel("Speed (0=Instant):");
        this.triggerBotSpeedSlider = new TriggerBotSpeedSlider(
                centerX - fieldWidth / 2, currentY, fieldWidth, BUTTON_HEIGHT,
                tempConfig.triggerBotSpeed / 1000.0);
        this.addDrawableChild(triggerBotSpeedSlider);
        currentY += BUTTON_HEIGHT + ELEMENT_SPACING;

        // Status button
        this.statusButton = ButtonWidget.builder(
                        Text.literal("Show Status"),
                        button -> showStatus())
                .dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Show current status")))
                .build();
        this.addDrawableChild(statusButton);
        currentY += BUTTON_HEIGHT + SECTION_SPACING;

        // Action buttons
        createActionButtons();

        // Initial validation
        validateAll();
    }

    // Custom slider class to handle speed settings
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

    private void addSection(String title) {
        sections.add(new Section(title, currentY));
        currentY += 20;
    }

    private void addFieldLabel(String label) {
        currentY += 3;
        // Label will be rendered in render() method
        currentY += 12;
    }

    private TextFieldWidget createTextField(String text, String placeholder) {
        TextFieldWidget field = new TextFieldWidget(
                this.textRenderer,
                centerX - fieldWidth / 2,
                currentY,
                fieldWidth,
                FIELD_HEIGHT,
                Text.literal(placeholder));
        field.setMaxLength(100);
        field.setText(text);
        return field;
    }

    private void createActionButtons() {
        // Ensure buttons fit on screen
        int buttonCount = 3;
        int totalButtonWidth = 60 * buttonCount;
        int spacing = 8;
        int totalWidth = totalButtonWidth + spacing * (buttonCount - 1);
        
        // Adjust if too wide
        if (totalWidth > this.width - SIDE_MARGIN * 2) {
            totalButtonWidth = (this.width - SIDE_MARGIN * 2 - spacing * (buttonCount - 1));
            totalButtonWidth = totalButtonWidth / buttonCount;
        } else {
            totalButtonWidth = 60;
        }

        int startX = centerX - totalWidth / 2;
        int y = Math.min(currentY, this.height - BOTTOM_MARGIN);

        this.cancelButton = ButtonWidget.builder(
                        Text.literal("Cancel"),
                        button -> this.close())
                .dimensions(startX, y, totalButtonWidth, BUTTON_HEIGHT)
                .build();
        this.addDrawableChild(cancelButton);

        this.resetButton = ButtonWidget.builder(
                        Text.literal("Reset"),
                        button -> this.resetToDefaults())
                .dimensions(startX + totalButtonWidth + spacing, y, totalButtonWidth, BUTTON_HEIGHT)
                .build();
        this.addDrawableChild(resetButton);

        this.doneButton = ButtonWidget.builder(
                        Text.literal("Save"),
                        button -> this.saveAndClose())
                .dimensions(startX + (totalButtonWidth + spacing) * 2, y, totalButtonWidth, BUTTON_HEIGHT)
                .build();
        this.addDrawableChild(doneButton);
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
        // Create new slider with updated value
        this.remove(triggerBotSpeedSlider);
        this.triggerBotSpeedSlider = new TriggerBotSpeedSlider(
                centerX - fieldWidth / 2, 
                triggerBotSpeedSlider.getY(), 
                fieldWidth, 
                BUTTON_HEIGHT,
                tempConfig.triggerBotSpeed / 1000.0);
        this.addDrawableChild(triggerBotSpeedSlider);

        masterToggleButton.setMessage(getToggleText("Master", tempConfig.enabled));
        autoSellToggleButton.setMessage(getToggleText("Auto-Sell", tempConfig.autoSellEnabled));
        triggerBotToggleButton.setMessage(getToggleText("TriggerBot", tempConfig.triggerBotEnabled));
    }

    private void saveAndClose() {
        if (!validationErrors.isEmpty()) {
            return;
        }

        // Copy temp config back to main config
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
        // Render dark background
        context.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Render title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 15, TITLE_COLOR);

        // Render sections
        for (Section section : sections) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal(section.title).formatted(Formatting.BOLD),
                    centerX,
                    section.y,
                    SECTION_COLOR);
        }

        // Render field labels
        int labelY = TOP_MARGIN + 35 + BUTTON_HEIGHT + SECTION_SPACING + 23;
        renderFieldLabel(context, "Item ID:", labelY, hasError("autoSellItem"));
        
        labelY += FIELD_HEIGHT + ELEMENT_SPACING + 15;
        renderFieldLabel(context, "Delay/Threshold:", labelY, hasError("autoSellDelay") || hasError("inventoryThreshold"));
        
        labelY += FIELD_HEIGHT + SECTION_SPACING + 23 + BUTTON_HEIGHT + ELEMENT_SPACING + 15;
        renderFieldLabel(context, "Entity ID:", labelY, hasError("triggerBotEntity"));
        
        labelY += FIELD_HEIGHT + ELEMENT_SPACING + 15;
        renderFieldLabel(context, "Speed:", labelY, false);

        // Render field backgrounds (validation indicators)
        if (!hasError("autoSellItem") && !autoSellItemField.getText().isEmpty()) {
            highlightField(context, autoSellItemField);
        }
        if (!hasError("triggerBotEntity") && !triggerBotEntityField.getText().isEmpty()) {
            highlightField(context, triggerBotEntityField);
        }

        // Render all widgets
        super.render(context, mouseX, mouseY, delta);

        // Render validation errors (compact)
        if (!validationErrors.isEmpty()) {
            int errorY = this.height - 45;
            for (ValidationError error : validationErrors) {
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        Text.literal("✗ " + error.message).formatted(Formatting.RED),
                        centerX,
                        errorY,
                        ERROR_COLOR);
                errorY -= 12;
            }
        }

        // Render status
        if (hasUnsavedChanges) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Unsaved changes").formatted(Formatting.YELLOW, Formatting.ITALIC),
                    centerX,
                    this.height - 10,
                    0xFFFF55);
        }
    }

    private void highlightField(DrawContext context, TextFieldWidget field) {
        context.fill(field.getX() - 1, field.getY() - 1,
                field.getX() + field.getWidth() + 1, field.getY() + field.getHeight() + 1,
                VALID_FIELD_COLOR);
    }

    private void renderFieldLabel(DrawContext context, String label, int y, boolean hasError) {
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(label).formatted(hasError ? Formatting.RED : Formatting.WHITE),
                centerX - fieldWidth / 2,
                y,
                hasError ? ERROR_COLOR : LABEL_COLOR);
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
    private static class Section {
        final String title;
        final int y;

        Section(String title, int y) {
            this.title = title;
            this.y = y;
        }
    }

    private static class ValidationError {
        final String fieldId;
        final String message;

        ValidationError(String fieldId, String message) {
            this.fieldId = fieldId;
            this.message = message;
        }
    }
}