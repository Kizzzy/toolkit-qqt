package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.DisplayLoaderAttribute;
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
                
                Track.StaticFrame sf = new Track.StaticFrame();
                sf.x = offsetX;
                sf.y = offsetY;
                sf.width = frame.width;
                sf.height = frame.height;
                sf.image = image;
                sf.extra = String.format("%02d/%02d", i + 1, img.count);
                
                track.sfs.add(sf);
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
