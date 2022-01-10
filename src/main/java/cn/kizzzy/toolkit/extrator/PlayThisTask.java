package cn.kizzzy.toolkit.extrator;

import cn.kizzzy.helper.LogHelper;
import cn.kizzzy.qqt.display.Display;

import java.util.List;

public class PlayThisTask implements Runnable {
    private int idx;
    private boolean playThis;
    private final List<Display> displays;
    
    public PlayThisTask(List<Display> displays) {
        this.displays = displays;
        this.playThis = true;
    }
    
    @Override
    public void run() {
        idx = 0;
        Display display = displays.get(idx);
        while (playThis) {
            try {
                display.play();
                Thread.sleep(125);
            } catch (InterruptedException e) {
                LogHelper.error(null, e);
            }
        }
    }
    
    public void stop() {
        playThis = false;
    }
}
