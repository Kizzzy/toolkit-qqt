package cn.kizzzy.display;

import cn.kizzzy.qqt.QqtImgItem;

import java.awt.image.BufferedImage;

public class Display {
    protected final String path;
    protected final DisplayContext context;
    protected boolean stopFlag;
    
    public Display() {
        this(null, null);
    }
    
    public Display(DisplayContext context, String path) {
        this.path = path;
        this.context = context;
    }
    
    public void init() {
    
    }
    
    public void prev() {
    
    }
    
    public void next() {
    
    }
    
    public void play() {
    
    }
    
    public void select(int layer) {
    
    }
    
    public void stop() {
        stopFlag = true;
    }
    
    protected int getLayoutX(QqtImgItem imageInfo) {
        switch (context.provideIndex()) {
            default:
                return 200;
        }
    }
    
    protected int getLayoutY(QqtImgItem imageInfo) {
        switch (context.provideIndex()) {
            default:
                return 200;
        }
    }
    
    protected String retrieveImageType(int type) {
        switch (type) {
            case 3:
            case 123456:
                return "ARGB0565";
            case 8:
                return "ARGB8888";
            default:
                return "Unknown";
        }
    }
    
    protected void wrapperImage(BufferedImage image) {
        if (context.isFilterColor()) {
            if (((image.getRGB(0, image.getHeight() - 1) >> 24) & 0xff) != 0) {
                for (int i = 0; i < image.getHeight(); ++i) {
                    for (int j = 0; j < image.getWidth(); ++j) {
                        int b1 = (image.getRGB(j, i)) & 0xff;
                        int g1 = (image.getRGB(j, i) >> 8) & 0xff;
                        int r1 = (image.getRGB(j, i) >> 16) & 0xff;
                        int a1 = Math.max(r1, Math.max(g1, b1));
                        
                        if (a1 != 0) {
                            r1 = r1 * 255 / a1;
                            g1 = g1 * 255 / a1;
                            b1 = b1 * 255 / a1;
                        }
                        
                        image.setRGB(j, i, (a1 << 24) | (r1 << 16) | (g1 << 8) | b1);
                    }
                }
            }
        }
    }
    
    protected void combineColor(BufferedImage image, int[] argb) {
        for (int i = 0; i < image.getHeight(); ++i) {
            for (int j = 0; j < image.getWidth(); ++j) {
                int b1 = (image.getRGB(j, i)) & 0xff;
                int g1 = (image.getRGB(j, i) >> 8) & 0xff;
                int r1 = (image.getRGB(j, i) >> 16) & 0xff;
                int a1 = (image.getRGB(j, i) >> 24) & 0xff;
                
                a1 = (int) ((a1 * argb[3]) / 255f);
                r1 = (int) ((r1 * argb[2]) / 255f);
                g1 = (int) ((g1 * argb[1]) / 255f);
                b1 = (int) ((b1 * argb[0]) / 255f);
                
                image.setRGB(j, i, (a1 << 24) | (r1 << 16) | (g1 << 8) | b1);
            }
        }
    }
}

