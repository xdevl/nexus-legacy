/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xdevl.wallpaper.nexus;

import static android.renderscript.ProgramStore.DepthFunc.ALWAYS;
import com.xdevl.wallpaper.nexus.ScriptC_nexus;
import com.xdevl.wallpaper.RenderScriptScene;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.renderscript.*;
import android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

class NexusRS extends RenderScriptScene
{
	
	private static final int MAX_COLORS=16 ;
	private static enum Background
    {
    	ORIGINAL(R.drawable.original_background),ALTERNATIVE(R.drawable.alternative_background) ;
    	
    	int resId ;
    	
    	Background(int resId)
    	{
    		this.resId=resId ;
    	}
    }
	
    private final BitmapFactory.Options mOptionsARGB = new BitmapFactory.Options();

    private ProgramVertexFixedFunction.Constants mPvOrthoAlloc;

    private int mInitialWidth;
    private int mInitialHeight;
    private float mWorldScaleX;
    private float mWorldScaleY;
    private float mXOffset;
    private Background mBackground=null ;
    private ScriptC_nexus mScript;

    public NexusRS(int width, int height) {
        super(width, height);

        mInitialWidth = width;
        mInitialHeight = height;
        mWorldScaleX = 1.0f;
        mWorldScaleY = 1.0f;

        mOptionsARGB.inScaled = false;
        mOptionsARGB.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        mXOffset = xOffset;
        mScript.set_gXOffset(xOffset);
    }

    public void update(Context context)
    {
    	SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(context) ;
    	Background background=Background.valueOf(sharedPreferences.getString(context.getString(R.string.key_background),context.getString(R.string.background_value_original))) ;
    	if(mBackground==null || mBackground==background)
    	{
	    	Allocation oldBackground=mScript.get_gTBackground() ;
	    	mScript.set_gTBackground(loadTexture(background.resId)) ;
	    	if(oldBackground!=null)
	    		oldBackground.destroy() ;
    	}
    	
    	Set<String> colors=sharedPreferences.getStringSet(context.getString(R.string.key_colors),
    			new HashSet<String>(Arrays.asList(context.getResources().getStringArray(R.array.default_colors)))) ;
    	float colorValues[]=new float[MAX_COLORS*3] ;
    	int i=0 ;
    	for(String colorLiteral: colors)
    	{
    		int color=Color.parseColor(colorLiteral) ;
    		colorValues[i++]=Color.red(color)/255f ;
    		colorValues[i++]=Color.green(color)/255f ;
    		colorValues[i++]=Color.blue(color)/255f ;
    	}

    	mScript.set_gColors(colorValues);
    	mScript.set_gColorNumber(colors.size());
    }
    
    @Override
    public void start(Context context) {
    	update(context) ;
        super.start(context);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height); // updates mWidth, mHeight

        // android.util.Log.d("NexusRS", String.format("resize(%d, %d)", width, height));

        mWorldScaleX = (float)mInitialWidth / width;
        mWorldScaleY = (float)mInitialHeight / height;
        mScript.set_gWorldScaleX(mWorldScaleX);
        mScript.set_gWorldScaleY(mWorldScaleY);
    }
    
    @Override
    protected ScriptC createScript() {
        mScript = new ScriptC_nexus(mRS, mResources, R.raw.nexus);

        
        createProgramFragmentStore();
        createProgramFragment();
        createProgramVertex();
        createState();

        update(mRS.getApplicationContext());
        mScript.set_gTPulse(loadTextureARGB(R.drawable.pulse));
        mScript.set_gTGlow(loadTextureARGB(R.drawable.glow));
        mScript.setTimeZone(TimeZone.getDefault().getID());
        mScript.invoke_initPulses();
        return mScript;
    }

    private void createState() {
        int mode;
        try {
            mode = mResources.getInteger(R.integer.nexus_mode);
        } catch (Resources.NotFoundException exc) {
            mode = 0; // standard nexus mode
        }

        mScript.set_gIsPreview(isPreview() ? 1 : 0);
        mScript.set_gMode(mode);
        mScript.set_gXOffset(0.f);
        mScript.set_gWorldScaleX(mWorldScaleX);
        mScript.set_gWorldScaleY(mWorldScaleY);
    }

    private Allocation loadTexture(int id) {
        return Allocation.createFromBitmapResource(mRS, mResources, id,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
    }

    private Allocation loadTextureARGB(int id) {
        Bitmap b = BitmapFactory.decodeResource(mResources, id, mOptionsARGB);
        return Allocation.createFromBitmap(mRS, b,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
    }


    private void createProgramFragment() {
        // sampler and program fragment for pulses
        ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.MODULATE,
                           ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
        ProgramFragment pft = builder.create();
        pft.bindSampler(Sampler.WRAP_LINEAR(mRS), 0);
        mScript.set_gPFTexture(pft);

        // sampler and program fragment for background image
        builder = new ProgramFragmentFixedFunction.Builder(mRS);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.MODULATE,
                           ProgramFragmentFixedFunction.Builder.Format.RGB, 0);
        ProgramFragment pft565 = builder.create();
        pft565.bindSampler(Sampler.CLAMP_NEAREST(mRS), 0);
        mScript.set_gPFTexture565(pft565);
    }

    private void createProgramFragmentStore() {
        ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
        builder.setDepthFunc(ALWAYS);
        builder.setBlendFunc(BlendSrcFunc.ONE, BlendDstFunc.ONE);
        builder.setDitherEnabled(false);
        ProgramStore solid = builder.create();
        mRS.bindProgramStore(solid);

        builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA, BlendDstFunc.ONE);
        mScript.set_gPSBlend(builder.create());
    }

    private void createProgramVertex() {
        mPvOrthoAlloc = new ProgramVertexFixedFunction.Constants(mRS);
        Matrix4f proj = new Matrix4f();
        proj.loadOrthoWindow(mWidth, mHeight);
        mPvOrthoAlloc.setProjection(proj);

        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(mRS);
        pvb.setTextureMatrixEnable(true);
        ProgramVertex pv = pvb.create();
        ((ProgramVertexFixedFunction)pv).bindConstants(mPvOrthoAlloc);
        mRS.bindProgramVertex(pv);
    }

    @Override
    public Bundle onCommand(String action, int x, int y, int z, Bundle extras,
            boolean resultRequested) {

        if (mWidth < mHeight) {
            // nexus.rs ignores the xOffset when rotated; we shall endeavor to do so as well
            x = (int) (x + mXOffset * mWidth / mWorldScaleX);
        }

        // android.util.Log.d("NexusRS", String.format(
        //     "dw=%d, bw=%d, xOffset=%g, x=%d",
        //     dw, bw, mWorldState.xOffset, x));

        if (WallpaperManager.COMMAND_TAP.equals(action)
                || WallpaperManager.COMMAND_SECONDARY_TAP.equals(action)
                || WallpaperManager.COMMAND_DROP.equals(action)) {
            mScript.invoke_addTap(x, y);
        }
        return null;
    }
}
