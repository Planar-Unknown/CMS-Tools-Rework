package com.dreu.planarcms.client;

import com.dreu.planarcms.config.DisplayConfig;
import com.dreu.planarcms.config.GeneralConfig;
import com.dreu.planarcms.util.Helpers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.dreu.planarcms.PlanarCMS.MODID;

public class PlanarConfigScreen extends Screen {
  private static final String OPTION_TRANSLATION_PREFIX = MODID + ".config.";
  private static final String OPTION_DESCRIPTION_PREFIX = MODID + ".config.tooltip.";
  private static final int CONTENT_TOP = 32;
  private static final int ROW_HEIGHT = 20;
  private static final int SECTION_HEIGHT = 20;
  private static final int CONTROL_WIDTH = 144;
  private static final int CONTROL_HEIGHT = 18;
  private static final int ROW_PADDING = 5;
  private static final int TOOLTIP_WIDTH = 260;
  private static final int TOOLTIP_MARGIN = 4;
  private static final int LABEL_HOVER_PADDING = 3;
  private static final int FOOTER_HEIGHT = 50;
  private static final int FOOTER_TOP_PADDING = 4;
  private static final int FOOTER_STATUS_Y = 7;
  private static final int FOOTER_BUTTON_Y = 25;
  private static final int SCROLL_SPEED = 12;
  private static final int SCROLL_BOTTOM_PADDING = 4;
  private static final int LABEL_COLOR = 0xFFFFFF;
  private static final int SECTION_COLOR = 0xFFD966;
  private static final int ERROR_COLOR = 0xFF5555;
  private static final int SUCCESS_COLOR = 0x55FF55;
  private static final int ROW_HOVER_COLOR = 0x66F2F2F2;
  private static final ClientTooltipPositioner BELOW_OPTION_POSITIONER = PlanarConfigScreen::positionTooltipBelowOption;

  private final Screen parent;
  private final List<ScreenRow> screenRows = new ArrayList<>();
  private final List<ScrollableWidget> scrollableWidgets = new ArrayList<>();

  private ScreenValues savedValues;
  private Button reloadButton;
  private Button doneButton;
  private EditBox presetBox;
  private Component statusMessage;
  private int statusColor = SUCCESS_COLOR;
  private int scrollOffset;
  private int maxScrollOffset;
  private int contentHeight;

  private Helpers.WailaPosition defaultWailaPosition;
  private String preset;

  private boolean showUnconfiguredBlocks;
  private boolean showBlockName;
  private boolean showModName;
  private boolean advancedWaila;
  private boolean showHardness;
  private boolean showExplosionResistance;
  private boolean showDefaultResistance;
  private boolean showDrops;
  private boolean showToolRequirements;
  private boolean showHeldToolPower;
  private boolean showMiningSpeed;
  private boolean showUnmineable;

  public PlanarConfigScreen(Screen parent) {
    super(Component.translatable(OPTION_TRANSLATION_PREFIX + "title"));
    this.parent = parent;
    ensureConfigsLoaded();
    loadValues();
    savedValues = getValues();
  }

  @Override
  protected void init() {
    screenRows.clear();
    scrollableWidgets.clear();
    presetBox = null;

    int rowWidth = Math.min(384, width - 32);
    int left = (width - rowWidth) / 2;
    int controlX = left + rowWidth - CONTROL_WIDTH - ROW_PADDING;
    int y = CONTENT_TOP;

    y = addSection(Component.translatable(OPTION_TRANSLATION_PREFIX + "general"), left, y);
    y = addWailaPositionRow(left, controlX, y);
    y = addEditRow("preset", left, controlX, y, preset, value -> {
      preset = value;
      updateReloadButton();
    });

    y = addSection(Component.translatable(OPTION_TRANSLATION_PREFIX + "display"), left, y + 2);
    y = addBooleanRow("showUnconfiguredBlocks", left, controlX, y, showUnconfiguredBlocks, value -> {
      showUnconfiguredBlocks = value;
      updateReloadButton();
    });
    y = addBooleanRow("showBlockName", left, controlX, y, showBlockName, value -> {
      showBlockName = value;
      updateReloadButton();
    });
    y = addBooleanRow("showModName", left, controlX, y, showModName, value -> {
      showModName = value;
      updateReloadButton();
    });
    y = addBooleanRow("advancedWaila", left, controlX, y, advancedWaila, value -> {
      advancedWaila = value;
      updateReloadButton();
    });
    y = addBooleanRow("showHardness", left, controlX, y, showHardness, value -> {
      showHardness = value;
      updateReloadButton();
    });
    y = addBooleanRow("showExplosionResistance", left, controlX, y, showExplosionResistance, value -> {
      showExplosionResistance = value;
      updateReloadButton();
    });
    y = addBooleanRow("showDefaultResistance", left, controlX, y, showDefaultResistance, value -> {
      showDefaultResistance = value;
      updateReloadButton();
    });
    y = addBooleanRow("showDrops", left, controlX, y, showDrops, value -> {
      showDrops = value;
      updateReloadButton();
    });
    y = addBooleanRow("showToolRequirements", left, controlX, y, showToolRequirements, value -> {
      showToolRequirements = value;
      updateReloadButton();
    });
    y = addBooleanRow("showHeldToolPower", left, controlX, y, showHeldToolPower, value -> {
      showHeldToolPower = value;
      updateReloadButton();
    });
    y = addBooleanRow("showMiningSpeed", left, controlX, y, showMiningSpeed, value -> {
      showMiningSpeed = value;
      updateReloadButton();
    });
    y = addBooleanRow("showUnmineable", left, controlX, y, showUnmineable, value -> {
      showUnmineable = value;
      updateReloadButton();
    });

    contentHeight = y - CONTENT_TOP + SCROLL_BOTTOM_PADDING;
    updateMaxScrollOffset();

    int buttonY = getFooterTop() + FOOTER_BUTTON_Y;
    reloadButton = addRenderableWidget(Button.builder(Component.translatable(OPTION_TRANSLATION_PREFIX + "reload"), button -> saveAndReload())
        .bounds(width / 2 - 155, buttonY, 150, 20)
        .build());
    doneButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
        .bounds(width / 2 + 5, buttonY, 150, 20)
        .build());

