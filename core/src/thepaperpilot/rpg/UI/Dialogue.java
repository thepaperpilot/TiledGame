package thepaperpilot.rpg.UI;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import thepaperpilot.rpg.Context;
import thepaperpilot.rpg.Event;
import thepaperpilot.rpg.Main;

import java.util.ArrayList;

public class Dialogue extends Table {
    protected final Context context;
    protected final ArrayList<Line> lines = new ArrayList<Line>();
    protected int line = 0;
    protected Option selected;
    private final Image face = new Image();
    private final Label nameLabel = new Label("", Main.skin, "dialogue");
    private final Table message = new Table(Main.skin);
    public ScrollText messageLabel;
    private float timer;
    private DialoguePrototype prototype;

    private Dialogue(final DialoguePrototype prototype, Context context) {
        this(prototype, context, 100);
    }

    private Dialogue(final DialoguePrototype prototype, Context context, float height) {
        super(Main.skin);
        this.context = context;
        this.prototype = prototype;
        setFillParent(true);
        pad(4);

        // create each part of the dialogue
        for (LinePrototype line : prototype.lines) {
            lines.add(new Line(line, this));
        }

        // if the dialogue is empty, let's go ahead and not do anything
        if (lines.size() == 0)
            return;

        // create the dialogue stage
        face.setScale(6); // exact value TBD
        messageLabel = new ScrollText();
        messageLabel.setAlignment(Align.topLeft);
        messageLabel.setWrap(true);
        message.top().left();
        message.setBackground(Main.skin.getDrawable("default-round"));
        bottom().left().add(face).bottom().left().expand();
        add(nameLabel).bottom().row();
        add(message).colspan(2).expandX().fillX().height(height).row();
        message.add(new Label("Click to continue...", Main.skin)).expand().center().bottom();

        timer = prototype.timer;
        if (prototype.timer == 0) {
            // left click to advance the dialogue
            addListener(new InputListener() {
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    advance(false);
                    return false;
                }

                public boolean keyDown(InputEvent event, int keycode) {
                    switch (keycode) {
                        case Input.Keys.E:
                        case Input.Keys.ENTER:
                            advance(true);
                            break;
                        case Input.Keys.UP:
                        case Input.Keys.W:
                        case Input.Keys.A:
                            if (selected == null || line == 0) return false;
                            Line currLine = lines.get(line - 1);
                            for (int i = 0; i < currLine.options.length; i++) {
                                if (selected == currLine.options[i]) {
                                    if (i == 0)
                                        selected = currLine.options[currLine.options.length - 1];
                                    else selected = currLine.options[i - 1];
                                    break;
                                }
                            }
                            updateSelected();
                            break;
                        case Input.Keys.DOWN:
                        case Input.Keys.S:
                        case Input.Keys.D:
                            if (selected == null || line == 0) return false;
                            currLine = lines.get(line - 1);
                            for (int i = 0; i < currLine.options.length; i++) {
                                if (selected == currLine.options[i]) {
                                    if (i == currLine.options.length - 1)
                                        selected = currLine.options[0];
                                    else selected = currLine.options[i + 1];
                                    break;
                                }
                            }
                            updateSelected();
                            break;
                        default:
                            return false;
                    }
                    return true;
                }
            });
        }

