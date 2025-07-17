package org.macro.cwrmacro.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
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
    private ButtonWidget doneButton;
    private ButtonWidget cancelButton;
    private ButtonWidget resetButton;
    private ButtonWidget statusButton;

    // UI Layout Constants
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 20;
    private static final int SMALL_FIELD_WIDTH = 80;
    private static final int ELEMENT_SPACING = 5;
    private static final int SECTION_SPACING = 15;
    private static final int TOP_MARGIN = 30;
    private static final int BOTTOM_MARGIN = 40;

    // Colors
    private static final int BACKGROUND_COLOR = 0x88000000;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int SECTION_COLOR = 0xFFD700;
    private static final int LABEL_COLOR = 0xAAAAAA;
    private static final int ERROR_COLOR = 0xFF5555;
    private static final int SUCCESS_COLOR = 0x55FF55;
    private static final int VALID_FIELD_COLOR = 0x5555FF55;

    // Validation
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_]+:[a-z0-9_/]+$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");

    // State
    private final List<ValidationError> validationErrors = new ArrayList<>();
    private boolean hasUnsavedChanges = false;

    // Layout tracking
    private final List<Section> sections = new ArrayList<>();
    private int currentY;

    public FarmHandConfigScreen(Screen parent) {
        super(Text.literal("FarmHand Configuration"));
        this.parent = parent;
        this.config = FarmHandConfig.getInstance();
        this.tempConfig = config.copy();
    }

    @Override
    protected void init() {
        this.validationErrors.clear();
        this.sections.clear();

        int centerX = this.width / 2;
        currentY = TOP_MARGIN;

        // Title
        currentY += 20;

        // General Settings Section
        Section generalSection = new Section("General Settings", currentY);
        sections.add(generalSection);
        currentY += 25;

        this.masterToggleButton = ButtonWidget.builder(
                        getToggleText("Master Toggle", tempConfig.enabled),
                        button -> {
                            tempConfig.enabled = !tempConfig.enabled;
                            button.setMessage(getToggleText("Master Toggle", tempConfig.enabled));
                            markAsChanged();
                        })
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Enable/disable the entire FarmHand mod")))
                .build();
        this.addDrawableChild(masterToggleButton);
        currentY += BUTTON_HEIGHT + SECTION_SPACING;

        // Auto-Sell Section
        Section autoSellSection = new Section("Auto-Sell Module", currentY);
        sections.add(autoSellSection);
        currentY += 25;

        this.autoSellToggleButton = ButtonWidget.builder(
                        getToggleText("Auto-Sell", tempConfig.autoSellEnabled),
                        button -> {
                            tempConfig.autoSellEnabled = !tempConfig.autoSellEnabled;
                            button.setMessage(getToggleText("Auto-Sell", tempConfig.autoSellEnabled));
                            markAsChanged();
                        })
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Automatically sell items using /sell hand command")))
                .build();
        this.addDrawableChild(autoSellToggleButton);
        currentY += BUTTON_HEIGHT + ELEMENT_SPACING;

        // Item ID field
        currentY += 5;
        this.autoSellItemField = new TextFieldWidget(
                this.textRenderer,
                centerX - FIELD_WIDTH / 2,
                currentY + 15,
                FIELD_WIDTH,
                FIELD_HEIGHT,
                Text.literal("Item ID"));
        this.autoSellItemField.setMaxLength(100);
        this.autoSellItemField.setText(tempConfig.autoSellItemId);
        this.autoSellItemField.setChangedListener(text -> {
            tempConfig.autoSellItemId = text;
            validateField("autoSellItem", text, "Item ID");
            markAsChanged();
        });
        this.addDrawableChild(autoSellItemField);
        currentY += FIELD_HEIGHT + 25;

        // Delay and Threshold fields (side by side)
        this.autoSellDelayField = new TextFieldWidget(
                this.textRenderer,
                centerX - FIELD_WIDTH / 2,
                currentY + 15,
                SMALL_FIELD_WIDTH,
                FIELD_HEIGHT,
                Text.literal("Delay (ms)"));
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
                centerX + 20,
                currentY + 15,
                SMALL_FIELD_WIDTH,
                FIELD_HEIGHT,
                Text.literal("Inventory Threshold"));
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
        currentY += FIELD_HEIGHT + 30;

        // TriggerBot Section
        Section triggerBotSection = new Section("TriggerBot Module", currentY);
        sections.add(triggerBotSection);
        currentY += 25;

        this.triggerBotToggleButton = ButtonWidget.builder(
                        getToggleText("TriggerBot", tempConfig.triggerBotEnabled),
                        button -> {
                            tempConfig.triggerBotEnabled = !tempConfig.triggerBotEnabled;
                            button.setMessage(getToggleText("TriggerBot", tempConfig.triggerBotEnabled));
                            markAsChanged();
                        })
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Automatically attack specified entities")))
                .build();
        this.addDrawableChild(triggerBotToggleButton);
        currentY += BUTTON_HEIGHT + ELEMENT_SPACING;

        // Entity ID field
        currentY += 5;
        this.triggerBotEntityField = new TextFieldWidget(
                this.textRenderer,
                centerX - FIELD_WIDTH / 2,
                currentY + 15,
                FIELD_WIDTH,
                FIELD_HEIGHT,
                Text.literal("Entity ID"));
        this.triggerBotEntityField.setMaxLength(100);
        this.triggerBotEntityField.setText(tempConfig.triggerBotEntityId);
        this.triggerBotEntityField.setChangedListener(text -> {
            tempConfig.triggerBotEntityId = text;
            validateField("triggerBotEntity", text, "Entity ID");
            markAsChanged();
        });
        this.addDrawableChild(triggerBotEntityField);
        currentY += FIELD_HEIGHT + 35;

        // Status button
        this.statusButton = ButtonWidget.builder(
                        Text.literal("Show Status"),
                        button -> showStatus())
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Show current module status")))
                .build();
        this.addDrawableChild(statusButton);
        currentY += BUTTON_HEIGHT + 25;

        // Action buttons
        createActionButtons(centerX, currentY);

        // Initial validation
        validateAll();
    }

    private void createActionButtons(int centerX, int y) {
        int buttonWidth = 80;
        int spacing = 10;
        int totalWidth = (buttonWidth * 3) + (spacing * 2);
        int startX = centerX - totalWidth / 2;

        this.cancelButton = ButtonWidget.builder(
                        Text.literal("Cancel"),
                        button -> this.close())
                .dimensions(startX, y, buttonWidth, BUTTON_HEIGHT)
                .build();
        this.addDrawableChild(cancelButton);

        this.resetButton = ButtonWidget.builder(
                        Text.literal("Reset"),
                        button -> this.resetToDefaults())
                .dimensions(startX + buttonWidth + spacing, y, buttonWidth, BUTTON_HEIGHT)
                .build();
        this.addDrawableChild(resetButton);

        this.doneButton = ButtonWidget.builder(
                        Text.literal("Save"),
                        button -> this.saveAndClose())
                .dimensions(startX + (buttonWidth + spacing) * 2, y, buttonWidth, BUTTON_HEIGHT)
                .build();
        this.addDrawableChild(doneButton);
    }

    private Text getToggleText(String label, boolean enabled) {
        return Text.literal(label + ": ")
                .append(Text.literal(enabled ? "Enabled" : "Disabled")
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED));
    }

    private void validateField(String fieldId, String value, String fieldName) {
        validationErrors.removeIf(e -> e.fieldId.equals(fieldId));

        if (fieldId.equals("autoSellItem") || fieldId.equals("triggerBotEntity")) {
            if (value.trim().isEmpty()) {
                validationErrors.add(new ValidationError(fieldId, fieldName + " cannot be empty"));
            } else if (!ID_PATTERN.matcher(value.toLowerCase()).matches()) {
                validationErrors.add(new ValidationError(fieldId, "Invalid " + fieldName + " format (use namespace:name)"));
            }
        } else if (fieldId.equals("autoSellDelay")) {
            if (value.trim().isEmpty()) {
                validationErrors.add(new ValidationError(fieldId, "Delay cannot be empty"));
            } else if (!NUMBER_PATTERN.matcher(value).matches()) {
                validationErrors.add(new ValidationError(fieldId, "Delay must be a number"));
            } else {
                int delay = Integer.parseInt(value);
                if (delay < 1000) {
                    validationErrors.add(new ValidationError(fieldId, "Delay must be at least 1000ms"));
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
                    validationErrors.add(new ValidationError(fieldId, "Threshold must be between 1 and 36"));
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

        autoSellItemField.setText(tempConfig.autoSellItemId);
        triggerBotEntityField.setText(tempConfig.triggerBotEntityId);
        autoSellDelayField.setText(String.valueOf(tempConfig.autoSellDelay));
        inventoryThresholdField.setText(String.valueOf(tempConfig.inventoryThreshold));

        masterToggleButton.setMessage(getToggleText("Master Toggle", tempConfig.enabled));
        autoSellToggleButton.setMessage(getToggleText("Auto-Sell", tempConfig.autoSellEnabled));
        triggerBotToggleButton.setMessage(getToggleText("TriggerBot", tempConfig.triggerBotEnabled));

        validateAll();
        markAsChanged();
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
        config.save();

        hasUnsavedChanges = false;
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render dark background
        context.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // Render title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, TITLE_COLOR);

        // Render sections
        for (Section section : sections) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal(section.title).formatted(Formatting.BOLD),
                    this.width / 2,
                    section.y,
                    SECTION_COLOR);
        }

        // Render field labels
        renderFieldLabel(context, "Item ID (hold to sell):", autoSellItemField.getY() - 15,
                hasError("autoSellItem"));
        renderFieldLabel(context, "Delay (ms):", autoSellDelayField.getY() - 15,
                hasError("autoSellDelay"));
        renderFieldLabel(context, "Threshold:", inventoryThresholdField.getY() - 15,
                hasError("inventoryThreshold"));
        renderFieldLabel(context, "Entity ID:", triggerBotEntityField.getY() - 15,
                hasError("triggerBotEntity"));

        // Render field backgrounds (green tint for valid fields)
        if (!hasError("autoSellItem") && !autoSellItemField.getText().isEmpty()) {
            context.fill(autoSellItemField.getX() - 1, autoSellItemField.getY() - 1,
                    autoSellItemField.getX() + FIELD_WIDTH + 1, autoSellItemField.getY() + FIELD_HEIGHT + 1,
                    VALID_FIELD_COLOR);
        }

        if (!hasError("triggerBotEntity") && !triggerBotEntityField.getText().isEmpty()) {
            context.fill(triggerBotEntityField.getX() - 1, triggerBotEntityField.getY() - 1,
                    triggerBotEntityField.getX() + FIELD_WIDTH + 1, triggerBotEntityField.getY() + FIELD_HEIGHT + 1,
                    VALID_FIELD_COLOR);
        }

        // Render all widgets
        super.render(context, mouseX, mouseY, delta);

        // Render validation errors
        int errorY = this.height - BOTTOM_MARGIN - 20;
        for (ValidationError error : validationErrors) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("✗ " + error.message).formatted(Formatting.RED),
                    this.width / 2,
                    errorY,
                    ERROR_COLOR);
            errorY -= 15;
        }

        // Render status messages
        if (hasUnsavedChanges) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Unsaved changes").formatted(Formatting.YELLOW, Formatting.ITALIC),
                    this.width / 2,
                    this.height - 15,
                    0xFFFF55);
        }

        // Render tooltips for text fields
        if (autoSellItemField.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("Item to hold when selling"),
                    Text.literal("Example: minecraft:diamond"),
                    Text.literal("Format: namespace:item_name").formatted(Formatting.GRAY)
            ), mouseX, mouseY);
        }

        if (autoSellDelayField.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("Delay between sells in milliseconds"),
                    Text.literal("Minimum: 1000ms (1 second)"),
                    Text.literal("Recommended: 3000ms (3 seconds)").formatted(Formatting.GRAY)
            ), mouseX, mouseY);
        }

        if (inventoryThresholdField.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("Inventory fullness trigger (1-36)"),
                    Text.literal("30 = sell when 30/36 slots full"),
                    Text.literal("Higher = wait for fuller inventory").formatted(Formatting.GRAY)
            ), mouseX, mouseY);
        }

        if (triggerBotEntityField.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("Example: minecraft:zombie"),
                    Text.literal("Format: namespace:entity_name").formatted(Formatting.GRAY)
            ), mouseX, mouseY);
        }
    }

    private void renderFieldLabel(DrawContext context, String label, int y, boolean hasError) {
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(label).formatted(hasError ? Formatting.RED : Formatting.WHITE),
                this.width / 2 - FIELD_WIDTH / 2,
                y,
                hasError ? ERROR_COLOR : LABEL_COLOR);
    }

    private boolean hasError(String fieldId) {
        return validationErrors.stream().anyMatch(e -> e.fieldId.equals(fieldId));
    }

    @Override
    public void close() {
        if (hasUnsavedChanges) {
            // You could implement a confirmation dialog here
            // For now, just discard changes
        }

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