package ethanjones.cubes.graphics.world;

import ethanjones.cubes.core.settings.Settings;
import ethanjones.cubes.graphics.world.ao.AmbientOcclusion;
import ethanjones.cubes.side.common.Cubes;
import ethanjones.cubes.world.client.WorldClient;
import ethanjones.cubes.world.storage.Area;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class WorldShaderProvider implements ShaderProvider {

  private static final String DEFAULT_VERTEX_SHADER = Gdx.files.internal("shaders/world.vertex.glsl").readString();
  private static final String DEFAULT_FRAGMENT_SHADER = Gdx.files.internal("shaders/world.fragment.glsl").readString();

  private static final int FEATURE_FOG = 1 << 0;
  private static final int FEATURE_AO = 1 << 1;

  private static final int MAX_FEATURE_FLAG = 2;
  private static final int COMBINATIONS = 2 * MAX_FEATURE_FLAG;
  private static final CubesShader[] shaders = new CubesShader[COMBINATIONS];

  @Override
  public Shader getShader(Renderable renderable) {
    int shader = 0;

    boolean fogFlag = Settings.getBooleanSettingValue(Settings.GRAPHICS_FOG);
    if (renderable.userData instanceof RenderingSettings) fogFlag &= ((RenderingSettings) renderable.userData).fogEnabled;
    if (fogFlag) shader |= FEATURE_FOG;

    boolean aoFlag = renderable.meshPart.mesh.getVertexAttributes() == CubesVertexAttributes.VERTEX_ATTRIBUTES_AO;
    if (aoFlag) shader |= FEATURE_AO;

    if (shaders[shader] == null) {
      if (shader == 0) {
        shaders[shader] = new CubesShader(renderable);
      } else {
        ArrayList<Feature> f = new ArrayList<Feature>();
        if (fogFlag) f.add(new FogFeature());
        if (aoFlag) f.add(new AmbientOcclusionFeature());
        shaders[shader] = new FeatureShader(renderable, f);
      }
      shaders[shader].init();
    }
    return shaders[shader];
  }

  @Override
  public void dispose() {
    for (int i = 0; i < shaders.length; i++) {
      if (shaders[i] != null) {
        shaders[i].dispose();
        shaders[i] = null;
      }
    }
  }

  protected static class CubesShader extends DefaultShader {

    protected int u_sunlight;
    protected int u_lightoverride;
    private boolean lightoverride = false;

    CubesShader(Renderable renderable, String prefix) {
      super(renderable, getConfig(), prefix);
    }

    CubesShader(Renderable renderable) {
      this(renderable, "");
    }

    private static Config getConfig() {
      Config config = new Config(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);
      config.numDirectionalLights = config.numPointLights = config.numSpotLights = config.numBones = 0;
      return config;
    }

    @Override
    public void init() {
      super.init();
      u_sunlight = program.fetchUniformLocation("u_sunlight", true);
      u_lightoverride = program.fetchUniformLocation("u_lightoverride", true);
    }

    @Override
    public void render(Renderable renderable) {
      if (renderable.userData instanceof RenderingSettings && ((RenderingSettings) renderable.userData).lightOverride != -1) {
        program.setUniformf(u_lightoverride, ((RenderingSettings) renderable.userData).lightOverride);
        lightoverride = true;
      } else if (lightoverride) {
        program.setUniformf(u_lightoverride, -1f);
        lightoverride = false;
      }
      super.render(renderable);
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
      super.begin(camera, context);
      if (Cubes.getClient() == null) {
        program.setUniformf(u_sunlight, 1f);
      } else {
        program.setUniformf(u_sunlight, Cubes.getClient().world.getWorldSunlight());
      }
      program.setUniformf(u_lightoverride, -1f);
    }

    @Override
    public int compareTo(Shader other) {
      return other == null ? -1 : 0;
    }

    @Override
    public boolean canRender(Renderable instance) {
      return true;
    }
  }

  public static class FeatureShader extends CubesShader {

    private final List<Feature> features;

    FeatureShader(Renderable renderable, List<Feature> features) {
      super(renderable, makePrefix(features));
      this.features = features;
    }

    @Override
    public void init() {
      super.init();
      for (Feature feature : features) {
        feature.init(program);
      }
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
      super.begin(camera, context);
      for (Feature feature : features) {
        feature.begin(program, camera, context);
      }
    }

    private static String makePrefix(List<Feature> features) {
      StringBuilder prefix = new StringBuilder();
      for (Feature feature : features) {
        prefix.append(feature.prefix());
      }
      return prefix.toString();
    }
  }

  private interface Feature {

    String prefix();

    void init(ShaderProgram program);

    void begin(ShaderProgram program, Camera camera, RenderContext context);
  }

  protected static class FogFeature implements Feature {

    private int u_cameraposition;
    private int u_skycolor;
    private int u_fogdistance;
    private int u_minfogdistance;

    @Override
    public String prefix() {
      return "#define feature_fog\n";
    }

    @Override
    public void init(ShaderProgram program) {
      u_cameraposition = program.fetchUniformLocation("u_cameraposition", true);
      u_skycolor = program.fetchUniformLocation("u_skycolor", true);
      u_fogdistance = program.fetchUniformLocation("u_fogdistance", true);
      u_minfogdistance = program.fetchUniformLocation("u_minfogdistance", true);
    }

    @Override
    public void begin(ShaderProgram program, Camera camera, RenderContext context) {
      WorldClient worldClient = ((WorldClient) Cubes.getClient().world);
      float distance = Settings.getIntegerSettingValue(Settings.GRAPHICS_VIEW_DISTANCE) * Area.SIZE_BLOCKS;
      float fogDistance = MathUtils.clamp(distance * 0.1f, 8f, 16f);

      program.setUniformf(u_cameraposition, Cubes.getClient().player.position);
      program.setUniformf(u_skycolor, worldClient.getSkyColour());
      program.setUniformf(u_fogdistance, fogDistance);
      program.setUniformf(u_minfogdistance, distance - fogDistance);
    }
  }

  protected static class AmbientOcclusionFeature implements Feature {

    private int u_aoTexture;
    private int ao_unit;
    private int u_aoUVTransform;

    @Override
    public String prefix() {
      return "#define feature_ao\n";
    }

    @Override
    public void init(ShaderProgram program) {
      u_aoTexture = program.fetchUniformLocation("u_aoTexture", false);
      u_aoUVTransform = program.fetchUniformLocation("u_aoUVTransform", false);
    }

    @Override
    public void begin(ShaderProgram program, Camera camera, RenderContext context) {
      TextureAttribute textureAttribute = AmbientOcclusion.getTextureAttribute();

      ao_unit = context.textureBinder.bind(textureAttribute.textureDescription);
      program.setUniformi(u_aoTexture, ao_unit);

      program.setUniformf(u_aoUVTransform, textureAttribute.offsetU, textureAttribute.offsetV, textureAttribute.scaleU, textureAttribute.scaleV);
    }
  }
}
