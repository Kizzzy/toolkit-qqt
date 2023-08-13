package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.DisplayLoaderAttribute;
import cn.kizzzy.javafx.display.image.Frame;
import cn.kizzzy.javafx.display.image.ImageArg;
import cn.kizzzy.javafx.display.image.ImageDisplayLoader;
import cn.kizzzy.javafx.display.image.Track;
import cn.kizzzy.qqt.ImgFile;
import cn.kizzzy.qqt.helper.QqtImgHelper;
import cn.kizzzy.vfs.IPackage;
import cn.kizzzy.vfs.tree.Leaf;

import java.awt.image.BufferedImage;

@DisplayLoaderAttribute(suffix = {
    "img",
})
public class ImgDisplay implements ImageDisplayLoader {
    
    @Override
    public ImageArg loadImage(IPackage vfs, Leaf leaf) throws Exception {
        ImgFile img = vfs.load(leaf.path, ImgFile.class);
        if (img == null) {
            return null;
        }
        
        Track track = new Track();
        int i = 0;
        for (ImgFile.Frame frame : img.frames) {
            BufferedImage image = QqtImgHelper.toImage(frame);
            if (image != null) {
                float offsetX = -img.maxWidth / 2f - img.offsetX + frame.offsetX;
                float offsetY = -img.maxHeight - img.offsetY + frame.offsetY + 20;
                
                Frame f = new Frame();
                f.x = offsetX;
                f.y = offsetY;
                f.width = frame.width;
                f.height = frame.height;
                f.image = image;
                f.time = 167;
                f.extra = String.format("%02d/%02d", i + 1, img.count);
                
                track.frames.add(f);
            }
            i++;
        }
        
        ImageArg arg = new ImageArg();
        arg.borderX = arg.pivotX - img.maxWidth / 2f;
        arg.borderY = arg.pivotY - 100;
        arg.borderW = 100;
        arg.borderH = 100;
        arg.tracks.add(track);
        return arg;
    }
}
