package cn.kizzzy.qqt.display;

import cn.kizzzy.event.IEventSource;

public interface DisplayContext extends IEventSource {
    
    int provideIndex();
    
    boolean isFilterColor();
    
    <T> T load(String path, Class<T> clazz);
}