    updateScrollableWidgetPositions();
    updateReloadButton();
  }

  @Override
  public void tick() {
    if (presetBox != null) presetBox.tick();
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(guiGraphics);
    updateScrollableWidgetPositions();
    guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
    guiGraphics.enableScissor(0, CONTENT_TOP, width, getContentBottom());
    renderScrollableContent(guiGraphics, mouseX, mouseY, partialTick);
    guiGraphics.disableScissor();
    renderFooter(guiGraphics, mouseX, mouseY, partialTick);
    renderHoveredTooltip(guiGraphics, mouseX, mouseY);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (maxScrollOffset > 0) {
      setScrollOffset(scrollOffset - (int) (delta * SCROLL_SPEED));
    }
    return true;
  }

  @Override
  public void onClose() {
    if (minecraft != null) minecraft.setScreen(parent);
  }

  private void renderScrollableContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    for (ScreenRow row : screenRows) {
      if (row.isOption() && row.containsRow(mouseX, mouseY, scrollOffset))
        guiGraphics.fill(row.left(), row.top(scrollOffset), row.right(), row.bottom(scrollOffset), ROW_HOVER_COLOR);
    }
    for (ScreenRow row : screenRows) {
      guiGraphics.drawString(font, row.label(), row.labelX(), row.labelY(scrollOffset), row.color(), false);
    }
    for (ScrollableWidget widget : scrollableWidgets) {
      if (widget.widget().visible)
        widget.widget().render(guiGraphics, mouseX, mouseY, partialTick);
    }
  }

  private void renderFooter(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    int footerTop = getFooterTop();
    guiGraphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
    guiGraphics.blit(BACKGROUND_LOCATION, 0, footerTop, 0, 0.0F, footerTop, width, height - footerTop, 32, 32);
    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    guiGraphics.fill(0, footerTop, width, footerTop + 1, 0xFF000000);

    Component message = getStatusMessage();
    if (message != null) {
      guiGraphics.drawCenteredString(font, message, width / 2, footerTop + FOOTER_STATUS_Y, statusColor);
    }
    if (reloadButton != null) reloadButton.render(guiGraphics, mouseX, mouseY, partialTick);
    if (doneButton != null) doneButton.render(guiGraphics, mouseX, mouseY, partialTick);
  }

  private int addSection(Component label, int left, int y) {
    screenRows.add(ScreenRow.section(label, left, y + 5));
    return y + SECTION_HEIGHT;
  }

  private int addBooleanRow(String optionKey, int left, int controlX, int y, boolean value, Consumer<Boolean> responder) {
    Component label = addOptionRow(optionKey, left, controlX, y);
    addScrollableWidget(CycleButton.onOffBuilder(value)
        .displayOnlyValue()
        .create(controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, label, (button, newValue) -> responder.accept(newValue)), y);
    return y + ROW_HEIGHT;
  }

  private int addEditRow(String optionKey, int left, int controlX, int y, String value, Consumer<String> responder) {
    Component label = addOptionRow(optionKey, left, controlX, y);
    EditBox editBox = new EditBox(font, controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, label);
    editBox.setValue(value);
    editBox.setResponder(responder);
    addScrollableWidget(editBox, y);
    editBox.setFilter(text -> !text.contains("\"") && !text.contains("\n") && !text.contains("\r"));
    presetBox = editBox;
    return y + ROW_HEIGHT;
  }

  private int addWailaPositionRow(int left, int controlX, int y) {
    Component label = addOptionRow("defaultWailaPosition", left, controlX, y);
    addScrollableWidget(CycleButton.<Helpers.WailaPosition>builder(position -> Component.translatable(OPTION_TRANSLATION_PREFIX + "waila." + position.toString().toLowerCase()))
        .withValues(List.of(Helpers.WailaPosition.LEFT, Helpers.WailaPosition.MIDDLE, Helpers.WailaPosition.RIGHT, Helpers.WailaPosition.INVISIBLE))
        .withInitialValue(defaultWailaPosition)
        .displayOnlyValue()
        .create(controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, label, (button, value) -> {
          defaultWailaPosition = value;
          updateReloadButton();
        }), y);
    return y + ROW_HEIGHT;
  }

  private <T extends AbstractWidget> T addScrollableWidget(T widget, int y) {
    scrollableWidgets.add(new ScrollableWidget(widget, y));
    return addRenderableWidget(widget);
  }

  private Component addOptionRow(String optionKey, int left, int controlX, int y) {
    Component label = Component.translatable(OPTION_TRANSLATION_PREFIX + optionKey);
    Component description = Component.translatable(OPTION_DESCRIPTION_PREFIX + optionKey);
    int labelX = left + ROW_PADDING;
    int labelY = y + 5;
    int rowRight = controlX + CONTROL_WIDTH + ROW_PADDING;
    int labelRight = labelX + font.width(label);
    screenRows.add(ScreenRow.option(label, description, labelX, labelY, labelX - LABEL_HOVER_PADDING, y - 1, labelRight + LABEL_HOVER_PADDING, y + CONTROL_HEIGHT + 1, left, y - 1, rowRight, y + CONTROL_HEIGHT + 1));
    return label;
  }

  private void renderHoveredTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    if (mouseY < CONTENT_TOP || mouseY >= getContentBottom()) return;
    for (ScreenRow row : screenRows) {
      if (row.hasDescription() && row.containsLabel(mouseX, mouseY, scrollOffset)) {
        guiGraphics.renderTooltip(font, font.split(row.description(), TOOLTIP_WIDTH), BELOW_OPTION_POSITIONER, row.labelX(), row.bottom(scrollOffset) + TOOLTIP_MARGIN);
        return;
      }
    }
  }

  private static Vector2i positionTooltipBelowOption(int screenWidth, int screenHeight, int x, int y, int tooltipWidth, int tooltipHeight) {
    int left = Math.max(TOOLTIP_MARGIN, Math.min(x, screenWidth - tooltipWidth - TOOLTIP_MARGIN));
    int top = Math.max(TOOLTIP_MARGIN, y);
    return new Vector2i(left, top);
  }

  private int getFooterTop() {
    return Math.max(CONTENT_TOP + ROW_HEIGHT, height - FOOTER_HEIGHT);
  }

  private int getContentBottom() {
    return Math.max(CONTENT_TOP, getFooterTop() - FOOTER_TOP_PADDING);
  }

  private void updateMaxScrollOffset() {
    int visibleContentHeight = Math.max(0, getContentBottom() - CONTENT_TOP);
    maxScrollOffset = Math.max(0, contentHeight - visibleContentHeight);
    setScrollOffset(scrollOffset);
  }

  private void setScrollOffset(int value) {
    scrollOffset = Math.max(0, Math.min(value, maxScrollOffset));
    updateScrollableWidgetPositions();
  }

  private void updateScrollableWidgetPositions() {
    int contentBottom = getContentBottom();
    for (ScrollableWidget widget : scrollableWidgets) {
      int y = widget.y() - scrollOffset;
      widget.widget().setY(y);
      widget.widget().visible = y + CONTROL_HEIGHT > CONTENT_TOP && y < contentBottom;
    }
  }

  private void saveAndReload() {
    if (preset.trim().isEmpty()) {
      updateReloadButton();
      return;
    }

    preset = preset.trim();
    GeneralConfig.save(defaultWailaPosition, preset);
    DisplayConfig.save(
        showUnconfiguredBlocks,
        showBlockName,
        showModName,
        advancedWaila,
        showHardness,
        showExplosionResistance,
        showDefaultResistance,
        showDrops,
        showToolRequirements,
        showHeldToolPower,
        showMiningSpeed,
        showUnmineable
    );

    Helpers.parseAndPopulateConfig();
    DisplayConfig.parse();
    DisplayConfig.populate();
    savedValues = getValues();
    statusMessage = Component.translatable(OPTION_TRANSLATION_PREFIX + "reloaded");
    statusColor = SUCCESS_COLOR;
    updateReloadButton();
  }

  private void updateReloadButton() {
    if (reloadButton != null) {
      reloadButton.active = hasChanges() && isValid();
    }
    if (isValid()) {
      if (statusMessage != null && statusColor == ERROR_COLOR) statusMessage = null;
    } else {
      statusMessage = getValidationMessage();
      statusColor = ERROR_COLOR;
    }
  }

  private Component getStatusMessage() {
    if (!isValid()) return getValidationMessage();
    return statusMessage;
  }

  private Component getValidationMessage() {
    if (preset.trim().isEmpty()) {
      return Component.translatable(OPTION_TRANSLATION_PREFIX + "invalidPreset");
    }
    return null;
  }

  private boolean hasChanges() {
    return !getValues().equals(savedValues);
  }

  private boolean isValid() {
    return !preset.trim().isEmpty();
  }

  private void ensureConfigsLoaded() {
    if (GeneralConfig.CONFIG == null) {
      GeneralConfig.parse();
      GeneralConfig.populate();
    }
    DisplayConfig.ensureLoaded();
  }

  private void loadValues() {
    defaultWailaPosition = GeneralConfig.DEFAULT_WAILA_POSITION;
    preset = GeneralConfig.getPreset();

    showUnconfiguredBlocks = DisplayConfig.SHOW_UNCONFIGURED_BLOCKS;
    showBlockName = DisplayConfig.SHOW_BLOCK_NAME;
    showModName = DisplayConfig.SHOW_MOD_NAME;
    advancedWaila = DisplayConfig.ADVANCED_WAILA;
    showHardness = DisplayConfig.SHOW_HARDNESS;
    showExplosionResistance = DisplayConfig.SHOW_EXPLOSION_RESISTANCE;
    showDefaultResistance = DisplayConfig.SHOW_DEFAULT_RESISTANCE;
    showDrops = DisplayConfig.SHOW_DROPS;
    showToolRequirements = DisplayConfig.SHOW_TOOL_REQUIREMENTS;
    showHeldToolPower = DisplayConfig.SHOW_HELD_TOOL_POWER;
    showMiningSpeed = DisplayConfig.SHOW_MINING_SPEED;
    showUnmineable = DisplayConfig.SHOW_UNMINEABLE;
  }

  private ScreenValues getValues() {
    return new ScreenValues(
        defaultWailaPosition,
        preset,
        showUnconfiguredBlocks,
        showBlockName,
        showModName,
        advancedWaila,
        showHardness,
        showExplosionResistance,
        showDefaultResistance,
        showDrops,
        showToolRequirements,
        showHeldToolPower,
        showMiningSpeed,
        showUnmineable
    );
  }

  private record ScreenRow(Component label, Component description, int labelX, int labelY, int labelHoverLeft, int labelHoverTop, int labelHoverRight, int labelHoverBottom, int color, int left, int top, int right, int bottom) {
    private static ScreenRow section(Component label, int x, int y) {
      return new ScreenRow(label, null, x, y, x, y, x, y, SECTION_COLOR, 0, 0, 0, 0);
    }

    private static ScreenRow option(Component label, Component description, int labelX, int labelY, int labelHoverLeft, int labelHoverTop, int labelHoverRight, int labelHoverBottom, int left, int top, int right, int bottom) {
      return new ScreenRow(label, description, labelX, labelY, labelHoverLeft, labelHoverTop, labelHoverRight, labelHoverBottom, LABEL_COLOR, left, top, right, bottom);
    }

    private boolean hasDescription() {
      return description != null;
    }

    private boolean isOption() {
      return hasDescription();
    }

    private int labelY(int scrollOffset) {
      return labelY - scrollOffset;
    }

    private int top(int scrollOffset) {
      return top - scrollOffset;
    }

    private int bottom(int scrollOffset) {
      return bottom - scrollOffset;
    }

    private boolean containsLabel(int mouseX, int mouseY, int scrollOffset) {
      return mouseX >= labelHoverLeft && mouseX <= labelHoverRight && mouseY >= labelHoverTop - scrollOffset && mouseY <= labelHoverBottom - scrollOffset;
    }

    private boolean containsRow(int mouseX, int mouseY, int scrollOffset) {
      return mouseX >= left && mouseX <= right && mouseY >= top - scrollOffset && mouseY <= bottom - scrollOffset;
    }
  }

  private record ScrollableWidget(AbstractWidget widget, int y) {}

  private record ScreenValues(
      Helpers.WailaPosition defaultWailaPosition,
      String preset,
      boolean showUnconfiguredBlocks,
      boolean showBlockName,
      boolean showModName,
      boolean advancedWaila,
      boolean showHardness,
      boolean showExplosionResistance,
      boolean showDefaultResistance,
      boolean showDrops,
      boolean showToolRequirements,
      boolean showHeldToolPower,
      boolean showMiningSpeed,
      boolean showUnmineable
  ) {}
}