        // start the dialogue
        next();
    }

    public void act(float delta) {
        super.act(delta);
        if (timer != 0) {
            timer -= delta;
            if (timer <= 0) {
                if (line > 0) {
                    for (Event event : lines.get(line - 1).events) {
                        event.run();
                    }
                }
                end();
            }
        }
    }

    private void advance(boolean override) {
        if (line > 0 && (override || lines.get(line  - 1).options.length == 0)) {
            if (messageLabel.getText().toString().equals(lines.get(line - 1).message)) {
                if (lines.get(line - 1).options.length == 0) next();
                else if (selected != null) selected.select(this);
            } else {
                messageLabel.finish();
            }
        }
    }

    protected void end() {
        line = 0;
        timer = prototype.timer;
        next();
        remove();
    }

    private void next() {
        if (line > 0) {
            // run last line's events
            for (Event event : lines.get(line - 1).events) {
                event.run();
            }
        }

        // check if we're done with the dialogue
        if (lines.size() <= line) {
            end();
            return;
        }

        // update the dialogue stage for the next part of the dialogue
        Line nextLine = lines.get(line);
        line++;
        face.setDrawable(nextLine.face);
        nameLabel.setText(nextLine.name);
        nameLabel.setVisible(nextLine.name != null);
        messageLabel.setMessage(nextLine.message);
        message.clearChildren();
        message.add(messageLabel).expandX().fillX().left().padBottom(5).row();
        if (nextLine.options.length == 0) {
            if (prototype.timer == 0)
                message.add(new Label("Click to continue...", Main.skin)).expand().center().bottom();
        } else {
            for (int i = 0; i < nextLine.options.length; i++) {
                nextLine.options[i].reset(this);
                message.add(nextLine.options[i]).left().padLeft(10).row();
            }
            selected = nextLine.options[0];
            updateSelected();
        }
        if (timer == 0) {
            setTouchable(nextLine.options.length == 0 ? Touchable.enabled : Touchable.childrenOnly);
        }

        Main.click();
    }

    public void updateSelected() {
        if (line == 0) return;
        for (int i = 0; i < lines.get(line - 1).options.length; i++) {
            Option option = lines.get(line - 1).options[i];
            if (selected == option) {
                option.setText(" > " + option.message);
                option.setColor(Color.ORANGE);
            } else {
                option.setText("> " + option.message);
                option.setColor(Color.WHITE);
            }
        }
    }

    public enum DialougeType {
        NORMAL,
        SMALL
    }

    public static class Option extends Label {
        final Event.EventPrototype[] events;
        final String message;

        public Option(String message, Event.EventPrototype[] events) {
            super("> " + message, Main.skin, "large");
            this.message = message;
            this.events = events;
        }

        public void reset(final Dialogue dialogue) {
            // do the actions when this button is clicked
            addListener(new InputListener() {
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    select(dialogue);
                    return true;
                }

                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    dialogue.selected = Option.this;
                    dialogue.updateSelected();
                }
            });
        }

        public void select(Dialogue dialogue) {
            Main.click();
            for (Event.EventPrototype ev : events) {
                new Event(ev, dialogue.context).run();
            }
            dialogue.selected = null;
            dialogue.next();
        }
    }

    private class ScrollText extends Label {
        private float time = 0;
        private int chars = 0;
        private String message = "";

        public ScrollText() {
            super("", Main.skin, "large");
        }

        public void act(float delta) {
            super.act(delta);
            if (!message.equals("")) {
                time += delta;
                if (chars < Math.min(message.length(), (int) (time * Main.TEXT_SPEED))) {
                    Main.click();
                    chars += 3;
                }
                setText(message.substring(0, Math.min(message.length(), (int) (time * Main.TEXT_SPEED))));
            }
        }

        public void setMessage(String message) {
            this.message = message;
            time = chars = 0;
        }

        public void finish() {
            chars = message.length();
            time = message.length();
            Main.click();
        }
    }

    public static class DialoguePrototype {
        public String name;
        public LinePrototype[] lines = new LinePrototype[]{};
        public DialougeType type = DialougeType.NORMAL;
        public Vector2 position;
        public Vector2 size;
        public int timer = 0;
        public boolean smallFont = false;

        public Dialogue getDialogue(Context context) {
            switch (type) {
                default:
                case NORMAL:
                    return new Dialogue(this, context);
                case SMALL:
                    if (position != null && size != null)
                        return new SmallDialogue(this, context, position, size, smallFont);
                    return new SmallDialogue(this, context, smallFont);
            }
        }
    }

    public static class LinePrototype {
        public String name;
        public String message;
        public String face;
        public Event.EventPrototype[] events = new Event.EventPrototype[]{};
        public Option[] options = new Option[]{};
    }

    static class Line {
        final String name;
        final String message;
        final Event[] events;
        final Option[] options;
        Drawable face;

        Line(LinePrototype prototype, Dialogue dialogue) {
            name = prototype.name;
            message = prototype.message;
            options = prototype.options;

            events = new Event[prototype.events.length];
            for (int i = 0; i < prototype.events.length; i++) {
                events[i] = new Event(prototype.events[i], dialogue.context);
            }

            // create the face for the talker
            if (prototype.face != null) {
                face = new Image(Main.getTexture(prototype.face)).getDrawable();
            }
        }
    }

    public static class SmallDialogue extends Dialogue {

        protected SmallDialogue(DialoguePrototype prototype, Context context, Vector2 position, Vector2 size, boolean smallFont) {
            super(prototype, context, size.y);
            setFillParent(false);
            setPosition(position.x - size.x / 2, position.y - size.y / 2);
            setSize(size.x, size.y);
            if (smallFont) this.messageLabel.setFontScale(.25f);
            this.messageLabel.setAlignment(Align.center);
        }

        private SmallDialogue(DialoguePrototype prototype, Context context, boolean smallFont) {
            this(prototype, context, new Vector2(320, 180), new Vector2(200, 100), smallFont);
        }
    }
}