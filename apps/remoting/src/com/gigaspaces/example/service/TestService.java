package com.gigaspaces.example.service;

import java.util.Collection;

import org.openspaces.core.GigaSpace;

import com.gigaspaces.example.IParameter;
import com.gigaspaces.example.IResult;
import com.gigaspaces.example.ITestService;
import com.gigaspaces.example.dto.SimpleEntry;
import com.gigaspaces.example.dto.SimpleResult;

public class TestService implements ITestService {
    
    private int entryId = 0;
    
    private GigaSpace gigaSpace;

    public IResult add(Collection<IParameter> parameters) {

        SimpleResult result = new SimpleResult();
        for (IParameter parameter : parameters) {
            result.setCode(result.getCode() + 1);

            if( result.getDescription() != null ) {
                result.setDescription(result.getDescription() + " " + parameter.getValue());
            } else {
                result.setDescription(parameter.getValue());
            }
        }

        SimpleEntry entry = new SimpleEntry();
        entry.setId(entryId++);
        entry.setValue(result.getDescription());
        gigaSpace.write(entry);

        return result;
    }

    
    public IResult add(IParameter [] parameters) {

        SimpleResult result = new SimpleResult();
        for (IParameter parameter : parameters) {
            result.setCode(result.getCode() + 2);

            if( result.getDescription() != null ) {
                result.setDescription(result.getDescription() + " " + parameter.getValue());
            } else {
                result.setDescription(parameter.getValue());
            }
        }

        SimpleEntry entry = new SimpleEntry();
        entry.setId(entryId++);
        entry.setValue(result.getDescription());
        gigaSpace.write(entry);
        return result;
    }

    public IResult add(IParameter parameter) {
        SimpleResult result = new SimpleResult();
        result.setCode(entryId);
        result.setDescription(parameter.getValue());

        SimpleEntry entry = new SimpleEntry();
        entry.setId(entryId++);
        entry.setValue(result.getDescription());
        gigaSpace.write(entry);
        return result;
    }
    
    public IResult modify( Collection<IParameter> parameters, IParameter parameter2 ) {
        SimpleResult result = new SimpleResult();
        result.setCode(entryId);
        result.setDescription(parameter2.getValue());

        for (IParameter parameter : parameters) {
            result.setCode(result.getCode() + 1);

            if( result.getDescription() != null ) {
                result.setDescription(result.getDescription() + " " + parameter.getValue());
            } else {
                result.setDescription(parameter.getValue());
            }
        }
        result.setCode(result.getCode() + 2);

        SimpleEntry entry = new SimpleEntry();
        entry.setId(entryId++);
        entry.setValue(result.getDescription());
        gigaSpace.write(entry);

        return result;
    }

    public IResult modify( IParameter parameter1, IParameter parameter2 ) {
        SimpleResult result = new SimpleResult();
        result.setCode(entryId);
        result.setDescription(parameter1.getValue() + parameter2.getValue());

        SimpleEntry entry = new SimpleEntry();
        entry.setId(entryId++);
        entry.setValue(result.getDescription());
        gigaSpace.write(entry);

        return result;
    }

    public IResult modify( IParameter parameter2, Collection<IParameter> parameters ) {
        SimpleResult result = new SimpleResult();
        result.setCode(entryId);
        result.setDescription(parameter2.getValue());

        for (IParameter parameter : parameters) {
            result.setCode(result.getCode() + 1);

            if( result.getDescription() != null ) {
                result.setDescription(result.getDescription() + " " + parameter.getValue());
            } else {
                result.setDescription(parameter.getValue());
            }
        }
        result.setCode(result.getCode() + 8);

        SimpleEntry entry = new SimpleEntry();
        entry.setId(entryId++);
        entry.setValue(result.getDescription());
        gigaSpace.write(entry);

        return result;
    }


    public void setGigaSpace(GigaSpace space) {
        gigaSpace = space;
    }
}
