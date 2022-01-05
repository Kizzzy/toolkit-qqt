package cn.kizzzy.display;

import cn.kizzzy.helper.LogHelper;
import javafx.application.Platform;

public class DisplayTask extends Thread {
    private Display display;
    private boolean play;
    
    public void startImpl() {
        play = true;
        this.start();
    }
    
    public void stopImpl() {
        play = false;
    }
    
    public Display getDisplay() {
        return display;
    }
    
    public void setDisplay(Display display) {
        this.display = display;
    }
    
    @Override
    public void run() {
        while (play) {
            try {
                if (display != null) {
                    Platform.runLater(display::play);
                }
                Thread.sleep(125);
            } catch (InterruptedException e) {
                LogHelper.error(null, e);
            }
        }
    }
}
