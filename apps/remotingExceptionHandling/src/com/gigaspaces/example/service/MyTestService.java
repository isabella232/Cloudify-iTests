package com.gigaspaces.example.service;

import com.gigaspaces.example.IMyTestService;
import com.gigaspaces.example.dto.MySimpleEntry;
import org.openspaces.core.GigaSpace;
import org.springframework.transaction.annotation.Transactional;

public class MyTestService implements IMyTestService {

    private int entryId = 0;

    private GigaSpace gigaSpace;

    @Transactional
    public void throwJiniException() throws Exception {
        MySimpleEntry entry = new MySimpleEntry();
        entry.setId(1111);
        gigaSpace.write(entry);
    }

    public void setGigaSpace(GigaSpace space) {
        gigaSpace = space;
    }
}
