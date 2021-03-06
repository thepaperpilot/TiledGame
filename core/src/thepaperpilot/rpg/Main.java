package thepaperpilot.rpg;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import thepaperpilot.rpg.Chapters.GameOver;
import thepaperpilot.rpg.Chapters.One.*;
import thepaperpilot.rpg.Chapters.Void;
import thepaperpilot.rpg.Screens.Area;
import thepaperpilot.rpg.Screens.Context;
import thepaperpilot.rpg.Screens.Title;
import thepaperpilot.rpg.Util.Constants;
import thepaperpilot.rpg.Util.Player;

import java.util.HashMap;
import java.util.Map;

public class Main extends Game implements Screen {
    public static final AssetManager manager = new AssetManager();
    public static TextureAtlas portraits;

    public static final Map<String, Context.ContextPrototype> contexts = new HashMap<String, Context.ContextPrototype>();
    public static Skin skin;
    public static Main instance;
    public static Sound bgm;
    public static long bgmId;
    private static Sound newBGM;
    private static long newId;
    private static float transition = 1;
    private static Stage loadingStage;
    private static Vector2 start;
    private static Vector2 end;

    public static void changeScreen(Screen screen) {
        if (screen == null)
            return;
        instance.setScreen(screen);
    }

    public static void changeContext(final String context) {
        if (instance.getScreen() instanceof Context) {
            final Context old = ((Context) instance.getScreen());
            old.stage.addAction(Actions.sequence(Actions.fadeOut(1), Actions.run(new Runnable() {
                @Override
                public void run() {
                    changeContext(contexts.get(context));
                }
            })));
        } else {
            changeContext(contexts.get(context));
        }
    }

    private static void changeContext(Context.ContextPrototype target) {
        Context con;
        if (start != null && end != null && target instanceof Area.AreaPrototype) {
            con = ((Area.AreaPrototype) target).getContext(start, end);
        } else con = target.getContext();
        con.show();
        con.render(0);
        changeScreen(con);
        start = end = null;
    }

    public static void changeContext(String context, Vector2 start) {
        changeContext(context, start, start);
    }

    public static void changeContext(String context, Vector2 start, Vector2 end) {
        if (contexts.get(context) instanceof Area.AreaPrototype) {
            Main.start = start;
            Main.end = end;
        }
        changeContext(context);
    }

    @Override
    public void create() {
        // use this so I can make a static changeScreen function
        // it basically makes Main a singleton
        instance = this;
        Player.setPreferences(Gdx.app.getPreferences("thepaperpilot.story.save"));

        if (Constants.PROFILING) GLProfiler.enable();

        // start loading all our assets
        // TODO make a giant texture of all the textures with an atlas file and an tsx file
        // and make it have 2 pixels between the tiles so there won't be black lines
        // and ideally have it filled with original art, not Kenney's (but his is cc0, so we can use it for now/ever. no rush)
        manager.load("skin.json", Skin.class);
        manager.load("characters/portraits.atlas", TextureAtlas.class);
        manager.load("characters/player.png", Texture.class);
        manager.load("title.png", Texture.class);
        manager.load("SFX/click1.ogg", Sound.class);
        manager.load("SFX/jingles_SAX03.ogg", Sound.class);
        manager.load("SFX/jingles_SAX05.ogg", Sound.class);
        manager.load("SFX/jingles_SAX07.ogg", Sound.class);
        manager.load("SFX/jingles_SAX15.ogg", Sound.class);
        manager.load("SFX/jingles_SAX16.ogg", Sound.class);

        changeScreen(this);
    }

    @Override
    public void show() {
        // show a basic loading screen
        loadingStage = new Stage(new ExtendViewport(200, 200));

        Label loadingLabel = new Label("Loading...", new Skin(Gdx.files.internal("skin.json")));
        loadingLabel.setFillParent(true);
        loadingLabel.setAlignment(Align.center);
        loadingStage.addActor(loadingLabel);
        loadingStage.addAction(Actions.sequence(Actions.alpha(0), Actions.forever(Actions.sequence(Actions.fadeIn(1), Actions.fadeOut(1)))));

        // basically a sanity check? loadingStage shouldn't have any input listeners
        // but I guess this'll help if the inputprocessor gets set to something it shouldn't
        Gdx.input.setInputProcessor(loadingStage);
    }

