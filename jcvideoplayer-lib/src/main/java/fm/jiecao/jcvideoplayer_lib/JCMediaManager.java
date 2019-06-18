package fm.jiecao.jcvideoplayer_lib;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;


import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * <p>统一管理MediaPlayer的地方,只有一个mediaPlayer实例，那么不会有多个视频同时播放，也节省资源。</p>
 * <p>Unified management MediaPlayer place, there is only one MediaPlayer instance, then there will be no more video broadcast at the same time, also save resources.</p>
 * Created by Nathen
 * On 2015/11/30 15:39
 */
public class JCMediaManager implements TextureView.SurfaceTextureListener, IjkMediaPlayer.OnPreparedListener, IjkMediaPlayer.OnCompletionListener, IjkMediaPlayer.OnBufferingUpdateListener, IjkMediaPlayer.OnSeekCompleteListener, IjkMediaPlayer.OnErrorListener, IjkMediaPlayer.OnInfoListener, IjkMediaPlayer.OnVideoSizeChangedListener {
    public static String TAG = "JieCaoVideoPlayer";

    private static JCMediaManager JCMediaManager;
    public static JCResizeTextureView textureView;
    public static SurfaceTexture savedSurfaceTexture;
    public IjkMediaPlayer mediaPlayer = new IjkMediaPlayer();
    public static String CURRENT_PLAYING_URL;
    public static boolean CURRENT_PLING_LOOP;
    public static Map<String, String> MAP_HEADER_DATA;
    public int currentVideoWidth = 0;
    public int currentVideoHeight = 0;

    public static final int HANDLER_PREPARE = 0;
    public static final int HANDLER_RELEASE = 2;
    HandlerThread mMediaHandlerThread;
    MediaHandler mMediaHandler;
    Handler mainThreadHandler;

    public static JCMediaManager instance() {
        if (JCMediaManager == null) {
            JCMediaManager = new JCMediaManager();
        }
        return JCMediaManager;
    }


    public JCMediaManager() {
        mMediaHandlerThread = new HandlerThread(TAG);
        mMediaHandlerThread.start();
        mMediaHandler = new MediaHandler((mMediaHandlerThread.getLooper()));
        mainThreadHandler = new Handler();

    }

    public Point getVideoSize() {
        if (currentVideoWidth != 0 && currentVideoHeight != 0) {
            return new Point(currentVideoWidth, currentVideoHeight);
        } else {
            return null;
        }
    }

    public class MediaHandler extends Handler {
        public MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    try {
                        currentVideoWidth = 0;
                        currentVideoHeight = 0;
                        mediaPlayer.release();
                        mediaPlayer = new IjkMediaPlayer();
                        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
                        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
                        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
                        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
                        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
                        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
                        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
                        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 1024 * 1024);
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setDataSource(CURRENT_PLAYING_URL, MAP_HEADER_DATA);
                        mediaPlayer.setLooping(CURRENT_PLING_LOOP);
                        mediaPlayer.setOnPreparedListener(JCMediaManager.this);
                        mediaPlayer.setOnCompletionListener(JCMediaManager.this);
                        mediaPlayer.setOnBufferingUpdateListener(JCMediaManager.this);
                        mediaPlayer.setScreenOnWhilePlaying(true);
                        mediaPlayer.setOnSeekCompleteListener(JCMediaManager.this);
                        mediaPlayer.setOnErrorListener(JCMediaManager.this);
                        mediaPlayer.setOnInfoListener(JCMediaManager.this);
                        mediaPlayer.setOnVideoSizeChangedListener(JCMediaManager.this);
                        mediaPlayer.prepareAsync();
                        mediaPlayer.setSurface(new Surface(savedSurfaceTexture));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case HANDLER_RELEASE:
                    mediaPlayer.release();
                    break;
            }
        }
    }

    public void prepare() {
        releaseMediaPlayer();
        Message msg = new Message();
        msg.what = HANDLER_PREPARE;
        mMediaHandler.sendMessage(msg);
    }

    public void releaseMediaPlayer() {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE;
        mMediaHandler.sendMessage(msg);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.i(TAG, "onSurfaceTextureAvailable [" + this.hashCode() + "] ");
        if (savedSurfaceTexture == null) {
            savedSurfaceTexture = surfaceTexture;
            prepare();
        } else {
            textureView.setSurfaceTexture(savedSurfaceTexture);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        // 如果SurfaceTexture还没有更新Image，则记录SizeChanged事件，否则忽略
        Log.i(TAG, "onSurfaceTextureSizeChanged [" + this.hashCode() + "] ");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return savedSurfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mediaPlayer.start();
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getCurrentJcvd() != null) {
                    JCVideoPlayerManager.getCurrentJcvd().onPrepared();
                }
            }
        });
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getCurrentJcvd() != null) {
                    JCVideoPlayerManager.getCurrentJcvd().onAutoCompletion();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, final int percent) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getCurrentJcvd() != null) {
                    JCVideoPlayerManager.getCurrentJcvd().setBufferProgress(percent);
                }
            }
        });
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getCurrentJcvd() != null) {
                    JCVideoPlayerManager.getCurrentJcvd().onSeekComplete();
                }
            }
        });
    }

    @Override
    public boolean onError(IMediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getCurrentJcvd() != null) {
                    JCVideoPlayerManager.getCurrentJcvd().onError(what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getCurrentJcvd() != null) {
                    JCVideoPlayerManager.getCurrentJcvd().onInfo(what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num,
                                   int sar_den) {
        currentVideoWidth = width;
        currentVideoHeight = height;
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.getCurrentJcvd() != null) {
                    JCVideoPlayerManager.getCurrentJcvd().onVideoSizeChanged();
                }
            }
        });
    }


}
