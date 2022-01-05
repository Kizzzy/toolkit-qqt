package cn.kizzzy.display;

import cn.kizzzy.event.IEventSource;
import cn.kizzzy.qqt.QqtFile;

public interface DisplayContext extends IEventSource {
    
    int provideIndex();
    
    boolean isFilterColor();
    
    QqtFile retrievePkgSubFile(String path);
    
    <T> T load(String path, Class<T> clazz);
}