    @Override
    public void render(float delta) {
        // render the loading screen
        // act shouldn't do anything, but putting it here is good practice, I guess?
        loadingStage.act();
        loadingStage.draw();

        // continue loading. If complete, do shit
        if (manager.update()) {
            if (skin == null) {
                skin = manager.get("skin.json", Skin.class);
                skin.getFont("large").getData().setScale(.5f);
                skin.getFont("large").getData().markupEnabled = true;
                skin.getFont("font").getData().setScale(.25f);
                skin.getFont("font").getData().markupEnabled = true;
                portraits = manager.get("characters/portraits.atlas", TextureAtlas.class);

                // create all the contexts
                contexts.put("gameover", new GameOver());
                contexts.put("welcome", new Void());
                contexts.put("intro", new Intro());
                contexts.put("falling", new Falling());
                contexts.put("corridor1", new Corridor1());
                contexts.put("puzzle1", new Puzzle1());
                contexts.put("scroll", new ScrollRoom());
                contexts.put("town1", new Town1());
                contexts.put("throne", new Throne());

                // show this screen while it loads
                changeScreen(new Title());
            }
        }
    }

    @Override
    public void hide() {
        /// we're a good garbage collector
        loadingStage.dispose();
    }

    @Override
    public void pause() {
        // we're a passthrough!
        if (getScreen() == this) return;
        super.pause();
    }

    @Override
    public void resume() {
        // we're a passthrough!
        if (getScreen() == this) return;
        super.pause();
    }

    @Override
    public void resize(int width, int height) {
        // we're a passthrough!
        if (getScreen() == this) return;
        if (getScreen() != null) {
            getScreen().resize(width, height);
        }
    }

    @Override
    public void dispose() {
        // we're a passthrough!
        if (getScreen() == this) return;
        if (getScreen() != null) {
            getScreen().dispose();
        }
        // also clean up our shit
        manager.dispose();
        skin.dispose();
    }

    @Override
    public void render() {
        // we're a passthrough!
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Transition bgms
        if (transition != 1) {
            if (transition > 1 || bgm == null) {
                transition = 1;
                if (bgm != null) bgm.stop(bgmId);
                bgm = newBGM;
                bgmId = newId;
                bgm.setVolume(bgmId, Player.music ? .25f : 0);
                newBGM = null;
            } else {
                transition += Gdx.graphics.getDeltaTime();
                if (transition > 0) {
                    bgm.setVolume(bgmId, Player.music ? (1 - transition) / 4f : 0);
                    newBGM.setVolume(newId, Player.music ? transition / 4f : 0);
                }
            }
        }

        getScreen().render(Gdx.graphics.getDeltaTime());

        if (Constants.PROFILING) {
            System.out.println("calls: " + GLProfiler.calls);
            System.out.println("drawCalls: " + GLProfiler.drawCalls);
            System.out.println("shaderSwitches: " + GLProfiler.shaderSwitches);
            System.out.println("textureBindings: " + GLProfiler.textureBindings);
            System.out.println("vertexCount: " + GLProfiler.vertexCount.total);
            System.out.println();
            GLProfiler.reset();
        }
    }

    public static Texture getTexture(String name) {
        name += ".png";
        manager.load(name, Texture.class);
        manager.finishLoadingAsset(name);
        return Main.manager.get(name, Texture.class);
    }

    public static void changeBGM(String bgm) {
        bgm = "BGM/" + bgm;
        manager.load(bgm, Sound.class);
        long time = TimeUtils.millis();
        manager.finishLoadingAsset(bgm);
        if (newBGM != null) {
            if (newBGM == manager.get(bgm, Sound.class)) return;
            transition = 1 - transition;
            if (Main.bgm != null) Main.bgm.stop(bgmId);
            Main.bgm = newBGM;
            bgmId = newId;
        }
        newBGM = manager.get(bgm, Sound.class);
        if (Main.bgm != newBGM) {
            time = TimeUtils.timeSinceMillis(time);
            if (time == 0)
                transition = 0;
            else transition = -1000f / time;
            newId = newBGM.loop(0);
        }
    }

    public static void click() {
        if (Player.sound) Main.manager.get("SFX/click1.ogg", Sound.class).play(1, MathUtils.random(.5f, 2), 0);
    }
}
