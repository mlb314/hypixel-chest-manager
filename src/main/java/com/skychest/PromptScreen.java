package com.skychest;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Tiny "type a name" screen. Returns to the chest screen afterwards. */
final class PromptScreen extends Screen {
    private final Screen parent;
    private final String label, initial;
    private final Consumer<String> done;
    private EditBox box;

    PromptScreen(Screen parent, String label, String initial, Consumer<String> done) {
        super(Component.literal(label));
        this.parent = parent; this.label = label; this.initial = initial; this.done = done;
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        Button title = Button.builder(Component.literal(label), b -> { }).bounds(cx - 100, cy - 44, 200, 20).build();
        title.active = false;
        addRenderableWidget(title);
        box = new EditBox(font, cx - 100, cy - 20, 200, 20, Component.literal(label));
        box.setMaxLength(40);
        box.setValue(initial);
        addRenderableWidget(box);
        setInitialFocus(box);
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            String v = box.getValue().trim();
            if (!v.isEmpty()) done.accept(v);
            minecraft.setScreen(parent);
        }).bounds(cx - 100, cy + 8, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> minecraft.setScreen(parent))
                .bounds(cx + 2, cy + 8, 98, 20).build());
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
