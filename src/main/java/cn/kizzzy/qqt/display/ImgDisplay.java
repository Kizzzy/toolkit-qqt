package cn.kizzzy.qqt.display;

import cn.kizzzy.javafx.display.DisplayLoaderAttribute;
import cn.kizzzy.javafx.display.image.Frame;
import cn.kizzzy.javafx.display.image.Track;
import cn.kizzzy.javafx.display.image.ImageArg;
import cn.kizzzy.javafx.display.image.ImageDisplayLoader;
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
        for (ImgFile.Frame item : img.frames) {
            BufferedImage image = QqtImgHelper.toImage(item);
            if (image != null) {
                float offsetX = -img.maxWidth / 2f - img.offsetX + item.offsetX;
                float offsetY = -img.maxHeight - img.offsetY + item.offsetY + 20;
                
                Frame frame = new Frame();
                frame.x = offsetX;
                frame.y = offsetY;
                frame.width = item.width;
                frame.height = item.height;
                frame.image = image;
                frame.time = 167;
                frame.extra = String.format("%02d/%02d", i + 1, img.count);
                
                track.frames.add(frame);
            }
            i++;
        }
        
        ImageArg arg = new ImageArg();
        arg.tracks.add(track);
        return arg;
    }
}
